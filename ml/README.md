# Pelatihan Model Deteksi Penyakit Padi (Fase 4, Task 0)

Track ML terpisah dari modul Android. Menghasilkan model TFLite yang **di-bundel
di assets aplikasi** dan dimuat on-device.

> **Catatan (2026-09-07):** rencana awal memakai Firebase ML Model Hosting, tapi
> layanan itu **deprecated** (shutdown Juni 2027) dan UI unggahnya dihapus dari
> Console. Jadi model kini **di-bundel langsung** di `app/src/main/assets/` —
> lebih sederhana: tanpa unggah Firebase, tanpa jaringan, tanpa App Check untuk model.

> **Status:** pipeline siap dijalankan. Model **belum** dilatih/dibundel —
> sampai itu terjadi, aplikasi berjalan normal dan menampilkan pesan jujur
> *"fitur belum tersedia"* alih-alih menebak.

> 🟢 **Paling mudah (disarankan):** buka **`ml/train_colab.ipynb`** di Google
> Colab lalu Run all, dan ikuti **`ml/COLAB_GUIDE.md`** (panduan klik-demi-klik).
> Sisa dokumen ini adalah rujukan teknis untuk `train.py` langsung.

## Lisensi & atribusi (WAJIB)

Dataset **Paddy Doctor** dilisensikan **Apache License 2.0**
([repo](https://github.com/paddydoc/paddy-doctor-dataset)) — boleh dipakai
komersial dengan atribusi. Isi `ml/NOTICE` **wajib** ikut disertakan pada rilis
aplikasi (mis. layar "Lisensi pihak ketiga"). Jangan pakai versi dataset dari
**kompetisi Kaggle** untuk produk — aturan kompetisi biasanya membatasi
penggunaan; ambil dari repo/IEEE DataPort resmi di atas.

## Kontrak yang menyatukan model ↔ aplikasi (JANGAN dilanggar)

| Hal | Nilai | Tempat di app |
|---|---|---|
| Ukuran input | 224×224×3 | `Constants.DISEASE_MODEL_INPUT_SIZE` |
| Pra-proses | piksel **mentah [0..255]** (MobileNetV3 `include_preprocessing=True` menormalkan sendiri) | `Constants.DISEASE_INPUT_MEAN=0`, `DISEASE_INPUT_STD=1` |
| Berkas model | `rice_disease_classifier.tflite` di `assets/` | `Constants.DISEASE_MODEL_ASSET` |
| Label | `disease_labels.txt`, 1 baris/kelas, urutan = output model | `assets/` + `DiseaseCatalog.kt` |

Nama label (mis. `blast`, `brown_spot`, `normal`) harus cocok dengan kunci di
`app/.../domain/ml/DiseaseCatalog.kt`. Label di luar katalog aman — aplikasi
menampilkannya sebagai "belum yakin", bukan crash.

## Cara menjalankan

### Opsi A — Google Colab (disarankan: GPU gratis)
TensorFlow GPU **tidak** jalan di Windows native (didrop sejak TF 2.11), dan
dataset besar. Colab mengatasi keduanya.

1. Buka notebook baru di Colab, Runtime → GPU.
2. Unduh dataset Paddy Doctor (Kaggle):
   ```python
   !pip -q install kaggle
   # taruh kaggle.json (API token akun Anda) di ~/.kaggle/
   !kaggle competitions download -c paddy-disease-classification -p paddy_data
   !cd paddy_data && unzip -q paddy-disease-classification.zip
   ```
   > Alternatif tanpa Kaggle: unduh dari repo/IEEE DataPort Paddy Doctor resmi.
3. Salin `train.py` ke Colab dan jalankan:
   ```python
   !python train.py --data_dir paddy_data/train_images --out_dir out
   # kalau akurasinya kurang, ulangi dengan backbone lebih besar:
   !python train.py --data_dir paddy_data/train_images --out_dir out --backbone large
   ```
4. Unduh `out/rice_disease_classifier.tflite` dan `out/disease_labels.txt`.

### Opsi B — Lokal (Windows, CPU-only)
```bash
py -m pip install -r requirements.txt
py train.py --data_dir <folder_dataset>
```
Lambat tanpa GPU, tapi jalan. (RTX 3050 di mesin ini tidak dipakai TF di Windows
native.)

## Setelah training (bundel ke aplikasi — bukan Firebase)

1. **Salin label:** `out/disease_labels.txt` → `app/src/main/assets/disease_labels.txt`
   (buat folder `assets/` bila belum ada). Commit berkas ini.
2. **Salin model:** `out/rice_disease_classifier.tflite` →
   `app/src/main/assets/rice_disease_classifier.tflite`. Commit (model diizinkan
   `.gitignore` khusus di path assets).
3. **Setel ambang:** isi `Constants.DISEASE_CONFIDENCE_THRESHOLD` dari **tabel
   kalibrasi** yang dicetak `train.py` di akhir — ambang terendah yang membuat
   dugaan yang ditampilkan benar minimal 90%. Ambang inilah pengaman terhadap
   diagnosis yakin-tapi-salah; jangan menebaknya.
4. **Atribusi:** pastikan isi `ml/NOTICE` tampil di aplikasi (layar lisensi).
5. **Rebuild & verifikasi di HP:** `./gradlew assembleDebug` lalu
   `adb install -r` (**jangan** Apply Changes Android Studio — konstanta `const`
   di-*inline* saat kompilasi). Foto daun → dugaan; foto acak → "belum yakin".

> **Versi runtime:** konverter TF baru memancarkan op `FULLY_CONNECTED` v12,
> yang butuh `org.tensorflow:tensorflow-lite` **>= 2.17.0** di aplikasi. Dengan
> 2.16.1 model gagal dimuat sama sekali.

## Berkas

| Berkas | Guna |
|---|---|
| `train.py` | Latih + evaluasi + kalibrasi ambang + ekspor TFLite & label |
| `train_colab.ipynb` | Notebook siap-jalan (disarankan) — dibangkitkan, jangan diedit tangan |
| `build_notebook.py` | Membangkitkan notebook dari `train.py` (`py ml/build_notebook.py`) |
| `COLAB_GUIDE.md` | Panduan klik-demi-klik |
| `requirements.txt` | Dependensi Python |
| `NOTICE` | Atribusi Apache 2.0 Paddy Doctor (wajib disertakan) |
