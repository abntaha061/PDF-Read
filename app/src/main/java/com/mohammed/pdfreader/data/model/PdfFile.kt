package com.mohammed.pdfreader.data.model

data class PdfFile(
    val id: Long = 0,
    val name: String,
    val path: String,
    val size: Long,
    val pageCount: Int = 0,
    val lastOpened: Long = 0,
    val readProgress: Int = 0,
    val isFavorite: Boolean = false
) {
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            else -> String.format("%.1fMB", size / (1024.0 * 1024.0))
        }
    }

    fun getDisplayName(): String {
        return name.removeSuffix(".pdf").removeSuffix(".PDF")
    }
}
