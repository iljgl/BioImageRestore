package org.BioImageRestore.help;

/**
 * HTML help content for the Deconvolution plugin.
 *
 * <p>The help pages cover:
 * <ul>
 *   <li>Optical parameters (excitation wavelength, NA, pixel size, iterations)</li>
 *   <li>Choice of deconvolution algorithm (Richardson-Lucy vs. Accelerated Landweber)</li>
 *   <li>Optional output of the theoretical PSF image</li>
 * </ul>
 *
 * <p>All pages are in plain English. Internal links use the same topic
 * strings as the UI help buttons, so navigation stays inside the
 * HelpBrowser window. External links are provided as clickable URLs.
 */
public class DeconvolutionHelpPages implements HelpPages {

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
            case "exWavelength":
                return page("Excitation wavelength", bodyExWavelength());
            case "numericalAperture":
                return page("Numerical aperture (NA)", bodyNumericalAperture());
            case "pixelSize":
                return page("Pixel size", bodyPixelSize());
            case "iterations":
                return page("Iterations", bodyIterations());
            case "richardsonLucy":
                return page("Richardson-Lucy (Poisson)", bodyRichardsonLucy());
            case "acceleratedLandweber":
                return page("Accelerated Landweber (Gaussian)", bodyAcceleratedLandweber());
            case "showPsf":
                return page("Output theoretical PSF", bodyShowPsf());
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

    // ---------- recA: Optical parameter pages ----------

    private String bodyExWavelength() {
        return "<p>The <b>excitation wavelength</b> is the wavelength of the light used "
                + "to excite the sample, expressed in micrometers (&micro;m).</p>"
                + "<p>Typical values for fluorescence microscopy are:</p>"
                + "<ul>"
                + "<li>488 nm = 0.488 &micro;m (blue/green excitation)</li>"
                + "<li>561 nm = 0.561 &micro;m (yellow excitation)</li>"
                + "<li>647 nm = 0.647 &micro;m (red excitation)</li>"
                + "</ul>"
                + "<p>This parameter is not a tunable knob; it must be taken from your "
                + "experimental setup (laser or LED). It is used to calculate the "
                + "theoretical point spread function (PSF), because diffraction "
                + "depends on wavelength.</p>"
                + seeAlso("numericalAperture|Numerical aperture",
                "pixelSize|Pixel size",
                "iterations|Iterations");
    }

    private String bodyNumericalAperture() {
        return "<p>The <b>numerical aperture (NA)</b> of an objective lens defines "
                + "how much light it can collect and its resolving power.</p>"
                + "<p>A higher NA corresponds to a smaller PSF and better resolution. "
                + "Typical values range from 0.4 (low magnification) to 1.49 "
                + "(oil‑immersion TIRF objectives).</p>"
                + "<p>The NA is normally printed on the side of the objective. "
                + "This value is needed to compute the theoretical PSF for deconvolution.</p>"
                + seeAlso("exWavelength|Excitation wavelength",
                "pixelSize|Pixel size",
                "iterations|Iterations");
    }

    private String bodyPixelSize() {
        return "<p>The <b>pixel size</b> is the physical size of one pixel in the "
                + "image, expressed in micrometers (&micro;m).</p>"
                + "<p>It can be calculated as:</p>"
                + "<div class='formula'>pixel size (&micro;m) = camera pixel size (&micro;m) "
                + "/ total magnification</div>"
                + "<p>For example, a 6.5 &micro;m camera pixel with 150&times; total "
                + "magnification gives 6.5 / 150 &asymp; 0.0433 &micro;m per pixel.</p>"
                + "<p>This value allows the PSF to be correctly sampled onto the image grid.</p>"
                + seeAlso("exWavelength|Excitation wavelength",
                "numericalAperture|Numerical aperture",
                "iterations|Iterations");
    }

    private String bodyIterations() {
        return "<p>The <b>number of iterations</b> controls how many times the "
                + "deconvolution algorithm refines its estimate.</p>"
                + "<ul>"
                + "<li><b>Increase</b> — recovers finer details but may also amplify "
                + "noise and create artifacts (especially in low‑light images).</li>"
                + "<li><b>Decrease</b> — leaves the image closer to the original, with "
                + "less sharpening and less noise amplification.</li>"
                + "</ul>"
                + "<p>The default of 10 iterations is a reasonable starting point for "
                + "most images. You may need to increase it if the image is heavily "
                + "blurred, or decrease it if noise becomes objectionable.</p>"
                + seeAlso("exWavelength|Excitation wavelength",
                "numericalAperture|Numerical aperture",
                "pixelSize|Pixel size");
    }

    // ---------- recB: Algorithm pages ----------

    private String bodyRichardsonLucy() {
        return "<p><b>Richardson-Lucy (RL)</b> deconvolution assumes that the image "
                + "noise follows a <b>Poisson distribution</b>, which is natural for "
                + "photon‑counting detectors such as CCD/CMOS cameras in fluorescence "
                + "microscopy.</p>"
                + "<p>Key properties:</p>"
                + "<ul>"
                + "<li>Produces non‑negative results automatically.</li>"
                + "<li>Well suited for low‑light, noisy images.</li>"
                + "<li>Converges slowly but reliably for Poisson data.</li>"
                + "</ul>"
                + "<p>Choose RL when your image is dominated by photon noise (dim "
                + "samples, short exposures).</p>"
                + references("<a href='https://doi.org/10.1364/JOSA.62.000055'>"
                        + "Richardson, W. H. (1972). Bayesian-based iterative method of "
                        + "image restoration. Journal of the Optical Society of America, "
                        + "62(1), 55–59.</a>",
                "<a href='https://doi.org/10.1086/111605'>"
                        + "Lucy, L. B. (1974). An iterative technique for the rectification "
                        + "of observed distributions. The Astronomical Journal, 79, 745–754.</a>");
    }

    private String bodyAcceleratedLandweber() {
        return "<p><b>Accelerated Landweber</b> is an iterative method that assumes "
                + "the noise is approximately <b>Gaussian</b> (least‑squares fitting).</p>"
                + "<p>It is often faster than standard Landweber and is well suited for "
                + "images with higher signal‑to‑noise ratios where the noise can be "
                + "modeled as additive Gaussian.</p>"
                + "<p>Key properties:</p>"
                + "<ul>"
                + "<li>Faster convergence than classic Landweber.</li>"
                + "<li>Works well for bright images with moderate noise.</li>"
                + "<li>May introduce ringing if the image does not match the Gaussian "
                + "noise model.</li>"
                + "</ul>"
                + "<p>Choose this algorithm when your image is relatively bright and "
                + "the noise is not strongly Poisson.</p>"
                + references("<a href='https://doi.org/10.2307/2372313'>"
                + "Landweber, L. (1951). An iteration formula for Fredholm integral "
                + "equations of the first kind. American Journal of Mathematics, "
                + "73(3), 615–624.</a>");
    }

    // ---------- recC: PSF output page ----------

    private String bodyShowPsf() {
        return "<p>When this option is selected, the plugin will <b>also</b> output the "
                + "computed theoretical point spread function (PSF) as a separate 16‑bit "
                + "image.</p>"
                + "<p>The PSF image shows the shape and size of the blur kernel used "
                + "during deconvolution. This can be useful for verifying that the optical "
                + "parameters (wavelength, NA, pixel size) are correctly set.</p>"
                + "<p>By default this option is <b>unchecked</b> because most users only "
                + "need the deconvolved result.</p>";
    }
}