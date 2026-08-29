package com.agribicara.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.entity.UserPreferenceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferenceDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: UserPreferenceDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.userPreferenceDao()
    }

    @After
    fun tearDown() {
        // Wajib ditutup supaya test tidak saling bocor
        database.close()
    }

    @Test
    fun observeMengembalikanNullSaatDatabaseKosong() = runTest {
        assertNull(dao.observe(Constants.USER_PREFERENCE_ID).first())
    }

    @Test
    fun upsertLaluObserveMengembalikanEntitasYangSama() = runTest {
        // Arrange
        val entity = UserPreferenceEntity(isOnboardingCompleted = true)

        // Act
        dao.upsert(entity)
        val observed = dao.observe(Constants.USER_PREFERENCE_ID).first()

        // Assert
        assertTrue(observed!!.isOnboardingCompleted)
        assertEquals(Constants.USER_PREFERENCE_ID, observed.id)
    }

    @Test
    fun upsertDuaKaliHanyaMenyisakanSatuBarisDenganNilaiTerbaru() = runTest {
        // Arrange & Act
        dao.upsert(UserPreferenceEntity(isOnboardingCompleted = false))
        dao.upsert(
            UserPreferenceEntity(
                isOnboardingCompleted = true,
                regionCode = "32.01",
                regionName = "Bogor",
            ),
        )

        // Assert
        val observed = dao.observe(Constants.USER_PREFERENCE_ID).first()
        assertTrue(observed!!.isOnboardingCompleted)
        assertEquals("32.01", observed.regionCode)
        assertEquals("Bogor", observed.regionName)
    }

    @Test
    fun getMengembalikanEntitasYangDisimpan() = runTest {
        dao.upsert(UserPreferenceEntity(isOnboardingCompleted = true))
        val fetched = dao.get(Constants.USER_PREFERENCE_ID)
        assertTrue(fetched!!.isOnboardingCompleted)
    }

    @Test
    fun markOnboardingCompletedMenyisipkanBarisSaatBelumAda() = runTest {
        dao.markOnboardingCompleted(Constants.USER_PREFERENCE_ID)

        val fetched = dao.get(Constants.USER_PREFERENCE_ID)
        assertTrue(fetched!!.isOnboardingCompleted)
    }

    @Test
    fun markOnboardingCompletedTidakMenimpaRegionYangSudahAda() = runTest {
        // Regresi: read-modify-write sebelumnya bisa mengembalikan region ke null
        // bila penulis lain (region picker Fase 2) menulis di sela baca dan tulis.
        dao.upsert(
            UserPreferenceEntity(
                isOnboardingCompleted = false,
                regionCode = "32.01",
                regionName = "Bogor",
            ),
        )

        dao.markOnboardingCompleted(Constants.USER_PREFERENCE_ID)

        val fetched = dao.get(Constants.USER_PREFERENCE_ID)
        assertTrue(fetched!!.isOnboardingCompleted)
        assertEquals("32.01", fetched.regionCode)
        assertEquals("Bogor", fetched.regionName)
    }
}
