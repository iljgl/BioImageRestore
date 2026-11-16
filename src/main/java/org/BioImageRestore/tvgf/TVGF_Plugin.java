package org.BioImageRestore.tvgf;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.BioImageRestore.tvgf.core.TVGFCore;
import org.BioImageRestore.help.HelpBrowser;
import org.BioImageRestore.help.HelpPages;
import org.BioImageRestore.help.TVGFHelpPages;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * TVGF (Total Variation Guided Filter) plugin with a ThunderSTORM-style
 * Swing parameter dialog. The dialog is split into two logical groups:
 * main parameters (dark-channel window, statistics window, lambda) and
 * an optional ROF pre-denoising step.
 */
public class TVGF_Plugin implements PlugIn {

    private static final HelpPages HELP = new TVGFHelpPages();
    private static final Color HELP_BUTTON_BLUE = new Color(220, 220, 220);
    private static final Color GROUP_BORDER_COLOR = new Color(128, 128, 128, 150);

    /** Holder for dialog results; null if the user cancels. */
    private static class Parameters {
        int darkChannelWindow;
        int statisticsWindow;
        double lambda;
        boolean applyRofDenoise;
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

        // Check if it is a time-series image.
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
                    parameters.darkChannelWindow,
                    parameters.statisticsWindow,
                    parameters.lambda,
                    parameters.applyRofDenoise);
        } else {
            resultImp = processSingleFrame(
                    imp,
                    parameters.darkChannelWindow,
                    parameters.statisticsWindow,
                    parameters.lambda,
                    parameters.applyRofDenoise);
        }

        resultImp.show();
    }

    // ------------------------------------------------------------------
    //  Swing parameter dialog
    // ------------------------------------------------------------------

    private Parameters showSwingDialog(ImagePlus imp) {
        JDialog dialog = new JDialog((Frame) null, "TVGF Parameters", true);
        try {
            dialog.setIconImage(IJ.getInstance().getIconImage());
        } catch (Exception ignored) {
            // Keep the default icon if ImageJ's icon is unavailable.
        }

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField darkChannelField = new JTextField("10", 8);
        JTextField statisticsField = new JTextField("20", 8);
        JTextField lambdaField = new JTextField("0.01", 8);
        JCheckBox rofDenoiseCheckBox = new JCheckBox("Apply ROF pre-denoising");

        // recA: Main parameters
        JPanel mainParametersPanel = createGroupPanel("Main parameters");
        addLabeledFieldRow(mainParametersPanel, 0, "Dark channel window",
                darkChannelField, "darkChannelWindow", dialog);
        addLabeledFieldRow(mainParametersPanel, 1, "Statistics window",
                statisticsField, "statisticsWindow", dialog);
        addLabeledFieldRow(mainParametersPanel, 2, "Lambda (λ)",
                lambdaField, "lambda", dialog);

        // recB: Preprocessing
        JPanel preprocessingPanel = createGroupPanel("Preprocessing");
        addCheckBoxRow(preprocessingPanel, 0, rofDenoiseCheckBox, "rofDenoise", dialog);

        mainParametersPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        preprocessingPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        root.add(mainParametersPanel);
        root.add(Box.createVerticalStrut(6));
        root.add(preprocessingPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        root.add(buttonPanel);

        final Parameters[] result = new Parameters[1];

        okButton.addActionListener(e -> {
            Parameters p = new Parameters();
            p.darkChannelWindow = (int) Math.round(parseDouble(darkChannelField, 10.0));
            p.statisticsWindow = (int) Math.round(parseDouble(statisticsField, 20.0));
            p.lambda = parseDouble(lambdaField, 0.01);
            p.applyRofDenoise = rofDenoiseCheckBox.isSelected();
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

    /** Adds one checkbox row that spans the first two columns. */
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
    //  Algorithm pipeline (unchanged except for renamed variables)
    // ------------------------------------------------------------------

    private ImagePlus processSingleFrame(ImagePlus imp,
                                         int darkChannelWindow,
                                         int statisticsWindow,
                                         double lambda,
                                         boolean applyRofDenoise) {
        ImageProcessor ip = imp.getProcessor();
        int w = ip.getWidth();
        int h = ip.getHeight();

        double[][] normalizedImage = loadAndNormalize(ip, w, h);
        double recordMax = getRecordMax(ip);

        IJ.showStatus("Running TVGF...");
        double[][] result = TVGFCore.process(
                normalizedImage,
                darkChannelWindow,
                statisticsWindow,
                lambda,
                applyRofDenoise);

        return createResultImage(result, imp.getTitle(), w, h, recordMax, "_TVGF.tif");
    }

    private ImagePlus processAllFrames(ImagePlus imp,
                                       int darkChannelWindow,
                                       int statisticsWindow,
                                       double lambda,
                                       boolean applyRofDenoise) {
        int w = imp.getWidth();
        int h = imp.getHeight();
        int numFrames = imp.getStackSize();
        ImageStack stack = imp.getStack();
        ImageStack resultStack = new ImageStack(w, h);

        for (int i = 1; i <= numFrames; i++) {
            IJ.showStatus("Processing frame " + i + " of " + numFrames);
            IJ.showProgress(i, numFrames);

            ImageProcessor ip = stack.getProcessor(i);
            double[][] normalizedImage = loadAndNormalize(ip, w, h);
            double recordMax = getRecordMax(ip);

            double[][] result = TVGFCore.process(
                    normalizedImage,
                    darkChannelWindow,
                    statisticsWindow,
                    lambda,
                    applyRofDenoise);

            short[] uint16Pixels = convertToUint16(result, w, h, recordMax);
            ShortProcessor shortProcessor = new ShortProcessor(w, h);
            shortProcessor.setPixels(uint16Pixels);
            resultStack.addSlice("Frame " + i, shortProcessor);
        }

        IJ.showStatus("TVGF Complete");
        IJ.showProgress(1.0);

        String originalTitle = imp.getTitle();
        String baseName = originalTitle.lastIndexOf(".") > 0
                ? originalTitle.substring(0, originalTitle.lastIndexOf("."))
                : originalTitle;
        String newTitle = baseName + "_TVGF.tif";

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

    private double[][] loadAndNormalize(ImageProcessor ip, int w, int h) {
        double recordMax = getRecordMax(ip);
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