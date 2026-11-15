package org.DenoiseStorm.help;

/**
 * HelpPages is the contract that every algorithm must implement to provide
 * its own set of help topics. It decouples the help browser (HelpBrowser)
 * from the actual content: the browser only knows how to render an HTML
 * string for a given topic name, while each algorithm supplies its own
 * strings.
 *
 * <p>A new algorithm (e.g. RL deconvolution) writes a class
 * {@code RLHelpPages implements HelpPages} with a switch over topic names.
 * No change to HelpBrowser or HelpUI is required.</p>
 */
public interface HelpPages {

    /**
     * Return the full HTML document (including &lt;html&gt;&lt;body&gt;) for the
     * given topic. The topic string is exactly the value inside
     * {@code <a href="...">} in other pages, so internal cross-links work
     * automatically.
     *
     * @param topic topic identifier, e.g. "fidelity", "β", "model"
     * @return complete HTML string ready to be set on a JEditorPane
     */
    String getPage(String topic);
}
