package com.cateatmars_dev.yoedodurian

import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar

class DiseaseDetailActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease_detail)

        val titleRes = intent.getIntExtra("TITLE_RES", -1)
        val labelExtra = intent.getStringExtra("DISEASE_KEY")

        val title = if (titleRes != -1) getString(titleRes) else (intent.getStringExtra("TITLE") ?: "")

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarDetail)
        toolbar.title = title
        toolbar.subtitle = null
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<TextView>(R.id.tvDetailTitle).text = title

        val guideContainer = findViewById<LinearLayout>(R.id.guideContainer)
        val diseaseKey = DiseaseGuideRenderer.resolveKey(titleRes, labelExtra)
        DiseaseGuideRenderer.populateGuide(this, guideContainer, diseaseKey)

        val imageRes = intent.getIntExtra("IMAGE_RES", -1)
        val ivDiseaseImage = findViewById<ImageView>(R.id.ivDiseaseImage)
        val tvImagePlaceholder = findViewById<TextView>(R.id.tvImagePlaceholder)

        if (imageRes != -1) {
            ivDiseaseImage.setImageResource(imageRes)
            tvImagePlaceholder.visibility = View.GONE
        } else {
            tvImagePlaceholder.visibility = View.VISIBLE
        }

        // Smooth card entrance animation
        findViewById<View>(R.id.cardDetailContent)?.apply {
            alpha = 0f
            translationY = 20f * resources.displayMetrics.density
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(340L)
                .setInterpolator(DecelerateInterpolator(1.5f))
                .start()
        }
    }
}
