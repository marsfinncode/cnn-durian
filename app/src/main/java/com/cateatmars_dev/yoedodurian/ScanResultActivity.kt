package com.cateatmars_dev.yoedodurian

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class ScanResultActivity : BaseActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_result)

        pieChart = findViewById(R.id.pieChart)
        toolbar = findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener { finish() }

        val filePath = intent.getStringExtra("SCAN_FILE_PATH")
        if (filePath != null) {
            loadChartData(filePath)
        } else {
            loadCombinedChartData()
        }

        UiAnimations.playStaggeredEntrance(findViewById<ViewGroup>(R.id.resultCardsContainer))
    }

    private fun loadChartData(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val json = file.readText()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val wrapper: Map<String, Any> = Gson().fromJson(json, type)

        val scanName = wrapper["name"] as? String ?: "Diseases"
        val countsRaw = wrapper["counts"] as? Map<*, *> ?: emptyMap<Any, Any>()

        val scanResultsMap = mutableMapOf<String, Int>()
        for ((k, v) in countsRaw) {
            if (k is String && v is Number) {
                scanResultsMap[k] = v.toInt()
            }
        }

        val entries = ArrayList<PieEntry>()
        for ((label, count) in scanResultsMap) {
            entries.add(PieEntry(count.toFloat(), ThemeUtils.getDisplayLabel(this, label)))
        }

        if (entries.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_data), Toast.LENGTH_SHORT).show()
            return
        }

        val dataSet = PieDataSet(entries, getString(R.string.chart_legend_diseases))

        val colors = ArrayList<Int>()
        for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
        for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c)
        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        data.setValueTextSize(14f)
        data.setValueTextColor(Color.WHITE)

        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.centerText = scanName
        pieChart.setCenterTextSize(16f)
        pieChart.setCenterTextColor(Color.BLACK)
        pieChart.legend.isEnabled = true
        pieChart.legend.isWordWrapEnabled = true
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.animateY(1400)
        pieChart.invalidate()
    }

    private fun loadCombinedChartData() {
        val scansDir = File(filesDir, "scans")
        if (!scansDir.exists() || scansDir.listFiles().isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.no_data), Toast.LENGTH_SHORT).show()
            pieChart.clear()
            pieChart.invalidate()
            return
        }

        val combinedMap = mutableMapOf<String, Int>()
        val wrapperType = object : TypeToken<Map<String, Any>>() {}.type
        val gson = Gson()

        for (file in scansDir.listFiles()!!) {
            try {
                val json = file.readText()
                val parsed: Map<String, Any> = gson.fromJson(json, wrapperType)
                val countsObj = parsed["counts"] as? Map<*, *>
                if (countsObj != null) {
                    for ((k, v) in countsObj) {
                        if (k is String && v is Number) {
                            combinedMap[k] = combinedMap.getOrDefault(k, 0) + v.toInt()
                        }
                    }
                } else {
                    for ((k, v) in parsed) {
                        if (v is Number) {
                            combinedMap[k] = combinedMap.getOrDefault(k, 0) + v.toInt()
                        }
                    }
                }
            } catch (_: Exception) {
                // Skip unreadable files
            }
        }

        val entries = ArrayList<PieEntry>()
        for ((label, count) in combinedMap) {
            entries.add(PieEntry(count.toFloat(), ThemeUtils.getDisplayLabel(this, label)))
        }

        if (entries.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_data), Toast.LENGTH_SHORT).show()
            pieChart.clear()
            pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, getString(R.string.chart_combined_title))
        val colors = ArrayList<Int>()
        for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
        for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c)
        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        data.setValueTextSize(14f)
        data.setValueTextColor(Color.WHITE)

        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.legend.isWordWrapEnabled = true
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.animateY(1400)
        pieChart.invalidate()
    }
}
