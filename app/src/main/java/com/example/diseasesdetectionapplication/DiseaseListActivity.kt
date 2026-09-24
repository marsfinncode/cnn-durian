package com.example.diseasesdetectionapplication

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class DiseaseListActivity : BaseActivity() {

    // Each disease entry: (titleRes, descRes, indicatorColor, badge, subtitle, imageRes)
    data class DiseaseItem(
        val titleRes: Int,
        val descRes: Int,
        val indicatorColor: Int,   // color resource id
        val badge: String,
        val subtitle: String,
        val imageRes: Int? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease_list)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.diseaseListContainer)

        val diseases = listOf(
            DiseaseItem(R.string.disease_phytophthora_title, R.string.disease_phytophthora_desc,
                R.color.severity_critical, "รุนแรง", "เชื้อรา Phytophthora palmivora", R.drawable.img_phytophthora),
            DiseaseItem(R.string.disease_anthracnose_title, R.string.disease_anthracnose_desc,
                R.color.severity_critical, "รุนแรง", "เชื้อราตระกูล Colletotrichum", R.drawable.img_anthracnose),
            DiseaseItem(R.string.disease_rhizoctonia_title, R.string.disease_rhizoctonia_desc,
                R.color.severity_mild, "ระวัง", "เชื้อรา Rhizoctonia solani", R.drawable.img_rhizoctonia),
            DiseaseItem(R.string.disease_phomopsis_title, R.string.disease_phomopsis_desc,
                R.color.severity_mild, "ระวัง", "เชื้อรา Phomopsis spp.", R.drawable.img_phomopsis),
            DiseaseItem(R.string.disease_algal_spot_title, R.string.disease_algal_spot_desc,
                R.color.severity_healthy, "เฝ้าดู", "สาหร่าย Cephaleuros virescens", R.drawable.img_algal_spot),
            DiseaseItem(R.string.disease_healthy_title, R.string.disease_healthy_desc,
                R.color.severity_healthy, "ปกติ", "ใบแข็งแรง สมบูรณ์", R.drawable.img_healthy),
            DiseaseItem(R.string.disease_others_title, R.string.disease_others_desc,
                R.color.textColorSecondary, "อื่นๆ", "อาการที่ไม่ตรงกับ 5 โรคหลัก", R.drawable.img_others)
        )

        val badgeColorMap = mapOf(
            "รุนแรง" to Pair(R.color.statusDanger, R.color.statusDangerSurface),
            "ระวัง"  to Pair(R.color.statusWarning, R.color.statusWarningSurface),
            "เฝ้าดู" to Pair(R.color.statusInfo, R.color.statusInfoSurface),
            "ปกติ"   to Pair(R.color.statusHealthy, R.color.statusHealthySurface),
            "อื่นๆ"  to Pair(R.color.textColorSecondary, R.color.background_light)
        )

        for (disease in diseases) {
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_disease_card, container, false) as MaterialCardView

            // Indicator dot color
            val indicator = cardView.findViewById<View>(R.id.viewIndicator)
            val dotDrawable = GradientDrawable()
            dotDrawable.shape = GradientDrawable.OVAL
            dotDrawable.setColor(getColor(disease.indicatorColor))
            indicator.background = dotDrawable

            // Title & subtitle
            cardView.findViewById<TextView>(R.id.tvCardTitle).text = getString(disease.titleRes)
            cardView.findViewById<TextView>(R.id.tvCardSubtitle).text = disease.subtitle

            // Badge
            val badge = cardView.findViewById<TextView>(R.id.tvBadge)
            badge.text = disease.badge
            val colors = badgeColorMap[disease.badge]
            if (colors != null) {
                badge.setTextColor(getColor(colors.first))
                val badgeBg = GradientDrawable()
                badgeBg.shape = GradientDrawable.RECTANGLE
                badgeBg.cornerRadius = 24f
                badgeBg.setColor(getColor(colors.second))
                badge.background = badgeBg
            }

            // Click
            cardView.setOnClickListener {
                val intent = Intent(this, DiseaseDetailActivity::class.java)
                intent.putExtra("TITLE", getString(disease.titleRes))
                intent.putExtra("DESC", getString(disease.descRes))
                if (disease.imageRes != null) {
                    intent.putExtra("IMAGE_RES", disease.imageRes)
                }
                startActivity(intent)
            }

            container.addView(cardView)
        }
    }
}
