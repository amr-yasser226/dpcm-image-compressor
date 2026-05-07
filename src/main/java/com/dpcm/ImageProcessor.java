package com.dpcm;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * Image I/O utilities for the DPCM codec.
 * <p>
 * Reads any image format supported by ImageIO, converts to grayscale using
 * the ITU-R BT.601 luminance formula, and exposes pixels as a 2-D int array
 * indexed [row][col] with values in [0, 255].
 */
public class ImageProcessor {

    /**
     * Reads an image file and returns a 2-D grayscale pixel array [row][col].
     * Applies ITU-R BT.601: gray = 0.299R + 0.587G + 0.114B.
     *
     * @param path absolute or relative path to an image file (PNG, JPEG, BMP …)
     * @return     2-D int array of grayscale values in [0, 255]
     * @throws IOException if the file cannot be read or decoded
     */
    public static int[][] readGrayscale(String path) throws IOException {
        BufferedImage img = ImageIO.read(new File(path));
        if (img == null) {
            throw new IOException("Cannot read image (unsupported format or file missing): " + path);
        }
        int h = img.getHeight();
        int w = img.getWidth();
        int[][] pixels = new int[h][w];

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int rgb = img.getRGB(j, i);
                int r   = (rgb >> 16) & 0xFF;
                int g   = (rgb >>  8) & 0xFF;
                int b   =  rgb        & 0xFF;
                pixels[i][j] = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
            }
        }
        return pixels;
    }

    /**
     * Writes a 2-D grayscale pixel array to a PNG file.
     * Parent directories are created automatically if they do not exist.
     *
     * @param pixels 2-D int array of grayscale values (values are clamped to [0, 255])
     * @param path   output file path (should end with ".png")
     * @throws IOException if the file cannot be written
     */
    public static void writeGrayscale(int[][] pixels, String path) throws IOException {
        int h = pixels.length;
        int w = pixels[0].length;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int v   = clamp(pixels[i][j]);
                int rgb = (v << 16) | (v << 8) | v;
                img.setRGB(j, i, rgb);
            }
        }

        File out = new File(path);
        if (out.getParentFile() != null) {
            out.getParentFile().mkdirs();
        }
        ImageIO.write(img, "png", out);
    }

    /**
     * Returns the width of a pixel array (number of columns).
     */
    public static int width(int[][] pixels) {
        return pixels[0].length;
    }

    /**
     * Returns the height of a pixel array (number of rows).
     */
    public static int height(int[][] pixels) {
        return pixels.length;
    }

    /**
     * Clamps a pixel value to the valid range [0, 255].
     *
     * @param v unclamped pixel value
     * @return  clamped value in [0, 255]
     */
    public static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
