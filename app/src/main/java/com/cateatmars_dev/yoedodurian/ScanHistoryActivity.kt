package com.cateatmars_dev.yoedodurian

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class ScanHistoryActivity : BaseActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvStorageUsed: TextView
    private lateinit var btnClearData: MaterialButton
    private lateinit var adapter: HistoryAdapter
    private var historyList = mutableListOf<ScanFile>()

    data class ScanFile(val file: File, val name: String, val timestamp: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_history)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarHistory)
        toolbar.setNavigationOnClickListener { finish() }

        rvHistory = findViewById(R.id.rvHistory)
        tvStorageUsed = findViewById(R.id.tvStorageUsed)
        btnClearData = findViewById(R.id.btnClearData)

        UiAnimations.applyPressEffect(btnClearData)

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(historyList) { scanFile ->
            val intent = Intent(this, ScanResultActivity::class.java)
            intent.putExtra("SCAN_FILE_PATH", scanFile.file.absolutePath)
            startActivity(intent)
        }
        rvHistory.adapter = adapter

        btnClearData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_clear_title))
                .setMessage(getString(R.string.confirm_clear_message))
                .setPositiveButton(getString(R.string.btn_delete)) { _, _ -> clearScanData() }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
        }

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        historyList.clear()
        val scansDir = File(filesDir, "scans")
        var totalSize = 0L

        if (scansDir.exists() && scansDir.listFiles() != null) {
            val files = scansDir.listFiles()!!
            files.sortByDescending { it.lastModified() }

            val gson = Gson()
            val type = object : TypeToken<Map<String, Any>>() {}.type

            for (f in files) {
                totalSize += f.length()
                try {
                    val json = f.readText()
                    val wrapper: Map<String, Any> = gson.fromJson(json, type)
                    val name = wrapper["name"] as? String ?: "Diseases"
                    val timestamp = wrapper["timestamp"] as? String ?: ""

                    historyList.add(ScanFile(f, name, timestamp))
                } catch (_: Exception) {
                    historyList.add(ScanFile(f, "Unknown Scan", ""))
                }
            }
        }

        adapter.notifyDataSetChanged()

        val sizeStr = if (totalSize > 1024 * 1024) {
            String.format("%.2f MB", totalSize / (1024.0 * 1024.0))
        } else {
            String.format("%.2f KB", totalSize / 1024.0)
        }
        tvStorageUsed.text = getString(R.string.storage_used, sizeStr)

        if (historyList.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_scan_history_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearScanData() {
        val scansDir = File(filesDir, "scans")
        if (scansDir.exists() && scansDir.listFiles() != null) {
            for (f in scansDir.listFiles()!!) {
                f.delete()
            }
        }
        loadHistory()
        Toast.makeText(this, getString(R.string.data_cleared), Toast.LENGTH_SHORT).show()
    }

    inner class HistoryAdapter(
        private val list: List<ScanFile>,
        private val onItemClick: (ScanFile) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        private var lastAnimatedPosition = -1

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvScanName)
            val tvDate: TextView = view.findViewById(R.id.tvScanDate)
            init {
                UiAnimations.applyPressEffect(view)
                view.setOnClickListener {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClick(list[pos])
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_scan_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvName.text = item.name
            holder.tvDate.text = getString(R.string.file_size_format, item.file.length() / 1024)

            if (position > lastAnimatedPosition) {
                lastAnimatedPosition = position
                holder.itemView.alpha = 0f
                holder.itemView.translationY = 18f * holder.itemView.resources.displayMetrics.density
                holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay((position * 40L).coerceAtMost(240L))
                    .setDuration(300L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }

        override fun getItemCount() = list.size
    }
}
