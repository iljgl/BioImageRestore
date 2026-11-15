package org.DenoiseStorm.deconvolution;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.DenoiseStorm.deconvolution.core.DeconvolutionCore;
import org.DenoiseStorm.deconvolution.core.PSFGenerator;
import org.DenoiseStorm.help.HelpBrowser;
import org.DenoiseStorm.help.HelpPages;
import org.DenoiseStorm.help.DeconvolutionHelpPages;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * ImageJ plugin for iterative deconvolution with a ThunderSTORM-style
 * Swing parameter dialog.
 *
 * <p>The dialog contains three logical groups:
 * <ul>
 *   <li>recA: Optical parameters used to generate the theoretical PSF</li>
 *   <li>recB: Choice of deconvolution algorithm</li>
 *   <li>recC: Optional output of the computed PSF image</li>
 * </ul>
 */
public class Deconvolution_Plugin implements PlugIn {

    private static final double Z_DEFOCUS = 0.0;    // focal plane
    private static final double R_CUTOFF = 2.0;     // radial cutoff in micrometers

    private static final String ALGO_RL = "Richardson-Lucy (Poisson)";
    private static final String ALGO_LW = "Accelerated Landweber (Gaussian)";

    private static final HelpPages HELP = new DeconvolutionHelpPages();
    private static final Color HELP_BUTTON_BLUE = new Color(0, 102, 204);
    private static final Color GROUP_BORDER_COLOR = new Color(128, 128, 128, 150);

    /** Holder for dialog results; null if the user cancels. */
    private static class Parameters {
        double exWavelength;
        double numericalAperture;
        double pixelSize;
        int iterations;
        int algorithmRule;      // 1 = RL (Poisson), 2 = LW (Gaussian)
        boolean showPsf;
    }

    @Override
    public void run(String arg) {
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            IJ.noImage();
            return;
        }

        Parameters parameters = showSwingDialog(imp);
        if (parameters == null) {
            return;
        }

        // --- Generate PSF (and display it if requested) ---
        IJ.showStatus("Generating PSF...");
        double[][] psf = PSFGenerator.generate(
                parameters.pixelSize,
                parameters.exWavelength,
                parameters.numericalAperture,
                Z_DEFOCUS,
                R_CUTOFF);

        if (parameters.showPsf) {
            displayPSF(psf, parameters.exWavelength,
                    parameters.numericalAperture, parameters.pixelSize);
        }

        // --- Check for time series ---
        int numFrames = imp.getStackSize();
        boolean processAllFrames = false;

        if (numFrames > 1) {
            int answer = JOptionPane.showConfirmDialog(
                    null,
                    "This is a time-series image with " + numFrames + " frames.\n"
                            + "Process all frames?",
                    "Process Frames",
                    JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.CLOSED_OPTION) {
                return;
            }
            processAllFrames = (answer == JOptionPane.YES_OPTION);
        }

        String suffix = (parameters.algorithmRule == 1)
                ? "_DeconvPoisson" : "_DeconvGaussian";

        ImagePlus resultImp;
        if (processAllFrames) {
            resultImp = processAllFrames(
                    imp,
                    psf,
                    parameters.iterations,
                    parameters.algorithmRule,
                    suffix);
        } else {
            resultImp = processSingleFrame(
                    imp,
                    psf,
                    parameters.iterations,
                    parameters.algorithmRule,
                    suffix);
        }

