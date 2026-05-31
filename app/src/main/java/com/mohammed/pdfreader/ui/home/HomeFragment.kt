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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.mohammed.pdfreader.R
import com.mohammed.pdfreader.adapter.PdfAdapter
import com.mohammed.pdfreader.data.model.PdfFile
import com.mohammed.pdfreader.ui.reader.PdfReaderActivity

class HomeFragment : Fragment() {

    private lateinit var rvFiles: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var fabOpen: ExtendedFloatingActionButton
    private lateinit var pdfAdapter: PdfAdapter
    private val viewModel: HomeViewModel by viewModels()

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> openPdfFromUri(uri) }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.loadPdfFiles()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                viewModel.loadPdfFiles()
            }
        }
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
        observeViewModel()
        checkAndLoadFiles()

        fabOpen.setOnClickListener { pickFile() }
    }

    private fun setupRecyclerView() {
        pdfAdapter = PdfAdapter(
            onFileClick = { file -> openPdfFromPath(file) },
            onFileLongClick = { file -> showFileOptions(file); true }
        )
        rvFiles.apply {
            adapter = pdfAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeViewModel() {
        viewModel.pdfFiles.observe(viewLifecycleOwner) { files ->
            pdfAdapter.submitList(files)
            updateEmptyState(files.isNotEmpty())
        }
    }

    private fun checkAndLoadFiles() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    viewModel.loadPdfFiles()
                } else {
                    showManageStorageDialog()
                }
            }
            else -> {
                val granted = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    viewModel.loadPdfFiles()
                } else {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    )
                }
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

    private fun openPdfFromUri(uri: Uri) {
        startActivity(
            Intent(requireContext(), PdfReaderActivity::class.java).apply {
                putExtra("pdf_uri", uri.toString())
                putExtra("pdf_name", uri.lastPathSegment ?: "PDF")
            }
        )
    }

    private fun openPdfFromPath(file: PdfFile) {
        startActivity(
            Intent(requireContext(), PdfReaderActivity::class.java).apply {
                putExtra("pdf_path", file.path)
                putExtra("pdf_name", file.getDisplayName())
            }
        )
    }

    private fun updateEmptyState(hasFiles: Boolean) {
        rvFiles.visibility = if (hasFiles) View.VISIBLE else View.GONE
        layoutEmpty.visibility = if (hasFiles) View.GONE else View.VISIBLE
    }

    private fun showManageStorageDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("إذن الوصول للملفات")
            .setMessage("نحتاج إذن الوصول الكامل لعرض ملفات PDF.\n\nاضغط 'الإعدادات' ثم فعّل 'السماح بإدارة جميع الملفات'")
            .setPositiveButton("فتح الإعدادات") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                manageStorageLauncher.launch(intent)
            }
            .setNegativeButton("تخطي") { _, _ ->
                updateEmptyState(false)
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("الإذن مرفوض")
            .setMessage("بدون إذن الوصول لن نتمكن من عرض ملفات PDF.")
            .setPositiveButton("فتح الإعدادات") { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                )
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showFileOptions(file: PdfFile) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(file.getDisplayName())
            .setItems(arrayOf("📖 فتح", "ℹ️ تفاصيل", "📤 مشاركة")) { _, which ->
                when (which) {
                    0 -> openPdfFromPath(file)
                    1 -> MaterialAlertDialogBuilder(requireContext())
                        .setTitle("تفاصيل الملف")
                        .setMessage(
                            "الاسم: ${file.getDisplayName()}\n" +
                            "الحجم: ${file.getFormattedSize()}\n" +
                            "المسار: ${file.path}"
                        )
                        .setPositiveButton("موافق", null)
                        .show()
                    2 -> startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(file.path))
                            }, "مشاركة الملف"
                        )
                    )
                }
            }.show()
    }
}
