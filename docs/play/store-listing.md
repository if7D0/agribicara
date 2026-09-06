# Teks Listing Play Store — AgriBicara

Seluruh teks Bahasa Indonesia. Batas karakter Play ditulis di tiap bagian; hitung
ulang setelah menyunting dengan perintah di bagian terakhir.

**Aturan yang mengikat dokumen ini:** hanya sebut fitur yang benar-benar ada di
build yang diunggah. Deteksi penyakit tanaman (Fase 4) berstatus `blocked` dan
**tidak boleh disebut sama sekali** — menjanjikan fitur yang tidak ada melanggar
kebijakan Play sekaligus membohongi petani yang mengunduhnya karena itu.

---

## Nama aplikasi (maksimum 30 karakter)

```
AgriBicara: Cuaca Petani
```

## Deskripsi singkat (maksimum 80 karakter)

Muncul di hasil pencarian dan di bawah ikon.

```
Tanya cuaca desa Anda cukup dengan berbicara. Jawaban langsung disuarakan.
```

## Deskripsi penuh (maksimum 4000 karakter)

```
AgriBicara membantu petani kecil mengetahui cuaca desanya sendiri tanpa perlu membaca grafik atau tabel angka.

Tekan tombol mikrofon yang besar, ajukan pertanyaan dalam Bahasa Indonesia, dan dengarkan jawabannya. Sesederhana berbicara kepada tetangga.

CONTOH PERTANYAAN

• "Kapan waktu terbaik memupuk padi minggu ini?"
• "Besok hujan tidak?"
• "Aman tidak kalau saya menjemur gabah hari ini?"

Jawabannya paling banyak tiga kalimat, memakai nama hari, bukan tanggal, dan disusun dari prakiraan cuaca desa Anda yang sebenarnya.

CUACA SAMPAI TINGKAT DESA

Prakiraan diambil dari BMKG untuk tiga hari pertama, sampai ke tingkat kelurahan atau desa, bukan hanya kota terdekat. Hari keempat sampai ketujuh dilengkapi dari Open-Meteo dan ditandai sebagai estimasi, supaya Anda tahu mana yang lebih bisa dipegang.

Anda memilih sendiri desa Anda dari daftar bertingkat: provinsi, kabupaten, kecamatan, lalu desa. Aplikasi tidak memakai GPS dan tidak meminta izin lokasi.

TETAP BERGUNA SAAT SINYAL HILANG

Prakiraan yang sudah diambil tersimpan di HP Anda. Ketika sinyal hilang, aplikasi tetap menampilkan data terakhir beserta keterangan bahwa data itu tersimpan, bukan baru.

PERINGATAN CUACA EKSTREM

Aplikasi memeriksa prakiraan secara berkala dan memberi tahu bila ada hujan sangat lebat atau cuaca berbahaya dalam dua hari ke depan. Peringatan yang sama tidak dikirim berulang-ulang.

DIRANCANG UNTUK DIPAKAI DI SAWAH

• Tulisan besar, tidak pernah lebih kecil dari 16sp
• Semua tombol minimal 48dp, mudah ditekan dengan tangan yang tidak bersih
• Warna berkontras tinggi supaya tetap terbaca di bawah matahari
• Mode gelap otomatis mengikuti setelan HP Anda
• Bisa juga mengetik pertanyaan bila Anda sedang tidak ingin bersuara

GRATIS DAN TANPA IKLAN

AgriBicara sepenuhnya gratis. Tidak ada iklan, tidak ada pembelian di dalam aplikasi, tidak ada akun yang perlu dibuat, dan tidak ada nomor telepon yang perlu diberikan.

Riwayat pertanyaan Anda tersimpan di HP Anda saja.

YANG PERLU ANDA KETAHUI

AgriBicara memberi gambaran cuaca dan saran umum bertani. Ia bukan pengganti penyuluh pertanian. Untuk hal yang menyangkut takaran pupuk atau pestisida, aplikasi akan mengarahkan Anda kepada petugas penyuluh setempat — dan itu memang disengaja.

Aplikasi membutuhkan sambungan internet untuk mengambil prakiraan baru dan untuk menjawab pertanyaan.

Sumber data: BMKG (Badan Meteorologi, Klimatologi, dan Geofisika) dan Open-Meteo.
```

---

## Kategori dan metadata

| Kolom | Nilai | Alasan |
|---|---|---|
| Kategori aplikasi | Weather | Yang benar-benar dikerjakan aplikasi hari ini. "Food & Drink" atau "Business" akan menyesatkan |
| Tag | Cuaca, Pertanian, Asisten suara | — |
| Negara distribusi | **Indonesia saja** untuk rilis pertama | Data BMKG hanya mencakup Indonesia; aplikasi tidak berguna di luar itu |
| Email kontak | **Belum diisi** | Tampil publik di listing. Keputusan pemilik akun, bukan sesuatu yang pantas diisi otomatis |
| Situs web | Opsional — boleh diarahkan ke halaman privacy policy | — |
| URL privacy policy | Dari repo publik `agribicara-legal` | Lihat `release-checklist.md` butir C10 |

---

## Screenshot yang perlu diambil

Minimal 2 diterima Play; **4 sangat disarankan** dan itu yang dipakai di sini.
Resolusi minimal 1080px pada sisi terpendek.

| # | Layar | Kenapa layar ini |
|---|---|---|
| 1 | Home dengan cuaca desa terisi | Menunjukkan nilai inti dalam satu pandangan |
| 2 | Layar suara sedang mendengarkan | Menunjukkan cara pakainya, bukan sekadar hasilnya |
| 3 | Jawaban AI tampil di layar | Membuktikan jawabannya nyata dan singkat |
| 4 | Region picker bertingkat | Menjelaskan mengapa cuacanya bisa se-spesifik desa |

Ambil dari perangkat sungguhan, bukan mockup:

```bash
export ANDROID_HOME="C:/Users/ACER/AppData/Local/Android/Sdk"
export MSYS_NO_PATHCONV=1
A="$ANDROID_HOME/platform-tools/adb.exe"
"$A" exec-out screencap -p > docs/play/assets/screenshot-1.png
```

---

## Memeriksa batas karakter

Jalankan setiap kali teks di atas disunting. Melewati batas membuat Play menolak
penyimpanan listing, dan pesannya tidak menyebut bagian mana yang kepanjangan.

```bash
awk '/^## Nama aplikasi/,/^## Deskripsi singkat/' docs/play/store-listing.md \
  | sed -n '/^```$/,/^```$/p' | sed '1d;$d' | awk '{printf "nama       %d/30\n", length($0)}'

awk '/^## Deskripsi singkat/,/^## Deskripsi penuh/' docs/play/store-listing.md \
  | sed -n '/^```$/,/^```$/p' | sed '1d;$d' | awk '{printf "singkat    %d/80\n", length($0)}'

awk '/^## Deskripsi penuh/,/^---$/' docs/play/store-listing.md \
  | sed -n '/^```$/,/^```$/p' | sed '1d;$d' \
  | awk '{n+=length($0)+1} END{printf "penuh      %d/4000\n", n}'
```