        resultImp.show();
        IJ.showStatus("Deconvolution Complete");
    }

    // ------------------------------------------------------------------
    //  Swing parameter dialog
    // ------------------------------------------------------------------

    private Parameters showSwingDialog(ImagePlus imp) {
        JDialog dialog = new JDialog((Frame) null, "Deconvolution Parameters", true);
        try {
            dialog.setIconImage(IJ.getInstance().getIconImage());
        } catch (Exception ignored) {
            // Keep the default icon if ImageJ's icon is unavailable.
        }

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField exWavelengthField = new JTextField("0.561", 8);
        JTextField numericalApertureField = new JTextField("1.49", 8);
        JTextField pixelSizeField = new JTextField("0.0433", 8);
        JTextField iterationsField = new JTextField("10", 8);

        JRadioButton rlRadio = new JRadioButton(ALGO_RL, true);
        JRadioButton lwRadio = new JRadioButton(ALGO_LW, false);
        ButtonGroup algorithmGroup = new ButtonGroup();
        algorithmGroup.add(rlRadio);
        algorithmGroup.add(lwRadio);

        JCheckBox showPsfCheckBox = new JCheckBox("Output theoretical PSF image");

        // recA: Optical parameters
        JPanel opticalPanel = createGroupPanel("Optical parameters");
        addLabeledFieldRow(opticalPanel, 0, "Excitation wavelength (µm)",
                exWavelengthField, "exWavelength", dialog);
        addLabeledFieldRow(opticalPanel, 1, "Numerical aperture (NA)",
                numericalApertureField, "numericalAperture", dialog);
        addLabeledFieldRow(opticalPanel, 2, "Pixel size (µm)",
                pixelSizeField, "pixelSize", dialog);
        addLabeledFieldRow(opticalPanel, 3, "Iterations",
                iterationsField, "iterations", dialog);

        // recB: Algorithm selection
        JPanel algorithmPanel = createGroupPanel("Algorithm selection");
        addRadioButtonRowWithHelp(algorithmPanel, 0, rlRadio,
                "richardsonLucy", dialog);
        addRadioButtonRowWithHelp(algorithmPanel, 1, lwRadio,
                "acceleratedLandweber", dialog);

        // recC: Output options
        JPanel outputPanel = createGroupPanel("Output options");
        addCheckBoxRow(outputPanel, 0, showPsfCheckBox,
                "showPsf", dialog);

        opticalPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        algorithmPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        outputPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        root.add(opticalPanel);
        root.add(Box.createVerticalStrut(6));
        root.add(algorithmPanel);
        root.add(Box.createVerticalStrut(6));
        root.add(outputPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        root.add(buttonPanel);

        final Parameters[] result = new Parameters[1];

        okButton.addActionListener(e -> {
            Parameters p = new Parameters();
            p.exWavelength = parseDouble(exWavelengthField, 0.561);
            p.numericalAperture = parseDouble(numericalApertureField, 1.49);
            p.pixelSize = parseDouble(pixelSizeField, 0.0433);
            p.iterations = Math.max(1, (int) Math.round(
                    parseDouble(iterationsField, 10.0)));
            p.algorithmRule = rlRadio.isSelected() ? 1 : 2;
            p.showPsf = showPsfCheckBox.isSelected();
            result[0] = p;
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.getContentPane().add(root);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return result[0];
    }

    /** Creates a group panel with a subtle gray border and title. */
    private JPanel createGroupPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(GROUP_BORDER_COLOR, 1, true),
                title,
                TitledBorder.LEFT,
                TitledBorder.ABOVE_TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12),
                new Color(70, 70, 70)));
        return panel;
    }

    /** Adds one row: label, field, and blue help button. */
    private void addLabeledFieldRow(JPanel panel, int row, String labelText,
                                    JComponent field, String topic, JDialog dialog) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.NONE;
        g.gridy = row;

        g.gridx = 0;
        panel.add(new JLabel(labelText), g);

        g.gridx = 1;
        panel.add(field, g);

        g.gridx = 2;
        panel.add(makeHelpButton(topic, dialog), g);
    }

    /** Adds a radio button row with its own help button. */
    private void addRadioButtonRowWithHelp(JPanel panel, int row,
                                           JRadioButton radioButton,
                                           String topic, JDialog dialog) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.NONE;
        g.gridy = row;

        g.gridx = 0;
        g.gridwidth = 2;   // radio button spans label and field columns
        panel.add(radioButton, g);

        g.gridx = 2;
        g.gridwidth = 1;
        panel.add(makeHelpButton(topic, dialog), g);
    }

    /** Adds one checkbox row with its own help button. */
    private void addCheckBoxRow(JPanel panel, int row, JCheckBox checkBox,
                                String topic, JDialog dialog) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 4, 2, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.NONE;
        g.gridy = row;

        g.gridx = 0;
        g.gridwidth = 2;
        panel.add(checkBox, g);

        g.gridx = 2;
        g.gridwidth = 1;
        panel.add(makeHelpButton(topic, dialog), g);
    }

    /** ThunderSTORM-style small blue "?" button. */
    private JButton makeHelpButton(String topic, JDialog dialog) {
        JButton button = new JButton("?");
        button.setForeground(Color.BLACK);
        button.setBackground(HELP_BUTTON_BLUE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        button.setMargin(new Insets(2, 8, 2, 8));
        button.addActionListener((ActionEvent e) ->
                HelpBrowser.show(dialog, HELP, topic));
        return button;
    }

    private static double parseDouble(JTextField field, double defaultValue) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ------------------------------------------------------------------
    //  Algorithm pipeline (unchanged except renamed variables)
    // ------------------------------------------------------------------

    private ImagePlus processSingleFrame(ImagePlus imp, double[][] psf,
                                         int iteration, int rule, String suffix) {
        ImageProcessor ip = imp.getProcessor();
        int w = ip.getWidth();
        int h = ip.getHeight();

        double[][] img = loadImage(ip, w, h);
        double recordMax = getMax(img);

        IJ.showStatus("Running Deconvolution...");
        IJ.showProgress(0);
        double[][] deconv = DeconvolutionCore.iterativeDeblur(img, psf, iteration, rule);
        IJ.showProgress(1);

        double[][] normalized = normalizeToRange(deconv, recordMax);
        return createResultImage(normalized, imp.getTitle(), w, h, suffix);
    }

    private ImagePlus processAllFrames(ImagePlus imp, double[][] psf,
                                       int iteration, int rule, String suffix) {
        int w = imp.getWidth();
        int h = imp.getHeight();
        int numFrames = imp.getStackSize();
        ImageStack stack = imp.getStack();
        ImageStack resultStack = new ImageStack(w, h);

        for (int i = 1; i <= numFrames; i++) {
            IJ.showStatus("Deconvolving frame " + i + " of " + numFrames);
            IJ.showProgress(i, numFrames);

            ImageProcessor ip = stack.getProcessor(i);
            double[][] img = loadImage(ip, w, h);

            double[][] deconv = DeconvolutionCore.iterativeDeblur(img, psf, iteration, rule);
            double recordMax = getMax(img);
            double[][] normalized = normalizeToRange(deconv, recordMax);

            short[] uint16Pixels = convertToUint16(normalized, w, h);
            ShortProcessor sp = new ShortProcessor(w, h);
            sp.setPixels(uint16Pixels);
            resultStack.addSlice("Frame " + i, sp);
        }

        IJ.showStatus("Deconvolution Complete");
        IJ.showProgress(1.0);

        String originalTitle = imp.getTitle();
        String baseName = originalTitle.lastIndexOf(".") > 0
                ? originalTitle.substring(0, originalTitle.lastIndexOf("."))
                : originalTitle;
        String newTitle = baseName + suffix + ".tif";

        return new ImagePlus(newTitle, resultStack);
    }

    private void displayPSF(double[][] psf, double exWavelength,
                            double numericalAperture, double pixelSize) {
        int h = psf.length;
        int w = psf[0].length;

        double maxPsf = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if (psf[j][i] > maxPsf) maxPsf = psf[j][i];
            }
        }

        short[] pixels = new short[w * h];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int val = (int) Math.round(psf[j][i] * 65535);
                pixels[j * w + i] = (short) Math.max(0, Math.min(65535, val));
            }
        }

        ShortProcessor sp = new ShortProcessor(w, h);
        sp.setPixels(pixels);

        String title = String.format("PSF_NA_%.2f_lambda_%.3fum_pixel_%.4fum.tif",
                numericalAperture, exWavelength, pixelSize);
        new ImagePlus(title, sp).show();
    }

    // --- Helper functions ---

    private double[][] loadImage(ImageProcessor ip, int w, int h) {
        double[][] img = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img[y][x] = ip.getPixel(x, y);
            }
        }
        return img;
    }

    private double getMax(double[][] img) {
        double max = 0;
        for (double[] row : img) {
            for (double v : row) {
                if (v > max) max = v;
            }
        }
        return max;
    }

    private double[][] normalizeToRange(double[][] img, double maxVal) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int h = img.length;
        int w = img[0].length;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if (img[j][i] < min) min = img[j][i];
                if (img[j][i] > max) max = img[j][i];
            }
        }

        double range = max - min;
        if (range < 1e-10) range = 1e-10;

        double[][] out = new double[h][w];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                out[j][i] = (img[j][i] - min) / range * maxVal;
            }
        }
        return out;
    }

    private ImagePlus createResultImage(double[][] result, String originalTitle,
                                        int w, int h, String suffix) {
        short[] uint16Pixels = convertToUint16(result, w, h);

        String baseName = originalTitle.lastIndexOf(".") > 0
                ? originalTitle.substring(0, originalTitle.lastIndexOf("."))
                : originalTitle;
        String newTitle = baseName + suffix + ".tif";

        ShortProcessor resultSp = new ShortProcessor(w, h);
        resultSp.setPixels(uint16Pixels);
        return new ImagePlus(newTitle, resultSp);
    }

    private short[] convertToUint16(double[][] result, int w, int h) {
        short[] uint16Pixels = new short[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double val = result[y][x];
                int uint16Val = Math.max(0, Math.min(65535, (int) Math.round(val)));
                uint16Pixels[y * w + x] = (short) uint16Val;
            }
        }
        return uint16Pixels;
    }
}