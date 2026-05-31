package com.mohammed.pdfreader.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mohammed.pdfreader.R
import com.mohammed.pdfreader.adapter.PdfAdapter
import com.mohammed.pdfreader.data.model.PdfFile

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
        if (permissions.values.all { it }) pickFile()
        else showPermissionDeniedDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFiles = view.findViewById(R.id.rv_files)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        fabOpen = view.findViewById(R.id.fab_open)

        setupRecyclerView()
        fabOpen.setOnClickListener { checkPermissionsAndOpen() }
        loadSampleFiles()
    }

    private fun setupRecyclerView() {
        pdfAdapter = PdfAdapter(
            onFileClick = { file ->
                Toast.makeText(context, "فتح: ${file.getDisplayName()}", Toast.LENGTH_SHORT).show()
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

    private fun loadSampleFiles() {
        val sampleFiles = listOf(
            PdfFile(
                id = 1,
                name = "قواعد اللغة الألمانية.pdf",
                path = "/sdcard/Download/german.pdf",
                size = 2_500_000,
                pageCount = 142,
                lastOpened = System.currentTimeMillis() - 86400000,
                readProgress = 35
            ),
            PdfFile(
                id = 2,
                name = "Deutsch Lernen A1.pdf",
                path = "/sdcard/Download/deutsch.pdf",
                size = 5_200_000,
                pageCount = 280,
                lastOpened = System.currentTimeMillis() - 3600000,
                readProgress = 72
            ),
            PdfFile(
                id = 3,
                name = "مفردات ألمانية-عربية.pdf",
                path = "/sdcard/Download/vocab.pdf",
                size = 800_000,
                pageCount = 48,
                lastOpened = System.currentTimeMillis(),
                readProgress = 0
            )
        )
        pdfAdapter.submitList(sampleFiles)
        updateEmptyState(sampleFiles.isNotEmpty())
    }

    private fun updateEmptyState(hasFiles: Boolean) {
        rvFiles.visibility = if (hasFiles) View.VISIBLE else View.GONE
        layoutEmpty.visibility = if (hasFiles) View.GONE else View.VISIBLE
    }

    private fun checkPermissionsAndOpen() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) pickFile()
                else showManageStorageDialog()
            }
            else -> {
                val granted = ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) pickFile()
                else permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        openFileLauncher.launch(intent)
    }

    private fun openPdfFile(uri: Uri) {
        Toast.makeText(context, "سيتم فتح الملف في المرحلة القادمة!", Toast.LENGTH_SHORT).show()
    }

    private fun showManageStorageDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("إذن الوصول للملفات")
            .setMessage("نحتاج إذن الوصول الكامل للملفات.\n\nاضغط 'الإعدادات' ثم فعّل 'السماح بإدارة جميع الملفات'")
            .setPositiveButton("فتح الإعدادات") { _, _ -> openAppSettings() }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("الإذن مرفوض")
            .setMessage("بدون إذن الوصول لن نتمكن من فتح ملفات PDF.")
            .setPositiveButton("فتح الإعدادات") { _, _ -> openAppSettings() }
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
        val options = arrayOf("فتح", "تفاصيل", "مشاركة", "حذف من القائمة")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(file.getDisplayName())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(context, "فتح الملف", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(context, "الحجم: ${file.getFormattedSize()}", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(context, "مشاركة", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(context, "حُذف من القائمة", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
}
