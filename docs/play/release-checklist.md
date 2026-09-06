# Checklist Rilis AgriBicara — langkah yang HARUS dikerjakan manusia

Berkas ini memuat bagian Fase 8 yang **tidak dikerjakan** oleh implementasi di
repo, dan tidak bisa: butuh akun Play Console, perangkat sungguhan, dan petani
sungguhan.

Statusnya **belum dikerjakan** sampai Anda mencentangnya sendiri. Jangan
menganggap fase ini selesai hanya karena kodenya hijau.

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

## A. Akun

- [ ] **A1.** Periksa https://play.google.com/console dan isi tabel *Status akun* di atas
- [ ] **A2.** Bila belum ada akun: daftar (biaya $25 sekali seumur hidup) dan selesaikan verifikasi identitas
- [ ] **A3.** Catat jenis akun dan tanggal pembuatannya — menentukan berlaku tidaknya syarat 12 tester

## B. App entry dan rantai penandatanganan

- [ ] **B1.** Buat app entry dengan package `com.agribicara.app`
- [ ] **B2.** Aktifkan **Play App Signing**
- [ ] **B3.** Unggah `app/build/outputs/bundle/release/app-release.aab`
- [ ] **B4.** Buka Play Console → **App signing**, salin **SHA-256 app signing key**
- [ ] **B5.** Buka **Firebase Console → Project settings → aplikasi Android**, tambahkan SHA-256 dari B4
- [ ] **B6.** Tambahkan juga SHA-256 **upload key** bila Anda ingin APK rilis hasil build lokal ikut berfungsi:
      ```
      E6:3B:A6:52:C0:51:A9:83:A9:CE:03:54:AF:4D:A5:91:A7:E9:57:79:4A:06:AB:AC:6A:A0:23:11:9B:6D:72:68
      ```
      (dicetak 2026-09-02 dari `agribicara-upload.jks`; verifikasi ulang dengan
      `keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab`)
- [ ] **B7.** Firebase Console → **App Check** → daftarkan penyedia **Play Integrity** untuk aplikasi ini
- [ ] **B8.** Bila build rilis yang di-*sideload* juga perlu berfungsi, sesuaikan setelan App Check agar tidak menuntut label `PLAY_RECOGNIZED` — aplikasi di luar Play tidak pernah mendapatkannya
- [ ] **B9.** Hapus token debug App Check lama yang sudah tidak dipakai

> **Ini titik gagal paling berbahaya di seluruh Fase 8.** Bila SHA-256 yang
> terdaftar keliru, App Check menolak **setiap** panggilan Gemini pada aplikasi
> hasil Play. Build sukses, test hijau, aplikasi terpasang dan terbuka normal —
> dan setiap pertanyaan petani dijawab pesan kegagalan yang terlihat persis
> seperti gangguan jaringan biasa. Tidak ada satu pun pemeriksaan otomatis di
> repo ini yang bisa menangkapnya. Satu-satunya penawarnya adalah butir D2.

## C. Listing dan kepatuhan

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

## D. Testing track

- [ ] **D1.** Unggah AAB ke **Internal Testing**, undang diri sendiri, pasang lewat Play
- [ ] **D2.** **Buktikan fitur AI hidup pada build hasil Play.** Tekan mic, ajukan pertanyaan nyata, pastikan jawabannya muncul. Ini pembuktian rantai App Check yang sesungguhnya; build lokal tidak membuktikan apa pun di sini
- [ ] **D3.** Picu satu crash non-fatal, pastikan muncul di konsol Crashlytics
- [ ] **D4.** Periksa ikon di launcher: normal, bulat, mode gelap, dan themed icon
- [ ] **D5.** Buat track **Closed Testing** dan rekrut **lebih dari 12 tester** — opt-out mengulang hitungan 14 hari dari nol, jadi cadangan itu perlu
- [ ] **D6.** Jaga minimal 12 tester tetap opted-in selama **14 hari berturut-turut**
- [ ] **D7.** Ajukan akses Production

## E. Beta test petani — dipindah dari Fase 7

Dua butir pertama adalah scope Fase 7 yang dipindah ke sini atas keputusan user,
karena keduanya menuntut hal yang tidak bisa disediakan Fase 7.

- [ ] **E1.** Rekrut 5–10 petani sungguhan. Ini sekaligus sumber **aksen daerah untuk STT** — jauh lebih jujur daripada satu orang menirukan aksen
- [ ] **E2.** Uji di perangkat **Android 7 / RAM 2GB** sungguhan. Emulator sudah ditolak sebagai pengganti karena tidak menangkap perilaku termal maupun CPU nyata. Success signal Fase 7 "crash-free di device low-end" **belum pernah diuji sampai butir ini dikerjakan**
- [ ] **E3.** Kumpulkan umpan balik terhadap hipotesis inti PRD: apakah petani benar-benar lebih suka berbicara daripada membaca. Ini belum pernah divalidasi sejak PRD ditulis
- [ ] **E4.** Pantau Crashlytics terhadap target PRD crash-free ≥99%
- [ ] **E5.** Nyalakan **TalkBack** dan telusuri aplikasi. Belum pernah dilakukan sekali pun sejak Fase 6
- [ ] **E6.** Verifikasi notifikasi cuaca ekstrem benar-benar muncul di layar. Belum pernah terjadi — butuh BMKG meramalkan hujan ≥50 mm

---

## Keputusan yang perlu diangkat sebelum Production

- [ ] **Firebase Analytics.** Sengaja tidak dipasang di Fase 8. Akibatnya **tiga dari lima success metric PRD tidak bisa diukur sama sekali**: retensi 7-hari, pertanyaan per pengguna aktif per minggu, dan persentase DAU pemakai deteksi penyakit. Crashlytics juga kehilangan breadcrumb otomatis dan metrik "crash-free users". Putuskan sadar sebelum Production, dan bila dipasang, perbarui `data-safety.md`
- [ ] **Cadangkan `agribicara-upload.jks` di luar repo.** Kehilangannya berarti tidak bisa lagi mengunggah update untuk `com.agribicara.app`, selamanya. Play App Signing menyelamatkan app signing key milik Google, **bukan** upload key Anda
- [ ] **Gerbang dedup peringatan (H3).** Hanya mengingat satu peringatan terakhir. Celahnya sudah dipaku test di Fase 7, tetapi belum diperbaiki; perbaikannya butuh perubahan skema
