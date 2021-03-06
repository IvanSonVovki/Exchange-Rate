package com.ivanshulin.myexchange

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomRecyclerAdapter(private val name: List<ExchangeRate>) :
    RecyclerView.Adapter<CustomRecyclerAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCharCode: TextView = itemView.findViewById(R.id.tv_item_char_code)
        val tvName: TextView = itemView.findViewById(R.id.tv_item_name)
        val tvNominal: TextView = itemView.findViewById(R.id.tv_item_nominal)
        val tvCost: TextView = itemView.findViewById(R.id.tv_cost)
        val tvDifference: TextView = itemView.findViewById(R.id.tv_difference)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val difference = name[position].value.toDouble() - name[position].previous.toDouble()
        holder.tvCharCode.text = name[position].charCode
        holder.tvName.text = name[position].name
        holder.tvNominal.text = name[position].nominal
        holder.tvCost.text = DISPLAY_FORMAT.format(name[position].value.toDouble())
        holder.tvDifference.text = DISPLAY_FORMAT.format(difference)
        holder.tvDifference.setTextColor(setColorDifference(difference))
    }

    override fun getItemCount(): Int {
        return name.size
    }

    private fun setColorDifference(difference: Double): Int {
        return if (difference > 0) Color.GREEN else Color.RED
    }

}