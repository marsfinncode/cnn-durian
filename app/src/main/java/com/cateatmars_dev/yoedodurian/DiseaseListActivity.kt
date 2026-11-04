package com.cateatmars_dev.yoedodurian

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

    data class DiseaseItem(
        val titleRes: Int,
        val descRes: Int,
        val indicatorColor: Int,
        val badgeRes: Int,
        val subtitleRes: Int,
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
                R.color.severity_critical, R.string.badge_critical, R.string.sub_phytophthora, R.drawable.img_phytophthora),
            DiseaseItem(R.string.disease_anthracnose_title, R.string.disease_anthracnose_desc,
                R.color.severity_critical, R.string.badge_critical, R.string.sub_anthracnose, R.drawable.img_anthracnose),
            DiseaseItem(R.string.disease_rhizoctonia_title, R.string.disease_rhizoctonia_desc,
                R.color.severity_mild, R.string.badge_warning, R.string.sub_rhizoctonia, R.drawable.img_rhizoctonia),
            DiseaseItem(R.string.disease_phomopsis_title, R.string.disease_phomopsis_desc,
                R.color.severity_mild, R.string.badge_warning, R.string.sub_phomopsis, R.drawable.img_phomopsis),
            DiseaseItem(R.string.disease_algal_spot_title, R.string.disease_algal_spot_desc,
                R.color.severity_healthy, R.string.badge_watch, R.string.sub_algal_spot, R.drawable.img_algal_spot),
            DiseaseItem(R.string.disease_healthy_title, R.string.disease_healthy_desc,
                R.color.severity_healthy, R.string.badge_healthy, R.string.sub_healthy, R.drawable.img_healthy),
            DiseaseItem(R.string.disease_others_title, R.string.disease_others_desc,
                R.color.textColorSecondary, R.string.badge_other, R.string.sub_others, R.drawable.img_others)
        )

        val badgeColorMap = mapOf(
            R.string.badge_critical to Pair(R.color.statusDanger, R.color.statusDangerSurface),
            R.string.badge_warning to Pair(R.color.statusWarning, R.color.statusWarningSurface),
            R.string.badge_watch to Pair(R.color.statusInfo, R.color.statusInfoSurface),
            R.string.badge_healthy to Pair(R.color.statusHealthy, R.color.statusHealthySurface),
            R.string.badge_other to Pair(R.color.textColorSecondary, R.color.background_light)
        )

        for (disease in diseases) {
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_disease_card, container, false) as MaterialCardView

            // Indicator dot color
            val indicator = cardView.findViewById<View>(R.id.viewIndicator)
            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(getColor(disease.indicatorColor))
            }
            indicator.background = dotDrawable

            // Title & subtitle
            cardView.findViewById<TextView>(R.id.tvCardTitle).text = getString(disease.titleRes)
            cardView.findViewById<TextView>(R.id.tvCardSubtitle).text = getString(disease.subtitleRes)

            // Badge
            val badge = cardView.findViewById<TextView>(R.id.tvBadge)
            badge.text = getString(disease.badgeRes)
            val colors = badgeColorMap[disease.badgeRes]
            if (colors != null) {
                badge.setTextColor(getColor(colors.first))
                val badgeBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 24f
                    setColor(getColor(colors.second))
                }
                badge.background = badgeBg
            }

            UiAnimations.applyPressEffect(cardView)

            cardView.setOnClickListener {
                val intent = Intent(this, DiseaseDetailActivity::class.java)
                intent.putExtra("TITLE_RES", disease.titleRes)
                intent.putExtra("DESC_RES", disease.descRes)
                intent.putExtra("TITLE", getString(disease.titleRes))
                intent.putExtra("DESC", getString(disease.descRes))
                if (disease.imageRes != null) {
                    intent.putExtra("IMAGE_RES", disease.imageRes)
                }
                startActivity(intent)
            }

            container.addView(cardView)
        }

        UiAnimations.playStaggeredEntrance(container, startDelayMs = 45L, stepMs = 45L)
    }
}
