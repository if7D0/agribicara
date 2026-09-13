# Panduan Lengkap: Menuntaskan Deteksi Penyakit Nyata

Panduan langkah-demi-langkah, ditujukan untuk yang belum terbiasa dengan ML /
Colab. Ikuti berurutan. Hasil akhirnya: aplikasi benar-benar bisa menebak
penyakit dari foto.

**Peta besar (7 langkah):**
1. Buka notebook di Google Colab
2. Aktifkan GPU
3. Ambil token Kaggle + gabung kompetisi
4. Run all → tunggu → unduh 2 berkas
5. Taruh `disease_labels.txt` ke aplikasi
6. Bundel model ke aplikasi (folder assets — bukan Firebase)
7. Setel ambang, atribusi, build & uji di HP

Perkiraan waktu: ~30–45 menit (kebanyakan menunggu training).

---

## Langkah 1 — Buka notebook di Colab

Berkasnya: `ml/train_colab.ipynb` (di repo ini).

**Cara termudah (unggah manual):**
1. Buka https://colab.research.google.com
2. Menu **File → Upload notebook → Browse**.
3. Pilih `ml/train_colab.ipynb` dari komputer Anda. Notebook terbuka.

**Alternatif (dari GitHub, setelah branch di-push):**
File → Open notebook → tab **GitHub** → tempel URL repo → pilih
`ml/train_colab.ipynb`.

---

## Langkah 2 — Aktifkan GPU (gratis)

1. Menu **Runtime → Change runtime type**.
2. **Hardware accelerator → T4 GPU** → **Save**.

> Tanpa GPU training tetap jalan tapi jauh lebih lambat. Sel pertama notebook
> akan memberitahu apakah GPU aktif.

---

## Langkah 3 — Token Kaggle + gabung kompetisi

Dataset diunduh dari Kaggle, jadi butuh token + menyetujui aturan sekali.

**3a. Ambil `kaggle.json` (versi LEGACY — penting):**
1. Login di https://www.kaggle.com
2. Klik foto profil (kanan atas) → **Settings**.
3. Gulir ke bagian **API**.
4. **Gunakan "Create Legacy API Key"** (di bawah "Legacy API Credentials").
   Ini mengunduh berkas **`kaggle.json`** berisi `username` + `key` — inilah yang
   dibutuhkan notebook.

> ⚠️ Tombol **"Create New API Token"** (default) kini memunculkan **token gaya
> baru** berupa teks berawalan `KGAT_` di sebuah jendela — BUKAN berkas
> `kaggle.json`, dan **tidak bisa dipakai** untuk notebook ini. Kalau yang muncul
> hanya teks `KGAT_...`, abaikan (dan sebaiknya expire token itu), lalu pakai
> **Create Legacy API Key**.
>
> 🔒 Jangan pernah menyimpan token ke dalam folder repo atau meng-commit-nya.
> `kaggle.json` hanya diunggah ke Colab, tidak masuk git.

**3b. Setujui aturan kompetisi (WAJIB, kalau tidak unduhan 403):**
1. Buka https://www.kaggle.com/competitions/paddy-disease-classification
2. Klik **Join Competition** → **I Understand and Accept**.

---

## Langkah 4 — Jalankan notebook

1. Di Colab: menu **Runtime → Run all**.
2. Saat sampai sel **"Unggah kaggle.json"**, akan muncul tombol **Choose Files**
   — pilih `kaggle.json` dari Langkah 3a.
3. Biarkan berjalan. Yang lama adalah sel **"5. Latih + kalibrasi ambang"**
   — ~30–50 menit di GPU (Tahap 2 membuka seluruh backbone). Aman ditinggal.
4. Di akhir sel itu, **catat dua hal**:
   - **Akurasi validasi keseluruhan** + akurasi per kelas.
   - **Tabel kalibrasi ambang** dan baris `=> Setel
     Constants.DISEASE_CONFIDENCE_THRESHOLD = ...` — angka itu dipakai di
     Langkah 7a. Jangan menebak ambang sendiri.

   > Kalau skrip malah mencetak *"TIDAK ADA ambang yang mencapai 90%
   > ketepatan"*, modelnya belum layak dipakai. Jalankan ulang sel latih dengan
   > `--backbone large` sebelum lanjut (lihat "Kalau akurasi rendah" di bawah).
