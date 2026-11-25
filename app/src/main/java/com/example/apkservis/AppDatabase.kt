package com.example.apkservis

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "kategori_servis")
data class KategoriServis(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val namaKategori: String,
    val deskripsi: String
)

@Entity(
    tableName = "riwayat_servis",
    foreignKeys = [
        ForeignKey(
            entity = KategoriServis::class,
            parentColumns = ["id"],
            childColumns = ["kategoriId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RiwayatServis(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val kategoriId: Int,
    val tanggal: String,      // Format tampil: "18 Nov 2026"
    val bulanTahun: String,   // Format filter: "2026-11"
    val odometer: Int,
    val catatanPart: String,
    val biaya: Double
)

@Dao
interface ServisDao {
    @Insert
    suspend fun insertKategori(kategori: KategoriServis)

    @Query("SELECT * FROM kategori_servis ORDER BY id DESC")
    fun getAllKategori(): Flow<List<KategoriServis>>

    @Delete
    suspend fun deleteKategori(kategori: KategoriServis)

    @Insert
    suspend fun insertRiwayat(riwayat: RiwayatServis)

    @Query("SELECT * FROM riwayat_servis WHERE kategoriId = :katId ORDER BY id DESC")
    fun getRiwayatByKategori(katId: Int): Flow<List<RiwayatServis>>

    @Query("SELECT SUM(biaya) FROM riwayat_servis WHERE kategoriId = :katId")
    fun getTotalBiayaByKategori(katId: Int): Flow<Double?>

    // Rekapan Total Biaya Per Bulan
    @Query("SELECT SUM(biaya) FROM riwayat_servis WHERE bulanTahun = :bulanTahun")
    fun getTotalBiayaByBulan(bulanTahun: String): Flow<Double?>

    // Ambil 1 Servis Terakhir pada Bulan Tersebut untuk Info Ringkasan Beranda
    @Query("SELECT * FROM riwayat_servis WHERE bulanTahun = :bulanTahun ORDER BY id DESC LIMIT 1")
    fun getServisTerakhirByBulan(bulanTahun: String): Flow<RiwayatServis?>

    @Delete
    suspend fun deleteRiwayat(riwayat: RiwayatServis)
}

@Database(entities = [KategoriServis::class, RiwayatServis::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun servisDao(): ServisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ApkServis.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}