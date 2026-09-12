# Checklist Rilis AgriBicara — langkah yang HARUS dikerjakan manusia

Berkas ini memuat bagian Fase 8 yang **tidak dikerjakan** oleh implementasi di
repo, dan tidak bisa: butuh akun, perangkat sungguhan, dan petani sungguhan.

Statusnya **belum dikerjakan** sampai Anda mencentangnya sendiri. Jangan
menganggap fase ini selesai hanya karena kodenya hijau.

> ## Jalur rilis berubah (2026-09-11): pilot dulu, Play Store belakangan
>
> "Selesai" kini berarti **pilot tervalidasi**, bukan aplikasi terbit di Play
> Store: APK rilis dibagikan ke 5–10 petani lewat **Firebase App Distribution**,
> dan metriknya kualitatif — umpan balik langsung dari petani, tanpa Firebase
> Analytics dan tanpa survei in-app.
>
> Alasannya biaya pintu masuk Play Console tidak sebanding sebelum aplikasi
> terbukti berguna: akun personal baru wajib menjalankan closed testing dengan
> 12 tester selama 14 hari berturut-turut (butir D5–D6) hanya untuk boleh
> *mengajukan* akses Production. Pilot menjawab pertanyaan yang lebih mendasar —
> apakah petani benar-benar lebih suka berbicara daripada membaca — dengan lima
> orang, bukan dua belas.
>
> **Yang berlaku sekarang: bagian P.** Bagian **A–D ditunda sampai setelah
> pilot**, dan sengaja TIDAK dihapus: seluruh isinya tetap berlaku begitu jalur
> Play ditempuh. Bagian E sebagian sudah diserap bagian P.

Yang **sudah** selesai dan terverifikasi di repo: signing config + AAB
bertanda tangan, Play Integrity di build rilis, Crashlytics, ikon adaptif +
themed + legacy, aset toko, privacy policy, jawaban Data Safety, dan teks
listing.

---

## Status akun

Diisi saat mengerjakan butir A1.

| Kolom | Isi |
|---|---|
| Akun Play Console ada? | *belum diperiksa* |
| Jenis akun | *personal / organisasi — belum diketahui* |
| Tanggal pembuatan akun | *belum diketahui* |
| App entry `com.agribicara.app` sudah ada? | *belum diketahui* |

