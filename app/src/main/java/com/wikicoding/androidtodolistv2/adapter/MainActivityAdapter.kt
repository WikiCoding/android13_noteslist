package com.wikicoding.androidtodolistv2.adapter

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.wikicoding.androidtodolistv2.R
import com.wikicoding.androidtodolistv2.model.TaskEntity

class MainActivityAdapter(private val list: ArrayList<TaskEntity>) :
    RecyclerView.Adapter<MainActivityAdapter.AdapterVH>() {

    private var onClicked: OnClickList? = null

    inner class AdapterVH(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        return AdapterVH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val model = list[position]
        holder.itemView.findViewById<TextView>(R.id.titleTv).text = model.title
        holder.itemView.findViewById<TextView>(R.id.timeTv).text = model.timestamp

        /** setting the white and grey stripes **/
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.colorLightGray
                )
            )
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
        }

        if (model.completed) {
            holder.itemView.findViewById<TextView>(R.id.titleTv)
                .setTextColor(Color.parseColor("#808080"))

            holder.itemView.findViewById<TextView>(R.id.titleTv).paintFlags = holder.itemView
                .findViewById<TextView>(R.id.titleTv).paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            holder.itemView.findViewById<TextView>(R.id.timeTv).paintFlags = holder.itemView
                .findViewById<TextView>(R.id.timeTv).paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.itemView.findViewById<TextView>(R.id.titleTv).paintFlags = Paint.HINTING_ON

            holder.itemView.findViewById<TextView>(R.id.timeTv).paintFlags = Paint.HINTING_ON

            holder.itemView.findViewById<TextView>(R.id.titleTv)
                .setTextColor(Color.BLACK)
        }

        holder.itemView.setOnClickListener {
            if (onClicked != null) {
                val indexClicked = list.indexOf(model)
                model.completed = !list[indexClicked].completed
                onClicked!!.onClick(indexClicked, model)

                holder.itemView.findViewById<TextView>(R.id.titleTv)
                    .setTextColor(Color.parseColor("#808080"))

                holder.itemView.findViewById<TextView>(R.id.titleTv).paintFlags = holder.itemView
                    .findViewById<TextView>(R.id.titleTv).paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                holder.itemView.findViewById<TextView>(R.id.timeTv).paintFlags = holder.itemView
                    .findViewById<TextView>(R.id.timeTv).paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface OnClickList {
        fun onClick(position: Int, model: TaskEntity)
    }

    fun setOnClick(onClick: OnClickList) {
        this.onClicked = onClick
    }

    fun findSwipedItem(position: Int): TaskEntity {
        return list[position]
    }
}