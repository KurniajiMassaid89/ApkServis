package com.example.apkservis

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: ServisAdapter
    private var bulanTerpilih: String = ""
    private var labelBulanTerpilih: String = ""
    private var jobRekapan: Job? = null

    private lateinit var tvLabelBulan: TextView
    private lateinit var tvTotalSemua: TextView
    private lateinit var tvInfoServisTerakhir: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        val now = Date()
        bulanTerpilih = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(now)
        labelBulanTerpilih = SimpleDateFormat("MMM yyyy", Locale("id", "ID")).format(now)

        val rvKategori = findViewById<RecyclerView>(R.id.rvKategori)
        val btnTambahKategori = findViewById<Button>(R.id.btnTambahKategori)
        val btnPilihBulan = findViewById<View>(R.id.btnPilihBulan)
        val tvJumlahKategori = findViewById<TextView>(R.id.tvJumlahKategori)

        tvLabelBulan = findViewById(R.id.tvLabelBulan)
        tvTotalSemua = findViewById(R.id.tvTotalSemua)
        tvInfoServisTerakhir = findViewById(R.id.tvInfoServisTerakhir)

        rvKategori.layoutManager = LinearLayoutManager(this)
        adapter = ServisAdapter(
            items = emptyList(),
            onClick = { item ->
                val kat = item.rawData as KategoriServis
                val intent = Intent(this, DetailActivity::class.java).apply {
                    putExtra("KATEGORI_ID", kat.id)
                    putExtra("KATEGORI_NAMA", kat.namaKategori)
                    putExtra("KATEGORI_DESK", kat.deskripsi)
                }
                startActivity(intent)
            },
            onLongClick = { item ->
                val kat = item.rawData as KategoriServis
                dialogHapusKategori(kat)
            }
        )
        rvKategori.adapter = adapter

        lifecycleScope.launch {
            db.servisDao().getAllKategori().collect { listKategori ->
                tvJumlahKategori.text = "Daftar Kategori (${listKategori.size} aktif)"
                val uiList = listKategori.map {
                    CardItemUI(
                        id = it.id,
                        ikon = pilihIkonOtomatis(it.namaKategori),
                        judul = it.namaKategori,
                        subJudul = it.deskripsi,
                        teksKanan = "Detail ›",
                        rawData = it
                    )
                }
                adapter.updateData(uiList)
            }
        }

        btnPilihBulan.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, _ ->
                    cal.set(year, month, 1)
                    bulanTerpilih = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                    labelBulanTerpilih = SimpleDateFormat("MMM yyyy", Locale("id", "ID")).format(cal.time)
                    muatRekapanBulanan()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnTambahKategori.setOnClickListener { dialogTambahKategori() }
        muatRekapanBulanan()
    }

    private fun muatRekapanBulanan() {
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        tvLabelBulan.text = "Rekapan $labelBulanTerpilih"

        jobRekapan?.cancel()
        jobRekapan = lifecycleScope.launch {
            launch {
                db.servisDao().getTotalBiayaByBulan(bulanTerpilih).collect { total ->
                    tvTotalSemua.text = formatRp.format(total ?: 0.0)
                }
            }
            launch {
                db.servisDao().getServisTerakhirByBulan(bulanTerpilih).collect { terakhir ->
                    if (terakhir != null) {
                        tvInfoServisTerakhir.text = "Terakhir (${terakhir.tanggal}): ${terakhir.catatanPart}"
                    } else {
                        tvInfoServisTerakhir.text = "Belum ada servis tercatat di $labelBulanTerpilih"
                    }
                }
            }
        }
    }

    private fun pilihIkonOtomatis(nama: String): String {
        val lower = nama.lowercase()
        return when {
            lower.contains("oli") -> "🛢️"
            lower.contains("cvt") || lower.contains("belt") || lower.contains("roller") -> "⚙️"
            lower.contains("rem") || lower.contains("ban") || lower.contains("kaki") -> "🛞"
            lower.contains("aki") || lower.contains("lampu") || lower.contains("listrik") -> "⚡"
            else -> "🔧"
        }
    }

    private fun dialogTambahKategori() {
        val view = layoutInflater.inflate(R.layout.dialog_form, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val til1 = view.findViewById<TextInputLayout>(R.id.til1)
        val til2 = view.findViewById<TextInputLayout>(R.id.til2)
        val etNama = view.findViewById<TextInputEditText>(R.id.etInput1)
        val etDesk = view.findViewById<TextInputEditText>(R.id.etInput2)

        tvTitle.text = "Kategori Servis Baru"
        til1.hint = "Nama Kategori (mis. Ganti Oli)"
        til2.hint = "Catatan Interval (mis. Tiap 2.000 km)"

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nama = etNama.text.toString().trim()
                val desk = etDesk.text.toString().trim()
                if (nama.isEmpty()) {
                    til1.error = "Nama kategori wajib diisi"
                } else {
                    lifecycleScope.launch {
                        db.servisDao().insertKategori(
                            KategoriServis(
                                namaKategori = nama,
                                deskripsi = desk.ifEmpty { "Perawatan berkala" }
                            )
                        )
                    }
                    Toast.makeText(this, "Kategori ditambahkan!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun dialogHapusKategori(kat: KategoriServis) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Kategori")
            .setMessage("Hapus '${kat.namaKategori}' beserta semua riwayat servis di dalamnya?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch { db.servisDao().deleteKategori(kat) }
                Toast.makeText(this, "Kategori dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}