package org.BioImageRestore.htvdenoise;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;
import ij.process.ImageProcessor;
import org.BioImageRestore.htvdenoise.core.SparseHessianCore;

import org.BioImageRestore.help.HelpBrowser;
import org.BioImageRestore.help.HelpPages;
import org.BioImageRestore.help.HTVHelpPages;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
/**
 * HTV Denoise plugin with a ThunderSTORM-style Swing parameter dialog.
 *
 * <p>The UI is organized into three focused groups:
 * <ol>
 *   <li>Main parameters: Fidelity (α), Hessian (β), Sparsity (γ).</li>
 *   <li>Stopping criteria: Relative change (ε), Max iterations (N_max).</li>
 *   <li>Solver mode: Stable mode.</li>
 * </ol>
 * Each parameter has a blue "?" button that opens its dedicated help page.
 */
public class HTVDenoise_Plugin implements PlugIn {
    private static final HelpPages HELP = new HTVHelpPages();
    private static final Color HELP_BUTTON_BLUE = new Color(220, 220, 220);
    private static final Color GROUP_BORDER_COLOR = new Color(128, 128, 128, 150);
    /** Holder for dialog results; null if the user cancels. */
    private static class Parameters {
        double fidelity;
        double hessian;
        double sparsity;
        double relChange;
        int maxIterations;
        boolean stableMode;
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
        // ADMM penalty parameter is kept fixed at 1.
        float mu = 1.0f;
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
        float fidelity = (float) parameters.fidelity;
        float hessian = (float) parameters.hessian;
        float sparsity = (float) parameters.sparsity;
        float relChange = (float) parameters.relChange;
        int maxIterations = parameters.maxIterations;
        int stableModeValue = parameters.stableMode ? 1 : 0;
        ImagePlus resultImp;
        if (processAllFrames) {
            resultImp = processAllFrames(
                    imp, fidelity, hessian, sparsity, mu, maxIterations,
                    stableModeValue, relChange);
        } else {
            resultImp = processSingleFrame(
                    imp, fidelity, hessian, sparsity, mu, maxIterations,
                    stableModeValue, relChange);
        }
        resultImp.show();
    }
    // ------------------------------------------------------------------
    //  Swing parameter dialog
    // ------------------------------------------------------------------
    private Parameters showSwingDialog(ImagePlus imp) {
        JDialog dialog = new JDialog((Frame) null, "HTV Denoise Parameters", true);
        try {
            dialog.setIconImage(IJ.getInstance().getIconImage());
        } catch (Exception ignored) {
            // Keep the default dialog icon if ImageJ's icon is unavailable.
        }
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTextField fidelityField = new JTextField("150", 8);
        JTextField hessianField = new JTextField("10", 8);
        JTextField sparsityField = new JTextField("15", 8);
        JTextField relChangeField = new JTextField("0.005", 8);
        JTextField maxIterationsField = new JTextField("75", 8);
        JCheckBox stableModeCheckBox = new JCheckBox("Use stable mode");
        // recA: Main parameters
        JPanel mainParametersPanel = createGroupPanel("Main parameters");
        addLabeledFieldRow(mainParametersPanel, 0, "Fidelity (α)",
                fidelityField, "fidelity", dialog);
        addLabeledFieldRow(mainParametersPanel, 1, "Hessian (β)",
                hessianField, "hessian", dialog);
        addLabeledFieldRow(mainParametersPanel, 2, "Sparsity (γ)",
                sparsityField, "sparsity", dialog);
        // recB: Stopping criteria
        JPanel stoppingPanel = createGroupPanel("Stopping criteria");
        addLabeledFieldRow(stoppingPanel, 0, "Relative change (ε)",
                relChangeField, "relChange", dialog);
        addLabeledFieldRow(stoppingPanel, 1, "Max iterations (N_max)",
                maxIterationsField, "maxIterations", dialog);
        // recC: Solver mode
        JPanel solverPanel = createGroupPanel("Solver mode");
        addCheckBoxRow(solverPanel, 0, stableModeCheckBox, "stable", dialog);
        mainParametersPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        stoppingPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        solverPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(mainParametersPanel);
        root.add(Box.createVerticalStrut(6));
        root.add(stoppingPanel);
        root.add(Box.createVerticalStrut(6));
        root.add(solverPanel);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        root.add(buttonPanel);
        final Parameters[] result = new Parameters[1];
        okButton.addActionListener(e -> {
            Parameters p = new Parameters();
            p.fidelity = parseDouble(fidelityField, 150.0);
            p.hessian = parseDouble(hessianField, 10.0);
            p.sparsity = parseDouble(sparsityField, 15.0);
            p.relChange = parseDouble(relChangeField, 0.005);
            p.maxIterations = (int) Math.round(parseDouble(maxIterationsField, 75.0));
            p.stableMode = stableModeCheckBox.isSelected();
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
    /** Creates a group panel with a subtle gray, semi-transparent rounded border. */
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
    //  Algorithm pipeline
    // ------------------------------------------------------------------
    private ImagePlus processSingleFrame(ImagePlus imp,
                                         float fidelity,
                                         float hessian,
                                         float sparsity,
                                         float mu,
                                         int maxIterations,
                                         int stable,
                                         float relChange) {
        ImageProcessor ip = imp.getProcessor();
        FloatProcessor floatProcessor = ip.convertToFloatProcessor();
        int w = floatProcessor.getWidth();
        int h = floatProcessor.getHeight();
        float[] pixels = (float[]) floatProcessor.getPixels();
        float[][] image2D = new float[h][w];
        for (int y = 0; y < h; y++) {
            System.arraycopy(pixels, y * w, image2D[y], 0, w);
        }
        SparseHessianCore.ProcessResult result = processWithWhileLoop(
                image2D, fidelity, hessian, sparsity, mu, maxIterations,
                stable, relChange, false, 0.0f);
        return createResultImage(result.image, imp.getTitle(), w, h);
    }
    private ImagePlus processAllFrames(ImagePlus imp,
                                       float fidelity,
                                       float hessian,
                                       float sparsity,
                                       float mu,
                                       int maxIterations,
                                       int stable,
                                       float relChange) {
        int w = imp.getWidth();
        int h = imp.getHeight();
        int numFrames = imp.getStackSize();
        ImageStack stack = imp.getStack();
        ImageStack resultStack = new ImageStack(w, h);
        float zeroPercent = 0.0f;
        for (int i = 1; i <= numFrames; i++) {
            IJ.showStatus("Processing frame " + i + " of " + numFrames);
            IJ.showProgress(i, numFrames);
            ImageProcessor ip = stack.getProcessor(i);
            FloatProcessor floatProcessor = ip.convertToFloatProcessor();
            float[] pixels = (float[]) floatProcessor.getPixels();
            float[][] image2D = new float[h][w];
            for (int y = 0; y < h; y++) {
                System.arraycopy(pixels, y * w, image2D[y], 0, w);
            }
            // The first frame is processed without the ZeroPercent criterion.
            // Its measured zero-fraction is then used as the target for all
            // remaining frames.
            boolean useZeroPercent = (i > 1);
            SparseHessianCore.ProcessResult result = processWithWhileLoop(
                    image2D, fidelity, hessian, sparsity, mu, maxIterations,
                    stable, relChange, useZeroPercent, zeroPercent);
            if (i == 1) {
                zeroPercent = result.zeroPercentValue;
            }
            short[] uint16Pixels = convertToUint16(result.image, w, h);
            ShortProcessor shortProcessor = new ShortProcessor(w, h);
            shortProcessor.setPixels(uint16Pixels);
            resultStack.addSlice("Frame " + i, shortProcessor);
        }
        IJ.showStatus("HTV Denoise Complete");
        IJ.showProgress(1.0);
        String originalTitle = imp.getTitle();
        String baseName = originalTitle.lastIndexOf(".") > 0
                ? originalTitle.substring(0, originalTitle.lastIndexOf("."))
                : originalTitle;
        String newTitle = baseName + "_HTV.tif";
        return new ImagePlus(newTitle, resultStack);
    }
    private SparseHessianCore.ProcessResult processWithWhileLoop(
            float[][] image2D,
            float fidelity,
            float hessian,
            float sparsity,
            float mu,
            int maxIterations,
            int stable,
            float relChange,
            boolean useZeroPercent,
            float zeroPercent) {
        float[][] currentImage = image2D;
        SparseHessianCore.ProcessResult result;
        while (true) {
            result = SparseHessianCore.process(
                    currentImage,
                    fidelity,
                    hessian,
                    sparsity,
                    mu,
                    maxIterations,
                    stable,
                    relChange,
                    useZeroPercent,
                    zeroPercent);
            currentImage = result.image;
            if (!useZeroPercent || result.zeroPercentReached) {
                return result;
            }
        }
    }
    private ImagePlus createResultImage(float[][] result,
                                        String originalTitle,
                                        int w,
                                        int h) {
        short[] uint16Pixels = convertToUint16(result, w, h);
        String baseName = originalTitle.lastIndexOf(".") > 0
                ? originalTitle.substring(0, originalTitle.lastIndexOf("."))
                : originalTitle;
        String newTitle = baseName + "_HTV.tif";
        ShortProcessor resultProcessor = new ShortProcessor(w, h);
        resultProcessor.setPixels(uint16Pixels);
        return new ImagePlus(newTitle, resultProcessor);
    }
    private short[] convertToUint16(float[][] result, int w, int h) {
        float[] resultPixels = new float[w * h];
        for (int y = 0; y < h; y++) {
            System.arraycopy(result[y], 0, resultPixels, y * w, w);
        }
        short[] uint16Pixels = new short[w * h];
        for (int i = 0; i < resultPixels.length; i++) {
            float floatValue = resultPixels[i];
            int uint16Value = Math.max(0, Math.min(65535, Math.round(floatValue)));
            uint16Pixels[i] = (short) uint16Value;
        }
        return uint16Pixels;
    }
}
//package org.BioImageRestore.htvdenoise;
//
//import ij.IJ;
//import ij.ImagePlus;
//import ij.ImageStack;
//import ij.plugin.PlugIn;
//import ij.process.FloatProcessor;
//import ij.process.ShortProcessor;
//import ij.process.ImageProcessor;
//import org.BioImageRestore.htvdenoise.core.SparseHessianCore;
//
//import org.BioImageRestore.help.HelpBrowser;
//import org.BioImageRestore.help.HelpPages;
//import org.BioImageRestore.help.HTVHelpPages;
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.ActionEvent;
//
///**
// * HTV Denoise plugin with a custom Swing JDialog (ThunderSTORM-style):
// * blue Fiji window icon + blue "?" help button next to each parameter.
// */
//public class HTVDenoise_Plugin implements PlugIn {
//
//    private static final HelpPages HELP = new HTVHelpPages();
//    private static final Color BLUE = new Color(0, 102, 204);
//
//    /** Holder for dialog results; null if user canceled. */
//    private static class Params {
//        double fidelity, hessian, sparsity, relChange;
//        int iterations;
//        boolean stable;
//    }
//
//    @Override
//    public void run(String s) {
//        ImagePlus imp = IJ.getImage();
//        if (imp == null) {
//            IJ.noImage();
//            return;
//        }
//
//        // 1. Swing dialog with blue "?" buttons
//        Params p = showSwingDialog(imp);
//        if (p == null) return;
//
//        //Set mu to 1
//        float mu = 1.0f;
//
//        // 2.Check if it is a time series image
//        int numFrames = imp.getStackSize();
//        boolean processAllFrames = false;
//
//        if (numFrames > 1) {
//            int ans = JOptionPane.showConfirmDialog(null,
//                    "This is a time-series image with " + numFrames + " frames.\n"
//                            + "Process all frames?",
//                    "Process Frames",
//                    JOptionPane.YES_NO_OPTION);
//            if (ans == JOptionPane.CLOSED_OPTION) return;
//            processAllFrames = (ans == JOptionPane.YES_OPTION);
//        }
//
//        // 3.process images
//        ImagePlus resultImp;
//        float fidelity = (float) p.fidelity;
//        float hessian = (float) p.hessian;
//        float sparsity = (float) p.sparsity;
//        int iter = p.iterations;
//
//        if (processAllFrames) {
//            resultImp = processAllFrames(imp, fidelity, hessian, sparsity, mu, iter,
//                    p.stable ? 1 : 0, (float) p.relChange);
//        } else {
//            resultImp = processSingleFrame(imp, fidelity, hessian, sparsity, mu, iter,
//                    p.stable ? 1 : 0, (float) p.relChange);
//        }
//
//        // 4.Display Results
//        resultImp.show();
//    }
//
//    // ------------------------------------------------------------------
//    //  Swing dialog (replaces GenericDialog)
//    // ------------------------------------------------------------------
//    private Params showSwingDialog(ImagePlus imp) {
//        JDialog dialog = new JDialog((Frame) null, "HTV Denoise Parameters", true);
//        try {
//            dialog.setIconImage(IJ.getInstance().getIconImage());
//        } catch (Exception ignored) {}
//
//        JPanel panel = new JPanel(new GridBagLayout());
//        GridBagConstraints g = new GridBagConstraints();
//        g.insets = new Insets(4, 6, 4, 6);
//        g.anchor = GridBagConstraints.WEST;
//        g.gridy = 0;
//
//        // --- input fields ---
//        JTextField fFidelity = new JTextField("150", 8);
//        JTextField fHessian     = new JTextField("10", 8);
//        JTextField fSparsity   = new JTextField("15", 8);
//        JTextField fIter     = new JTextField("50", 8);
//        JCheckBox  cStable    = new JCheckBox("Use stable ():", false);
//        JTextField fRelChange      = new JTextField("0.0100", 8);
//
//        // --- rows: label + field + blue ? button ---
//        addRow(panel, g, "Fidelity (α), keep 150:", fFidelity, "fidelity", dialog);
//        addRow(panel, g, "hessian (β), (1-50):",      fHessian,     "hessian",     dialog);
//        addRow(panel, g, "sparsity (γ), (1-50):",  fSparsity,   "sparsity",   dialog);
//        addRow(panel, g, "Max Iterations, (50-100):",   fIter,     "iterations", dialog);
//        addRow(panel, g, cStable, "stable", dialog);
//        addRow(panel, g, "relChange , (0.01-0.001):", fRelChange,    "relChange",   dialog);
//
//        // --- OK / Cancel ---
//        JButton ok = new JButton("OK");
//        JButton cancel = new JButton("Cancel");
//        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//        btnPanel.add(ok);
//        btnPanel.add(cancel);
//        g.gridx = 0; g.gridwidth = 3;
//        g.fill = GridBagConstraints.NONE;
//        panel.add(btnPanel, g);
//
//        final Params[] result = new Params[1];
//
//        ok.addActionListener(e -> {
//            Params p = new Params();
//            p.fidelity    = parse(fFidelity, 150);
//            p.hessian       = parse(fHessian, 10);
//            p.sparsity     = parse(fSparsity, 15);
//            p.iterations = (int) parse(fIter, 100);
//            p.stable      = cStable.isSelected();
//            p.relChange        = parse(fRelChange, 0.001);
//            result[0] = p;
//            dialog.dispose();
//        });
//        cancel.addActionListener(e -> { dialog.dispose(); });
//
//        dialog.getContentPane().add(panel);
//        dialog.pack();
//        dialog.setLocationRelativeTo(null);
//        dialog.setVisible(true);   // blocks until dispose
//
//        return result[0];
//    }
//
//    /** Add a row: label (col 0) + field (col 1) + blue ? button (col 2). */
//    private void addRow(JPanel panel, GridBagConstraints g,
//                        String labelText, JComponent field, String topic, JDialog dialog) {
//        g.gridwidth = 1;
//        g.fill = GridBagConstraints.NONE;
//
//        g.gridx = 0;
//        panel.add(new JLabel(labelText), g);
//
//        g.gridx = 1;
//        panel.add(field, g);
//
//        g.gridx = 2;
//        panel.add(makeHelpButton(topic, dialog), g);
//
//        g.gridy++;
//    }
//
//    /** Add a checkbox row that spans the label column, with ? button at col 2. */
//    private void addRow(JPanel panel, GridBagConstraints g,
//                        JCheckBox check, String topic, JDialog dialog) {
//        g.gridwidth = 1;
//        g.fill = GridBagConstraints.NONE;
//
//        g.gridx = 0; g.gridwidth = 2;
//        panel.add(check, g);
//
//        g.gridx = 2; g.gridwidth = 1;
//        panel.add(makeHelpButton(topic, dialog), g);
//
//        g.gridy++;
//    }
//
//    /** Blue ThunderSTORM-style "?" button. */
//    private JButton makeHelpButton(String topic, JDialog dialog) {
//        JButton b = new JButton("?");
//        b.setForeground(Color.BLACK);
//        b.setBackground(BLUE);
//        b.setFocusPainted(false);
//        b.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
//        b.setMargin(new Insets(2, 8, 2, 8));
//        b.addActionListener((ActionEvent e) -> HelpBrowser.show(dialog, HELP, topic));
//        return b;
//    }
//
//    private static double parse(JTextField f, double dflt) {
//        try { return Double.parseDouble(f.getText().trim()); }
//        catch (Exception e) { return dflt; }
//    }
//
//    // ------------------------------------------------------------------
//    //  Algorithm pipeline (unchanged from original)
//    // ------------------------------------------------------------------
//    private ImagePlus processSingleFrame(ImagePlus imp, float fidelity, float hessian, float sparsity,
//                                         float mu, int maxIter, int stable, float relChange) {
//        ImageProcessor ip = imp.getProcessor();
//        FloatProcessor fp = ip.convertToFloatProcessor();
//        int w = fp.getWidth();
//        int h = fp.getHeight();
//        float[] pixels = (float[]) fp.getPixels();
//
//        float[][] img2D = new float[h][w];
//        for (int y = 0; y < h; y++) {
//            System.arraycopy(pixels, y * w, img2D[y], 0, w);
//        }
//
//        SparseHessianCore.ProcessResult res = processWithWhileLoop(
//                img2D, fidelity, hessian, sparsity, mu, maxIter,
//                stable, relChange, false, 0.0f);
//
//        return createResultImage(res.image, imp.getTitle(), w, h);
//    }
//
//    private ImagePlus processAllFrames(ImagePlus imp, float fidelity, float hessian, float sparsity,
//                                       float mu, int maxIter, int stable, float relChange) {
//        int w = imp.getWidth();
//        int h = imp.getHeight();
//        int numFrames = imp.getStackSize();
//        ImageStack stack = imp.getStack();
//        ImageStack resultStack = new ImageStack(w, h);
//
//        float zeroPercent = 0.0f;
//
//        for (int i = 1; i <= numFrames; i++) {
//            IJ.showStatus("Processing frame " + i + " of " + numFrames);
//            IJ.showProgress(i, numFrames);
//
//            ImageProcessor ip = stack.getProcessor(i);
//            FloatProcessor fp = ip.convertToFloatProcessor();
//            float[] pixels = (float[]) fp.getPixels();
//
//            float[][] img2D = new float[h][w];
//            for (int y = 0; y < h; y++) {
//                System.arraycopy(pixels, y * w, img2D[y], 0, w);
//            }
//
//
//            boolean useZeroPercent = (i > 1);
//            SparseHessianCore.ProcessResult res = processWithWhileLoop(
//                    img2D, fidelity, hessian, sparsity, mu, maxIter,
//                    stable, relChange, useZeroPercent, zeroPercent);
//
//            if (i == 1) {
//                zeroPercent = res.zeroPercentValue;
//            }
//
//            short[] uint16Pixels = convertToUint16(res.image, w, h);
//            ShortProcessor sp = new ShortProcessor(w, h);
//            sp.setPixels(uint16Pixels);
//            resultStack.addSlice("Frame " + i, sp);
//        }
//
//        IJ.showStatus("HTV Denoise Complete");
//        IJ.showProgress(1.0);
//
//        String originalTitle = imp.getTitle();
//        String baseName = originalTitle.lastIndexOf(".") > 0
//                ? originalTitle.substring(0, originalTitle.lastIndexOf("."))
//                : originalTitle;
//        String newTitle = baseName + "_HTV.tif";
//
//        return new ImagePlus(newTitle, resultStack);
//    }
//
//    private SparseHessianCore.ProcessResult processWithWhileLoop(float[][] img2D, float fidelity, float hessian, float sparsity,
//                                                                 float mu, int maxIter, int stable, float relChange,
//                                                                 boolean useZeroPercent, float zeroPercent) {
//        float[][] currentImg = img2D;
//        SparseHessianCore.ProcessResult result;
//
//        while (true) {
//            result = SparseHessianCore.process(
//                    currentImg, fidelity, hessian, sparsity, mu, maxIter, stable, relChange,
//                    useZeroPercent, zeroPercent
//            );
//
//            currentImg = result.image;
//
//            if (!useZeroPercent || result.zeroPercentReached) {
//                return result;
//            }
//        }
//    }
//
//    private ImagePlus createResultImage(float[][] result, String originalTitle, int w, int h) {
//        short[] uint16Pixels = convertToUint16(result, w, h);
//
//        String baseName = originalTitle.lastIndexOf(".") > 0
//                ? originalTitle.substring(0, originalTitle.lastIndexOf("."))
//                : originalTitle;
//        String newTitle = baseName + "_HTV.tif";
//
//        ShortProcessor resultSp = new ShortProcessor(w, h);
//        resultSp.setPixels(uint16Pixels);
//        return new ImagePlus(newTitle, resultSp);
//    }
//
//    private short[] convertToUint16(float[][] result, int w, int h) {
//        float[] resultPixels = new float[w * h];
//        for (int y = 0; y < h; y++) {
//            System.arraycopy(result[y], 0, resultPixels, y * w, w);
//        }
//
//        short[] uint16Pixels = new short[w * h];
//        for (int i = 0; i < resultPixels.length; i++) {
//            float floatVal = resultPixels[i];
//            int uint16Val = Math.max(0, Math.min(65535, Math.round(floatVal)));
//            uint16Pixels[i] = (short) uint16Val;
//        }
//        return uint16Pixels;
//    }
//}
