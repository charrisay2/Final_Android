#!/usr/bin/env bash
# Tạo keystore để ký bản release (chạy 1 lần, giữ file .jks cẩn thận).
# Sau khi chạy, điền mật khẩu vào keystore.properties (copy từ keystore.properties.example).
set -euo pipefail

OUT_DIR="$(cd "$(dirname "$0")/.." && pwd)/release"
mkdir -p "$OUT_DIR"

keytool -genkeypair \
  -v \
  -keystore "$OUT_DIR/vinho-release.jks" \
  -alias vinho \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype JKS

echo
echo "Xong. Keystore: $OUT_DIR/vinho-release.jks"
echo "Lấy SHA-1 (cần thêm vào Firebase Console -> Project settings -> Your apps):"
keytool -list -v -keystore "$OUT_DIR/vinho-release.jks" -alias vinho | grep -E "SHA1|SHA-1"
