# Kebijakan Privasi AgriBicara

**Berlaku sejak:** 2 September 2026
**Terakhir diperbarui:** 2 September 2026
**Aplikasi:** AgriBicara (`com.agribicara.app`)

AgriBicara adalah aplikasi gratis untuk petani kecil di Indonesia. Anda bertanya
dengan suara, dan aplikasi menjawab dengan suara berdasarkan prakiraan cuaca desa
Anda.

Dokumen ini menjelaskan data apa yang aplikasi pakai, ke mana data itu pergi, dan
apa yang tidak pernah kami lakukan. Ditulis sesederhana mungkin, karena Anda yang
memakainya, bukan pengacara.

---

## Ringkas

- **Tidak ada akun.** Anda tidak perlu mendaftar, tidak perlu nomor telepon, dan
  tidak perlu email.
- **Tidak ada iklan.** Aplikasi ini gratis dan tidak menampilkan iklan.
- **Kami tidak menjual data Anda.** Kepada siapa pun.
- **Aplikasi tidak memakai GPS** dan tidak meminta izin lokasi. Anda memilih
  sendiri desa Anda dari daftar.
- **Riwayat pertanyaan Anda tersimpan di HP Anda saja** dan tidak pernah dikirim
  ke mana pun.

---

## Data yang aplikasi pakai

### 1. Suara Anda

Ketika Anda menekan tombol mikrofon dan berbicara, suara Anda diproses oleh
layanan pengenalan suara bawaan Android (milik Google) untuk diubah menjadi
teks. AgriBicara tidak menyimpan rekaman suara Anda dan tidak mengirimkannya ke
server kami — kami memang tidak punya server sendiri.

Izin yang dipakai: **Mikrofon**. Anda bisa menolaknya; aplikasi tetap bisa
dipakai lewat pengetikan teks.

### 2. Pertanyaan Anda dan cuaca desa Anda

Teks pertanyaan Anda digabung dengan ringkasan prakiraan cuaca desa Anda, lalu
dikirim ke layanan kecerdasan buatan Google (Gemini melalui Firebase AI Logic)
supaya jawabannya sesuai dengan cuaca yang sebenarnya. Jawabannya dikembalikan ke
HP Anda dan dibacakan.

Yang dikirim hanya teks pertanyaan dan angka cuaca. Tidak ada nama, nomor
telepon, atau identitas Anda yang ikut dikirim — aplikasi memang tidak
memilikinya.

### 3. Pilihan desa Anda

Untuk mengambil prakiraan cuaca, aplikasi mengirim **kode wilayah** desa yang
Anda pilih (contoh: `11.01.01.2001`) ke tiga layanan publik:

- **BMKG** (`api.bmkg.go.id`) — prakiraan cuaca resmi Indonesia, hari 1–3
- **Open-Meteo** (`api.open-meteo.com`) — cadangan bila BMKG tidak bisa
  dihubungi, dan estimasi hari 4–7
- **wilayah.id** (`wilayah.id`) — daftar nama provinsi sampai desa

Yang dikirim adalah **kode desa yang Anda pilih sendiri**, bukan lokasi HP Anda.
Aplikasi tidak pernah membaca GPS.

### 4. Laporan kerusakan aplikasi

Bila aplikasi berhenti mendadak atau mengalami kesalahan, laporan teknisnya
dikirim ke **Firebase Crashlytics** (milik Google) supaya kami bisa
memperbaikinya. Laporan berisi jenis perangkat, versi Android, dan letak
kesalahan di dalam kode.

**Isi pertanyaan Anda dan nama desa Anda tidak ikut dikirim dalam laporan ini.**

### 5. Data yang hanya tersimpan di HP Anda

Hal-hal berikut disimpan di dalam HP Anda dan **tidak pernah dikirim ke mana
pun**:

- Riwayat pertanyaan dan jawaban
- Prakiraan cuaca yang tersimpan agar bisa dilihat saat tidak ada sinyal
- Desa yang Anda pilih
- Catatan peringatan cuaca yang sudah dikirim, agar tidak dikirim berulang

Semuanya hilang bila Anda menghapus data aplikasi atau mencopot aplikasinya.

---

## Izin yang diminta aplikasi

| Izin | Untuk apa | Kalau ditolak |
|---|---|---|
| Mikrofon | Mendengar pertanyaan Anda | Aplikasi tetap jalan; gunakan pengetikan teks |
| Internet | Mengambil cuaca dan jawaban | Aplikasi hanya menampilkan cuaca tersimpan |
| Status jaringan | Mengetahui Anda sedang online atau tidak | — |
| Notifikasi | Memberi peringatan cuaca ekstrem | Aplikasi tetap jalan penuh, hanya tanpa peringatan |

Aplikasi **tidak** meminta izin lokasi, kamera, kontak, penyimpanan, maupun
telepon.

---

## Pihak ketiga

Data yang keluar dari HP Anda hanya menuju:

- **Google** (Firebase AI Logic, pengenalan suara Android, Crashlytics) —
  [kebijakan privasi Google](https://policies.google.com/privacy)
- **BMKG** — badan meteorologi pemerintah Indonesia
- **Open-Meteo** — layanan cuaca terbuka
- **wilayah.id** — daftar wilayah administratif Indonesia

Kami tidak menjual, menyewakan, atau menukarkan data Anda kepada siapa pun.

---

## Anak-anak

Aplikasi ini ditujukan untuk petani dewasa. Kami tidak mengumpulkan data secara
sengaja dari anak di bawah 13 tahun.

---

## Menghapus data Anda

Karena tidak ada akun dan tidak ada data Anda di server kami, menghapus data
cukup dilakukan dari HP Anda:

**Setelan → Aplikasi → AgriBicara → Penyimpanan → Hapus data**

atau cukup copot aplikasinya.

---

## Perubahan kebijakan ini

Bila kebijakan ini berubah, tanggal "Terakhir diperbarui" di atas ikut berubah dan
versi barunya diterbitkan di halaman yang sama.

---

## Hubungi kami

Pertanyaan tentang kebijakan ini bisa dikirim ke:

**[ISI ALAMAT EMAIL KONTAK DI SINI SEBELUM DITERBITKAN]**

> Catatan untuk pengembang, hapus baris ini sebelum menerbitkan: alamat email
> kontak sengaja dikosongkan. Alamat ini akan tampil publik di halaman web dan di
> listing Play Store, jadi pemilihannya keputusan Anda — bukan sesuatu yang
> pantas diisi otomatis dengan alamat pribadi.
