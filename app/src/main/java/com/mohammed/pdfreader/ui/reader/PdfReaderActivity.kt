package com.mohammed.pdfreader.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mohammed.pdfreader.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfReaderActivity : AppCompatActivity() {

    private lateinit var rvPages: RecyclerView
    private lateinit var seekBar: SeekBar
    private lateinit var tvPageInfo: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var toolbar: Toolbar

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var totalPages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_reader)

        rvPages = findViewById(R.id.rv_pdf_pages)
        seekBar = findViewById(R.id.seekbar_pages)
        tvPageInfo = findViewById(R.id.tv_page_info)
        progressLoading = findViewById(R.id.progress_loading)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val pdfPath = intent.getStringExtra("pdf_path")
        val pdfUri = intent.getStringExtra("pdf_uri")
        val pdfName = intent.getStringExtra("pdf_name") ?: "PDF"

        toolbar.title = pdfName

        when {
            pdfPath != null -> openFromPath(pdfPath)
            pdfUri != null -> openFromUri(Uri.parse(pdfUri))
        }
    }

    private fun openFromPath(path: String) {
        lifecycleScope.launch {
            progressLoading.visibility = View.VISIBLE
            withContext(Dispatchers.IO) {
                try {
                    val file = File(path)
                    parcelFileDescriptor = ParcelFileDescriptor.open(
                        file, ParcelFileDescriptor.MODE_READ_ONLY
                    )
                    pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
                    totalPages = pdfRenderer!!.pageCount
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            progressLoading.visibility = View.GONE
            setupPdfViewer()
        }
    }

    private fun openFromUri(uri: Uri) {
        lifecycleScope.launch {
            progressLoading.visibility = View.VISIBLE
            withContext(Dispatchers.IO) {
                try {
                    parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                    pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
                    totalPages = pdfRenderer!!.pageCount
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            progressLoading.visibility = View.GONE
            setupPdfViewer()
        }
    }

    private fun setupPdfViewer() {
        if (pdfRenderer == null) return

        val adapter = PdfPageAdapter(pdfRenderer!!, totalPages)
        rvPages.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(this@PdfReaderActivity)
        }

        seekBar.max = totalPages - 1
        tvPageInfo.text = "1 / $totalPages"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    rvPages.scrollToPosition(progress)
                    tvPageInfo.text = "${progress + 1} / $totalPages"
                }
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        (rvPages.layoutManager as LinearLayoutManager).let { lm ->
            rvPages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    val firstVisible = lm.findFirstVisibleItemPosition()
                    if (firstVisible >= 0) {
                        seekBar.progress = firstVisible
                        tvPageInfo.text = "${firstVisible + 1} / $totalPages"
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
    }
}

// Adapter لعرض صفحات PDF
class PdfPageAdapter(
    private val renderer: PdfRenderer,
    private val pageCount: Int
) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = pageCount

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPage: ImageView = itemView.findViewById(R.id.iv_page)

        fun bind(position: Int) {
            val page = renderer.openPage(position)
            val width = ivPage.context.resources.displayMetrics.widthPixels
            val height = (width.toFloat() / page.width * page.height).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            ivPage.setImageBitmap(bitmap)
        }
    }
}
