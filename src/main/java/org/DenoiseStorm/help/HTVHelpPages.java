package org.DenoiseStorm.help;

/**
 * HTML help content for the HTV Denoise plugin.
 *
 * <p>Symbols follow the HTV energy functional used by the plugin:
 * <ul>
 *   <li>α — fidelity weight</li>
 *   <li>β — Hessian regularizer weight</li>
 *   <li>γ — sparsity regularizer weight</li>
 *   <li>ε — relative-change stopping tolerance</li>
 * </ul>
 *
 * <p>All pages are plain English. Internal links use the same topic strings
 * as the UI help buttons, so navigation stays inside the HelpBrowser window.
 */
public class HTVHelpPages implements HelpPages {

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
            case "fidelity":
                return page("Fidelity (&alpha;)", bodyFidelity());
            case "hessian":
                return page("Hessian (&beta;)", bodyHessian());
            case "sparsity":
                return page("Sparsity (&gamma;)", bodySparsity());
            case "htvModel":
                return page("HTV energy functional", bodyHtvModel());
            case "relChange":
                return page("Relative change (&epsilon;)", bodyRelChange());
            case "maxIterations":
                return page("Maximum iteration limit (N<sub>max</sub>)", bodyMaxIterations());
            case "stable":
                return page("Stable mode", bodyStableMode());
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

    private String bodyFidelity() {
        return "<p>The <b>fidelity weight (&alpha;)</b> controls how strongly the "
                + "denoised image must match the raw input image.</p>"
                + "<ul>"
                + "<li><b>Increase &alpha;</b> — result stays closer to the raw data; "
                + "weak signals are kept better, but more noise remains.</li>"
                + "<li><b>Decrease &alpha;</b> — smoothing becomes stronger; the "
                + "background looks cleaner, but fine structures may be lost.</li>"
                + "</ul>"
                + "<div class='formula'>Fidelity term: &nbsp; (&alpha;/2) &#8214;u &minus; f&#8214;"
                + "<sub>2</sub><sup>2</sup></div>"
                + "<p class='range'><b>Recommended range:</b> 50&ndash;300. "
                + "<b>Default:</b> 150.</p>"
                + seeAlso(
                "hessian|Hessian (&beta;)",
                "sparsity|Sparsity (&gamma;)",
                "htvModel|HTV energy functional");
    }

    private String bodyHessian() {
        return "<p>The <b>Hessian weight (&beta;)</b> penalizes second-order curvature. "
                + "It encourages piecewise-affine regions and is especially useful for "
                + "filamentous or membrane-like structures.</p>"
                + "<ul>"
                + "<li><b>Increase &beta;</b> — smoother interiors and stronger noise "
                + "suppression, but curved details may be flattened.</li>"
                + "<li><b>Decrease &beta;</b> — edges and curves are preserved better, "
                + "but less smoothing is applied.</li>"
                + "</ul>"
                + "<div class='formula'>Hessian regularizer: &nbsp; &beta; &#8214;H(u)&#8214;"
                + "<sub>*</sub>, &nbsp; H(u) = [u<sub>xx</sub>, u<sub>xy</sub>; "
                + "u<sub>xy</sub>, u<sub>yy</sub>]</div>"
                + "<p class='range'><b>Recommended range:</b> 1&ndash;50. "
                + "<b>Default:</b> 10.</p>"
                + seeAlso(
                "fidelity|Fidelity (&alpha;)",
                "sparsity|Sparsity (&gamma;)",
                "htvModel|HTV energy functional");
    }

    private String bodySparsity() {
        return "<p>The <b>sparsity weight (&gamma;)</b> pushes low-intensity background "
                + "pixels toward exactly zero, creating a sparse and clean background.</p>"
                + "<ul>"
                + "<li><b>Increase &gamma;</b> — background becomes cleaner and darker, "
                + "but weak signals may also shrink.</li>"
                + "<li><b>Decrease &gamma;</b> — weak structures are kept more easily, "
                + "but the background may remain elevated.</li>"
                + "</ul>"
                + "<div class='formula'>Sparsity regularizer: &nbsp; &gamma; &#8214;u&#8214;"
                + "<sub>1</sub></div>"
                + "<p class='range'><b>Recommended range:</b> 1&ndash;50. "
                + "<b>Default:</b> 15.</p>"
                + seeAlso(
                "fidelity|Fidelity (&alpha;)",
                "hessian|Hessian (&beta;)",
                "htvModel|HTV energy functional");
    }

