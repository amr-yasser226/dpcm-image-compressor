package com.dpcm;

/**
 * Uniform scalar quantizer for DPCM prediction residuals.
 *
 * <p>Prediction errors (residuals) lie in the integer range [-255, 255],
 * spanning 511 distinct values.  The range is partitioned into {@code L}
 * uniform bins of equal width:
 *
 * <pre>
 *   step  = 511.0 / L
 *   index = floor((error + 255) / step),  clamped to [0, L-1]
 *   error'= index * step + step/2 - 255   (bin midpoint reconstruction)
 * </pre>
 *
 * <p>The number of bits required to represent one quantized index is
 * {@code ceil(log2 L)}, which determines the compression ratio directly:
 * {@code CR = 8 / ceil(log2 L)}.
 */
public class Quantizer {

    /**
     * Maps a prediction residual to a quantization bin index.
     *
     * @param error  prediction residual in [-255, 255]
     * @param levels number of quantization levels L (e.g. 8, 16, 32)
     * @return       bin index in [0, L-1]
     */
    public static int quantize(double error, int levels) {
        double step = 511.0 / levels;
        int idx = (int) Math.floor((error + 255.0) / step);
        return Math.max(0, Math.min(levels - 1, idx));
    }

    /**
     * Reconstructs a residual value from its quantization bin index.
     * Returns the midpoint of the bin to minimise reconstruction error.
     *
     * @param idx    bin index in [0, L-1]
     * @param levels number of quantization levels L
     * @return       reconstructed residual (bin midpoint)
     */
    public static double dequantize(int idx, int levels) {
        double step = 511.0 / levels;
        return idx * step + step / 2.0 - 255.0;
    }

    /**
     * Returns the number of bits needed to store one quantized index.
     *
     * @param levels number of quantization levels L
     * @return       ceil(log2 L)
     */
    public static int bitsPerIndex(int levels) {
        return (int) Math.ceil(Math.log(levels) / Math.log(2));
    }
}