> **Kenapa tanggal pembuatan akun penting.** Akun **personal** yang dibuat setelah
> **13 November 2023** wajib menjalankan **closed testing dengan minimal 12 tester
> yang opted-in selama 14 hari BERTURUT-TURUT** sebelum boleh mengajukan akses
> Production. Internal testing **tidak dihitung**. Akun organisasi tidak terkena
> syarat ini. Sumber: [App testing requirements for new personal developer
> accounts](https://support.google.com/googleplay/android-developer/answer/14151465).
>
> Konsekuensinya langsung mengenai rencana PRD: scope Fase 8 menulis "beta test
> 5-10 petani" dan success signal "minimal 5 tester". Untuk akun personal baru,
> angka itu **tidak cukup** untuk naik ke Production.

---

## A. Akun — *ditunda sampai setelah pilot*

- [ ] **A1.** Periksa https://play.google.com/console dan isi tabel *Status akun* di atas
- [ ] **A2.** Bila belum ada akun: daftar (biaya $25 sekali seumur hidup) dan selesaikan verifikasi identitas
- [ ] **A3.** Catat jenis akun dan tanggal pembuatannya — menentukan berlaku tidaknya syarat 12 tester

## B. App entry dan rantai penandatanganan — *B6–B8 SUDAH dikerjakan*

- [ ] **B1.** Buat app entry dengan package `com.agribicara.app`
- [ ] **B2.** Aktifkan **Play App Signing**
- [ ] **B3.** Unggah `app/build/outputs/bundle/release/app-release.aab`
- [ ] **B4.** Buka Play Console → **App signing**, salin **SHA-256 app signing key**
- [ ] **B5.** Buka **Firebase Console → Project settings → aplikasi Android**, tambahkan SHA-256 dari B4
- [x] **B6.** *(selesai 2026-09-11)* Tambahkan juga SHA-256 **upload key** bila Anda ingin APK rilis hasil build lokal ikut berfungsi:
      ```
      E6:3B:A6:52:C0:51:A9:83:A9:CE:03:54:AF:4D:A5:91:A7:E9:57:79:4A:06:AB:AC:6A:A0:23:11:9B:6D:72:68
      ```
      (dicetak 2026-09-02 dari `agribicara-upload.jks`; verifikasi ulang dengan
      `keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab`)
- [x] **B7.** *(selesai 2026-09-11)* Firebase Console → **App Check** → daftarkan penyedia **Play Integrity** untuk aplikasi ini
- [x] **B8.** *(selesai 2026-09-11)* Bila build rilis yang di-*sideload* juga perlu berfungsi, sesuaikan setelan App Check agar tidak menuntut label `PLAY_RECOGNIZED` — aplikasi di luar Play tidak pernah mendapatkannya
- [ ] **B9.** Hapus token debug App Check lama yang sudah tidak dipakai

> **Ini titik gagal paling berbahaya di seluruh Fase 8.** Bila SHA-256 yang
> terdaftar keliru, App Check menolak **setiap** panggilan Gemini pada aplikasi
> hasil Play. Build sukses, test hijau, aplikasi terpasang dan terbuka normal —
> dan setiap pertanyaan petani dijawab pesan kegagalan yang terlihat persis
> seperti gangguan jaringan biasa. Tidak ada satu pun pemeriksaan otomatis di
> repo ini yang bisa menangkapnya. Satu-satunya penawarnya adalah butir D2.

### Hasil B6–B8 (2026-09-11): fitur AI terbukti hidup di APK rilis sideload

Dikerjakan berurutan, dan urutannya penting — tiap kegagalan menunjuk butir yang
kurang. Di layar, ketiga kegagalan tampil dengan pesan yang **sama persis**
("Layanan jawaban sedang tidak bisa dihubungi"), jadi logcat adalah satu-satunya
cara membedakannya:

| Uji | Logcat | Artinya | Penawarnya |
|---|---|---|---|
| APK rilis, App Check apa adanya | `400 App not registered` | Penyedia Play Integrity belum terdaftar | B7 |
| Setelah B6 + B7 | `403 App attestation failed` | Token Play Integrity didapat, tapi verdict ditolak — aplikasi sideload dinilai `UNRECOGNIZED_VERSION` | B8 |
| Setelah B8 | `onRequestIntegrityToken` sukses, tanpa `Error obtaining AppCheck token` | **Lulus.** Pertanyaan lewat mic dijawab dan dibacakan TTS | — |

Tiga hal yang mudah salah dibaca:

1. **App Check TETAP `Enforced`.** Yang dilonggarkan hanya syarat
   `PLAY_RECOGNIZED`, bukan enforcement-nya. Penyalahguna kuota tetap harus
   memakai perangkat Android sungguhan dengan Play Services, jadi API ini tidak
   bisa ditembak dari skrip atau server. Opsi *Unenforced* sempat disiapkan
   sebagai pilihan terakhir dan ternyata **tidak diperlukan**.
2. **`Too many attempts` yang muncul sesudah tiap kegagalan bukan masalah
   terpisah** — itu backoff SDK. Force-stop aplikasi sebelum uji ulang.
3. **Pola ujinya:** `adb shell am force-stop com.agribicara.app && adb logcat -c`,
   uji di HP, lalu `adb logcat -d > logcat.txt`. Jangan jalankan `logcat` di
   background.

> **PENGINGAT untuk hari Anda pindah ke Play Store:** kembalikan syarat
> `PLAY_RECOGNIZED` begitu aplikasi didistribusikan lewat Play. Pelonggaran di
> B8 ada semata karena APK pilot dipasang langsung ke HP petani, di luar Play.
> Membiarkannya longgar sesudah itu berarti menerima APK hasil bongkar-pasang
> yang tidak dikenali Google sebagai klien yang sah.

## C. Listing dan kepatuhan — *ditunda, KECUALI C1–C5*

- [ ] **C1.** Buat repo publik baru, mis. `if7D0/agribicara-legal`
- [ ] **C2.** Salin `docs/legal/privacy-policy.html` ke repo itu sebagai `index.html`
- [ ] **C3.** **Isi alamat email kontak** di dalamnya — masih placeholder, dan halaman ini publik
- [ ] **C4.** Settings → Pages → Deploy from branch → `main` / `(root)`
- [ ] **C5.** Buka URL-nya dari jaringan seluler, bukan hanya dari komputer sendiri
- [ ] **C6.** Masukkan URL itu ke Play Console → Store settings → Privacy policy
- [ ] **C7.** Isi form **Data safety** persis mengikuti `docs/play/data-safety.md`
- [ ] **C8.** Jalankan checklist konsistensi silang di bagian 3 dokumen itu
- [ ] **C9.** Isi kuesioner **content rating**
- [ ] **C10.** Unggah `docs/play/assets/icon-512.png` dan `docs/play/assets/feature-graphic.png`
- [ ] **C11.** Ambil 4 screenshot dari perangkat sungguhan (perintah ada di `store-listing.md`) dan unggah
- [ ] **C12.** Isi nama aplikasi, deskripsi singkat, dan deskripsi penuh dari `docs/play/store-listing.md`
- [ ] **C13.** Tetapkan kategori **Weather**, email kontak, dan distribusi **Indonesia saja**

## D. Testing track — *ditunda sampai setelah pilot*

- [ ] **D1.** Unggah AAB ke **Internal Testing**, undang diri sendiri, pasang lewat Play
- [ ] **D2.** **Buktikan fitur AI hidup pada build hasil Play.** Tekan mic, ajukan pertanyaan nyata, pastikan jawabannya muncul. Ini pembuktian rantai App Check yang sesungguhnya; build lokal tidak membuktikan apa pun di sini
- [ ] **D3.** Picu satu crash non-fatal, pastikan muncul di konsol Crashlytics
- [ ] **D4.** Periksa ikon di launcher: normal, bulat, mode gelap, dan themed icon
- [ ] **D5.** Buat track **Closed Testing** dan rekrut **lebih dari 12 tester** — opt-out mengulang hitungan 14 hari dari nol, jadi cadangan itu perlu
- [ ] **D6.** Jaga minimal 12 tester tetap opted-in selama **14 hari berturut-turut**
- [ ] **D7.** Ajukan akses Production

## E. Beta test petani — dipindah dari Fase 7, *kini dikerjakan lewat bagian P*

Dua butir pertama adalah scope Fase 7 yang dipindah ke sini atas keputusan user,
karena keduanya menuntut hal yang tidak bisa disediakan Fase 7.

> **Jangan kerjakan bagian ini dua kali.** E1–E5 sudah diserap P6–P9; yang
> membedakan hanya jalur distribusinya (App Distribution, bukan Play closed
> testing). E6 tetap di sini karena tidak bergantung jalur mana pun.

- [ ] **E1.** Rekrut 5–10 petani sungguhan. Ini sekaligus sumber **aksen daerah untuk STT** — jauh lebih jujur daripada satu orang menirukan aksen
- [ ] **E2.** Uji di perangkat **Android 7 / RAM 2GB** sungguhan. Emulator sudah ditolak sebagai pengganti karena tidak menangkap perilaku termal maupun CPU nyata. Success signal Fase 7 "crash-free di device low-end" **belum pernah diuji sampai butir ini dikerjakan**
- [ ] **E3.** Kumpulkan umpan balik terhadap hipotesis inti PRD: apakah petani benar-benar lebih suka berbicara daripada membaca. Ini belum pernah divalidasi sejak PRD ditulis
- [ ] **E4.** Pantau Crashlytics terhadap target PRD crash-free ≥99%
- [ ] **E5.** Nyalakan **TalkBack** dan telusuri aplikasi. Belum pernah dilakukan sekali pun sejak Fase 6
- [ ] **E6.** Verifikasi notifikasi cuaca ekstrem benar-benar muncul di layar. Belum pernah terjadi — butuh BMKG meramalkan hujan ≥50 mm

---

## P. Pilot lewat Firebase App Distribution — **ini yang berlaku sekarang**

Enam kriteria di bawah inilah definisi "selesai" yang disepakati 2026-09-11.
Butir P1 sudah terpenuhi; sisanya menuntut akun, perangkat, dan manusia.

- [x] **P1.** Fitur AI terbukti menjawab di APK rilis hasil sideload *(2026-09-11, lihat hasil B6–B8 di atas)*
- [ ] **P2.** Salin cadangan `agribicara-upload.jks` ke **luar mesin ini** — flashdisk, atau ZIP berkata sandi di penyimpanan awan. Kehilangan upload key berarti tidak bisa lagi mengunggah update untuk `com.agribicara.app`, selamanya. **Kerjakan sebelum unggahan pertama, bukan sesudahnya**
- [ ] **P3.** Firebase Console → **App Distribution**: aktifkan, lalu buat grup tester `pilot-petani`
- [ ] **P4.** Terbitkan privacy policy lewat butir **C1–C5** (repo publik + GitHub Pages, dan **isi alamat email kontak** yang masih placeholder). Tanpa Play listing, halaman ini satu-satunya tempat petani bisa membacanya — dan suara mereka memang dikirim ke Gemini
- [ ] **P5.** Unggah APK rilis ke App Distribution, lalu **pasang langsung di HP petani saat bertemu**. Alur undangan email + aplikasi App Tester terlalu berat untuk pembaca yang dituju aplikasi ini
- [ ] **P6.** Rekrut 5–10 petani dan catat umpan baliknya: suara vs membaca, STT menangkap aksen daerah atau tidak, hasil deteksi membantu atau tidak *(menggantikan E1 dan E3)*
- [ ] **P7.** Uji di perangkat murah sungguhan, idealnya Android 7 / RAM 2GB — pinjam dari salah satu petani pilot *(menggantikan E2)*
- [ ] **P8.** Nyalakan **TalkBack** sekali dan telusuri aplikasi; coba juga "Ambil foto" lewat kamera langsung *(menggantikan E5)*
- [ ] **P9.** Pantau **Crashlytics** selama pilot, target crash-free ≥99% *(menggantikan E4)*

**Diterima tetap tak terbukti:** notifikasi cuaca ekstrem di layar (butir E6)
menuntut BMKG meramalkan hujan ≥50 mm. Kalau itu tidak terjadi selama pilot,
catat jujur di report Fase 8 — jangan diklaim bekerja.

**Naikkan versi tiap build pilot** (mis. `1.0.0` → `1.0.1-pilot`). App
Distribution menampilkannya ke tester, dan Crashlytics memisahkan laporan per
versi; dua build berbeda dengan versi sama tidak bisa dipisahkan lagi
belakangan.

---

## Keputusan yang perlu diangkat sebelum Production

- [ ] **Firebase Analytics.** Sengaja tidak dipasang di Fase 8. Akibatnya **tiga dari lima success metric PRD tidak bisa diukur sama sekali**: retensi 7-hari, pertanyaan per pengguna aktif per minggu, dan persentase DAU pemakai deteksi penyakit. Crashlytics juga kehilangan breadcrumb otomatis dan metrik "crash-free users". Putuskan sadar sebelum Production, dan bila dipasang, perbarui `data-safety.md`
- [ ] **Cadangkan `agribicara-upload.jks` di luar repo.** Kehilangannya berarti tidak bisa lagi mengunggah update untuk `com.agribicara.app`, selamanya. Play App Signing menyelamatkan app signing key milik Google, **bukan** upload key Anda
- [ ] **Gerbang dedup peringatan (H3).** Hanya mengingat satu peringatan terakhir. Celahnya sudah dipaku test di Fase 7, tetapi belum diperbaiki; perbaikannya butuh perubahan skema
