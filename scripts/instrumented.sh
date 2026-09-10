#!/usr/bin/env bash
#
# Menjalankan SELURUH instrumented test, satu kelas per proses.
#
# Kenapa tidak sekali jalan seperti biasa: satu proses yang menjalankan
# seluruh 92 test TERTAHAN tanpa sebab yang pernah berhasil ditemukan. Yang
# sudah terbukti dengan pengukuran:
#
#   74 test  OK 3,0 dtk     tanpa paket presentation
#   78 test  OK 16,5 dtk    local + ml + compose
#   89 test  OK 16,8 dtk    tanpa paket ml
#   92 test  TERTAHAN       suite penuh
#
# Titik jatuhnya berpindah antara WeatherCacheDaoTest dan
# TfliteImageClassifierTest, jadi ini pola AMBANG: sesuatu menumpuk di dalam
# proses uji dan baru melewati batas pada ukuran penuh. Hipotesis yang sudah
# diuji dan GUGUR, supaya tidak diulang: memori (41 MB dari batas 256 MB),
# thread (42), inisialisasi Firebase/App Check, penjadwalan WorkManager,
# MockK (itu penyebab gantungan LAIN yang sudah diperbaiki terpisah),
# interaksi TFLite dengan Compose, dan pembekuan proses oleh power management
# perangkat.
#
# Android Test Orchestrator seharusnya menjadi obatnya, tetapi di mesin ini
# ia GAGAL BIND: "Cannot connect to androidx.test.orchestrator.
# OrchestratorService", bahkan untuk satu kelas. Nasib yang sama dengan
# connectedDebugAndroidTest. Isolasi per-kelas memberi hasil yang sama
# tanpa menuntut apa pun selain adb.
#
# Daftar kelas DIBANGKITKAN dari pohon sumber, bukan diketik. Kalau ditulis
# tangan, kelas test baru akan diam-diam tidak pernah dijalankan.
#
# Pakai: scripts/instrumented.sh [-e opsi nilai ...]
set -uo pipefail

: "${ANDROID_HOME:=$HOME/AppData/Local/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb.exe"
[ -x "$ADB" ] || ADB="$ANDROID_HOME/platform-tools/adb"
RUNNER="com.agribicara.app.test/com.agribicara.app.AgriBicaraTestRunner"
AKAR="$(cd "$(dirname "$0")/.." && pwd)"

# force-stop KEDUA paket. com.agribicara.app.test adalah paket TERPISAH dan
# tidak ikut mati bersama com.agribicara.app; sisa proses dari run sebelumnya
# menabrak run berikutnya dan menghasilkan gejala yang menyerupai flaky.
bersihkan() {
  "$ADB" shell am force-stop com.agribicara.app.test >/dev/null 2>&1
  "$ADB" shell am force-stop com.agribicara.app >/dev/null 2>&1
}

kelas=$(
  find "$AKAR/app/src/androidTest" -name "*Test.kt" | sort | while read -r f; do
    pkg=$(sed -n 's/^package \(.*\)$/\1/p' "$f" | head -1)
    echo "$pkg.$(basename "$f" .kt)"
  done
)

jumlah=0; gagal=0; kelas_gagal=""
for k in $kelas; do
  bersihkan
  keluaran=$("$ADB" shell am instrument -w -e class "$k" "$@" "$RUNNER" 2>&1)
  ringkas=$(printf '%s' "$keluaran" | grep -E "^OK \(|^Tests run" | head -1)
  n=$(printf '%s' "$ringkas" | grep -oE "[0-9]+" | head -1)
  if printf '%s' "$ringkas" | grep -q "^OK"; then
    printf "  OK    %-4s %s\n" "${n:-0}" "$k"
    jumlah=$((jumlah + ${n:-0}))
  else
    printf "  GAGAL      %s\n" "$k"
    printf '%s\n' "$keluaran" | tail -5 | sed 's/^/         /'
    gagal=$((gagal + 1)); kelas_gagal="$kelas_gagal $k"
  fi
done

echo
if [ "$gagal" -eq 0 ]; then
  echo "SEMUA HIJAU: $jumlah test, $(printf '%s\n' "$kelas" | wc -l | tr -d ' ') kelas"
else
  echo "GAGAL di $gagal kelas:$kelas_gagal"
fi
bersihkan
exit "$gagal"
