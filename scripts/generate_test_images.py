#!/usr/bin/env python3
"""
generate_test_images.py
───────────────────────
Generates synthetic grayscale PNG test images for the DPCM codec.

Images produced
───────────────
  test01_gradient.png      — smooth horizontal gradient (low residuals → ideal for DPCM)
  test02_checkerboard.png  — hard high-frequency pattern (stress test for predictors)
  test03_ramp_noise.png    — diagonal ramp with added Gaussian noise (mixed case)
  test04_lena_like.png     — portrait-like synthetic face texture (realistic scenario)

Run:  python3 scripts/generate_test_images.py
"""

import os
import math
import random

# ── minimal PNG writer (no external dependencies) ──────────────────────────────

import struct
import zlib

def _make_png(pixels: list[list[int]], path: str) -> None:
    """Write a 2-D list of uint8 values as an 8-bit grayscale PNG."""
    height = len(pixels)
    width  = len(pixels[0])

    def chunk(name: bytes, data: bytes) -> bytes:
        c = name + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

    raw = b""
    for row in pixels:
        raw += b"\x00" + bytes(row)

    png  = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 0, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)
    print(f"  Written: {path}  ({width}x{height})")


def _clamp(v: float) -> int:
    return max(0, min(255, int(round(v))))


# ── image generators ───────────────────────────────────────────────────────────

def gradient(w: int = 256, h: int = 256) -> list[list[int]]:
    """Smooth left-to-right horizontal gradient from 10 to 245."""
    return [[_clamp(10 + (245 - 10) * j / (w - 1)) for j in range(w)]
            for _ in range(h)]


def checkerboard(w: int = 256, h: int = 256, tile: int = 16) -> list[list[int]]:
    """Black-and-white checkerboard with given tile size."""
    px = []
    for i in range(h):
        row = []
        for j in range(w):
            v = 240 if ((i // tile) + (j // tile)) % 2 == 0 else 15
            row.append(v)
        px.append(row)
    return px


def ramp_noise(w: int = 256, h: int = 256, sigma: float = 18.0) -> list[list[int]]:
    """Diagonal ramp with superimposed Gaussian noise."""
    rng = random.Random(42)
    px  = []
    for i in range(h):
        row = []
        for j in range(w):
            base  = (i + j) / (w + h - 2) * 220 + 18
            noise = rng.gauss(0, sigma)
            row.append(_clamp(base + noise))
        px.append(row)
    return px


def portrait_like(w: int = 256, h: int = 256) -> list[list[int]]:
    """
    Synthetic portrait-like texture:
    background + soft elliptical face + Gaussian shading + hair band + eyes.
    """
    rng  = random.Random(7)
    cx   = w // 2
    cy   = h // 2
    rx   = w * 0.30
    ry   = h * 0.38

    px = []
    for i in range(h):
        row = []
        for j in range(w):
            # background: dark grey with slight grain
            bg = _clamp(50 + rng.gauss(0, 4))

            # face ellipse
            ex = (j - cx) / rx
            ey = (i - cy) / ry
            dist = math.sqrt(ex * ex + ey * ey)
            if dist <= 1.0:
                # skin tone shading
                brightness = 190 - 40 * dist + rng.gauss(0, 3)
                # highlight on top
                if ey < -0.3:
                    brightness += 20
                v = _clamp(brightness)
            else:
                v = bg

            # hair — dark band above face
            if ey < -0.85 and abs(ex) < 0.6:
                v = _clamp(30 + rng.gauss(0, 5))

            # eyes — simple dark ovals
            for eye_x in [cx - int(rx * 0.35), cx + int(rx * 0.35)]:
                eye_y = cy - int(ry * 0.2)
                if math.sqrt(((j - eye_x) / 10) ** 2 + ((i - eye_y) / 6) ** 2) < 1.0:
                    v = _clamp(30 + rng.gauss(0, 4))

            row.append(v)
        px.append(row)
    return px


# ── entry point ────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    out_dir = "testcases/images"
    print(f"Generating test images → {out_dir}/\n")

    specs = [
        ("test01_gradient.png",     gradient()),
        ("test02_checkerboard.png", checkerboard()),
        ("test03_ramp_noise.png",   ramp_noise()),
        ("test04_portrait.png",     portrait_like()),
    ]

    for name, pixels in specs:
        _make_png(pixels, os.path.join(out_dir, name))

    print("\nDone.")
