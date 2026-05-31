package com.mohammed.pdfreader.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mohammed.pdfreader.R
import com.mohammed.pdfreader.adapter.PdfAdapter
import com.mohammed.pdfreader.data.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeFragment : Fragment() {

    private lateinit var rvFiles: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var fabOpen: FloatingActionButton
    private lateinit var pdfAdapter: PdfAdapter

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> openPdfFile(uri) }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) loadRealPdfFiles()
        else showPermissionDeniedDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFiles = view.findViewById(R.id.rv_files)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        fabOpen = view.findViewById(R.id.fab_open)

        setupRecyclerView()
        fabOpen.setOnClickListener { pickFile() }
        
        checkPermissionsAndLoadFiles()
    }

    private fun setupRecyclerView() {
        pdfAdapter = PdfAdapter(
            onFileClick = { file ->
                Toast.makeText(context, "جاري فتح: ${file.getDisplayName()}", Toast.LENGTH_SHORT).show()
            },
            onFileLongClick = { file ->
                showFileOptions(file)
                true
            }
        )
        rvFiles.apply {
            adapter = pdfAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun checkPermissionsAndLoadFiles() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) loadRealPdfFiles()
                else showManageStorageDialog()
            }
            else -> {
                val granted = ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) loadRealPdfFiles()
                else permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    private fun loadRealPdfFiles() {
        viewLifecycleOwner.lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) { fetchPdfsFromDevice() }
            pdfAdapter.submitList(files)
            updateEmptyState(files.isNotEmpty())
        }
    }

    private fun fetchPdfsFromDevice(): List<PdfFile> {
        val pdfList = mutableListOf<PdfFile>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

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

        requireContext().contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Unknown"
                val path = cursor.getString(pathCol) ?: continue
                val size = cursor.getLong(sizeCol)
                val date = cursor.getLong(dateCol) * 1000 // Convert to milliseconds

                val file = File(path)
                if (file.exists()) {
                    pdfList.add(
                        PdfFile(
                            id = id,
                            name = name,
                            path = path,
                            size = size,
                            lastOpened = date,
                            pageCount = 0, 
                            readProgress = 0
                        )
                    )
                }
            }
        }
        return pdfList
    }

    private fun updateEmptyState(hasFiles: Boolean) {
        rvFiles.visibility = if (hasFiles) View.VISIBLE else View.GONE
        layoutEmpty.visibility = if (hasFiles) View.GONE else View.VISIBLE
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        openFileLauncher.launch(intent)
    }

    private fun openPdfFile(uri: Uri) {
        Toast.makeText(context, "سيتم برمجة فتح الملفات في الخطوة القادمة!", Toast.LENGTH_SHORT).show()
    }

    private fun showManageStorageDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("إذن الوصول للملفات")
            .setMessage("نحتاج إذن الوصول للملفات عشان نقدر نعرض ملفات الـ PDF بتاعتك.\n\nاضغط 'الإعدادات' ثم فعّل 'السماح بإدارة جميع الملفات'")
            .setPositiveButton("الإعدادات") { _, _ -> openAppSettings() }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("الإذن مرفوض")
            .setMessage("بدون إذن الوصول مش هنقدر نعرض ملفاتك.")
            .setPositiveButton("الإعدادات") { _, _ -> openAppSettings() }
            .setNegativeButton("لاحقاً", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    private fun showFileOptions(file: PdfFile) {
        val options = arrayOf("فتح", "تفاصيل")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(file.getDisplayName())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(context, "فتح الملف", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(context, "المسار: ${file.path}\nالحجم: ${file.getFormattedSize()}", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }
}
