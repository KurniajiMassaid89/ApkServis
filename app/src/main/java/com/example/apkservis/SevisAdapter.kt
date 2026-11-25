package com.example.apkservis

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class CardItemUI(
    val id: Int,
    val ikon: String,
    val judul: String,
    val subJudul: String,
    val teksKanan: String,
    val rawData: Any
)

class ServisAdapter(
    private var items: List<CardItemUI>,
    private val onClick: (CardItemUI) -> Unit,
    private val onLongClick: (CardItemUI) -> Unit
) : RecyclerView.Adapter<ServisAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIkon: TextView = view.findViewById(R.id.tvIkon)
        val tvJudul: TextView = view.findViewById(R.id.tvJudul)
        val tvSubJudul: TextView = view.findViewById(R.id.tvSubJudul)
        val tvKanan: TextView = view.findViewById(R.id.tvKanan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvIkon.text = item.ikon
        holder.tvJudul.text = item.judul
        holder.tvSubJudul.text = item.subJudul
        holder.tvKanan.text = item.teksKanan

        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CardItemUI>) {
        items = newItems
        notifyDataSetChanged()
    }
}