5. Di sel terakhir, browser akan **mengunduh 2 berkas**:
   - `disease_labels.txt`
   - `rice_disease_classifier.tflite`

> Kalau koneksi Colab putus di tengah jalan, buka lagi dan **Run all** dari atas.

---

## Langkah 5 — Taruh label ke aplikasi

Aplikasi membaca daftar kelas dari `assets`. Nama & urutan harus dari training.

1. Di repo, buat folder bila belum ada: `app/src/main/assets/`
2. Salin `disease_labels.txt` ke situ → `app/src/main/assets/disease_labels.txt`
3. Commit berkas ini (ia kecil dan memang bagian dari aplikasi).

> Kalau berkas ini tidak ada, aplikasi akan selalu bilang **"model belum
> tersedia"** meskipun modelnya sudah diunggah — karena classifier butuh label
> untuk memetakan output model.

---

## Langkah 6 — Bundel model ke aplikasi (bukan Firebase)

> **Kenapa berubah:** Firebase ML Model Hosting sudah **deprecated** (shutdown
> Juni 2027) dan tombol "Add custom model" dihapus dari Console. Jadi model
> **di-bundel langsung** di aplikasi — sebenarnya lebih mudah: tanpa unggah,
> tanpa jaringan, tanpa App Check untuk model.

1. Buka folder `app/src/main/assets/` (sama dengan Langkah 5; buat bila belum ada).
2. **Salin** `rice_disease_classifier.tflite` (dari Langkah 4) ke situ →
   `app/src/main/assets/rice_disease_classifier.tflite`.
3. Sekarang di `assets/` ada **dua** berkas: `disease_labels.txt` +
   `rice_disease_classifier.tflite`. Nama harus persis (cocok dengan
   `Constants.DISEASE_MODEL_ASSET` dan `DISEASE_LABELS_ASSET`).
4. Commit keduanya. Model diizinkan git lewat aturan khusus di `.gitignore`
   (`!app/src/main/assets/*.tflite`).

Tidak ada langkah Firebase sama sekali → lanjut Langkah 7.

---

## Langkah 7 — Ambang, atribusi, build, uji

**7a. Setel ambang keyakinan.** Buka
`app/.../core/common/Constants.kt` → `DISEASE_CONFIDENCE_THRESHOLD`.
- Isi dengan angka dari **tabel kalibrasi** di Langkah 4 (baris `=> Setel
  Constants.DISEASE_CONFIDENCE_THRESHOLD = ...`). Itu ambang terendah yang
  membuat dugaan yang ditampilkan benar minimal 90%.
- Prinsipnya: **lebih baik "belum yakin" daripada diagnosis salah** — petani
  bisa salah beli pestisida. Menurunkan ambang menaikkan jumlah foto yang dapat
  jawaban, tapi menurunkan ketepatannya; tabel kalibrasi menunjukkan
  pertukaran itu dalam angka.

**7b. Atribusi (wajib, lisensi Apache 2.0).** Tampilkan isi `ml/NOTICE` di
aplikasi — mis. tambahkan layar/entri "Lisensi pihak ketiga". (Bisa dikerjakan
terpisah; yang penting sebelum rilis publik.)

**7c. Build & pasang:**
```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="C:/Users/<nama-anda>/AppData/Local/Android/Sdk"
./gradlew assembleDebug
"$ANDROID_HOME/platform-tools/adb.exe" install -r -t app/build/outputs/apk/debug/app-debug.apk
```

> ⚠️ **Jangan pakai tombol Run / Apply Changes Android Studio untuk fitur ini.**
> `DISEASE_CONFIDENCE_THRESHOLD` dan konstanta lain adalah `const` Kotlin yang
> di-*inline* saat kompilasi. Hot-swap parsial pernah membuat aplikasi
> menjalankan ambang lama diam-diam sementara kode sumbernya sudah berubah —
> gejalanya "belum yakin" padahal skornya jelas di atas ambang. Selalu
> `assembleDebug` + `adb install -r`.

**7d. Uji di HP (tanpa internet pun bisa — deteksi sepenuhnya offline):**
- Buka aplikasi → **Cek penyakit daun** → **Ambil foto** / **Pilih dari galeri**.
- Foto **satu daun padi** di latar polos → harus muncul **dugaan + keyakinan**.
- Foto acak (tangan, tembok) → harus muncul **"belum yakin"**, bukan diagnosis.
- Foto daun sehat → **"daun tampak sehat"**.

