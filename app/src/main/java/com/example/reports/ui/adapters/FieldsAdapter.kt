package com.example.reports.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.Field

class FieldsAdapter(
    private val onEdit: (Field) -> Unit,
    private val onDelete: (Field) -> Unit
) : ListAdapter<Field, FieldsAdapter.FieldViewHolder>(FieldDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_field, parent, false)
        return FieldViewHolder(view)
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FieldViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvName)
        private val tvType = itemView.findViewById<TextView>(R.id.tvType)

        fun bind(field: Field) {
            tvName.text = field.name
            tvType.text = "${field.type.name} • ${if (field.isRequired) "Обязательное" else "Необязательное"}"
            
            itemView.setOnClickListener { onEdit(field) }
            itemView.setOnLongClickListener {
                onDelete(field)
                true
            }
        }
    }

    class FieldDiffCallback : DiffUtil.ItemCallback<Field>() {
        override fun areItemsTheSame(oldItem: Field, newItem: Field) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Field, newItem: Field) = oldItem == newItem
    }
}
