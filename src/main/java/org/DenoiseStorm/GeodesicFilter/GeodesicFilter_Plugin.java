package org.DenoiseStorm.GeodesicFilter;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.DenoiseStorm.GeodesicFilter.Core.GeodesicFilterCore;
import org.DenoiseStorm.help.HelpBrowser;
import org.DenoiseStorm.help.HelpPages;
import org.DenoiseStorm.help.GeodesicHelpPages;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Geodesic Filter plugin with a ThunderSTORM-style Swing parameter dialog.
 *
 * <p>The dialog contains a single parameter group:
 * <ul>
 *   <li>Window size (automatically adjusted to an odd number)</li>
 *   <li>Alpha (α) — balances spatial distance and intensity difference</li>
 *   <li>Sigma (σ) — controls the fall-off of weights with geodesic distance</li>
 * </ul>
 * Each parameter has a blue "?" button opening its dedicated help page.
 */
public class GeodesicFilter_Plugin implements PlugIn {

    private static final HelpPages HELP = new GeodesicHelpPages();
    private static final Color HELP_BUTTON_BLUE = new Color(0, 102, 204);
    private static final Color GROUP_BORDER_COLOR = new Color(128, 128, 128, 150);

    /** Holder for dialog results; null if the user cancels. */
    private static class Parameters {
        int windowSize;
        double alpha;
        double sigma;
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

        ImagePlus resultImp;
        if (processAllFrames) {
            resultImp = processAllFrames(
                    imp,
                    parameters.windowSize,
                    parameters.alpha,
                    parameters.sigma);
        } else {
            resultImp = processSingleFrame(
                    imp,
                    parameters.windowSize,
                    parameters.alpha,
                    parameters.sigma);
        }

        resultImp.show();
    }

    // ------------------------------------------------------------------
    //  Swing parameter dialog
    // ------------------------------------------------------------------

    private Parameters showSwingDialog(ImagePlus imp) {
        JDialog dialog = new JDialog((Frame) null, "Geodesic Filter Parameters", true);
        try {
            dialog.setIconImage(IJ.getInstance().getIconImage());
        } catch (Exception ignored) {
            // Keep the default icon if ImageJ's icon is unavailable.
        }

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField windowSizeField = new JTextField("7", 8);
        JTextField alphaField = new JTextField("5.0", 8);
        JTextField sigmaField = new JTextField("1.5", 8);

        // recA: Main parameters
        JPanel mainParametersPanel = createGroupPanel("Main parameters");
        addLabeledFieldRow(mainParametersPanel, 0, "Window size (pixels)",
                windowSizeField, "windowSize", dialog);
        addLabeledFieldRow(mainParametersPanel, 1, "Alpha (α)",
                alphaField, "alpha", dialog);
        addLabeledFieldRow(mainParametersPanel, 2, "Sigma (σ)",
                sigmaField, "sigma", dialog);

        mainParametersPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(mainParametersPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        root.add(buttonPanel);

        final Parameters[] result = new Parameters[1];

        okButton.addActionListener(e -> {
            Parameters p = new Parameters();
            // Ensure window size is an odd number >= 3.
            int rawWindow = (int) Math.round(parseDouble(windowSizeField, 7.0));
            p.windowSize = toOdd(rawWindow);
            p.alpha = parseDouble(alphaField, 5.0);
            p.sigma = parseDouble(sigmaField, 1.5);
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

    /** Converts any integer to the nearest odd value >= 3. */
    private static int toOdd(int value) {
        value = Math.max(3, value);
        return (value % 2 == 0) ? value + 1 : value;
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

    private ImagePlus processSingleFrame(ImagePlus imp,
                                         int windowSize,
                                         double alpha,
                                         double sigma) {
        ImageProcessor ip = imp.getProcessor();
        int w = ip.getWidth();
        int h = ip.getHeight();

        double recordMax = getRecordMax(ip);
        double[][] normalizedImage = loadAndNormalize(ip, w, h, recordMax);

        IJ.showStatus("Running Geodesic Filter...");
        IJ.showProgress(0);
        double[][] filtered = GeodesicFilterCore.process(
                normalizedImage, windowSize, alpha, sigma);
        IJ.showProgress(1);

        return createResultImage(filtered, imp.getTitle(), w, h, recordMax, "_Geodesic.tif");
    }

    private ImagePlus processAllFrames(ImagePlus imp,
                                       int windowSize,
                                       double alpha,
                                       double sigma) {
        int w = imp.getWidth();
        int h = imp.getHeight();
        int numFrames = imp.getStackSize();
        ImageStack stack = imp.getStack();
        ImageStack resultStack = new ImageStack(w, h);

        for (int i = 1; i <= numFrames; i++) {
            IJ.showStatus("Processing frame " + i + " of " + numFrames);
            IJ.showProgress(i, numFrames);

            ImageProcessor ip = stack.getProcessor(i);
            double recordMax = getRecordMax(ip);
            double[][] normalizedImage = loadAndNormalize(ip, w, h, recordMax);

            double[][] filtered = GeodesicFilterCore.process(
                    normalizedImage, windowSize, alpha, sigma);

            short[] uint16Pixels = convertToUint16(filtered, w, h, recordMax);
            ShortProcessor shortProcessor = new ShortProcessor(w, h);
            shortProcessor.setPixels(uint16Pixels);
            resultStack.addSlice("Frame " + i, shortProcessor);
        }

        IJ.showStatus("Geodesic Filter Complete");
        IJ.showProgress(1.0);

        String originalTitle = imp.getTitle();
        String baseName = originalTitle.lastIndexOf(".") > 0
                ? originalTitle.substring(0, originalTitle.lastIndexOf("."))
                : originalTitle;
        String newTitle = baseName + "_Geodesic.tif";

        return new ImagePlus(newTitle, resultStack);
    }

    // --- auxiliary functions ---

    private double getRecordMax(ImageProcessor ip) {
        int w = ip.getWidth();
        int h = ip.getHeight();
        double recordMax = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int value = ip.getPixel(x, y);
                if (value > recordMax) {
                    recordMax = value;
                }
            }
        }
        return recordMax;
    }

    private double[][] loadAndNormalize(ImageProcessor ip, int w, int h, double recordMax) {
        double[][] normalized = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                normalized[y][x] = ip.getPixel(x, y) / recordMax;
            }
        }
        return normalized;
    }

    private ImagePlus createResultImage(double[][] result,
                                        String originalTitle,
                                        int w,
                                        int h,
                                        double recordMax,
                                        String suffix) {
        short[] uint16Pixels = convertToUint16(result, w, h, recordMax);

        String baseName = originalTitle.lastIndexOf(".") > 0
                ? originalTitle.substring(0, originalTitle.lastIndexOf("."))
                : originalTitle;
        String newTitle = baseName + suffix;

        ShortProcessor resultProcessor = new ShortProcessor(w, h);
        resultProcessor.setPixels(uint16Pixels);
        return new ImagePlus(newTitle, resultProcessor);
    }

    private short[] convertToUint16(double[][] result,
                                    int w,
                                    int h,
                                    double recordMax) {
        short[] uint16Pixels = new short[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double floatValue = result[y][x] * recordMax;
                int uint16Value = Math.max(0, Math.min(65535, (int) Math.round(floatValue)));
                uint16Pixels[y * w + x] = (short) uint16Value;
            }
        }
        return uint16Pixels;
    }
}