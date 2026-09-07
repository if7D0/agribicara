"""
Membangkitkan `train_colab.ipynb` dari `train.py`.

KENAPA ADA: versi pertama notebook menyalin ulang logika latih secara inline,
terpisah dari `train.py`. Dua sumber kebenaran itu langsung menggigit — resep
latih diperbaiki di satu tempat sementara yang benar-benar dijalankan di Colab
adalah salinan yang satunya. Sekarang notebook hanya MENULIS `train.py` apa
adanya lalu menjalankannya, jadi tidak ada lagi yang bisa menyimpang.

Pakai:
  py ml/build_notebook.py        # -> menimpa ml/train_colab.ipynb
"""

import io
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
TRAIN_PY = os.path.join(HERE, "train.py")
OUT_IPYNB = os.path.join(HERE, "train_colab.ipynb")


def md(text):
    return {"cell_type": "markdown", "metadata": {}, "source": text.strip().splitlines(keepends=True)}


def code(text):
    return {
        "cell_type": "code",
        "execution_count": None,
        "metadata": {},
        "outputs": [],
        "source": text.strip().splitlines(keepends=True),
    }


def build():
    train_src = io.open(TRAIN_PY, encoding="utf-8").read()

    cells = [
        md(
            """
# AgriBicara — Latih Model Deteksi Penyakit Padi (Fase 4)

Notebook ini melatih klasifikator penyakit daun padi (10 kelas, dataset
**Paddy Doctor**, Apache 2.0) lalu mengekspor 2 berkas untuk di-**bundel** ke
aplikasi Android: `rice_disease_classifier.tflite` + `disease_labels.txt`.

**Sebelum mulai:** Runtime → Ubah jenis runtime → **GPU (T4)**. Tanpa GPU,
latihan bisa berjam-jam.

Seluruh logika latih ada di satu sel (`train.py`) yang disalin persis dari
repo — jangan mengedit logika di notebook, edit `ml/train.py` lalu jalankan
`py ml/build_notebook.py`.
"""
        ),
        md("## 1. Cek GPU"),
        code(
            """
import tensorflow as tf
print("TensorFlow:", tf.__version__)
gpus = tf.config.list_physical_devices("GPU")
print("GPU:", gpus if gpus else "TIDAK ADA — set Runtime > Ubah jenis runtime > GPU (T4)")
"""
        ),
        md(
            """
## 2. Unggah kaggle.json (token API Kaggle)

Cara dapat `kaggle.json`:
1. Buka https://www.kaggle.com → klik foto profil → **Settings**.
2. Bagian **API** → klik **Create Legacy API Key**. Berkas `kaggle.json` terunduh.
   (Tombol "Create New Token" gaya baru memberi token `KGAT_...` berupa teks —
   itu BUKAN `kaggle.json` dan tidak bisa dipakai di sini.)
3. Jalankan sel di bawah, lalu pilih berkas `kaggle.json` itu.

**PENTING:** buka juga halaman kompetisi
https://www.kaggle.com/competitions/paddy-disease-classification
dan klik **Join Competition** (setujui aturan). Tanpa ini, unduhan gagal (403).
"""
        ),
        code(
            """
from google.colab import files
import os
print("Pilih berkas kaggle.json ...")
files.upload()
os.makedirs("/root/.kaggle", exist_ok=True)
os.replace("kaggle.json", "/root/.kaggle/kaggle.json")
os.chmod("/root/.kaggle/kaggle.json", 0o600)
print("OK, kaggle.json terpasang.")
"""
        ),
        md(
            """
## 3. Unduh dataset Paddy Doctor

Sumber ini (kompetisi Kaggle) adalah distribusi dari penulis Paddy Doctor.
Datanya sama dengan yang berlisensi Apache 2.0. Kalau Anda ingin nol-ambiguitas
soal lisensi untuk rilis komersial, Anda boleh mengganti sumber dengan salinan
dari IEEE DataPort (Apache 2.0) — cukup arahkan `DATA_DIR` ke folder Anda yang
berisi subfolder per kelas.
"""
        ),
        code(
            """
!pip -q install kaggle
!kaggle competitions download -c paddy-disease-classification -p /content/paddy
!cd /content/paddy && unzip -q -o paddy-disease-classification.zip
import os
DATA_DIR = "/content/paddy/train_images"   # ganti bila pakai sumber lain
print("Kelas ditemukan:", sorted(os.listdir(DATA_DIR)))
"""
        ),
        md(
            """
## 4. Tulis skrip latih

Sel ini menulis `train.py` **persis** seperti di repo (`ml/train.py`). Kalau
Anda ingin mengubah resep latih, ubah di repo lalu bangkitkan ulang notebook —
jangan mengedit di sini, nanti hasil Colab dan repo berbeda diam-diam.
"""
        ),
        code("%%writefile train.py\n" + train_src),
        md(
            """
## 5. Latih + kalibrasi ambang

Dua tahap: (1) kepala klasifikasi di atas backbone beku, (2) fine-tune
**seluruh** backbone dengan LR kecil dan cosine decay. `EarlyStopping` menyimpan
bobot **terbaik**, bukan epoch terakhir.

Di akhir, skrip mencetak tabel kalibrasi ambang — pakai angka itu untuk menyetel
`Constants.DISEASE_CONFIDENCE_THRESHOLD` di aplikasi. Jangan menebak.

Kalau hasilnya masih kurang, jalankan ulang dengan `--backbone large`
(lebih akurat, model ~3x lebih besar).
"""
        ),
        code(
            """
!python train.py --data_dir "$DATA_DIR" --out_dir /content/out
"""
        ),
        md("## 6. Unduh 2 berkas ke komputer"),
        code(
            """
from google.colab import files
files.download("/content/out/disease_labels.txt")
files.download("/content/out/rice_disease_classifier.tflite")
"""
        ),
        md(
            """
## 7. Setelah ini (di komputer / repo)

Model **DI-BUNDEL** di aplikasi (Firebase ML Model Hosting sudah deprecated) —
jadi tidak ada unggah Firebase; cukup taruh 2 berkas di `assets/`.

1. **Label** → salin `disease_labels.txt` ke `app/src/main/assets/disease_labels.txt`.
2. **Model** → salin `rice_disease_classifier.tflite` ke
   `app/src/main/assets/rice_disease_classifier.tflite`, lalu commit keduanya.
3. **Ambang** → setel `Constants.DISEASE_CONFIDENCE_THRESHOLD` dari tabel
   kalibrasi di langkah 5 (angka terukur, bukan tebakan).
4. **Atribusi** → tampilkan isi `ml/NOTICE` di aplikasi (layar Lisensi).
5. **Rebuild & pasang ulang:**
   ```
   ./gradlew assembleDebug
   adb install -r -t app/build/outputs/apk/debug/app-debug.apk
   ```
   **Jangan** pakai tombol Run / Apply Changes Android Studio untuk fitur ini —
   nilai `const` Kotlin di-*inline* saat kompilasi, jadi hot-swap parsial pernah
   membuat aplikasi menjalankan ambang lama diam-diam.
6. **Uji di HP** → daun sakit ⇒ dugaan; daun sehat ⇒ "sehat"; foto acak ⇒
   "belum yakin". Deteksi jalan sepenuhnya offline.

**Versi runtime:** konverter TF baru memancarkan op `FULLY_CONNECTED` v12, yang
butuh `org.tensorflow:tensorflow-lite` **>= 2.17.0** di aplikasi. Dengan 2.16.1
model gagal dimuat ("Didn't find op for builtin opcode").

Detail klik-klik ada di `ml/COLAB_GUIDE.md`.
"""
        ),
    ]

    nb = {
        "cells": cells,
        "metadata": {
            "accelerator": "GPU",
            "colab": {"provenance": [], "toc_visible": True},
            "kernelspec": {"display_name": "Python 3", "name": "python3"},
            "language_info": {"name": "python"},
        },
        "nbformat": 4,
        "nbformat_minor": 0,
    }

    with io.open(OUT_IPYNB, "w", encoding="utf-8") as f:
        json.dump(nb, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"Ditulis: {OUT_IPYNB} ({len(cells)} sel)")


if __name__ == "__main__":
    build()
