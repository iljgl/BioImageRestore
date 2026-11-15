package org.DenoiseStorm.htvdenoise.core;

import org.DenoiseStorm.htvdenoise.math.FFTOps;
import org.DenoiseStorm.htvdenoise.math.IterativeShrinkage;

public class SparseHessianCore {

    // Return result class, including the processed image, whether it meets the
    // ZeroPercent standard, and the actual ZeroPercent value of the returned image.
    public static class ProcessResult {
        public float[][] image;
        public boolean zeroPercentReached;
        public float zeroPercentValue;

        public ProcessResult(float[][] image, boolean zeroPercentReached, float zeroPercentValue) {
            this.image = image;
            this.zeroPercentReached = zeroPercentReached;
            this.zeroPercentValue = zeroPercentValue;
        }
    }

    // Main processing method: New parameters "useZeroPercent" and "zeroPercent" have been added.
    public static ProcessResult process(float[][] f, float fidelity, float hessian, float sparsity,
                                        float mu, int maxIter, int stable, float relChange,
                                        boolean useZeroPercent, float zeroPercent) {
        int h = f.length;
        int w = f[0].length;

        // normalization
        float recordMax = 0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (f[y][x] > recordMax) recordMax = f[y][x];

        float[][] fNorm = new float[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                fNorm[y][x] = f[y][x] / recordMax;

        // initialization variable
        float[][] bxx = new float[h][w];
        float[][] byy = new float[h][w];
        float[][] bxy = new float[h][w];
        float[][] bl1 = new float[h][w];
        float[][] g = FFTOps.copy(fNorm); // Initial solution: g = f
        float[][] prevG;

        // Pre-computed FFT frequency domain kernel (Normalization denominator)
        float[][] xxFFT = FFTOps.operationXX(w, h);
        float[][] yyFFT = FFTOps.operationYY(w, h);
        float[][] xyFFT = FFTOps.operationXY(w, h);

        float[][] denom = new float[h][w];// denominator
        float fidelityOverMu = fidelity / mu;
        float p1Sq = sparsity * sparsity;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                denom[y][x] = fidelityOverMu + p1Sq + xxFFT[y][x] + yyFFT[y][x] + 2  * xyFFT[y][x];
            }
        }

        // main loop
        boolean converged = false;
        boolean zeroPercentReached = false;
        float near_rel = 3 * relChange;
        float actualZeroPercent = 0.0f;

        for (int iter = 0; iter < maxIter; iter++) {
            prevG = g;

            // Construct the g_update
            float[][] gUpdate = FFTOps.multiply(fNorm, fidelityOverMu);

            // Iterative update
            IterativeShrinkage.IterResult xxRes = IterativeShrinkage.iterXX(g, bxx, hessian, 1.0f, mu);
            bxx = xxRes.b;
            gUpdate = FFTOps.add(gUpdate, xxRes.L);

            IterativeShrinkage.IterResult yyRes = IterativeShrinkage.iterYY(g, byy, hessian, 1.0f, mu);
            byy = yyRes.b;
            gUpdate = FFTOps.add(gUpdate, yyRes.L);

            IterativeShrinkage.IterResult xyRes = IterativeShrinkage.iterXY(g, bxy, hessian, 2.0f, mu);
            bxy = xyRes.b;
            gUpdate = FFTOps.add(gUpdate, xyRes.L);

            IterativeShrinkage.IterResult spRes = IterativeShrinkage.iterSparse(g, bl1, sparsity, stable, mu);
            bl1 = spRes.b;
            gUpdate = FFTOps.add(gUpdate, spRes.L);

            // Frequency domain solution
            g = FFTOps.solveInFourierDomain(gUpdate, denom);

            // Convergence check
            if (iter > 5) {
                float diff = FFTOps.norm(FFTOps.subtract(g, prevG)) / (FFTOps.norm(prevG) + 1e-10f);

                if (diff < near_rel) {
                    stable = 1;
                }

                // Prioritize the inspection of the ZeroPercent convergence criterion
                if (useZeroPercent) {
                    // perform post-processing to obtain finalG
                    float[][] finalG = postProcess(g, recordMax);

                    // Check the pixel ratios of 0, 1, and 2 in whole image.
                    actualZeroPercent = computeZeroPercent(finalG);
                    zeroPercentReached = actualZeroPercent >= zeroPercent;
                    if (zeroPercentReached) {
                        converged = true;
                        break;
                    }
                }
                // If ZeroPercent is not used or the condition is not met, then check the convergence criteria related to the relative change.
                else if (diff < relChange) {
                    converged = true;
                    break;
                }
            }
        }

//        if (!converged) {
//            System.out.println("Reached maximum iteration (" + maxIter + ").");
//        }

        // Final post-processing
        float[][] finalG = postProcess(g, recordMax);
        actualZeroPercent = computeZeroPercent(finalG);

        return new ProcessResult(finalG, zeroPercentReached, actualZeroPercent);
    }

    // Post-processing methods: truncation, normalization, inverse normalization
    private static float[][] postProcess(float[][] g, float recordMax) {
        int h = g.length;
        int w = g[0].length;

        // truncation
        float[][] middleG = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                middleG[y][x] = Math.max(0, g[y][x]);
            }
        }

        // normalization
        float gMax = 0;
        float gMin = middleG[0][0];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (middleG[y][x] > gMax) gMax = middleG[y][x];
                if (middleG[y][x] < gMin) gMin = middleG[y][x];
            }
        }
        float gMax_min0 = gMax - gMin;
        float gMax_min = Math.max(gMax_min0, 1e-6f);

        // inverse normalization
        float[][] finalG = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                finalG[y][x] = (middleG[y][x] - gMin) * recordMax / gMax_min;
            }
        }

        return finalG;
    }

    // Compute the fraction of background-like pixels (rounded to 0, 1 or 2).
    private static float computeZeroPercent(float[][] finalG) {
        int h = finalG.length;
        int w = finalG[0].length;
        int pixelNum = h * w;

        int count0 = 0;
        int count1 = 0;
        int count2 = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Check after rounding off
                int val = Math.round(finalG[y][x]);
                if (val == 0) count0++;
                else if (val == 1) count1++;
                else if (val == 2) count2++;
            }
        }

        return (count0 + count1 + count2) / (float) pixelNum;
    }
}