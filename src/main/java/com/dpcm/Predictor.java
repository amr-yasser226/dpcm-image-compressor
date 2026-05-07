package com.dpcm;

/**
 * 2-D Feed Backward Predictor strategies for DPCM.
 *
 * <p>All predictors operate on the <em>reconstructed</em> pixel buffer, not
 * on the original image.  This guarantees that the decoder can reproduce the
 * identical prediction sequence using only the quantized residuals — no side
 * channel to the original is required.
 *
 * <p>Neighbour notation (standard DPCM convention):
 * <pre>
 *   c  b
 *   a  X          a = left,  b = above,  c = upper-left,  X = current
 * </pre>
 *
 * <p>Boundary pixels that lack a neighbour fall back to 128 (mid-grey) or
 * to the nearest available neighbour, avoiding edge-case biases.
 */
public class Predictor {

    // ─── Predictor type enum ────────────────────────────────────────────────

    public enum Type {

        /** Order-1: horizontal predictor — P(i,j) = a */
        ORDER_1("Order-1"),

        /**
         * Order-2: plane predictor — P(i,j) = a + b − c
         * <p>Equivalent to fitting a plane through the three known neighbours.
         */
        ORDER_2("Order-2"),

        /**
         * Adaptive LOCO-A predictor (JPEG-LS, ISO 14495-1).
         * <pre>
         *   if   c &ge; max(a, b) → P = min(a, b)
         *   elif c &le; min(a, b) → P = max(a, b)
         *   else                  → P = a + b − c
         * </pre>
         */
        ADAPTIVE("Adaptive");

        private final String label;

        Type(String label) { this.label = label; }

        public String getLabel() { return label; }

        /**
         * Resolves a 1-based integer (1, 2, 3) to the corresponding {@link Type}.
         */
        public static Type fromInt(int i) {
            switch (i) {
                case 1: return ORDER_1;
                case 2: return ORDER_2;
                case 3: return ADAPTIVE;
                default: throw new IllegalArgumentException(
                    "Predictor type must be 1, 2, or 3.  Got: " + i);
            }
        }
    }

    // ─── Core prediction method ─────────────────────────────────────────────

    /**
     * Computes the predicted pixel value at position (i, j).
     *
     * @param rec  reconstructed pixel buffer — only positions already visited
     *             (row-major scan order before (i, j)) contain valid values
     * @param i    current row
     * @param j    current column
     * @param type predictor strategy to apply
     * @return     predicted value (already clamped to [0, 255] where required)
     */
    public static int predict(int[][] rec, int i, int j, Type type) {
        switch (type) {

            // ── Order-1: use left neighbour ──────────────────────────────────
            case ORDER_1:
                return (j > 0) ? rec[i][j - 1] : 128;

            // ── Order-2: plane predictor ─────────────────────────────────────
            case ORDER_2:
                if (i == 0 && j == 0) return 128;
                if (i == 0)           return rec[i][j - 1];
                if (j == 0)           return rec[i - 1][j];
                return ImageProcessor.clamp(
                    rec[i][j - 1] + rec[i - 1][j] - rec[i - 1][j - 1]);

            // ── Adaptive LOCO-A (JPEG-LS) ────────────────────────────────────
            case ADAPTIVE: {
                if (i == 0 && j == 0) return 128;
                if (i == 0)           return rec[i][j - 1];
                if (j == 0)           return rec[i - 1][j];

                int a = rec[i][j - 1];          // left
                int b = rec[i - 1][j];          // above
                int c = rec[i - 1][j - 1];      // upper-left

                if      (c >= Math.max(a, b)) return Math.min(a, b);
                else if (c <= Math.min(a, b)) return Math.max(a, b);
                else                          return a + b - c;
            }

            default:
                return 128;
        }
    }
}
