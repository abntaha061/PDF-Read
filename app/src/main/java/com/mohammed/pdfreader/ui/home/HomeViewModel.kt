package com.mohammed.pdfreader.ui.home

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.data.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _pdfFiles = MutableLiveData<List<PdfFile>>()
    val pdfFiles: LiveData<List<PdfFile>> = _pdfFiles

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadPdfFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val files = scanPdfFiles()
            _pdfFiles.postValue(files)
            _isLoading.postValue(false)
        }
    }

    private fun scanPdfFiles(): List<PdfFile> {
        val pdfList = mutableListOf<PdfFile>()
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val cursor: Cursor? = getApplication<Application>().contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val name = it.getString(nameCol) ?: continue
                val path = it.getString(dataCol) ?: continue
                val size = it.getLong(sizeCol)
                val date = it.getLong(dateCol) * 1000L

                pdfList.add(
                    PdfFile(
                        id = id,
                        name = name,
                        path = path,
                        size = size,
                        lastOpened = date
                    )
                )
            }
        }
        return pdfList
    }
}
