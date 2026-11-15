package org.DenoiseStorm.help;

import ij.IJ;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.net.URI;

/**
 * HelpBrowser is a reusable, non-modal help window for ImageJ plugins.
 *
 * <p>Mechanism: it hosts a {@link JEditorPane} in "text/html" mode. When the
 * user clicks an internal link {@code <a href="topic">}, the hyperlink event
 * is intercepted and the SAME window swaps its content to the corresponding
 * HelpPages page — the system browser is NOT opened. External links starting
 * with "http" are delegated to the desktop browser.</p>
 *
 * <p>This mirrors how ThunderSTORM implements its "?" buttons: every help
 * page is just an HTML string bundled inside the jar, and the navigation
 * between pages is handled entirely in-process by this class.</p>
 *
 * Usage:
 * <pre>
 *   HelpBrowser.show(parentDialog, new HTVHelpPages(), "fidelity");
 * </pre>
 */
public class HelpBrowser extends JDialog {

    /** The HTML renderer; never editable. */
    private final JEditorPane pane;

    /** Content provider owned by the calling algorithm. */
    private final HelpPages pages;

    private HelpBrowser(Window parent, HelpPages pages, String title) {
        super(parent == null ? JOptionPane.getFrameForComponent(null) : parent,
                title, ModalityType.MODELESS);
        this.pages = pages;

        pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);

        // Intercept hyperlink clicks: internal topics -> swap page in this window;
        // external URLs -> open in system browser.
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String href = e.getDescription();
                if (href == null) return;
                if (href.startsWith("http")) {
                    openExternal(href);
                } else {
                    showPage(href);
                }
            }
        });

        JScrollPane sp = new JScrollPane(pane);
        sp.setPreferredSize(new Dimension(640, 520));
        getContentPane().add(sp);
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Open a help window showing the given topic.
     *
     * @param parent the dialog that triggered this help (for centering)
     * @param pages  the algorithm-specific content provider
     * @param topic  initial topic to display
     */
    public static void show(Window parent, HelpPages pages, String topic) {
        show(parent, pages, topic, "DenoiseStorm Help");
    }

    public static void show(Window parent, HelpPages pages, String topic, String title) {
        HelpBrowser browser = new HelpBrowser(parent, pages, title);
        browser.showPage(topic);
        browser.setVisible(true);
    }

    /** Replace the current HTML content with the page for {@code topic}. */
    private void showPage(String topic) {
        pane.setText(pages.getPage(topic));
        pane.setCaretPosition(0);
    }

    /** Delegate an external http(s) link to the system browser. */
    private void openExternal(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            IJ.showMessage("Cannot open URL: " + url);
        }
    }
}