    private String bodyHtvModel() {
        return "<p>The HTV denoising model minimizes an energy functional with three "
                + "complementary terms:</p>"
                + "<div class='formula'>E(u) = (&alpha;/2) &#8214;u &minus; f&#8214;"
                + "<sub>2</sub><sup>2</sup> + &beta; &#8214;H(u)&#8214;"
                + "<sub>*</sub> + &gamma; &#8214;u&#8214;<sub>1</sub></div>"
                + "<ul>"
                + "<li><b>Fidelity term (&alpha;):</b> keeps the estimate <i>u</i> close "
                + "to the observed image <i>f</i>.</li>"
                + "<li><b>Hessian regularizer (&beta;):</b> penalizes curvature and "
                + "favors piecewise-affine images.</li>"
                + "<li><b>Sparsity regularizer (&gamma;):</b> favors images with many "
                + "zero or near-zero background pixels.</li>"
                + "</ul>"
                + "<p>Changing one weight changes its influence relative to the other "
                + "two terms. For example, increasing &alpha; increases data fidelity, "
                + "while the same visual effect could also be seen as reducing &beta; "
                + "and &gamma;.</p>"
                + seeAlso(
                "fidelity|Fidelity (&alpha;)",
                "hessian|Hessian (&beta;)",
                "sparsity|Sparsity (&gamma;)")
                + references("<a href='https://doi.org/10.1038/s41587-021-01092-2'>"
                + "Zhao, W. et al. Sparse deconvolution improves the resolution "
                + "of live-cell super-resolution fluorescence microscopy. "
                + "Nat. Biotechnol. (2021). doi:10.1038/s41587-021-01092-2</a>");
    }

    // ---------- Stopping criteria pages ----------

    private String bodyRelChange() {
        return "<p><b>Relative change (&epsilon;)</b> is a practical engineering "
                + "stopping criterion. After each iteration, the solver computes:</p>"
                + "<div class='formula'>relative change = &#8214;u<sup>k+1</sup> "
                + "&minus; u<sup>k</sup>&#8214; / (&#8214;u<sup>k</sup>&#8214; + 1e-10)</div>"
                + "<p>When this value falls below &epsilon;, the solver stops early.</p>"
                + "<ul>"
                + "<li><b>Smaller &epsilon;</b> — tighter convergence, usually a better "
                + "result, but longer runtime.</li>"
                + "<li><b>Larger &epsilon;</b> — faster stopping, but some residual "
                + "noise or blur may remain.</li>"
                + "</ul>"
                + "<p class='range'><b>Recommended range:</b> 0.01&ndash;0.001. "
                + "<b>Default:</b> 0.005.</p>"
                + seeAlso("maxIterations|Max iterations (N<sub>max</sub>)");
    }

    private String bodyMaxIterations() {
        return "<p><b>Maximum iteration limit (N<sub>max</sub>)</b> is the hard upper "
                + "bound for the solver loop. The algorithm always stops after at most "
                + "N<sub>max</sub> iterations, even if the relative-change criterion "
                + "has not been reached.</p>"
                + "<ul>"
                + "<li><b>Increase N<sub>max</sub></b> — allows longer convergence; "
                + "useful for low-SNR images, but runtime increases.</li>"
                + "<li><b>Decrease N<sub>max</sub></b> — faster, but the solver may "
                + "stop before reaching the desired convergence.</li>"
                + "</ul>"
                + "<p class='range'><b>Recommended range:</b> 50&ndash;100. "
                + "<b>Default:</b> 75.</p>"
                + seeAlso("relChange|Relative change (&epsilon;)");
    }

    // ---------- Solver mode page ----------

    private String bodyStableMode() {
        return "<p><b>Stable mode</b> selects the update rule used in the sparsity "
                + "shrinkage step of the ADMM solver.</p>"
                + "<ul>"
                + "<li><b>Unchecked (default):</b> uses the mathematically optimal "
                + "proximal formula. This is faster and is recommended for most data.</li>"
                + "<li><b>Checked:</b> uses the original stable variant from the HTV "
                + "implementation. It is slower, but can be more stable and may keep "
                + "slightly more fine detail in challenging low-SNR data.</li>"
                + "</ul>"
                + "<p>Enable this only when the default result is visually unstable or "
                + "when you prefer more conservative convergence.</p>"
                + references("<a href='https://doi.org/10.1038/s41587-021-01092-2'>"
                + "Zhao, W. et al. Sparse deconvolution improves the resolution "
                + "of live-cell super-resolution fluorescence microscopy. "
                + "Nat. Biotechnol. (2021). doi:10.1038/s41587-021-01092-2</a>");
    }
}