> Model dibundel di dalam aplikasi, jadi deteksi jalan **sepenuhnya offline** —
> tidak perlu internet sama sekali untuk fitur ini.

---

## Kalau masih "fitur belum tersedia"

Periksa berurutan:
1. Kedua berkas **ada** di `app/src/main/assets/`?
   - `rice_disease_classifier.tflite`
   - `disease_labels.txt`
2. **Nama berkas persis** cocok dengan `Constants.DISEASE_MODEL_ASSET`
   (`rice_disease_classifier.tflite`) dan `DISEASE_LABELS_ASSET`
   (`disease_labels.txt`)? Salah satu huruf saja = tidak ketemu.
3. Sudah **rebuild** (`assembleDebug`) setelah menaruh berkas? Assets ikut saat build.
4. Label tidak kosong dan urutannya = keluaran model (dari `train.py`).

> Tidak ada urusan Firebase / internet / App Check untuk model lagi — model
> dibaca langsung dari dalam APK.

## Kalau hasilnya aneh (yakin tapi selalu salah)

Hampir selalu **mismatch pra-proses**. Pastikan model dilatih dengan notebook ini
(MobileNetV3 `include_preprocessing=True`, input mentah [0..255]) DAN
`Constants.DISEASE_INPUT_MEAN=0`, `DISEASE_INPUT_STD=1`. Keduanya harus sepasang.

## Kalau aplikasi bilang "Foto belum bisa diperiksa"

Itu pesan kegagalan umum, bukan soal kualitas foto. Lihat penyebab aslinya:

```bash
adb logcat -d | grep -A5 "Deteksi penyakit gagal"
```

Penyebab yang sudah pernah terjadi:

- **`Didn't find op for builtin opcode 'FULLY_CONNECTED' version '12'`** —
  konverter TF di Colab lebih baru daripada runtime TFLite di aplikasi. Runtime
  wajib **`org.tensorflow:tensorflow-lite` >= 2.17.0** (`register.cc` mendukung
  `FULLY_CONNECTED` sampai v11 di 2.16.1, v12 di 2.17.0). Perbaiki di
  `gradle/libs.versions.toml`. 2.17.0 adalah rilis terakhir jalur
  `org.tensorflow`; kalau konverter kelak memancarkan v13, pindah ke LiteRT
  (`com.google.ai.edge.litert`).

## Kalau akurasi rendah

Latihan pertama proyek ini hanya mencapai **val_accuracy 0.586** (loss 1.211;
tebakan acak 10 kelas = 2.303) dengan `bacterial_leaf_blight` di **0.04**.
Penyebabnya resep latih, bukan datanya:

| Gejala | Penyebab | Sudah diperbaiki di `train.py` |
| --- | --- | --- |
| Akurasi mentok ~0.6 | Backbone dibekukan; fine-tune hanya 30 lapisan atas @ LR 1e-5 | Tahap 2 membuka **seluruh** backbone @ LR 1e-4 + cosine decay |
| Kelas kecil ~0.04 | Tanpa penyeimbang kelas | `class_weight` gaya *balanced* |
| Model bagus lalu memburuk | Yang diekspor epoch **terakhir** | `EarlyStopping(restore_best_weights=True)` |
| Ambang cuma tebakan | Tidak ada kalibrasi | Tabel kalibrasi ambang dicetak di akhir |

Kalau setelah perbaikan itu akurasinya masih kurang, jalankan sel latih dengan
backbone yang lebih besar:

```
!python train.py --data_dir "$DATA_DIR" --out_dir /content/out --backbone large
```

MobileNetV3-**Large** lebih akurat tapi modelnya ~3x lebih besar (APK ikut
membengkak) dan inferensi lebih lambat di HP murah — pakai hanya bila `small`
memang tidak cukup.

> **Catatan lapangan:** akurasi validasi selalu lebih optimistis daripada foto
> HP sungguhan. Dataset Paddy Doctor kebanyakan berisi satu daun di latar cukup
> polos; foto dengan tangan, banyak daun, dan latar berair akan mendapat
> keyakinan lebih rendah. Panduan foto di layar deteksi ada untuk alasan ini.
