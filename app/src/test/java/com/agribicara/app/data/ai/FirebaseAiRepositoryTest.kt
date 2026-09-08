package com.agribicara.app.data.ai

import android.content.Context
import com.agribicara.app.R
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.core.common.NetworkResult
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Kebijakan di sekeliling panggilan AI: mengulang, berhenti, dan memilih pesan.
 *
 * Inilah kode yang menentukan apa yang dibaca petani ketika sesuatu gagal —
 * dan kegagalan adalah keadaan normal, bukan kasus tepi, pada sinyal desa.
 * Sebelumnya kelas ini nol test karena membangun model Firebase menuntut
 * FirebaseApp yang hidup; [AiTextGenerator] dibuat justru untuk membuka ini.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAiRepositoryTest {

    private val context = mockk<Context>()

    private val dispatchers = object : DispatcherProvider {
        private val test = UnconfinedTestDispatcher()
        override val io = test
        override val default = test
        override val main = test
    }

    /**
     * Generator palsu yang menjalankan satu perilaku per panggilan.
     *
     * Sengaja bukan mock: jumlah panggilan adalah bagian dari yang diuji
     * (percobaan ulang tepat [Constants.AI_RETRY_COUNT] kali), dan menghitung
     * sendiri lebih jelas dibaca daripada rangkaian `coVerify`.
     */
    private class FakeGenerator(
        private vararg val behaviours: () -> String?,
    ) : AiTextGenerator {
        var calls = 0
            private set

        override suspend fun generate(prompt: String): String? {
            val behaviour = behaviours[minOf(calls, behaviours.size - 1)]
            calls++
            return behaviour()
        }
    }

    /**
     * Exception yang `cause`-nya dihitung saat dibaca, bukan disimpan.
     *
     * Satu-satunya cara membentuk rantai `cause` SIKLIK dari test: konstruktor
     * dan `initCause` masing-masing hanya bisa dipakai sekali, dan menembus
     * field-nya dengan refleksi ditolak JPMS.
     */
    private class SiklikException(private val penyebab: () -> Throwable?) :
        Exception("siklik") {
        override val cause: Throwable? get() = penyebab()
    }

    private fun repository(
        generator: AiTextGenerator,
        isDebugBuild: Boolean = true,
    ) = FirebaseAiRepository(
        generator = generator,
        dispatchers = dispatchers,
        context = context,
        isDebugBuild = isDebugBuild,
    )

    @Before
    fun setUp() {
        // Tiap pesan dibedakan supaya assertion benar-benar menguji PILIHAN
        // pesannya, bukan sekadar "ada pesan".
        every { context.getString(R.string.error_ai_offline) } returns "offline"
        every { context.getString(R.string.error_ai_timeout) } returns "timeout"
        every { context.getString(R.string.error_ai_unavailable) } returns "unavailable"
        every { context.getString(R.string.error_ai_blocked) } returns "blocked"
        every { context.getString(R.string.error_ai_app_check_debug) } returns "appcheck-debug"
    }

    @Test
    fun `jawaban model dikembalikan sebagai Success`() = runTest {
        val generator = FakeGenerator({ "Hari ini cerah." })

        val result = repository(generator).ask("kapan memupuk")

        assertEquals(NetworkResult.Success("Hari ini cerah."), result)
        assertEquals(1, generator.calls)
    }

    @Test
    fun `spasi di ujung jawaban dibuang`() = runTest {
        val generator = FakeGenerator({ "  Hari ini cerah.\n" })

        val result = repository(generator).ask("kapan memupuk")

        assertEquals(NetworkResult.Success("Hari ini cerah."), result)
    }

    @Test
    fun `jawaban null menjadi pesan diblokir dan TIDAK diulang`() = runTest {
        // Filter keamanan Gemini mengembalikan text null. Mengulang tidak akan
        // mengubah apa pun, hanya menahan layar dua kali lebih lama.
        val generator = FakeGenerator({ null })

        val result = repository(generator).ask("pertanyaan terlarang")

        assertEquals(NetworkResult.Error("blocked"), result)
        assertEquals(1, generator.calls)
    }

    @Test
    fun `jawaban berisi spasi saja diperlakukan sama dengan kosong`() = runTest {
        // Kalau lolos, petani melihat layar jawaban kosong tanpa penjelasan.
        val generator = FakeGenerator({ "   " })

        val result = repository(generator).ask("kapan memupuk")

        assertEquals(NetworkResult.Error("blocked"), result)
    }

    @Test
    fun `kegagalan sesaat diulang sekali lalu berhasil`() = runTest {
        val generator = FakeGenerator(
            { throw IOException("TLS handshake gagal") },
            { "Besok cerah." },
        )

        val result = repository(generator).ask("kapan memupuk")

        assertEquals(NetworkResult.Success("Besok cerah."), result)
        assertEquals(2, generator.calls)
    }

    @Test
    fun `percobaan berhenti tepat setelah AI_RETRY_COUNT pengulangan`() = runTest {
        val generator = FakeGenerator({ throw IOException("jaringan putus") })

        repository(generator).ask("kapan memupuk")

        // Bukan angka ajaib: kalau AI_RETRY_COUNT berubah, test ikut berubah.
        assertEquals(Constants.AI_RETRY_COUNT + 1, generator.calls)
    }

    @Test
    fun `kegagalan jaringan memberi pesan offline`() = runTest {
        val generator = FakeGenerator({ throw UnknownHostException("tidak ada DNS") })

        val result = repository(generator).ask("kapan memupuk")

        assertEquals("offline", (result as NetworkResult.Error).message)
    }

    @Test
    fun `IOException yang terbungkus tetap terbaca sebagai masalah jaringan`() = runTest {
        // SDK Firebase membungkus kegagalan jaringan di dalam exception-nya
        // sendiri, jadi rantai `cause` HARUS ditelusuri, bukan lapisan teratas.
        val generator = FakeGenerator({
            throw IllegalStateException("gagal", IOException("socket tertutup"))
        })

        val result = repository(generator).ask("kapan memupuk")

        assertEquals("offline", (result as NetworkResult.Error).message)
    }

    @Test
    fun `timeout dibedakan dari tidak ada jaringan`() = runTest {
        val generator = FakeGenerator({ throw AiTimeoutException() })

        val result = repository(generator).ask("kapan memupuk")

        // "Lambat" dan "tidak tersambung" menuntut tindakan berbeda: yang satu
        // layak dicoba lagi sekarang, yang lain tidak akan berhasil sampai
        // sinyalnya kembali.
        assertEquals("timeout", (result as NetworkResult.Error).message)
    }

    @Test
    fun `kegagalan selain jaringan memberi pesan layanan`() = runTest {
        val generator = FakeGenerator({ throw IllegalStateException("kuota habis") })

        val result = repository(generator).ask("kapan memupuk")

        assertEquals("unavailable", (result as NetworkResult.Error).message)
    }

    @Test
    fun `penyebab asli ikut dibawa untuk log tapi pesannya tetap ramah`() = runTest {
        val penyebab = IllegalStateException("kuota habis")
        val generator = FakeGenerator({ throw penyebab })

        val result = repository(generator).ask("kapan memupuk") as NetworkResult.Error

        assertEquals(penyebab, result.cause)
        assertEquals("unavailable", result.message)
    }

    @Test
    fun `penolakan App Check di build debug menyebut penyebabnya terang-terangan`() = runTest {
        val generator = FakeGenerator({ throw AiAppCheckException() })

        val result = repository(generator, isDebugBuild = true).ask("kapan memupuk")

        // Pesan generik di sini pernah menyembunyikan fitur yang sebenarnya
        // utuh selama satu sesi penuh; hanya SETELANNYA yang salah.
        assertEquals("appcheck-debug", (result as NetworkResult.Error).message)
    }

    @Test
    fun `penolakan App Check di build rilis tetap memberi pesan layanan biasa`() = runTest {
        val generator = FakeGenerator({ throw AiAppCheckException() })

        val result = repository(generator, isDebugBuild = false).ask("kapan memupuk")

        // Petani tidak punya Firebase Console. Menyebut "debug token" kepadanya
        // hanya menakutkan tanpa memberi satu pun langkah yang bisa ia ambil.
        // Inilah cabang yang tidak akan pernah bisa diuji seandainya
        // BuildConfig.DEBUG dibaca langsung — ia selalu true di unit test.
        assertEquals("unavailable", (result as NetworkResult.Error).message)
    }

    @Test
    fun `penolakan App Check yang terbungkus IOException tidak salah didiagnosis offline`() =
        runTest {
            // Penukaran token bisa gagal sebagai kegagalan jaringan. Kalau
            // cabang IOException diperiksa lebih dulu, developer dikirim
            // memburu sinyal padahal masalahnya pendaftaran token.
            val generator = FakeGenerator({
                throw AiAppCheckException(IOException("gagal menukar token"))
            })

            val result = repository(generator, isDebugBuild = true).ask("kapan memupuk")

            assertEquals("appcheck-debug", (result as NetworkResult.Error).message)
        }

    @Test(timeout = 1_000)
    fun `rantai cause siklik tidak membuat pemetaan pesan berputar selamanya`() = runTest {
        // A.cause = B, B.cause = A. Tanpa .take(), generateSequence berputar
        // SELAMANYA dan aplikasi menggantung di dalam penanganan kegagalan.
        //
        // timeout WAJIB ada di sini: tanpanya kegagalan test ini berupa build
        // yang menggantung tanpa batas di CI, bukan test merah yang menjelaskan
        // dirinya sendiri.
        // Siklusnya dibuat dengan meng-override `cause`, BUKAN lewat refleksi:
        // `Throwable.cause` hanya bisa diisi sekali lewat konstruktor/initCause,
        // dan menembusnya dengan setAccessible ditolak JPMS
        // ("module java.base does not opens java.lang").
        var b: Throwable? = null
        val a = SiklikException { b }
        b = SiklikException { a }

        val generator = FakeGenerator({ throw b as Throwable })

        val result = repository(generator).ask("kapan memupuk")

        // Selesai, dan pesannya tetap yang benar untuk kegagalan tak dikenal.
        assertEquals("unavailable", (result as NetworkResult.Error).message)
    }

    @Test
    fun `pembatalan coroutine diteruskan bukan ditelan sebagai kegagalan`() = runTest {
        // Menelan CancellationException membuat percobaan ulang berjalan di
        // coroutine yang sudah dibatalkan — structured concurrency rusak tanpa
        // gejala yang terlihat.
        val generator = FakeGenerator({ throw CancellationException("dibatalkan") })

        var terlempar = false
        try {
            repository(generator).ask("kapan memupuk")
        } catch (e: CancellationException) {
            terlempar = true
        }

        assertTrue("CancellationException harus diteruskan", terlempar)
        assertEquals("tidak boleh diulang setelah dibatalkan", 1, generator.calls)
    }
}
