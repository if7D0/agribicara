# Jawaban Form Data Safety — Play Console

Lembar jawaban untuk bagian **Data safety** di Play Console, beserta alasan tiap
pilihan.

Dokumen ini **wajib konsisten dengan `docs/legal/privacy-policy.md`**, baris per
baris. Ketidakcocokan antara Data Safety dan privacy policy adalah salah satu
penyebab penolakan Play yang paling umum, dan ia tidak tertangkap alat apa pun —
hanya peninjau manusia yang membandingkan keduanya.

Setiap klaim di bawah ditelusuri ke kode, bukan ke ingatan. Sumbernya disebut di
kolom terakhir.

---

## Ringkasan yang diverifikasi dari kode (2026-09-02)

| Fakta | Nilai | Sumber |
|---|---|---|
| Izin yang diminta | `INTERNET`, `ACCESS_NETWORK_STATE`, `RECORD_AUDIO`, `POST_NOTIFICATIONS` | `AndroidManifest.xml` |
| Izin lokasi | **Tidak ada** | `AndroidManifest.xml` |
| Endpoint HTTP | `api.bmkg.go.id`, `api.open-meteo.com`, `wilayah.id` | grep seluruh `app/src/main/java` |
| SDK yang mengirim data | Firebase AI Logic, Firebase Crashlytics, Firebase App Check | `app/build.gradle.kts` |
| Firebase Analytics | **Tidak dipasang** | `app/build.gradle.kts` |
| Autentikasi / akun | **Tidak ada** | grep `FirebaseAuth` — nihil |
| Penyimpanan lokal | 5 tabel Room, tidak satu pun disinkronkan | `data/local/entity/` |

---

## Bagian 1 — Pengumpulan dan pembagian data

> **Istilah Play yang mudah tertukar.** *Collected* = keluar dari perangkat menuju
> server Anda atau pihak lain. *Shared* = diteruskan ke **pihak ketiga yang
> terpisah**. Riwayat Room **bukan** keduanya: ia tidak pernah meninggalkan HP.

### Audio → Voice or sound recordings

| Pertanyaan | Jawaban |
|---|---|
| Collected? | **Ya** |
| Shared? | Tidak |
| Processed ephemerally? | Ya |
| Required or optional? | **Optional** — aplikasi tetap berfungsi lewat input teks |
| Purpose | App functionality |

**Alasan.** Suara ditangkap `SpeechRecognizer` milik Android dan dapat diproses di
server Google, bukan hanya di perangkat. AgriBicara sendiri tidak menyimpan dan
tidak mengirim rekaman ke server mana pun — proyek ini memang tidak punya server.

**Catatan kejujuran.** Ada tafsir yang membolehkan audio lewat API sistem tidak
dideklarasikan sama sekali. Tafsir itu **sengaja tidak dipakai**: audio memang
bisa meninggalkan perangkat, dan salah-deklarasi ke arah "lebih terbuka" hanya
merugikan persepsi, sedangkan ke arah sebaliknya melanggar kebijakan.

### App activity → Other user-generated content

| Pertanyaan | Jawaban |
|---|---|
| Collected? | **Ya** |
| Shared? | Tidak |
| Processed ephemerally? | Tidak |
| Required or optional? | **Optional** |
| Purpose | App functionality |

**Alasan.** Teks pertanyaan petani dikirim ke Gemini lewat Firebase AI Logic
(`FirebaseTextGenerator`, `PromptBuilder`). Jawabannya disimpan di riwayat lokal.
Tidak ephemeral karena jawabannya di-cache di perangkat sampai
`Constants.ANSWER_CACHE_HOURS`.

### App info and performance → Crash logs

| Pertanyaan | Jawaban |
|---|---|
| Collected? | **Ya** |
| Shared? | Tidak |
| Required or optional? | Required |
| Purpose | App functionality, Analytics |

### App info and performance → Diagnostics

| Pertanyaan | Jawaban |
|---|---|
| Collected? | **Ya** |
| Shared? | Tidak |
| Required or optional? | Required |
| Purpose | App functionality, Analytics |

**Alasan keduanya.** `CrashReportingTree` meneruskan WARN/ERROR/ASSERT ke
Crashlytics, dan Crashlytics mengirim laporan crash beserta jenis perangkat dan
versi Android. **Isi pertanyaan petani dan nama wilayah tidak ikut** — itu aturan
yang dinyatakan di KDoc `CrashReportingTree` dan berlaku sejak README Fase 1.

### Device or other IDs

