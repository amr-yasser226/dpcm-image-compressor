#!/bin/bash
# run_tests.sh
# Batch test runner for DPCM codec.

IMAGE_DIR="testcases/images"
RESULTS_DIR="testcases/results"

mkdir -p out
mkdir -p "$RESULTS_DIR"

# Compile Java classes
echo "Compiling Java classes..."
javac -d out src/main/java/com/dpcm/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed."
    exit 1
fi

# Run tests for each image in the directory
for img in "$IMAGE_DIR"/*.png; do
    if [ -f "$img" ]; then
        echo "--------------------------------------------------------------------------------"
        echo "Processing: $img"
        java -cp out com.dpcm.DPCMCodec <<EOF
2
$img
$RESULTS_DIR
3
EOF
    fi
done

echo "Tests completed. Results are in $RESULTS_DIR"
