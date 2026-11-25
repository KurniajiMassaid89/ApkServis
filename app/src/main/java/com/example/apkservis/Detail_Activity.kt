package com.example.apkservis

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
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
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: ServisAdapter
    private var kategoriId: Int = -1
    private var namaKategori: String = ""
    private var deskKategori: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        db = AppDatabase.getDatabase(this)
        kategoriId = intent.getIntExtra("KATEGORI_ID", -1)
        namaKategori = intent.getStringExtra("KATEGORI_NAMA") ?: "Detail Servis"
        deskKategori = intent.getStringExtra("KATEGORI_DESK") ?: ""

        val tvNamaKategoriDetail = findViewById<TextView>(R.id.tvNamaKategoriDetail)
        val tvTotalBiaya = findViewById<TextView>(R.id.tvTotalBiaya)
        val tvInfoRiwayat = findViewById<TextView>(R.id.tvInfoRiwayat)
        val rvRiwayat = findViewById<RecyclerView>(R.id.rvRiwayat)
        val btnTambahRiwayat = findViewById<Button>(R.id.btnTambahRiwayat)
        val btnBack = findViewById<TextView>(R.id.btnBack)
        val btnHapusKategoriDetail = findViewById<TextView>(R.id.btnHapusKategoriDetail)

        tvNamaKategoriDetail.text = namaKategori
        btnBack.setOnClickListener { finish() }

        btnHapusKategoriDetail.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Kategori")
                .setMessage("Hapus '$namaKategori' beserta seluruh riwayat servisnya?")
                .setPositiveButton("Hapus") { _, _ ->
                    lifecycleScope.launch {
                        db.servisDao().deleteKategori(
                            KategoriServis(id = kategoriId, namaKategori = namaKategori, deskripsi = deskKategori)
                        )
                        finish()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }

        rvRiwayat.layoutManager = LinearLayoutManager(this)
        adapter = ServisAdapter(
            items = emptyList(),
            onClick = { },
            onLongClick = { item ->
                val riwayat = item.rawData as RiwayatServis
                dialogHapusRiwayat(riwayat)
            }
        )
        rvRiwayat.adapter = adapter

        lifecycleScope.launch {
            db.servisDao().getRiwayatByKategori(kategoriId).collect { listRiwayat ->
                tvInfoRiwayat.text = "Total pengeluaran dari ${listRiwayat.size} kali servis"
                val uiList = listRiwayat.map {
                    CardItemUI(
                        id = it.id,
                        ikon = "🧾",
                        judul = "+ ${formatRp.format(it.biaya)}",
                        subJudul = "${it.catatanPart}  •  Odo ${it.odometer} km",
                        teksKanan = it.tanggal,
                        rawData = it
                    )
                }
                adapter.updateData(uiList)
            }
        }

        lifecycleScope.launch {
            db.servisDao().getTotalBiayaByKategori(kategoriId).collect { total ->
                tvTotalBiaya.text = formatRp.format(total ?: 0.0)
            }
        }

        btnTambahRiwayat.setOnClickListener { dialogTambahRiwayat() }
    }

    private fun dialogTambahRiwayat() {
        val view = layoutInflater.inflate(R.layout.dialog_form, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val til1 = view.findViewById<TextInputLayout>(R.id.til1)
        val til2 = view.findViewById<TextInputLayout>(R.id.til2)
        val til3 = view.findViewById<TextInputLayout>(R.id.til3)
        val til4 = view.findViewById<TextInputLayout>(R.id.til4)

        val etTanggal = view.findViewById<TextInputEditText>(R.id.etInput1)
        val etPart = view.findViewById<TextInputEditText>(R.id.etInput2)
        val etOdometer = view.findViewById<TextInputEditText>(R.id.etInput3)
        val etBiaya = view.findViewById<TextInputEditText>(R.id.etInput4)

        til3.visibility = View.VISIBLE
        til4.visibility = View.VISIBLE

        tvTitle.text = "Catat Servis Baru"

        val sdfTampil = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val sdfBulan = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        var bulanTerpilih = sdfBulan.format(Date())

        etTanggal.setText(sdfTampil.format(Date()))
        til1.hint = "Tanggal Servis (Klik untuk ganti)"
        etTanggal.isFocusable = false
        etTanggal.isClickable = true
        etTanggal.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                etTanggal.setText(sdfTampil.format(cal.time))
                bulanTerpilih = sdfBulan.format(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        til2.hint = "Part / Tindakan (mis. Oli SPX2 + Gardan)"
        etPart.inputType = InputType.TYPE_CLASS_TEXT

        til3.hint = "Odometer saat servis (km)"
        etOdometer.inputType = InputType.TYPE_CLASS_NUMBER

        til4.hint = "Total Biaya (Rp)"
        etBiaya.inputType = InputType.TYPE_CLASS_NUMBER

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val tgl = etTanggal.text.toString().trim()
                val part = etPart.text.toString().trim()
                val odoTxt = etOdometer.text.toString().trim()
                val biayaTxt = etBiaya.text.toString().trim()

                til2.error = if (part.isEmpty()) "Wajib diisi" else null
                til3.error = if (odoTxt.isEmpty()) "Wajib diisi" else null
                til4.error = if (biayaTxt.isEmpty()) "Wajib diisi" else null

                if (part.isNotEmpty() && odoTxt.isNotEmpty() && biayaTxt.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.servisDao().insertRiwayat(
                            RiwayatServis(
                                kategoriId = kategoriId,
                                tanggal = tgl,
                                bulanTahun = bulanTerpilih,
                                odometer = odoTxt.toInt(),
                                catatanPart = part,
                                biaya = biayaTxt.toDouble()
                            )
                        )
                    }
                    Toast.makeText(this, "Riwayat servis tersimpan!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun dialogHapusRiwayat(riwayat: RiwayatServis) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Riwayat")
            .setMessage("Hapus catatan '${riwayat.catatanPart}'?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch { db.servisDao().deleteRiwayat(riwayat) }
                Toast.makeText(this, "Riwayat dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}