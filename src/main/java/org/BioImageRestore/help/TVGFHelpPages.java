package org.BioImageRestore.help;

/**
 * HTML help content for the TVGF (Total Variation Guided Filter) plugin.
 *
 * <p>Symbols follow the TVGF model used by the plugin:
 * <ul>
 *   <li><b>G</b> — observed (or pre-denoised) image used as guidance</li>
 *   <li><b>S0</b> — quarter-window dark channel estimate</li>
 *   <li><b>S</b> — estimated scattered-light field</li>
 *   <li><b>λ</b> — regularization weight balancing data fidelity and smoothness</li>
 * </ul>
 *
 * <p>All pages are in plain English. Internal links use the same topic
 * strings as the UI help buttons, so navigation stays inside the
 * HelpBrowser window.
 */
public class TVGFHelpPages implements HelpPages {

    private static final String CSS =
            "<style>"
                    + "body{font-family:'Segoe UI',Arial,sans-serif;font-size:13px;margin:16px;line-height:1.5;color:#222;}"
                    + "h2{font-size:16px;margin-top:0;border-bottom:1px solid #cccccc;padding-bottom:4px;}"
                    + "a{color:#0000cc;text-decoration:none;}"
                    + "a:hover{text-decoration:underline;}"
                    + ".formula{font-family:Georgia,serif;font-style:italic;margin:12px 0;padding:8px;"
                    + "background:#f6f6f6;border-left:3px solid #0066cc;}"
                    + ".range{font-size:12px;color:#444444;margin-top:6px;}"
                    + ".ref{font-size:12px;color:#555555;margin-top:14px;}"
                    + "ul{margin:6px 0 6px 18px;}"
                    + "</style>";

    @Override
    public String getPage(String topic) {
        switch (topic) {
            case "darkChannelWindow":
                return page("Dark channel window", bodyDarkChannelWindow());
            case "statisticsWindow":
                return page("Statistics window", bodyStatisticsWindow());
            case "lambda":
                return page("Lambda (&lambda;)", bodyLambda());
            case "rofDenoise":
                return page("ROF pre-denoising", bodyRofDenoise());
            default:
                return page("Not found",
                        "<p>No help page for topic: <b>" + topic + "</b></p>");
        }
    }

    // ---------- HTML shell helpers ----------

    private String page(String title, String body) {
        return "<html><head>" + CSS + "</head><body>"
                + "<h2>" + title + "</h2>"
                + body
                + "</body></html>";
    }

