package com.example.diseasesdetectionapplication

import android.os.Bundle
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar

class DiseaseDetailActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease_detail)

        val title = intent.getStringExtra("TITLE") ?: ""
        val desc = intent.getStringExtra("DESC") ?: ""

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarDetail)
        toolbar.title = title
        toolbar.subtitle = "กรมวิชาการเกษตร"
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<TextView>(R.id.tvDetailTitle).text = title

        // Render HTML formatted content
        findViewById<TextView>(R.id.tvDetailDesc).text =
            android.text.Html.fromHtml(desc, android.text.Html.FROM_HTML_MODE_COMPACT)
            
        val imageRes = intent.getIntExtra("IMAGE_RES", -1)
        val ivDiseaseImage = findViewById<android.widget.ImageView>(R.id.ivDiseaseImage)
        val tvImagePlaceholder = findViewById<TextView>(R.id.tvImagePlaceholder)
        
        if (imageRes != -1) {
            ivDiseaseImage.setImageResource(imageRes)
            tvImagePlaceholder.visibility = android.view.View.GONE
        } else {
            tvImagePlaceholder.visibility = android.view.View.VISIBLE
        }
    }
}
