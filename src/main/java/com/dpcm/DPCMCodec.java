package com.dpcm;

import java.io.*;
import java.util.Scanner;

/**
 * 2-D Feed Backward Predictive Coding (DPCM) - Main CLI entry point.
 *
 * <p>Compile:
 * <pre>  javac -d out src/main/java/com/dpcm/*.java</pre>
 *
 * <p>Execute:
 * <pre>  java -cp out com.dpcm.DPCMCodec</pre>
 *
 * <p>The encoder and decoder both operate in feed-backward mode: the
 * predictor reads from the <em>reconstructed</em> pixel buffer rather than
 * the original, so the decoder can reproduce the exact same predictions
 * using only the transmitted quantized residuals.
 */
public class DPCMCodec {

    // --- Encoder -----------------------------------------------------------

    /**
     * Encodes a grayscale image using 2-D DPCM.
     *
     * <p>For each pixel in raster-scan order:
     * <ol>
     *   <li>Predict the pixel value from already-reconstructed neighbours.
     *   <li>Compute the residual: error = original - predicted.
     *   <li>Quantize the residual to a bin index.
     *   <li>Dequantize and add back to the predictor to get the reconstructed value.
     * </ol>
     *
     * @param original       original grayscale pixel array [h][w]
     * @param type           predictor strategy
     * @param levels         number of quantization levels L
     * @param reconstructed  output buffer; filled with feed-backward reconstructed pixels
     * @return               2-D array of quantized residual indices [h][w]
     */
    public static int[][] encode(int[][] original, Predictor.Type type,
                                 int levels, int[][] reconstructed) {
        int h = original.length;
        int w = original[0].length;
        int[][] quantized = new int[h][w];

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int    pred   = Predictor.predict(reconstructed, i, j, type);
                int    error  = original[i][j] - pred;
                int    qIdx   = Quantizer.quantize(error, levels);
                double recErr = Quantizer.dequantize(qIdx, levels);

                quantized[i][j]      = qIdx;
                reconstructed[i][j]  = ImageProcessor.clamp(
                        (int) Math.round(pred + recErr));
            }
        }
        return quantized;
    }

    // --- Decoder -----------------------------------------------------------

    /**
     * Decodes a 2-D array of quantized residual indices back to a grayscale
     * pixel array, using the identical feed-backward predictor as the encoder.
     *
     * @param quantized quantized residual indices [h][w]
     * @param type      predictor strategy (must match encoder)
     * @param levels    number of quantization levels L (must match encoder)
     * @return          reconstructed grayscale pixel array [h][w]
     */
    public static int[][] decode(int[][] quantized, Predictor.Type type, int levels) {
        int h = quantized.length;
        int w = quantized[0].length;
        int[][] reconstructed = new int[h][w];

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int    pred   = Predictor.predict(reconstructed, i, j, type);
                double recErr = Quantizer.dequantize(quantized[i][j], levels);
                reconstructed[i][j] = ImageProcessor.clamp(
                        (int) Math.round(pred + recErr));
            }
        }
        return reconstructed;
    }

    // --- Single-image run --------------------------------------------------

    static void runSingle(String imgPath, Predictor.Type type,
                          int levels, String outPath) {
        try {
            int[][] original     = ImageProcessor.readGrayscale(imgPath);
            int h = ImageProcessor.height(original);
            int w = ImageProcessor.width(original);
            int[][] reconstructed = new int[h][w];

            int[][] quantized = encode(original, type, levels, reconstructed);
            int[][] decoded   = decode(quantized, type, levels);

            ImageProcessor.writeGrayscale(decoded, outPath);

            double mse   = Metrics.mse(original, decoded);
            double psnr  = Metrics.psnr(mse);
            double ratio = Metrics.compressionRatio(levels);
            long origBytes = (long) h * w;
            long encBytes  = Metrics.encodedSizeBytes(w, h, levels);

            System.out.println("\n=== DPCM Results ===");
            System.out.printf("Image            : %s  (%d x %d)%n", imgPath, w, h);
            System.out.printf("Predictor        : %s%n", type.getLabel());
            System.out.printf("Quantiz. levels  : %d  (%d bits/pixel)%n",
                              levels, Quantizer.bitsPerIndex(levels));
            System.out.printf("Original size    : %,d bytes%n", origBytes);
            System.out.printf("Encoded size     : %,d bytes%n", encBytes);
            System.out.printf("Compression ratio: %.4fx%n", ratio);
            System.out.printf("MSE              : %.4f%n", mse);
            System.out.printf("PSNR             : %.2f dB%n", psnr);
            System.out.printf("Reconstructed    : %s%n", outPath);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // --- Batch analysis ----------------------------------------------------

    static void runBatch(String imgPath, String resultsDir) {
        int[]            levelSet = {8, 16, 32};
        Predictor.Type[] types    = {
            Predictor.Type.ORDER_1,
            Predictor.Type.ORDER_2,
            Predictor.Type.ADAPTIVE
        };

        try {
            int[][] original = ImageProcessor.readGrayscale(imgPath);
            int h = ImageProcessor.height(original);
            int w = ImageProcessor.width(original);
            String base = new File(imgPath).getName().replaceFirst("[.][^.]+$", "");

            new File(resultsDir).mkdirs();

            System.out.printf("%n%-12s %-8s %10s %10s %10s %14s %14s%n",
                "Predictor", "Levels", "MSE", "PSNR(dB)", "Ratio",
                "Orig(B)", "Enc(B)");
            System.out.println("-".repeat(82));

            for (Predictor.Type type : types) {
                for (int levels : levelSet) {
                    int[][] rec      = new int[h][w];
                    int[][] quantized = encode(original, type, levels, rec);
                    int[][] decoded   = decode(quantized, type, levels);

                    double mse   = Metrics.mse(original, decoded);
                    double psnr  = Metrics.psnr(mse);
                    double ratio = Metrics.compressionRatio(levels);
                    long origBytes = (long) h * w;
                    long encBytes  = Metrics.encodedSizeBytes(w, h, levels);

                    String outName = String.format("%s/%s_%s_L%d.png",
                        resultsDir, base, type.name().toLowerCase(), levels);
                    ImageProcessor.writeGrayscale(decoded, outName);

                    System.out.printf("%-12s %-8d %10.4f %10.2f %10.4fx %14d %14d%n",
                        type.getLabel(), levels, mse, psnr, ratio, origBytes, encBytes);
                }
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // --- Main menu ---------------------------------------------------------

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n+------------------------------------------+");
            System.out.println("|   2-D Feed Backward DPCM Codec           |");
            System.out.println("|   DSAI 325  -  Spring 2026               |");
            System.out.println("+------------------------------------------+");
            System.out.println("|  1. Encode & Decode Image                |");
            System.out.println("|  2. Batch Analysis (all combos)          |");
            System.out.println("|  3. Exit                                 |");
            System.out.println("+------------------------------------------+");
            System.out.print("Choice: ");
            String ch = sc.nextLine().trim();

            if (ch.equals("1")) {
                System.out.print("Input image path: ");
                String imgPath = sc.nextLine().trim();

                System.out.print("Predictor type (1=Order-1, 2=Order-2, 3=Adaptive): ");
                int ptInt = Integer.parseInt(sc.nextLine().trim());
                Predictor.Type type = Predictor.Type.fromInt(ptInt);

                System.out.print("Quantization levels (e.g. 8, 16, 32): ");
                int levels = Integer.parseInt(sc.nextLine().trim());

                System.out.print("Output reconstructed image path: ");
                String outPath = sc.nextLine().trim();

                runSingle(imgPath, type, levels, outPath);

            } else if (ch.equals("2")) {
                System.out.print("Input image path: ");
                String imgPath = sc.nextLine().trim();

                System.out.print("Results directory (e.g. testcases/results): ");
                String resultsDir = sc.nextLine().trim();

                runBatch(imgPath, resultsDir);

            } else if (ch.equals("3")) {
                System.out.println("Goodbye.");
                break;

            } else {
                System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
        sc.close();
    }
}
