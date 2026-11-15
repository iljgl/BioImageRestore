package org.DenoiseStorm.help;

/**
 * HTML help content for the Geodesic Filter plugin.
 *
 * <p>All pages are in plain English. Internal links use the same topic
 * strings as the UI help buttons, so navigation stays inside the
 * HelpBrowser window.
 */
public class GeodesicHelpPages implements HelpPages {

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
            case "windowSize":
                return page("Window size", bodyWindowSize());
            case "alpha":
                return page("Alpha (&alpha;)", bodyAlpha());
            case "sigma":
                return page("Sigma (&sigma;)", bodySigma());
            case "geodesicPrinciple":
                return page("Geodesic filtering principle", bodyGeodesicPrinciple());
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

    private String bodyWindowSize() {
        return "<p>The <b>window size</b> defines the local square neighborhood (in pixels) "
                + "around each pixel that is used to compute the weighted average. "
                + "Any even value is automatically adjusted to the next odd number "
                + "because the window is symmetric about the center pixel.</p>"
                + "<ul>"
                + "<li><b>Increase</b> — more neighboring pixels contribute to the "
                + "average, which suppresses noise more strongly. However, if the window "
                + "becomes larger than the structures you want to preserve, details may "
                + "be blurred.</li>"
                + "<li><b>Decrease</b> — finer details and edges are retained, but "
                + "noise may not be reduced sufficiently.</li>"
                + "</ul>"
                + "<p>A good starting point is to choose a size comparable to, or slightly "
                + "larger than, the typical feature size of the objects you care about.</p>"
                + "<p class='range'><b>Default:</b> 7. <b>Recommended range:</b> 3&ndash;21.</p>"
                + seeAlso("alpha|Alpha (&alpha;)",
                "sigma|Sigma (&sigma;)",
                "geodesicPrinciple|Geodesic filtering principle");
    }

    private String bodyAlpha() {
        return "<p><b>Alpha (&alpha;)</b> balances the influence of spatial distance and "
                + "pixel intensity difference when computing local distances.</p>"
                + "<ul>"
                + "<li><b>Increase &alpha;</b> — intensity differences become more "
                + "important. The filter is less likely to average across edges, which "
                + "preserves boundaries but may also preserve noise because small "
                + "intensity fluctuations are treated as meaningful.</li>"
                + "<li><b>Decrease &alpha;</b> — spatial proximity dominates, making the "
                + "filter behave more like a standard Gaussian blur. Noise is reduced "
                + "more aggressively but edges may become softened.</li>"
                + "</ul>"
                + "<p>Images are normalized to the range [0,1] internally, so a reasonable "
                + "starting value is <b>&alpha; = 5.0</b>.</p>"
                + "<p class='range'><b>Default:</b> 5.0. <b>Recommended range:</b> 1&ndash;10.0.</p>"
                + seeAlso("windowSize|Window size",
                "sigma|Sigma (&sigma;)",
                "geodesicPrinciple|Geodesic filtering principle");
    }

    private String bodySigma() {
        return "<p><b>Sigma (&sigma;)</b> is the distance scale parameter that controls "
                + "how quickly the filter weights decay as geodesic distance increases.</p>"
                + "<ul>"
                + "<li><b>Increase &sigma;</b> — weights decrease more slowly, so pixels "
                + "further from the center still contribute. This gives stronger smoothing "
                + "but may blur edges.</li>"
                + "<li><b>Decrease &sigma;</b> — only pixels very close in geodesic "
                + "distance matter, so edges are kept sharp but noise reduction is weaker.</li>"
                + "</ul>"
                + "<p>Physically, &sigma; sets the effective “range” of influence: similar "
                + "to the standard deviation in a Gaussian kernel, it determines how far "
                + "the similarity information is propagated.</p>"
                + "<p class='range'><b>Default:</b> 1.5. <b>Recommended range:</b> 1.0&ndash;2.0.</p>"
                + seeAlso("windowSize|Window size",
                "alpha|Alpha (&alpha;)",
                "geodesicPrinciple|Geodesic filtering principle");
    }

    private String bodyGeodesicPrinciple() {
        return "<p><b>Geodesic filtering</b> is an edge‑preserving smoothing technique. "
                + "Unlike a standard Gaussian blur, which treats all neighbors equally, "
                + "it computes the distance between two pixels along the “surface” of the "
                + "image.</p>"
                + "<p>Imagine the image intensity as a landscape: valleys are dark areas, "
                + "peaks are bright areas. A geodesic path is the shortest route over this "
                + "landscape. If two pixels are on opposite sides of an edge, the path "
                + "must climb the slope, making it longer than the straight spatial "
                + "distance. Therefore, weights drop sharply across edges, preventing "
                + "the filter from mixing signals on either side.</p>"
                + "<p>In flat regions (similar intensities), the geodesic distance is "
                + "almost the same as the spatial distance, so the filter behaves like a "
                + "regular Gaussian blur and reduces noise effectively.</p>"
                + "<p>This balance between spatial proximity and intensity similarity is "
                + "what preserves edges while smoothing noise.</p>"
                + seeAlso("windowSize|Window size",
                "alpha|Alpha (&alpha;)",
                "sigma|Sigma (&sigma;)")
                + references("<a href='https://doi.org/10.3390/jimaging11050167'>"
                + "Boulanger, P., & Bin Hasan, S. (2025). Comparing Geodesic "
                + "Filtering to State-of-the-Art Algorithms: A Comprehensive "
                + "Study and CUDA Implementation. Journal of Imaging, 11(5), 167. "
                + "doi:10.3390/jimaging11050167</a>");
    }
}