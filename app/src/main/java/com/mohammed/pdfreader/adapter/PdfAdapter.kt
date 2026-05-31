package com.mohammed.pdfreader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mohammed.pdfreader.R
import com.mohammed.pdfreader.data.model.PdfFile
import java.text.SimpleDateFormat
import java.util.*

class PdfAdapter(
    private val onFileClick: (PdfFile) -> Unit,
    private val onFileLongClick: (PdfFile) -> Boolean
) : ListAdapter<PdfFile, PdfAdapter.PdfViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_file, parent, false)
        return PdfViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PdfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_file_name)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_file_info)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_file_date)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_read)
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_pdf_icon)

        fun bind(file: PdfFile) {
            tvName.text = file.getDisplayName()
            tvInfo.text = "${file.getFormattedSize()} • ${file.pageCount} صفحة"
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tvDate.text = sdf.format(Date(file.lastOpened))
            progressBar.progress = file.readProgress
            progressBar.visibility = if (file.readProgress > 0) View.VISIBLE else View.GONE
            itemView.setOnClickListener { onFileClick(file) }
            itemView.setOnLongClickListener { onFileLongClick(file) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PdfFile>() {
        override fun areItemsTheSame(old: PdfFile, new: PdfFile) = old.id == new.id
        override fun areContentsTheSame(old: PdfFile, new: PdfFile) = old == new
    }
}