| Pertanyaan | Jawaban |
|---|---|
| Collected? | **Ya** |
| Shared? | Tidak |
| Required or optional? | Required |
| Purpose | App functionality, Analytics |

**Alasan.** Crashlytics dan App Check memakai Firebase Installation ID untuk
mengelompokkan laporan dan memverifikasi keaslian aplikasi. Tidak dipakai untuk
iklan maupun pelacakan lintas aplikasi — tidak ada SDK iklan di proyek ini.

### Location → Approximate location

| Pertanyaan | Jawaban |
|---|---|
| Collected? | **Tidak** |

**Alasan, dan ini butir yang paling perlu dibaca peninjau.** Aplikasi **tidak
punya izin lokasi apa pun** dan **tidak pernah membaca GPS**. Yang dikirim ke BMKG
adalah kode wilayah yang **dipilih sendiri pengguna dari daftar bertingkat**
(provinsi → kabupaten → kecamatan → kelurahan), misalnya `11.01.01.2001`. Ia
preferensi yang diketik pengguna, bukan lokasi yang diukur perangkat.

Bentuk ini bukan pilihan desain sukarela: spike BMKG 2026-08-29 membuktikan API
BMKG **hanya** menerima kode `adm4` dan tidak menyediakan reverse-geocode sama
sekali, sehingga region picker manual menjadi wajib dan GPS menjadi tidak berguna.

> **Risiko yang dinyatakan terbuka.** Peninjau bisa berpendapat kode kelurahan
> adalah informasi lokasi terlepas dari cara memperolehnya. Bila listing ditolak
> dengan alasan itu, **jangan berdebat** — ubah jawaban ini menjadi *Collected:
> Ya, Approximate location, App functionality, Optional*, lalu perbarui privacy
> policy agar tetap cocok. Privacy policy sudah menyebut pengiriman kode wilayah
> secara eksplisit, jadi tidak ada yang perlu disembunyikan; yang berubah hanya
> label kategorinya.

### Yang TIDAK dikumpulkan sama sekali

Personal info, Financial info, Health and fitness, Messages, Photos and videos,
Files and docs, Calendar, Contacts, Web browsing history, Installed apps.

Tidak ada akun, tidak ada login, tidak ada kamera, tidak ada akses penyimpanan.

---

## Bagian 2 — Praktik keamanan

| Pertanyaan | Jawaban | Alasan |
|---|---|---|
| Data dienkripsi saat transit? | **Ya** | Seluruh endpoint HTTPS; Firebase SDK memakai TLS |
| Pengguna bisa meminta data dihapus? | **Ya** | Tidak ada data tersimpan di sisi kami. Riwayat ada di perangkat dan hilang lewat Setelan → Aplikasi → AgriBicara → Hapus data, atau dengan mencopot aplikasi |
| Mengikuti Families Policy? | **Tidak** | Target pengguna petani dewasa |
| Sudah menjalani penilaian keamanan independen? | Tidak | Belum pernah; jangan diklaim |

---

## Bagian 3 — Konsistensi silang

Sebelum menyimpan form, cocokkan tiap baris berikut dengan
`docs/legal/privacy-policy.md`:

- [ ] Daftar izin di policy **sama persis** dengan `AndroidManifest.xml` — empat izin, tidak lebih dan tidak kurang
- [ ] Policy menyatakan tidak ada GPS; form menjawab Location **tidak** dikumpulkan
- [ ] Policy menyebut Crashlytics; form mendeklarasikan Crash logs dan Diagnostics
- [ ] Policy menyebut audio diproses layanan Google; form mendeklarasikan Voice recordings
- [ ] Policy menyatakan riwayat hanya di perangkat; form **tidak** mendeklarasikannya sebagai collected
- [ ] Policy menyatakan tidak ada iklan dan tidak ada penjualan data; form tidak menandai purpose Advertising di butir mana pun
- [ ] Alamat email kontak di policy sudah diisi (masih placeholder saat dokumen ini ditulis)

---

## Yang berubah bila Firebase Analytics kelak dipasang

Analytics **sengaja tidak dipasang** di Fase 8 — lihat bagian *NOT Building* pada
plan. Bila kelak dipasang demi mengukur success metric PRD (retensi 7-hari,
pertanyaan per pengguna per minggu), form ini **wajib** diperbarui: tambah
*App activity → App interactions* sebagai collected, dan tinjau ulang butir
*Device or other IDs*. Mengirim Analytics tanpa memperbarui deklarasi adalah
pelanggaran kebijakan, bukan kelalaian administratif.
