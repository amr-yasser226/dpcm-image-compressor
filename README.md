# 2-D Feed Backward Predictive Coding (DPCM)

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.java.com)

A Java implementation of **Differential Pulse Code Modulation (DPCM)** for grayscale image compression. This project implements a 2-D feed-backward predictive coding system with multiple predictor types and uniform scalar quantization. Developed as part of the **DSAI 325 — Introduction to Information Theory** course at Zewail City of Science and Technology.

## Algorithm Overview

DPCM reduces the data rate by predicting the current pixel value from its neighbors and transmitting only the quantized prediction error (residual). The "feed-backward" design ensures the predictor uses reconstructed values, keeping the encoder and decoder in perfect synchronization without extra metadata.

### Predictor Strategies

1.  **Order-1 (Horizontal)**: $P(i,j) = \hat{X}(i, j-1)$
2.  **Order-2 (Plane)**: $P(i,j) = \hat{X}(i, j-1) + \hat{X}(i-1, j) - \hat{X}(i-1, j-1)$
3.  **Adaptive (LOCO-A)**: Edge-detecting predictor used in JPEG-LS.

### Compression Pipeline

```
Original Image -> Grayscale Conversion -> DPCM Encoding -> Quantized Residuals
                                              ^               |
                                              |               v
                                         Predictor <- Dequantization
```

## Features

- **Multiple Predictors**: Choose between Order-1, Order-2, and Adaptive (LOCO-A) strategies.
- **Configurable Quantization**: Test with 8, 16, 32, or custom levels.
- **Grayscale Processing**: Automated ITU-R BT.601 luminance conversion.
- **Batch Analysis**: Automatically sweep across parameters to generate performance tables.
- **Metrics**: Automated calculation of MSE, PSNR, and Compression Ratio.
- **Scientific Report**: Comprehensive LaTeX documentation included.

## Project Structure

```
dpcm-image-compressor/
├── src/
│   └── main/java/com/dpcm/
│       ├── DPCMCodec.java        # Main CLI & Pipeline
│       ├── ImageProcessor.java   # Image I/O & Grayscale
│       ├── Predictor.java        # Prediction Logic
│       ├── Quantizer.java        # Uniform Quantization
│       └── Metrics.java          # Performance Metrics
├── docs/
│   ├── report.tex                # LaTeX Source
│   └── report.pdf                # Compiled Report
├── testcases/
│   ├── images/                   # Synthetic Test Images
│   └── results/                  # Reconstructed Outputs
├── scripts/
│   ├── generate_test_images.py   # Test Data Generator
│   └── run_tests.sh              # Batch Execution Script
├── README.md
├── LICENSE
└── .gitignore
```

## How to Run

### 1. Generate Test Images
```bash
python3 scripts/generate_test_images.py
```

### 2. Compile and Run
```bash
javac -d out src/main/java/com/dpcm/*.java
java -cp out com.dpcm.DPCMCodec
```

### 3. Automated Batch Testing
```bash
bash scripts/run_tests.sh
```

## Performance Results (Portrait Image)

| Predictor | Levels | MSE | PSNR (dB) | Ratio |
| :--- | :--- | :--- | :--- | :--- |
| Order-1 | 8 | 331.37 | 22.93 | 2.67x |
| Order-1 | 32 | 19.08 | 35.32 | 1.60x |
| Order-2 | 32 | 19.89 | 35.15 | 1.60x |
| Adaptive | 32 | 19.08 | 35.32 | 1.60x |

## Academic Context

- **Course**: DSAI 325 — Introduction to Information Theory
- **Institution**: Zewail City of Science and Technology
- **Semester**: Spring 2026

## License

This project is licensed under the [MIT License](LICENSE).
