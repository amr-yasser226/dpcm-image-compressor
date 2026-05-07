package com.dpcm;

/**
 * Quality and compression metrics for DPCM evaluation.
 *
 * <p>Three metrics are computed:
 * <ul>
 *   <li><b>MSE</b> - Mean Squared Error between original and reconstructed pixels.
 *   <li><b>PSNR</b> - Peak Signal-to-Noise Ratio (dB), derived from MSE.
 *   <li><b>Compression Ratio</b> - original bits per pixel (8) divided by
 *       encoded bits per pixel (ceil(log2 L)).
 * </ul>
 */
public class Metrics {

    /**
     * Computes the Mean Squared Error (MSE) between two grayscale pixel arrays
     * of identical dimensions.
     *
     * <pre>
     *   MSE = (1 / N) * sum (original[i][j] - reconstructed[i][j])^2
     * </pre>
     *
     * @param original      original pixel array
     * @param reconstructed reconstructed pixel array
     * @return              MSE value (0 = lossless)
     */
    public static double mse(int[][] original, int[][] reconstructed) {
        int h = original.length;
        int w = original[0].length;
        double sum = 0.0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                double d = original[i][j] - reconstructed[i][j];
                sum += d * d;
            }
        }
        return sum / ((double) h * w);
    }

    /**
     * Computes the Peak Signal-to-Noise Ratio (PSNR) in decibels.
     *
     * <pre>
     *   PSNR = 10 * log10(255^2 / MSE)
     * </pre>
     *
     * Returns {@link Double#POSITIVE_INFINITY} when MSE = 0 (lossless).
     *
     * @param mse mean squared error
     * @return    PSNR in dB
     */
    public static double psnr(double mse) {
        if (mse == 0.0) return Double.POSITIVE_INFINITY;
        return 10.0 * Math.log10(255.0 * 255.0 / mse);
    }

    /**
     * Computes the compression ratio for a given number of quantization levels.
     *
     * <pre>
     *   Original : 8 bits/pixel
     *   Encoded  : ceil(log2 L) bits/pixel
     *   CR       = 8 / ceil(log2 L)
     * </pre>
     *
     * @param levels number of quantization levels L
     * @return       compression ratio (> 1 means compressed)
     */
    public static double compressionRatio(int levels) {
        return 8.0 / Quantizer.bitsPerIndex(levels);
    }

    /**
     * Computes the encoded file size in bytes for an image of given dimensions.
     *
     * @param width  image width  in pixels
     * @param height image height in pixels
     * @param levels number of quantization levels L
     * @return       encoded size in bytes (rounded up to the nearest byte)
     */
    public static long encodedSizeBytes(int width, int height, int levels) {
        long totalBits = (long) width * height * Quantizer.bitsPerIndex(levels);
        return (totalBits + 7) / 8;
    }
}
