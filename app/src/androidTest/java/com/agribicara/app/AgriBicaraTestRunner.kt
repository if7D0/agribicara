package com.agribicara.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Runner instrumented yang menukar [AgriBicaraApp] dengan [Application] polos.
 *
 * Ada karena penyelidikan gantungan suite instrumented. Tanpa runner ini,
 * seluruh test instrumented berjalan di atas Application PRODUKSI: onCreate-nya
 * memasang App Check lewat jaringan, menyalakan Crashlytics, membuat channel
 * notifikasi, dan — yang paling merusak — memanggil
 * WeatherCheckScheduler.schedule(), yang mendaftarkan pekerjaan periodik
 * SUNGGUHAN ke WorkManager sistem.
 *
 * Akibatnya WeatherCheckWorker bisa bangun di tengah suite, membuka database
 * on-disk sungguhan dan menembak BMKG, sementara test yang sedang berjalan
 * memakai kolam thread Room yang sama. Test DAO yang isinya panggilan polos
 * lalu tertahan sampai timeout 60 detik milik runTest, atau lebih lama.
 *
 * Gejalanya menyesatkan justru karena bergantung waktu: satu paket dijalankan
 * sendiri selesai dalam 2 detik dan selalu hijau, sedangkan suite penuh yang
 * berjalan 100 detik memberi worker itu cukup waktu untuk bangun. Itulah
 * sebabnya gantungannya selama ini terbaca sebagai flaky tanpa pola — yang
 * bergeser bukan kesalahannya, melainkan test mana yang kebetulan sedang
 * berjalan saat worker menyala.
 *
 * Application polos aman untuk seluruh test yang ada: tidak satu pun meng-cast
 * ke [AgriBicaraApp], semuanya hanya memakainya sebagai Context.
 * ManifestInitializerTest membaca dari PackageManager, bukan dari instance
 * Application. WeatherCheckWorkerTest memakai TestListenableWorkerBuilder
 * dengan pabriknya sendiri, jadi tidak menuntut Configuration.Provider.
 *
 * Kalau kelak ada test instrumented yang butuh Hilt, runner ini harus diganti
 * HiltTestApplication, BUKAN dikembalikan ke AgriBicaraApp.
 */
class AgriBicaraTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, Application::class.java.name, context)
}
