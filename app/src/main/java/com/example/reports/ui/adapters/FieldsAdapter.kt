package com.example.reports.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.data.Field

class FieldsAdapter(
    private val onEdit: (Field) -> Unit,
    private val onDelete: (Field) -> Unit
) : ListAdapter<Field, FieldsAdapter.FieldViewHolder>(FieldDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return FieldViewHolder(view)
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FieldViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1 = itemView.findViewById<TextView>(android.R.id.text1)
        private val text2 = itemView.findViewById<TextView>(android.R.id.text2)

        fun bind(field: Field) {
            text1.text = "${field.name} (${field.type.name})"
            text2.text = "Категории: ${field.categoryIds.joinToString(", ")}"
            
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