    private String seeAlso(String... links) {
        StringBuilder sb = new StringBuilder("<p><b>See also:</b> ");
        for (int i = 0; i < links.length; i++) {
            String[] kv = links[i].split("\\|");
            sb.append("<a href='").append(kv[0]).append("'>").append(kv[1]).append("</a>");
            if (i < links.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(".</p>");
        return sb.toString();
    }

    private String references(String... entries) {
        StringBuilder sb = new StringBuilder(
                "<p class='ref'><b>References</b></p><ol class='ref'>");
        for (String entry : entries) {
            sb.append("<li>").append(entry).append("</li>");
        }
        sb.append("</ol>");
        return sb.toString();
    }

    // ---------- Main parameter pages ----------

    private String bodyDarkChannelWindow() {
        return "<p>The <b>dark channel window</b> is the size (in pixels) of the "
                + "quarter-window used to estimate the dark channel <i>S</i><sup>0</sup>.</p>"
                + "<ul>"
                + "<li><b>Increase</b> — a larger window may capture more distant dark "
                + "information, but it can mix unrelated background and increase computation.</li>"
                + "<li><b>Decrease</b> — a smaller window better preserves local dark "
                + "structures and runs faster, but may become too noisy.</li>"
                + "</ul>"
                + "<p>A good heuristic: choose a size slightly larger than the smallest "
                + "feature of the largest object of interest. For rod-shaped cells, it "
                + "should exceed the short-axis length of the cell. Avoid values much "
                + "larger than necessary to prevent mixing information from distant areas.</p>"
                + "<p class='range'><b>Default:</b> 10. Recommended: 5&ndash;30, depending "
                + "on image resolution.</p>"
                + seeAlso(
                "statisticsWindow|Statistics window",
                "lambda|Lambda (&lambda;)");
    }

    private String bodyStatisticsWindow() {
        return "<p>The <b>statistics window</b> is the local neighborhood size used to "
                + "compute the linear-fit parameters <i>a</i> and <i>b</i> that relate "
                + "the guidance image <i>G</i> to the scattered-light estimate <i>S</i>.</p>"
                + "<ul>"
                + "<li><b>Increase</b> — more samples stabilize the fit, but local "
                + "linearity may break down and details can be over-smoothed.</li>"
                + "<li><b>Decrease</b> — preserves local adaptivity, but the fit may "
                + "become unstable with insufficient samples.</li>"
                + "</ul>"
                + "<p>It should be at least as large as the dark channel window, and "
                + "typically 1.5 to 3 times larger to repair block artifacts caused by "
                + "the quarter-window operator.</p>"
                + "<p class='range'><b>Default:</b> 20. Recommended: 1.5&ndash;3&times; "
                + "the dark channel window size.</p>"
                + seeAlso(
                "darkChannelWindow|Dark channel window",
                "lambda|Lambda (&lambda;)");
    }

    private String bodyLambda() {
        return "<p>The <b>regularization weight (&lambda;)</b> balances two objectives "
                + "inside each local window:</p>"
                + "<div class='formula'>"
                + "&#949;(a,b) = &#931; (S &minus; S<sub>0</sub>)<sup>2</sup> "
                + "+ &lambda; &#931; |&#8711;S|"
                + "</div>"
                + "<ul>"
                + "<li>The first term keeps the estimated scattered light <i>S</i> "
                + "close to the dark channel estimate <i>S</i><sup>0</sup>.</li>"
                + "<li>The second term penalizes the gradient of <i>S</i>, enforcing "
                + "smoothness of the scattered light field.</li>"
                + "</ul>"
                + "<p>Practical effects:</p>"
                + "<ul>"
                + "<li><b>Increase &lambda;</b> — the scattered-light estimate becomes "
                + "smoother and less noisy, but may deviate from the dark channel prior "
                + "and under-correct weak structures.</li>"
                + "<li><b>Decrease &lambda;</b> — the estimate follows <i>S</i><sup>0</sup> "
                + "more closely, preserving fine details, but noise and block artifacts "
                + "may remain.</li>"
                + "</ul>"
                + "<p class='range'><b>Recommended range:</b> 0.1&ndash;0.001. "
                + "<b>Default:</b> 0.01.</p>"
                + seeAlso(
                "darkChannelWindow|Dark channel window",
                "statisticsWindow|Statistics window")
                + references("<a href='https://doi.org/10.1109/ACCESS.2025.3584403'>"
                + "Gong, Y. Removing Scattered Light in Biomedical Images via "
                + "Total Variation Guided Filter. IEEE Access, vol. 13, pp. 1-12, "
                + "2025. doi:10.1109/ACCESS.2025.3584403</a>");
    }

    // ---------- Preprocessing page ----------

    private String bodyRofDenoise() {
        return "<p><b>Apply ROF pre-denoising</b> optionally smooths the input image "
                + "with a total-variation (ROF) denoising step before using it as the "
                + "guidance image <i>G</i> in the TVGF model.</p>"
                + "<ul>"
                + "<li><b>Unchecked (default)</b> — the raw image is used directly as "
                + "guidance. This is suitable for low-noise or high-SNR images where "
                + "denoising could make the output look unnatural.</li>"
                + "<li><b>Checked</b> — the image is first denoised, which often leads "
                + "to a more visually acceptable result when noise is strong. The "
                + "pre-denoised image serves as a cleaned guidance, improving the "
                + "stability of local statistics.</li>"
                + "</ul>"
                + "<p>Even with high noise, leaving this option off can still produce "
                + "acceptable outputs, but enabling it usually yields a cleaner "
                + "scattered-light estimate. Choose based on visual preference.</p>";
    }
}