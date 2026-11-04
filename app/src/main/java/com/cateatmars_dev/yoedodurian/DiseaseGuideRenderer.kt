package com.cateatmars_dev.yoedodurian

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Html
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

/**
 * Renders structured, farmer-friendly visual guides for each durian leaf class.
 * Shared across DiseaseDetailActivity (Disease Database) and ImageAnalysisActivity (Image Detection).
 */
object DiseaseGuideRenderer {

    data class SymptomItem(
        val partTag: String,
        val highlight: String,
        val detail: String
    )

    data class ActionStepSection(
        val iconRes: Int,
        val badgeColorHex: String,
        val surfaceColorHex: String,
        val borderColorHex: String,
        val stepTitle: String,
        val stepSubtitle: String,
        val bullets: List<String>
    )

    data class StructuredDiseaseGuide(
        val key: String,
        val pathogenLabel: String,
        val pathogenValue: String,
        val riskSeasonLabel: String,
        val riskSeasonValue: String,
        val urgentHeader: String,
        val urgentBody: String,
        val urgentColorHex: String,
        val urgentBgHex: String,
        val symptomsTitle: String,
        val symptoms: List<SymptomItem>,
        val stepsHeader: String,
        val actionSections: List<ActionStepSection>,
        val cautionTitle: String?,
        val cautionBullets: List<String>,
        val referenceButtonTitle: String?,
        val referenceButtonSubtitle: String?,
        val referenceUrl: String?
    )

    private const val URL_DOA_PHYTOPHTHORA = "https://www.doa.go.th/share/attachment.php?aid=2976"
    private const val URL_DOAE_DURIAN_PDF = "https://esc.doae.go.th/wp-content/uploads/2024/01/%E0%B9%82%E0%B8%A3%E0%B8%84%E0%B8%97%E0%B8%B8%E0%B9%80%E0%B8%A3%E0%B8%B5%E0%B8%A2%E0%B8%99%E0%B9%81%E0%B8%A5%E0%B8%B0%E0%B8%81%E0%B8%B2%E0%B8%A3%E0%B8%9B%E0%B9%89%E0%B8%AD%E0%B8%87%E0%B8%81%E0%B8%B1%E0%B8%99%E0%B8%81%E0%B8%B3%E0%B8%88%E0%B8%B1%E0%B8%94.pdf"

    fun resolveKey(titleRes: Int, label: String?): String {
        if (!label.isNullOrBlank()) {
            return when (label.trim()) {
                "Phytophtora", "Phytophthora" -> "Phytophtora"
                "Anthracnose" -> "Anthracnose"
                "Rhizoctonia" -> "Rhizoctonia"
                "Phomopsis" -> "Phomopsis"
                "Algal spot", "Algal Spot" -> "Algal spot"
                "Healthy" -> "Healthy"
                else -> "Other"
            }
        }
        return when (titleRes) {
            R.string.disease_phytophthora_title -> "Phytophtora"
            R.string.disease_anthracnose_title -> "Anthracnose"
            R.string.disease_rhizoctonia_title -> "Rhizoctonia"
            R.string.disease_phomopsis_title -> "Phomopsis"
            R.string.disease_algal_spot_title -> "Algal spot"
            R.string.disease_healthy_title -> "Healthy"
            else -> "Other"
        }
    }

    private fun isEnglish(context: Context): Boolean {
        val locales = context.resources.configuration.locales
        val lang = if (!locales.isEmpty) locales[0].language else "th"
        return lang.startsWith("en", ignoreCase = true)
    }

    private fun isNightMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    fun populateGuide(context: Context, container: LinearLayout, diseaseKey: String) {
        container.removeAllViews()
        val guide = if (isEnglish(context)) {
            getEnglishGuide(diseaseKey)
        } else {
            getThaiGuide(diseaseKey)
        }
        val night = isNightMode(context)

        // 1. Pathogen & Risk Season Summary Box
        container.addView(buildMetaSummaryCard(context, guide, night))

        // 2. Urgent Farmer Action Banner
        container.addView(buildUrgentActionBanner(context, guide, night))

        // 3. Symptoms Checklist Section
        container.addView(buildSectionHeader(
            context = context,
            iconRes = R.drawable.ic_symptom_search,
            iconBgColor = Color.parseColor("#0284C7"),
            title = guide.symptomsTitle,
            topMarginDp = 18
        ))
        container.addView(buildSymptomsCard(context, guide.symptoms, night))

        // 4. Step-by-Step Prevention & Treatment Header
        container.addView(buildSectionHeader(
            context = context,
            iconRes = R.drawable.ic_bio_control,
            iconBgColor = ContextCompat.getColor(context, R.color.colorPrimary),
            title = guide.stepsHeader,
            topMarginDp = 20
        ))

        // Render each step card (Cultural, Biological, Chemical)
        for (section in guide.actionSections) {
            container.addView(buildActionStepCard(context, section, night))
        }

        // 5. Caution / Safety Box (if applicable)
        if (!guide.cautionTitle.isNullOrBlank() && guide.cautionBullets.isNotEmpty()) {
            container.addView(buildCautionBox(context, guide.cautionTitle, guide.cautionBullets, night))
        }

        // 6. Clickable Reference Document Button (Replaces old underlined text link)
        if (!guide.referenceUrl.isNullOrBlank() && !guide.referenceButtonTitle.isNullOrBlank()) {
            container.addView(buildReferenceButton(
                context = context,
                title = guide.referenceButtonTitle,
                subtitle = guide.referenceButtonSubtitle ?: "",
                url = guide.referenceUrl,
                night = night
            ))
        }
    }

    private fun buildMetaSummaryCard(
        context: Context,
        guide: StructuredDiseaseGuide,
        night: Boolean
    ): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padH = dp(context, 10)
            val padV = dp(context, 12)
            setPadding(padH, padV, padH, padV)
            background = roundedRect(
                fillColor = if (night) Color.parseColor("#133227") else Color.parseColor("#F1F7F4"),
                strokeColor = if (night) Color.parseColor("#234E3E") else Color.parseColor("#D5E6DF"),
                strokeWidthPx = dp(context, 1),
                radiusDp = dpF(context, 14f)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 12)
            }
        }

        // Row 1: Pathogen
        val rowPathogen = buildMetaRow(
            context = context,
            badgeText = guide.pathogenLabel,
            badgeBgColor = if (night) Color.parseColor("#1E4638") else Color.parseColor("#DCFCE7"),
            badgeTextColor = if (night) Color.parseColor("#6EE7B7") else Color.parseColor("#047857"),
            valueHtml = guide.pathogenValue
        )
        card.addView(rowPathogen)

        // Divider
        val divider = View(context).apply {
            setBackgroundColor(if (night) Color.parseColor("#234E3E") else Color.parseColor("#DCEAE3"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 1)
            ).apply {
                topMargin = dp(context, 10)
                bottomMargin = dp(context, 10)
            }
        }
        card.addView(divider)

        // Row 2: High-Risk Condition / Season
        val rowRisk = buildMetaRow(
            context = context,
            badgeText = guide.riskSeasonLabel,
            badgeBgColor = if (night) Color.parseColor("#451A03") else Color.parseColor("#FEF3C7"),
            badgeTextColor = if (night) Color.parseColor("#FCD34D") else Color.parseColor("#B45309"),
            valueHtml = guide.riskSeasonValue
        )
        card.addView(rowRisk)

        return card
    }

    private fun buildMetaRow(
        context: Context,
        badgeText: String,
        badgeBgColor: Int,
        badgeTextColor: Int,
        valueHtml: String
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val badge = TextView(context).apply {
            text = badgeText
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(badgeTextColor)
            setPadding(dp(context, 10), dp(context, 3), dp(context, 10), dp(context, 3))
            background = roundedRect(badgeBgColor, 0, 0, dpF(context, 8f))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 5)
            }
        }

        val valueTv = TextView(context).apply {
            text = Html.fromHtml(valueHtml, Html.FROM_HTML_MODE_COMPACT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary))
            setLineSpacing(dpF(context, 3f), 1.0f)
        }

        row.addView(badge)
        row.addView(valueTv)
        return row
    }

    private fun buildUrgentActionBanner(
        context: Context,
        guide: StructuredDiseaseGuide,
        night: Boolean
    ): View {
        val accentColor = Color.parseColor(guide.urgentColorHex)
        val bgColor = if (night) Color.parseColor("#2A1810") else Color.parseColor(guide.urgentBgHex)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padH = dp(context, 10)
            val padV = dp(context, 12)
            setPadding(padH, padV, padH, padV)
            background = roundedRect(
                fillColor = bgColor,
                strokeColor = accentColor,
                strokeWidthPx = dp(context, 1.5f),
                radiusDp = dpF(context, 14f)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 8)
            }
        }

        val iconBadge = ImageView(context).apply {
            setImageResource(R.drawable.ic_urgent_alert)
            setColorFilter(Color.WHITE)
            val pad = dp(context, 6)
            setPadding(pad, pad, pad, pad)
            background = ovalDrawable(accentColor)
            layoutParams = LinearLayout.LayoutParams(dp(context, 30), dp(context, 30)).apply {
                marginEnd = dp(context, 8)
            }
        }

        val titleTv = TextView(context).apply {
            text = guide.urgentHeader.replace("⚡ ", "").replace("🌟 ", "")
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (night) Color.WHITE else accentColor)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        headerRow.addView(iconBadge)
        headerRow.addView(titleTv)

        val bodyTv = TextView(context).apply {
            text = Html.fromHtml(guide.urgentBody, Html.FROM_HTML_MODE_COMPACT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary))
            setLineSpacing(dpF(context, 4f), 1.0f)
        }

        container.addView(headerRow)
        container.addView(bodyTv)
        return container
    }

    private fun buildSectionHeader(
        context: Context,
        iconRes: Int,
        iconBgColor: Int,
        title: String,
        topMarginDp: Int
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(context, topMarginDp)
                bottomMargin = dp(context, 10)
            }
        }

        val iconView = ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(Color.WHITE)
            val pad = dp(context, 7)
            setPadding(pad, pad, pad, pad)
            background = ovalDrawable(iconBgColor)
            layoutParams = LinearLayout.LayoutParams(dp(context, 34), dp(context, 34)).apply {
                marginEnd = dp(context, 10)
            }
        }

        val titleTv = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary))
        }

        row.addView(iconView)
        row.addView(titleTv)
        return row
    }

    private fun buildSymptomsCard(
        context: Context,
        symptoms: List<SymptomItem>,
        night: Boolean
    ): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padH = dp(context, 10)
            val padV = dp(context, 12)
            setPadding(padH, padV, padH, padV)
            background = roundedRect(
                fillColor = if (night) Color.parseColor("#112A20") else Color.parseColor("#FFFFFF"),
                strokeColor = if (night) Color.parseColor("#1E4638") else Color.parseColor("#E2E8F0"),
                strokeWidthPx = dp(context, 1),
                radiusDp = dpF(context, 14f)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        symptoms.forEachIndexed { index, item ->
            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) topMargin = dp(context, 12)
                }
            }

            // Top row: Part Pill Badge + Bold Highlight
            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(context, 4)
                }
            }

            val partBadge = TextView(context).apply {
                text = item.partTag
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (night) Color.parseColor("#7DD3FC") else Color.parseColor("#0369A1"))
                setPadding(dp(context, 10), dp(context, 3), dp(context, 10), dp(context, 3))
                background = roundedRect(
                    fillColor = if (night) Color.parseColor("#0C4A6E") else Color.parseColor("#E0F2FE"),
                    strokeColor = 0,
                    strokeWidthPx = 0,
                    radiusDp = dpF(context, 8f)
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(context, 8)
                }
            }

            val highlightTv = TextView(context).apply {
                text = item.highlight
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            topRow.addView(partBadge)
            topRow.addView(highlightTv)

            val detailTv = TextView(context).apply {
                text = Html.fromHtml(item.detail, Html.FROM_HTML_MODE_COMPACT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                setTextColor(ContextCompat.getColor(context, R.color.textColorSecondary))
                setLineSpacing(dpF(context, 4f), 1.0f)
            }

            itemLayout.addView(topRow)
            itemLayout.addView(detailTv)
            card.addView(itemLayout)

            if (index < symptoms.size - 1) {
                val div = View(context).apply {
                    setBackgroundColor(if (night) Color.parseColor("#1E3A2F") else Color.parseColor("#F1F5F9"))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(context, 1)
                    ).apply {
                        topMargin = dp(context, 12)
                    }
                }
                card.addView(div)
            }
        }

        return card
    }

    private fun buildActionStepCard(
        context: Context,
        section: ActionStepSection,
        night: Boolean
    ): View {
        val badgeColor = Color.parseColor(section.badgeColorHex)
        val bgColor = if (night) Color.parseColor("#112A20") else Color.parseColor(section.surfaceColorHex)
        val borderColor = if (night) Color.parseColor("#1E4638") else Color.parseColor(section.borderColorHex)

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padH = dp(context, 10)
            val padV = dp(context, 12)
            setPadding(padH, padV, padH, padV)
            background = roundedRect(
                fillColor = bgColor,
                strokeColor = borderColor,
                strokeWidthPx = dp(context, 1),
                radiusDp = dpF(context, 14f)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 12)
            }
        }

        // Header Row with Graphic Icon + Step Title + Step Subtitle
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 10)
            }
        }

        val iconView = ImageView(context).apply {
            setImageResource(section.iconRes)
            setColorFilter(Color.WHITE)
            val pad = dp(context, 8)
            setPadding(pad, pad, pad, pad)
            background = roundedRect(badgeColor, 0, 0, dpF(context, 10f))
            layoutParams = LinearLayout.LayoutParams(dp(context, 38), dp(context, 38)).apply {
                marginEnd = dp(context, 12)
            }
        }

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val stepTitleTv = TextView(context).apply {
            text = section.stepTitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (night) Color.WHITE else badgeColor)
        }

        val stepSubTv = TextView(context).apply {
            text = section.stepSubtitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(ContextCompat.getColor(context, R.color.textColorSecondary))
        }

        titleCol.addView(stepTitleTv)
        titleCol.addView(stepSubTv)
        headerRow.addView(iconView)
        headerRow.addView(titleCol)
        card.addView(headerRow)

        // Bullets
        section.bullets.forEachIndexed { idx, bulletHtml ->
            val bulletRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (idx > 0) topMargin = dp(context, 8)
                }
            }

            val dotView = View(context).apply {
                background = ovalDrawable(badgeColor)
                layoutParams = LinearLayout.LayoutParams(dp(context, 6), dp(context, 6)).apply {
                    topMargin = dp(context, 8)
                    marginEnd = dp(context, 8)
                }
            }

            val textTv = TextView(context).apply {
                text = Html.fromHtml(bulletHtml, Html.FROM_HTML_MODE_COMPACT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary))
                setLineSpacing(dpF(context, 4f), 1.0f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            bulletRow.addView(dotView)
            bulletRow.addView(textTv)
            card.addView(bulletRow)
        }

        return card
    }

    private fun buildCautionBox(
        context: Context,
        title: String,
        bullets: List<String>,
        night: Boolean
    ): View {
        val warnColor = Color.parseColor("#D97706")
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padH = dp(context, 10)
            val padV = dp(context, 12)
            setPadding(padH, padV, padH, padV)
            background = roundedRect(
                fillColor = if (night) Color.parseColor("#2A1B08") else Color.parseColor("#FFFBEB"),
                strokeColor = if (night) Color.parseColor("#92400E") else Color.parseColor("#FDE68A"),
                strokeWidthPx = dp(context, 1),
                radiusDp = dpF(context, 14f)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(context, 4)
                bottomMargin = dp(context, 16)
            }
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 6)
            }
        }

        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_caution_note)
            setColorFilter(warnColor)
            layoutParams = LinearLayout.LayoutParams(dp(context, 22), dp(context, 22)).apply {
                marginEnd = dp(context, 8)
            }
        }

        val titleTv = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (night) Color.parseColor("#FBBF24") else Color.parseColor("#92400E"))
        }

        headerRow.addView(icon)
        headerRow.addView(titleTv)
        box.addView(headerRow)

        bullets.forEach { line ->
            val tv = TextView(context).apply {
                text = Html.fromHtml("• $line", Html.FROM_HTML_MODE_COMPACT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary))
                setLineSpacing(dpF(context, 3f), 1.0f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(context, 4)
                }
            }
            box.addView(tv)
        }

        return box
    }

    private fun buildReferenceButton(
        context: Context,
        title: String,
        subtitle: String,
        url: String,
        night: Boolean
    ): View {
        val primaryColor = ContextCompat.getColor(context, R.color.colorPrimary)
        val primaryDark = ContextCompat.getColor(context, R.color.colorPrimaryDark)

        val buttonCard = MaterialCardView(context).apply {
            radius = dpF(context, 14f)
            cardElevation = dpF(context, 2f)
            strokeWidth = dp(context, 1.5f)
            strokeColor = primaryColor
            setCardBackgroundColor(if (night) Color.parseColor("#0B3B2B") else Color.parseColor("#ECFDF5"))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(context, 6)
                bottomMargin = dp(context, 4)
            }
        }

        val innerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = dp(context, 16)
            val padV = dp(context, 14)
            setPadding(padH, padV, padH, padV)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val docIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_doc_reference)
            setColorFilter(Color.WHITE)
            val pad = dp(context, 8)
            setPadding(pad, pad, pad, pad)
            background = roundedRect(primaryColor, 0, 0, dpF(context, 10f))
            layoutParams = LinearLayout.LayoutParams(dp(context, 40), dp(context, 40)).apply {
                marginEnd = dp(context, 14)
            }
        }

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleTv = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (night) Color.WHITE else primaryDark)
        }

        val subTv = TextView(context).apply {
            text = subtitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(ContextCompat.getColor(context, R.color.textColorSecondary))
        }

        textCol.addView(titleTv)
        textCol.addView(subTv)

        val launchIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_launch)
            setColorFilter(if (night) Color.WHITE else primaryDark)
            layoutParams = LinearLayout.LayoutParams(dp(context, 22), dp(context, 22)).apply {
                marginStart = dp(context, 8)
            }
        }

        innerRow.addView(docIcon)
        innerRow.addView(textCol)
        innerRow.addView(launchIcon)
        buttonCard.addView(innerRow)

        UiAnimations.applyPressEffect(buttonCard)
        buttonCard.setOnClickListener {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
                Toast.makeText(context, context.getString(R.string.cannot_open_browser), Toast.LENGTH_SHORT).show()
            }
        }

        return buttonCard
    }

    private fun roundedRect(
        fillColor: Int,
        strokeColor: Int,
        strokeWidthPx: Int,
        radiusDp: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp
            setColor(fillColor)
            if (strokeWidthPx > 0 && strokeColor != 0) {
                setStroke(strokeWidthPx, strokeColor)
            }
        }
    }

    private fun ovalDrawable(fillColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
        }
    }

    private fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun dp(context: Context, value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun dpF(context: Context, value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        )
    }

    // =========================================================================
    // THAI KNOWLEDGE BASE (Verified with DOA & DOAE Guidelines)
    // =========================================================================
    private fun getThaiGuide(key: String): StructuredDiseaseGuide {
        return when (key) {
            "Phytophtora" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "🦠 เชื้อสาเหตุ",
                pathogenValue = "เชื้อรา <b>Phytophthora palmivora</b> (โรคร้ายแรงอันดับ 1 ของทุเรียน)",
                riskSeasonLabel = "🌧️ ช่วงเสี่ยงระบาดหนัก",
                riskSeasonValue = "<b>ฤดูฝน / ดินแฉะน้ำขัง</b> ความชื้นสัมพัทธ์สูงกว่า 80% และแปลงที่ระบายน้ำไม่ดี",
                urgentHeader = "⚡ สิ่งที่เกษตรกรต้องทำทันที!",
                urgentBody = "<b>1. รีบขุดร่องระบายน้ำรอบโคนต้น</b> อย่าให้น้ำขังแฉะเด็ดขาด<br/>" +
                        "<b>2. สำรวจโคนต้นและกิ่งใหญ่</b> หากพบแผลมีน้ำยางไหล ให้รีบถากเปลือกที่เน่าออกแล้วทาป้ายยาทันที ก่อนแผลลามรอบต้นจนต้นตาย",
                urgentColorHex = "#DC2626",
                urgentBgHex = "#FEF2F2",
                symptomsTitle = "จุดสังเกตอาการของโรค",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "ใบ",
                        highlight = "แผลฉ่ำน้ำคล้ายโดนน้ำร้อนลวก",
                        detail = "เกิดแผลฉ่ำน้ำสีน้ำตาลอ่อนถึงดำตามแผ่นใบ ใบเหลืองซีดเหี่ยวลู่และร่วงหล่นอย่างรวดเร็ว"
                    ),
                    SymptomItem(
                        partTag = "ลำต้นและกิ่ง",
                        highlight = "มีคราบน้ำยางสีน้ำตาลแดงไหลซึม",
                        detail = "เปลือกลำต้นชื้นฉ่ำน้ำ ช่วงเช้าเห็นหยดน้ำยางสีน้ำตาลแดงไหลออก เมื่อถากเปลือกดูจะพบเนื้อเยื่อข้างในเน่าเป็นสีน้ำตาลเข้ม"
                    ),
                    SymptomItem(
                        partTag = "ระบบราก",
                        highlight = "รากฝอยเน่าเปื่อยยุ่ย ยอดเหี่ยว",
                        detail = "รากฝอยเปลี่ยนเป็นสีน้ำตาลเข้ม เปื่อยยุ่ยดึงขาดง่าย ทำให้ใบปลายกิ่งเหี่ยวเฉาและกิ่งแห้งตาย"
                    )
                ),
                stepsHeader = "แนวทางการป้องกันและกำจัด 3 ขั้นตอน",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "ขั้นที่ 1: การจัดการสวนและวิธีกล",
                        stepSubtitle = "ตัดวงจรเชื้อในดินและรักษาแผลเบื้องต้น",
                        bullets = listOf(
                            "<b>จัดการน้ำและดิน:</b> ยกโคกปลูกและทำร่องระบายน้ำไม่ให้น้ำท่วมขัง ปรับสภาพดินด้วยปูนขาวหรือโดโลไมท์ให้ค่า <b>pH อยู่ที่ 6.0 – 6.5</b>",
                            "<b>ถากแผลรักษาทันที:</b> ใช้มีดสะอาดถากเปลือกส่วนที่เน่าออกให้ถึงเนื้อไม้ดี แล้วทาแผลด้วยสารเคมีหรือไตรโคเดอร์มา <b>ซ้ำทุก 7 วัน</b> จนกว่าแผลจะแห้ง",
                            "<b>ทำความสะอาดเครื่องมือ:</b> เช็ดมีดถากแผลด้วยแอลกอฮอล์ 70% ทุกครั้งก่อนเปลี่ยนต้น เพื่อไม่ให้เชื้อติดไปต้นอื่น"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#0D9488",
                        surfaceColorHex = "#F0FDFA",
                        borderColorHex = "#99F6E4",
                        stepTitle = "ขั้นที่ 2: การใช้ชีวภัณฑ์ (ปลอดภัย ยั่งยืน)",
                        stepSubtitle = "ใช้เชื้อราดีควบคุมเชื้อราร้ายในดิน",
                        bullets = listOf(
                            "<b>หว่านทางดินรอบทรงพุ่ม:</b> ใช้ <b>เชื้อราไตรโคเดอร์มา 1 กก. + รำละเอียด 4 กก. + ปุ๋ยหมัก 100 กก.</b> คลุกให้เข้ากัน หว่านรอบโคนต้นและแนวชายพุ่ม อัตรา <b>3–5 กก./ต้น</b> ทุก 2–3 เดือน",
                            "<b>ฉีดพ่นหรือราดโคนต้น:</b> ใช้หัวเชื้อไตรโคเดอร์มา <b>100 กรัม ต่อน้ำ 20 ลิตร</b> (1 กก./น้ำ 200 ลิตร) พ่นใต้ทรงพุ่มและลำต้นในช่วงเย็นแดดอ่อน",
                            "<b>ทาแผลที่ลำต้น:</b> ผสมหัวเชื้อไตรโคเดอร์มาเข้มข้นกับปูนแดงหรือน้ำสะอาด ทาบริเวณแผลที่ถากเปลือกเน่าออกแล้ว"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_chemical_spray,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "ขั้นที่ 3: การใช้สารเคมีอย่างถูกวิธี",
                        stepSubtitle = "อัตราผสมมาตรฐานต่อถังพ่น 20 ลิตร (เมื่อระบาดรุนแรง)",
                        bullets = listOf(
                            "<b>สูตรทาแผลที่โคนต้น/กิ่ง:</b> ใช้ <b>ฟอสอีทิล-อะลูมิเนียม 80% WP</b> (อัตรา <b>80–100 กรัม/น้ำ 1 ลิตร</b>) หรือ <b>เมทาแลกซิล 25% WP</b> (อัตรา <b>50–60 กรัม/น้ำ 1 ลิตร</b>) ทาแผลทุก 7 วัน",
                            "<b>สูตรฉีดพ่นทรงพุ่ม / ราดโคนต้น:</b> ใช้ <b>ฟอสอีทิล-อะลูมิเนียม 80% WP</b> อัตรา <b>30–50 กรัม ต่อน้ำ 20 ลิตร</b> หรือ <b>เมทาแลกซิล 25% WP</b> อัตรา <b>30–50 กรัม ต่อน้ำ 20 ลิตร</b> พ่นห่างกันทุก 7–10 วัน",
                            "<b>กรณีต้นใหญ่ระบาดหนัก (ฝังเข็มลำต้น):</b> ใช้ <b>ฟอสโฟนิก แอซิด 40% SL</b> อัตรา <b>20 มล. ต่อต้น</b> อัดเข้าลำต้นเพื่อกระตุ้นภูมิคุ้มกันและหยุดเชื้อเร่งด่วน"
                        )
                    )
                ),
                cautionTitle = "ข้อควรระวังสำคัญสำหรับเกษตรกร",
                cautionBullets = listOf(
                    "<b>ห้ามผสมไตรโคเดอร์มาร่วมกับสารเคมีฆ่าเชื้อรา</b> ในถังเดียวกันเด็ดขาด และควรเว้นระยะห่างกันอย่างน้อย <b>7–10 วัน</b>",
                    "<b>ควรสลับกลุ่มสารเคมี</b> (เช่น สลับระหว่าง ฟอสอีทิล-อะลูมิเนียม กับ เมทาแลกซิล) ไม่ใช้ยาตัวเดิมซ้ำติดกันเกิน 2–3 ครั้ง เพื่อป้องกันเชื้อดื้อยา"
                ),
                referenceButtonTitle = "เปิดเอกสารวิชาการอ้างอิงฉบับเต็ม",
                referenceButtonSubtitle = "แหล่งข้อมูลทางการ: กรมวิชาการเกษตร (DOA)",
                referenceUrl = URL_DOA_PHYTOPHTHORA
            )

            "Anthracnose" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "🦠 เชื้อสาเหตุ",
                pathogenValue = "เชื้อรา <b>Colletotrichum gloeosporioides</b> (แพร่กระจายทางลมและฝน)",
                riskSeasonLabel = "🌦️ ช่วงเสี่ยงระบาดหนัก",
                riskSeasonValue = "<b>สภาพอากาศร้อนชื้น / ฝนตกสลับแดด</b> โดยเฉพาะระยะแตกใบอ่อน ดอกบาน และผลพัฒนา",
                urgentHeader = "⚡ สิ่งที่เกษตรกรต้องทำทันที!",
                urgentBody = "<b>รีบตัดใบ กิ่งแห้ง หรือผลที่เป็นแผลจุดสีน้ำตาลออกไปเผาทำลายนอกแปลงทันที</b> เพื่อตัดแหล่งสร้างสปอร์ไม่ให้ปลิวไปติดใบอ่อนและผลชุดอื่น",
                urgentColorHex = "#EA580C",
                urgentBgHex = "#FFF7ED",
                symptomsTitle = "จุดสังเกตอาการของโรค",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "ใบ",
                        highlight = "แผลไหม้สีน้ำตาลแดง มีวงซ้อนกันเป็นชั้น",
                        detail = "มักเริ่มจากปลายใบหรือขอบใบ ขยายเป็นวงสีน้ำตาลซ้อนกัน มีจุดตุ่มสีดำเล็กๆ (กลุ่มสปอร์) เรียงอยู่ตรงกลางแผล ทำให้ใบแห้งกรอบและร่วง"
                    ),
                    SymptomItem(
                        partTag = "ดอก",
                        highlight = "ดอกช้ำเป็นสีน้ำตาลดำและร่วงหล่น",
                        detail = "หากระบาดช่วงดอกตูมหรือดอกบาน จะทำให้กลีบดอกช้ำเน่าดำและร่วงหล่นทั้งช่อ"
                    ),
                    SymptomItem(
                        partTag = "ผลทุเรียน",
                        highlight = "แผลยุบตัวสีน้ำตาลเข้มตามร่องหนาม",
                        detail = "เกิดแผลเน่าแห้งสีน้ำตาลดำบริเวณหนาม และลุกลามทำให้ผลเน่าเสียหายโดยเฉพาะช่วงใกล้เก็บเกี่ยว"
                    )
                ),
                stepsHeader = "แนวทางการป้องกันและกำจัด 3 ขั้นตอน",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "ขั้นที่ 1: การจัดการทรงพุ่มและสุขอนามัยสวน",
                        stepSubtitle = "ลดความชื้นสะสมและแหล่งเพาะเชื้อรา",
                        bullets = listOf(
                            "<b>ตัดแต่งกิ่งให้โปร่ง:</b> หลังเก็บเกี่ยวต้องตัดแต่งกิ่งทึบ กิ่งแห้ง ให้แสงแดดส่องถึงและอากาศถ่ายเทสะดวก",
                            "<b>เผาทำลายเศษซากที่เป็นโรค:</b> เก็บใบและผลที่ร่วงหล่นออกจากแปลงไปทำลาย <b>ห้ามกองหมักไว้ใต้โคนต้น</b>",
                            "<b>ปกป้องระยะแตกใบอ่อน:</b> สำรวจแปลงอย่างใกล้ชิดทุกครั้งที่ทุเรียนเริ่มแทงยอดใบอ่อน"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#0D9488",
                        surfaceColorHex = "#F0FDFA",
                        borderColorHex = "#99F6E4",
                        stepTitle = "ขั้นที่ 2: การใช้ชีวภัณฑ์ป้องกัน",
                        stepSubtitle = "ฉีดพ่นเคลือบใบอ่อนและทรงพุ่ม",
                        bullets = listOf(
                            "<b>พ่นเชื้อราไตรโคเดอร์มา:</b> อัตรา <b>100 กรัม ต่อน้ำ 20 ลิตร</b> พ่นเคลือบทรงพุ่มช่วงเย็นทุก 7–10 วัน เพื่อยับยั้งการงอกของสปอร์",
                            "<b>พ่นแบคทีเรีย บาซิลลัส ซับทิลิส (BS):</b> ตามอัตราแนะนำบนฉลาก พ่นป้องกันตั้งแต่ระยะเริ่มแตกใบอ่อนและก่อนดอกบาน"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_chemical_spray,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "ขั้นที่ 3: การใช้สารเคมีอย่างถูกวิธี",
                        stepSubtitle = "อัตราผสมมาตรฐานต่อน้ำ 20 ลิตร (พ่นทุก 5–7 วันเมื่อระบาด)",
                        bullets = listOf(
                            "<b>โพรคลอราซ (Prochloraz 45% EC):</b> อัตรา <b>20–30 มล. ต่อน้ำ 20 ลิตร</b> (ออกฤทธิ์กำจัดเชื้อได้ดีเยี่ยม)",
                            "<b>อะซอกซีสโตรบิน (Azoxystrobin 25% SC):</b> อัตรา <b>10–20 มล. ต่อน้ำ 20 ลิตร</b> (ดูดซึมและคุ้มครองใบใหม่)",
                            "<b>ไดฟีโนโคนาโซล (Difenoconazole 25% EC):</b> อัตรา <b>10–15 มล. ต่อน้ำ 20 ลิตร</b>",
                            "<b>แมนโคเซบ (Mancozeb 80% WP):</b> อัตรา <b>40–50 กรัม ต่อน้ำ 20 ลิตร</b> (เหมาะสำหรับพ่นป้องกันล่วงหน้าช่วงแตกใบอ่อน)"
                        )
                    )
                ),
                cautionTitle = "ข้อควรระวังสำคัญสำหรับเกษตรกร",
                cautionBullets = listOf(
                    "<b>หลีกเลี่ยงการพ่นยาชนิดเดียวติดต่อกันเกิน 2 ครั้ง</b> ให้สลับกลุ่มสารเคมี (เช่น สลับ โพรคลอราซ กับ อะซอกซีสโตรบิน) เพื่อไม่ให้เชื้อราดื้อยา",
                    "<b>ผสมสารจับใบ</b> ทุกครั้งที่พ่นในช่วงฤดูฝน เพื่อช่วยให้ตัวยาเกาะติดผิวใบทุเรียนที่มีความมันเงาได้ดีขึ้น"
                ),
                referenceButtonTitle = "เปิดคู่มือโรคทุเรียนและการป้องกันกำจัด",
                referenceButtonSubtitle = "แหล่งข้อมูลทางการ: กรมส่งเสริมการเกษตร (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )

            "Rhizoctonia" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "🦠 เชื้อสาเหตุ",
                pathogenValue = "เชื้อรา <b>Rhizoctonia solani</b> (โรคใบติด / โรคใบร่วงในหน้าฝน)",
                riskSeasonLabel = "🌧️ ช่วงเสี่ยงระบาดหนัก",
                riskSeasonValue = "<b>ฤดูฝนที่มีฝนตกชุกต่อเนื่อง</b> ทรงพุ่มทึบ อับลม และช่วงที่ต้นแตกใบอ่อนหนาแน่น",
                urgentHeader = "⚡ สิ่งที่เกษตรกรต้องทำทันที!",
                urgentBody = "<b>รีบตัดกิ่งที่มีใบแห้งติดกันเป็นกระจุก และกวาดใบที่ร่วงใต้โคนต้นไปเผาทำลายนอกแปลงทันที!</b> เพราะเส้นใยและเม็ดสเคลอโรเตียมในใบที่ร่วงจะอยู่ข้ามปีและกลับมาระบาดซ้ำได้รวดเร็วมาก",
                urgentColorHex = "#D97706",
                urgentBgHex = "#FFFBEB",
                symptomsTitle = "จุดสังเกตอาการของโรค",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "ใบอ่อน/เพสลาด",
                        highlight = "แผลช้ำคล้ายน้ำร้อนลวกที่ปลายใบ",
                        detail = "เริ่มจากแผลสีเขียวซีดฉ่ำน้ำแล้วเปลี่ยนเป็นสีน้ำตาลอย่างรวดเร็ว ลามทั่วแผ่นใบภายในไม่กี่วัน"
                    ),
                    SymptomItem(
                        partTag = "จุดเด่นสำคัญ",
                        highlight = "มีเส้นใยสีขาวคล้ายใยแมงมุมยึดใบติดกัน",
                        detail = "เมื่ออากาศชื้นจัด เชื้อราจะสร้างเส้นใยสีขาวยึดใบหลายๆ ใบให้ติดกันเป็นกระจุกคาอยู่บนกิ่ง ไม่ร่วงหล่นตามปกติ"
                    ),
                    SymptomItem(
                        partTag = "กิ่งและยอด",
                        highlight = "ใบร่วงโกร๋นและปลายกิ่งแห้งตาย (Dieback)",
                        detail = "หากระบาดรุนแรง ใบจะไหม้แห้งและร่วงจนเหลือแต่กิ่งเปล่า ส่งผลให้ยอดแห้งตายและต้นโทรมหนัก"
                    )
                ),
                stepsHeader = "แนวทางการป้องกันและกำจัด 3 ขั้นตอน",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "ขั้นที่ 1: การตัดแต่งกิ่งและควบคุมปุ๋ย (สำคัญที่สุด)",
                        stepSubtitle = "ลดความชื้นในทรงพุ่มและลดความอ่อนแอของใบ",
                        bullets = listOf(
                            "<b>ตัดแต่งกิ่งให้โปร่งก่อนเข้าหน้าฝน:</b> เปิดช่องแสงให้แดดส่องถึงภายในทรงพุ่ม ลดการเบียดเสียดของใบ",
                            "<b>งดใส่ปุ๋ยไนโตรเจนสูงช่วงระบาด:</b> ชะลอการใช้ปุ๋ยสูตรตัวหน้าสูง (เช่น 46-0-0 หรือ 25-7-7) ในช่วงฝนตกชุก เพราะจะเร่งให้แตกใบอ่อนที่บอบบางและติดโรคง่าย",
                            "<b>เก็บกวาดใบเผาทำลาย:</b> เก็บใบที่เป็นโรคทั้งบนต้นและที่ร่วงบนพื้นดินไปเผานอกสวนอย่างสม่ำเสมอ"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#0D9488",
                        surfaceColorHex = "#F0FDFA",
                        borderColorHex = "#99F6E4",
                        stepTitle = "ขั้นที่ 2: การใช้ชีวภัณฑ์ป้องกันล่วงหน้า",
                        stepSubtitle = "ใช้ก่อนเข้าฤดูฝน 1–2 เดือน หรือช่วงเริ่มแตกใบอ่อน",
                        bullets = listOf(
                            "<b>พ่นและหว่านเชื้อราไตรโคเดอร์มา:</b> ผสมน้ำพ่นทรงพุ่มและหว่านลงดินรอบโคนต้นเพื่อทำลายเม็ดสเคลอโรเตียมของเชื้อราที่สะสมในดิน",
                            "<b>พ่น บาซิลลัส ซับทิลิส (BS):</b> พ่นเคลือบใบอ่อนและใบเพสลาดทุก 7–10 วันในแปลงที่มีประวัติการระบาด"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_chemical_spray,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "ขั้นที่ 3: การใช้สารเคมีอย่างถูกวิธี",
                        stepSubtitle = "อัตราผสมมาตรฐานต่อน้ำ 20 ลิตร (พ่นทุก 7 วันเมื่อพบอาการ)",
                        bullets = listOf(
                            "<b>เฮกซะโคนาโซล (Hexaconazole 5% SC):</b> อัตรา <b>20–30 มล. ต่อน้ำ 20 ลิตร</b> (สารดูดซึม หยุดการลุกลามของเส้นใยได้ดีมาก)",
                            "<b>คอปเปอร์ออกซีคลอไรด์ 85% WP:</b> อัตรา <b>30–50 กรัม ต่อน้ำ 20 ลิตร</b> หรือ <b>คอปเปอร์ไฮดรอกไซด์ 77% WP:</b> อัตรา <b>20–30 กรัม ต่อน้ำ 20 ลิตร</b>",
                            "<b>วาลิดามัยซิน (Validamycin 3% SL):</b> อัตรา <b>30–40 มล. ต่อน้ำ 20 ลิตร</b> หรือ <b>พีนไซคูรอน (Pencycuron 25% WP):</b> อัตรา <b>20–30 กรัม ต่อน้ำ 20 ลิตร</b>"
                        )
                    )
                ),
                cautionTitle = "ข้อควรระวังสำคัญสำหรับเกษตรกร",
                cautionBullets = listOf(
                    "<b>สารกลุ่มคอปเปอร์</b> ไม่ควรพ่นในระยะดอกบานหรือขณะแดดจัด เพราะอาจทำให้ดอกร่วงหรือเกิดอาการใบไหม้ได้",
                    "ควรพ่นสารให้เปียกทั่วถึงทั้ง <b>ด้านบนใบ ด้านใต้ใบ และกิ่งด้านในทรงพุ่ม</b>"
                ),
                referenceButtonTitle = "เปิดคู่มือโรคทุเรียนและการป้องกันกำจัด",
                referenceButtonSubtitle = "แหล่งข้อมูลทางการ: กรมส่งเสริมการเกษตร (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )

            "Phomopsis" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "🦠 เชื้อสาเหตุ",
                pathogenValue = "เชื้อรา <b>Phomopsis spp.</b> (โรคใบจุดโฟมอปซิส)",
                riskSeasonLabel = "🌤️ ช่วงเสี่ยงระบาดหนัก",
                riskSeasonValue = "<b>ต้นทุเรียนอ่อนแอ / ขาดน้ำหรือธาตุอาหาร</b> หรือมีบาดแผลจากแมลงและลมแรง",
                urgentHeader = "⚡ สิ่งที่เกษตรกรต้องทำทันที!",
                urgentBody = "<b>เก็บกวาดใบที่เป็นจุดแผลร่วงหล่นไปเผาทำลาย</b> พร้อมเร่งบำรุงต้นให้แข็งแรงและให้น้ำอย่างสม่ำเสมอ เพราะเชื้อนี้มักแฝงตัวและลุกลามหนักเมื่อต้นทุเรียนเกิดความเครียดหรืออ่อนแอ",
                urgentColorHex = "#854D0E",
                urgentBgHex = "#FEFCE8",
                symptomsTitle = "จุดสังเกตอาการของโรค",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "ใบแก่และใบเพสลาด",
                        highlight = "จุดแผลสีน้ำตาลเข้ม มีวงเหลืองล้อมรอบชัดเจน",
                        detail = "เริ่มจากจุดเล็กๆ สีน้ำตาลเข้มแล้วขยายออก รอบแผลมีวงสีเหลือง (Yellow halo) ล้อมรอบอย่างชัดเจน"
                    ),
                    SymptomItem(
                        partTag = "กลางแผล",
                        highlight = "กลางแผลสีขาวอมเทา มีจุดดำเล็กๆ (พิคนิเดียม)",
                        detail = "เมื่อแผลเก่าลง เนื้อเยื่อตรงกลางจะเปลี่ยนเป็นสีขาวอมเทา และมีตุ่มเม็ดสีดำเล็กๆ ซึ่งเป็นแหล่งสร้างสปอร์กระจายอยู่ตรงกลาง"
                    ),
                    SymptomItem(
                        partTag = "ผลกระทบต่อต้น",
                        highlight = "ใบเหลืองร่วงก่อนกำหนด สังเคราะห์แสงลดลง",
                        detail = "หากเกิดจุดแผลจำนวนมากบนใบ จะทำให้ใบเหลืองและร่วงหล่น ส่งผลให้การสะสมอาหารของต้นลดลง"
                    )
                ),
                stepsHeader = "แนวทางการป้องกันและกำจัด 3 ขั้นตอน",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "ขั้นที่ 1: การบำรุงต้นและทำความสะอาดแปลง",
                        stepSubtitle = "เสริมความแข็งแรงให้ต้นทุเรียนต้านทานโรค",
                        bullets = listOf(
                            "<b>บำรุงน้ำและธาตุอาหารให้เพียงพอ:</b> ดูแลระบบน้ำและให้ปุ๋ยตามระยะการเจริญเติบโต อย่าให้ต้นขาดน้ำหรือทรุดโทรมหลังเก็บเกี่ยว",
                            "<b>ป้องกันแมลงทำลายใบอ่อน:</b> ควบคุมเพลี้ยไฟ เพลี้ยจักจั่นฝอย ไม่ให้ดูดกินสร้างบาดแผลซึ่งเป็นช่องทางให้เชื้อราเข้าทำลาย",
                            "<b>กำจัดใบที่เป็นโรค:</b> เก็บใบที่ร่วงหล่นใต้โคนต้นนำไปเผาทำลายนอกแปลงเพื่อลดปริมาณสปอร์สะสม"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#0D9488",
                        surfaceColorHex = "#F0FDFA",
                        borderColorHex = "#99F6E4",
                        stepTitle = "ขั้นที่ 2: การใช้ชีวภัณฑ์ป้องกัน",
                        stepSubtitle = "ลดปริมาณเชื้อราสะสมในทรงพุ่ม",
                        bullets = listOf(
                            "<b>พ่นเชื้อราไตรโคเดอร์มา:</b> อัตรา <b>100 กรัม ต่อน้ำ 20 ลิตร</b> พ่นให้ทั่วทรงพุ่มทุก 10–14 วันในช่วงที่มีความชื้นสูง",
                            "<b>พ่น บาซิลลัส ซับทิลิส (BS):</b> ช่วยเคลือบผิวใบป้องกันการเข้าทำลายของสปอร์เชื้อราในระยะใบอ่อนถึงใบเพสลาด"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_chemical_spray,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "ขั้นที่ 3: การใช้สารเคมีอย่างถูกวิธี",
                        stepSubtitle = "อัตราผสมมาตรฐานต่อน้ำ 20 ลิตร (พ่นทุก 7–10 วัน)",
                        bullets = listOf(
                            "<b>เฮกซะโคนาโซล (Hexaconazole 5% SC):</b> อัตรา <b>20–30 มล. ต่อน้ำ 20 ลิตร</b> (สารดูดซึมยับยั้งเชื้อรา)",
                            "<b>ไดฟีโนโคนาโซล (Difenoconazole 25% EC):</b> อัตรา <b>10–15 มล. ต่อน้ำ 20 ลิตร</b>",
                            "<b>คาร์เบนดาซิม (Carbendazim 50% SC):</b> อัตรา <b>20–30 มล. ต่อน้ำ 20 ลิตร</b>",
                            "<b>แมนโคเซบ (Mancozeb 80% WP):</b> อัตรา <b>40–50 กรัม ต่อน้ำ 20 ลิตร</b> (สามารถใช้พ่นสลับหรือร่วมเพื่อป้องกันการลุกลาม)"
                        )
                    )
                ),
                cautionTitle = "ข้อควรระวังสำคัญสำหรับเกษตรกร",
                cautionBullets = listOf(
                    "อาการจุดเหลืองอาจคล้ายอาการขาดธาตุอาหาร ให้สังเกต <b>ตุ่มจุดสีดำเล็กๆ ตรงกลางแผลสีเทาขาว</b> เพื่อยืนยันว่าเป็นโรคใบจุดโฟมอปซิส",
                    "ควรสลับกลุ่มสารเคมีทุก 2 ครั้ง เพื่อป้องกันเชื้อราสร้างความต้านทาน"
                ),
                referenceButtonTitle = "เปิดคู่มือโรคทุเรียนและการป้องกันกำจัด",
                referenceButtonSubtitle = "แหล่งข้อมูลทางการ: กรมส่งเสริมการเกษตร (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )

            "Algal spot" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "🦠 เชื้อสาเหตุ",
                pathogenValue = "สาหร่ายปรสิต <b>Cephaleuros virescens</b> (โรคจุดสนิม / จุดสาหร่าย)",
                riskSeasonLabel = "🌿 ช่วงเสี่ยงระบาดหนัก",
                riskSeasonValue = "<b>สวนที่ทรงพุ่มทึบ แสงแดดส่องไม่ถึง</b> อากาศอับชื้น และมีฝนตกชุก",
                urgentHeader = "⚡ สิ่งที่เกษตรกรต้องทำทันที!",
                urgentBody = "<b>ตัดแต่งกิ่งที่ทึบและซ้อนทับกันออกทันทีเพื่อให้แสงแดดส่องถึงภายในทรงพุ่ม!</b> เพราะสาหร่ายชนิดนี้แพ้แสงแดดและการถ่ายเทอากาศที่ดี หากปล่อยไว้จะลามเข้ากิ่งอ่อนทำให้กิ่งแห้งแตกได้",
                urgentColorHex = "#0D9488",
                urgentBgHex = "#F0FDFA",
                symptomsTitle = "จุดสังเกตอาการของโรค",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "ระยะเริ่มต้น",
                        highlight = "จุดกลมนูนสีเขียวปนเทาบนแผ่นใบ",
                        detail = "เกิดจุดกลมเล็กๆ นูนขึ้นมาจากผิวใบด้านบน มีขอบเขตชัดเจน มักพบมากในใบแก่ด้านในทรงพุ่ม"
                    ),
                    SymptomItem(
                        partTag = "ระยะเจริญเต็มที่",
                        highlight = "ฟูเป็นขุยคล้ายกำมะหยี่ สีส้มแดงหรือสีสนิมเหล็ก",
                        detail = "เมื่อสาหร่ายสร้างอับสปอร์ จะเปลี่ยนเป็นสีน้ำตาลแดงหรือสีสนิมเหล็ก มีลักษณะฟูเป็นขุยคล้ายกำมะหยี่อย่างชัดเจน"
                    ),
                    SymptomItem(
                        partTag = "กิ่งและลำต้น",
                        highlight = "เปลือกกิ่งแตกร่อน กิ่งแห้งทรุดโทรม",
                        detail = "รากเทียมของสาหร่ายจะชอนไชเข้าไปในเนื้อเยื่อใบและเปลือกกิ่ง ทำให้ใบเหลืองร่วงและกิ่งขนาดเล็กแห้งตาย"
                    )
                ),
                stepsHeader = "แนวทางการป้องกันและกำจัด 3 ขั้นตอน",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "ขั้นที่ 1: การจัดการทรงพุ่มและแสงแดด",
                        stepSubtitle = "ลดความชื้นและปรับสภาพแวดล้อมไม่ให้เหมาะต่อสาหร่าย",
                        bullets = listOf(
                            "<b>ตัดแต่งกิ่งในทรงพุ่มให้โปร่ง:</b> ให้แสงแดดส่องลอดผ่านถึงกิ่งด้านในและโคนต้นได้ทั่วถึง",
                            "<b>กำจัดวัชพืชรอบโคนต้น:</b> ลดความชื้นสะสมในแปลงปลูก และตัดกิ่งที่ถูกสาหร่ายเข้าทำลายรุนแรงไปเผาทำลาย",
                            "<b>บำรุงต้นให้สมบูรณ์:</b> ใส่ปุ๋ยอินทรีย์และปุ๋ยเคมีตามระยะ เพื่อให้ต้นแตกใบใหม่ทดแทนได้อย่างรวดเร็ว"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_chemical_spray,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "ขั้นที่ 2: การฉีดพ่นสารกำจัดสาหร่าย",
                        stepSubtitle = "อัตราผสมมาตรฐานต่อน้ำ 20 ลิตร (พ่นซ้ำทุก 7–10 วัน)",
                        bullets = listOf(
                            "<b>คอปเปอร์ออกซีคลอไรด์ (Copper oxychloride 85% WP):</b> อัตรา <b>30–50 กรัม ต่อน้ำ 20 ลิตร</b> พ่นให้ทั่วใบและกิ่งที่เป็นโรค",
                            "<b>คอปเปอร์ไฮดรอกไซด์ (Copper hydroxide 77% WP):</b> อัตรา <b>20–30 กรัม ต่อน้ำ 20 ลิตร</b>",
                            "<b>เทคนิคการพ่น:</b> ผสมสารจับใบทุกครั้ง และเน้นพ่นให้เปียกทั่ว <b>ท้องใบ กิ่งด้านใน และลำต้น</b> บริเวณที่พบคราบสีสนิม"
                        )
                    )
                ),
                cautionTitle = "ข้อควรระวังสำคัญสำหรับเกษตรกร",
                cautionBullets = listOf(
                    "หลังจากพ่นสารคอปเปอร์จนสาหร่ายตายแล้ว <b>จุดแผลจะเปลี่ยนเป็นสีน้ำตาลคล้ำหรือเทาซีด</b> แต่รอยแผลเดิมจะยังคงติดอยู่บนใบ ไม่หลุดลอกออก ให้สังเกตว่าไม่มีขุยสีส้มแดงเกิดใหม่บนใบชุดถัดไป",
                    "ห้ามใช้สารคอปเปอร์เกินอัตราที่กำหนดและหลีกเลี่ยงการพ่นขณะดอกบาน"
                ),
                referenceButtonTitle = "เปิดคู่มือโรคทุเรียนและการป้องกันกำจัด",
                referenceButtonSubtitle = "แหล่งข้อมูลทางการ: กรมส่งเสริมการเกษตร (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )

            "Healthy" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "✅ สถานะสุขภาพใบ",
                pathogenValue = "<b>ใบทุเรียนปกติ สมบูรณ์แข็งแรง</b> ไม่พบร่องรอยการเข้าทำลายของโรคหลัก",
                riskSeasonLabel = "🛡️ การเฝ้าระวัง",
                riskSeasonValue = "หมั่นสำรวจแปลงทุก <b>7–14 วัน</b> โดยเฉพาะช่วงเปลี่ยนฤดูและระยะแตกใบอ่อน",
                urgentHeader = "🌟 ต้นทุเรียนมีสุขภาพใบสมบูรณ์ดี",
                urgentBody = "<b>คงมาตรฐานการดูแลสวนอย่างต่อเนื่อง</b> ทั้งการจัดการน้ำ ธาตุอาหาร และการรักษาความโปร่งของทรงพุ่ม เพื่อป้องกันไม่ให้โรคเข้าทำลายในระยะถัดไป",
                urgentColorHex = "#059669",
                urgentBgHex = "#ECFDF5",
                symptomsTitle = "ลักษณะของใบทุเรียนที่สมบูรณ์แข็งแรง",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "สีและผิวใบ",
                        highlight = "สีเขียวเข้มสม่ำเสมอ ผิวใบมันเงา",
                        detail = "แผ่นใบเรียบตึง มีความหนาสมบูรณ์ ไม่มีจุดแผลฉ่ำน้ำ รอยไหม้ หรือขุยสนิมเกาะบนผิวใบ"
                    ),
                    SymptomItem(
                        partTag = "การสังเคราะห์แสง",
                        highlight = "สะสมอาหารเลี้ยงลำต้น ดอก และผลได้เต็มที่",
                        detail = "ใบเพสลาดและใบแก่ทำงานได้เต็มประสิทธิภาพ ช่วยให้ต้นทุเรียนพร้อมสำหรับการออกดอกและติดผล"
                    )
                ),
                stepsHeader = "แนวทางการดูแลรักษาเชิงป้องกัน",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "1. การดูแลน้ำ ดิน และธาตุอาหาร",
                        stepSubtitle = "เสริมสร้างภูมิคุ้มกันตามธรรมชาติให้ต้นทุเรียน",
                        bullets = listOf(
                            "<b>การให้น้ำอย่างเหมาะสม:</b> ให้น้ำสม่ำเสมอแต่ไม่ขังแฉะ และตรวจสอบระบบระบายน้ำในแปลงก่อนเข้าฤดูฝน",
                            "<b>การให้ปุ๋ยและธาตุอาหารรอง:</b> เติมปุ๋ยอินทรีย์ร่วมกับธาตุอาหารรอง-เสริม เช่น <b>แคลเซียม-โบรอน และแมกนีเซียม</b> เพื่อช่วยให้ผนังเซลล์ใบแข็งแรง",
                            "<b>การตัดแต่งกิ่ง:</b> แต่งกิ่งแขนงด้านในและกิ่งแห้งออกเพื่อให้แสงแดดส่องถึงโคนต้น"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#0D9488",
                        surfaceColorHex = "#F0FDFA",
                        borderColorHex = "#99F6E4",
                        stepTitle = "2. การใช้ชีวภัณฑ์คุ้มครองสวน",
                        stepSubtitle = "ป้องกันโรครากเน่าโคนเน่าและโรคทางใบล่วงหน้า",
                        bullets = listOf(
                            "<b>หว่านเชื้อราไตรโคเดอร์มาทางดิน:</b> ผสมปุ๋ยหมักหว่านรอบทรงพุ่มทุก <b>2–3 เดือน</b> เพื่อคุมปริมาณเชื้อไฟทอปธอราในดินไม่ให้เพิ่มจำนวน",
                            "<b>สำรวจใต้ใบสม่ำเสมอ:</b> ตรวจเช็กแมลงศัตรูพืช เช่น เพลี้ยไฟ ไรแดง และเพลี้ยไก่แจ้ ในระยะเริ่มแทงยอดอ่อน"
                        )
                    )
                ),
                cautionTitle = null,
                cautionBullets = emptyList(),
                referenceButtonTitle = "เปิดคู่มือการดูแลและป้องกันโรคทุเรียน",
                referenceButtonSubtitle = "แหล่งข้อมูลทางการ: กรมส่งเสริมการเกษตร (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )

            else -> StructuredDiseaseGuide(
                key = "Other",
                pathogenLabel = "🔎 สาเหตุที่พบ",
                pathogenValue = "<b>ความผิดปกติอื่นๆ ที่ไม่ใช่ 5 โรคเชื้อรา/สาหร่ายหลัก</b>",
                riskSeasonLabel = "⚠️ ปัจจัยที่เกี่ยวข้อง",
                riskSeasonValue = "<b>แมลงศัตรูพืช / อาการขาดธาตุอาหาร / สภาพอากาศแปรปรวน</b> (แดดจัด ลมแรง หรือดินเค็ม)",
                urgentHeader = "⚡ คำแนะนำการตรวจสอบเบื้องต้นทันที!",
                urgentBody = "<b>พลิกดูใต้ใบและยอดอ่อนเพื่อสำรวจตัวแมลง</b> พร้อมตรวจสอบความชื้นในดินและประวัติการใส่ปุ๋ย/พ่นยา เพื่อให้แก้ไขได้ตรงสาเหตุโดยไม่พ่นยาฆ่าเชื้อราผิดประเภท",
                urgentColorHex = "#475569",
                urgentBgHex = "#F8FAFC",
                symptomsTitle = "แยกแยะสาเหตุความผิดปกติที่พบบ่อย",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "แมลงศัตรูพืช",
                        highlight = "รอยดูดกิน ใบหงิกงอ หรือมีคราบฝุ่นสีขาว/ใยบาง",
                        detail = "เกิดจาก <b>เพลี้ยไก่แจ้</b> (มีปุยขาว), <b>เพลี้ยไฟ</b> (ขอบใบม้วนงอเป็นสีน้ำตาล), หรือ <b>ไรแดง</b> (ผิวใบด้านซีดเป็นจุดขาวละเอียด)"
                    ),
                    SymptomItem(
                        partTag = "ขาดธาตุอาหาร",
                        highlight = "ใบเหลืองอย่างมีรูปแบบชัดเจน เช่น เหลืองระหว่างเส้นใบ",
                        detail = "เช่น <b>ขาดแมกนีเซียม</b> (ใบแก่เหลืองแต่เส้นใบยังเขียว), <b>ขาดสังกะสี</b> (ใบเล็กเรียวและเหลือง), หรือ <b>ขาดไนโตรเจน</b> (ใบเหลืองซีดทั้งใบ)"
                    ),
                    SymptomItem(
                        partTag = "สภาพแวดล้อม",
                        highlight = "ขอบใบไหม้แห้งจากแดดจัด ขาดน้ำ หรือปุ๋ยเคมีเข้มข้นเกินไป",
                        detail = "ปลายใบหรือขอบใบแห้งเป็นสีน้ำตาลโดยไม่มีจุดสปอร์เชื้อรา มักเกิดพร้อมกันทั้งต้นหรือด้านที่รับแดดจัด"
                    )
                ),
                stepsHeader = "แนวทางการจัดการและแก้ไขอย่างถูกจุด",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "1. วิธีตรวจสอบและแก้ไขตามอาการ",
                        stepSubtitle = "วินิจฉัยแยกแยะก่อนตัดสินใจใช้สารเคมี",
                        bullets = listOf(
                            "<b>กรณีพบแมลงศัตรูพืช:</b> เลือกใช้สารป้องกันกำจัดแมลงให้ตรงชนิด เช่น สไปโรเตตรามีต, อะบาเมกติน, หรือ อิมิดาคลอพริด ตามคำแนะนำกรมวิชาการเกษตร",
                            "<b>กรณีอาการขาดธาตุอาหาร:</b> ตรวจวัดค่า pH ดิน (ควรอยู่ที่ 5.5–6.5) เพื่อให้รากดูดปุ๋ยได้ดี และพ่นปุ๋ยทางใบเสริมธาตุอาหารรอง-เสริม",
                            "<b>กรณีขอบใบไหม้จากดินเค็ม/ขาดน้ำ:</b> ปรับการให้น้ำอย่างเพียงพอและตรวจเช็กค่าความเค็มของน้ำที่ใช้รด"
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "2. การส่งตัวอย่างปรึกษาผู้เชี่ยวชาญ",
                        stepSubtitle = "เมื่ออาการไม่ชัดเจนหรือลุกลามหลายต้น",
                        bullets = listOf(
                            "<b>เก็บตัวอย่างใบที่มีอาการหลายระยะ</b> ใส่ถุงพลาสติกมัดปากถุง และถ่ายภาพทรงพุ่มรวมถึงบริเวณโคนต้น",
                            "<b>นำปรึกษาสำนักงานเกษตรอำเภอ</b> หรือกลุ่มอารักขาพืช สำนักวิจัยและพัฒนาการเกษตรในพื้นที่ เพื่อตรวจวินิจฉัยอย่างแม่นยำ"
                        )
                    )
                ),
                cautionTitle = "ข้อควรระวังสำคัญสำหรับเกษตรกร",
                cautionBullets = listOf(
                    "<b>ไม่ควรพ่นสารเคมีฆ่าเชื้อราโดยไม่จำเป็น</b> หากอาการเกิดจากแมลงหรือการขาดธาตุอาหาร เพราะจะทำให้สิ้นเปลืองต้นทุนและไม่ได้ผล"
                ),
                referenceButtonTitle = "เปิดคู่มือโรคและการดูแลทุเรียนฉบับเต็ม",
                referenceButtonSubtitle = "แหล่งข้อมูลทางการ: กรมส่งเสริมการเกษตร (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )
        }
    }

    // =========================================================================
    // ENGLISH KNOWLEDGE BASE (Verified with DOA & DOAE Guidelines)
    // =========================================================================
    private fun getEnglishGuide(key: String): StructuredDiseaseGuide {
        return when (key) {
            "Phytophtora" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "🦠 Pathogen",
                pathogenValue = "<b>Phytophthora palmivora</b> oomycete (Most destructive durian disease)",
                riskSeasonLabel = "🌧️ High-Risk Condition",
                riskSeasonValue = "<b>Heavy monsoon rains / Waterlogged soil</b>, relative humidity > 80%, and poor orchard drainage",
                urgentHeader = "⚡ Immediate Farmer Action Required!",
                urgentBody = "<b>1. Dig drainage trenches immediately</b> to eliminate standing water around the trunk.<br/>" +
                        "<b>2. Inspect the trunk and main branches</b> — if oozing reddish-brown lesions are found, scrape off infected bark and apply fungicide paste immediately before girdling kills the tree.",
                urgentColorHex = "#DC2626",
                urgentBgHex = "#FEF2F2",
                symptomsTitle = "Key Symptoms to Identify",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "Leaves",
                        highlight = "Water-soaked scalded-looking dark blotches",
                        detail = "Light brown water-soaked lesions rapidly turning dark brown/black; leaves turn yellow, droop, and shed prematurely."
                    ),
                    SymptomItem(
                        partTag = "Trunk & Branches",
                        highlight = "Reddish-brown resinous exudate oozing from bark",
                        detail = "Wet bark lesions visible in early morning with reddish-brown gum drops. Scraping bark reveals dark brown necrotic tissue underneath."
                    ),
                    SymptomItem(
                        partTag = "Root System",
                        highlight = "Rotten brown feeder roots & terminal shoot wilting",
                        detail = "Feeder roots decay and slough off easily, starving the canopy and causing severe branch dieback."
                    )
                ),
                stepsHeader = "3-Step Prevention & Treatment Plan",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "Step 1: Orchard Drainage & Mechanical Care",
                        stepSubtitle = "Eliminate waterlogging and treat trunk cankers early",
                        bullets = listOf(
                            "<b>Soil & Water Management:</b> Maintain raised planting mounds and drainage channels. Balance soil pH to <b>6.0 – 6.5</b> using agricultural lime or dolomite.",
                            "<b>Wound Surgery:</b> Scrape away rotted bark down to healthy wood and paint with fungicide or Trichoderma paste <b>every 7 days</b> until dry.",
                            "<b>Tool Sterilization:</b> Disinfect pruning and scraping knives with 70% alcohol between trees to prevent cross-contamination."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#0D9488",
                        surfaceColorHex = "#F0FDFA",
                        borderColorHex = "#99F6E4",
                        stepTitle = "Step 2: Biological Control (Eco-Safe)",
                        stepSubtitle = "Suppress soil-borne pathogen population with beneficial fungi",
                        bullets = listOf(
                            "<b>Soil Broadcast:</b> Mix <b>1 kg Trichoderma + 4 kg fine rice bran + 100 kg compost</b>; broadcast around the drip line at <b>3–5 kg/tree</b> every 2–3 months.",
                            "<b>Soil & Canopy Spray:</b> Mix fresh Trichoderma at <b>100 g per 20 L water</b> (1 kg/200 L) and spray soil surface and lower trunk in late afternoon."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_chemical_spray,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "Step 3: Targeted Chemical Control",
                        stepSubtitle = "Standard dosage per 20 Liters of water (for severe infections)",
                        bullets = listOf(
                            "<b>Trunk Wound Paste:</b> Apply <b>Fosetyl-aluminium 80% WP</b> (<b>80–100 g per 1 L water</b>) or <b>Metalaxyl 25% WP</b> (<b>50–60 g per 1 L water</b>) onto scraped wounds every 7 days.",
                            "<b>Foliar Spray / Soil Drench:</b> Use <b>Fosetyl-aluminium 80% WP</b> at <b>30–50 g per 20 L water</b> or <b>Metalaxyl 25% WP</b> at <b>30–50 g per 20 L water</b> every 7–10 days.",
                            "<b>Trunk Injection (Severe Cases):</b> Inject <b>Phosphonic acid 40% SL</b> at <b>20 ml per tree</b> into the trunk to rapidly halt systemic infection."
                        )
                    )
                ),
                cautionTitle = "Important Farmer Precautions",
                cautionBullets = listOf(
                    "<b>Never tank-mix Trichoderma with chemical fungicides</b>; allow at least a <b>7–10 day interval</b> between biological and chemical applications.",
                    "<b>Rotate fungicide modes of action</b> (e.g., alternate Fosetyl-aluminium and Metalaxyl) to prevent fungicide resistance."
                ),
                referenceButtonTitle = "Open Official Reference Document",
                referenceButtonSubtitle = "Official Source: Dept. of Agriculture (DOA)",
                referenceUrl = URL_DOA_PHYTOPHTHORA
            )

            "Anthracnose" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "🦠 Pathogen",
                pathogenValue = "<b>Colletotrichum gloeosporioides</b> fungus (Wind & rain-splash borne)",
                riskSeasonLabel = "🌦️ High-Risk Condition",
                riskSeasonValue = "<b>Hot, humid weather & intermittent rain</b>, especially during new leaf flushes, flowering, and fruit development",
                urgentHeader = "⚡ Immediate Farmer Action Required!",
                urgentBody = "<b>Prune and remove infected leaves, twigs, and rotten fruit to burn outside the orchard immediately!</b> This stops spore dispersal onto healthy young flushes and developing fruit.",
                urgentColorHex = "#EA580C",
                urgentBgHex = "#FFF7ED",
                symptomsTitle = "Key Symptoms to Identify",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "Leaves",
                        highlight = "Reddish-brown necrotic blotches with concentric rings",
                        detail = "Starts at leaf tips or margins, expanding inward with concentric target-like rings and tiny black spore dots in the center."
                    ),
                    SymptomItem(
                        partTag = "Flowers",
                        highlight = "Dark brown/black floral blight and premature drop",
                        detail = "Infected flower buds turn dark brown to black and drop off in clusters during humid spells."
                    ),
                    SymptomItem(
                        partTag = "Fruit",
                        highlight = "Sunken dark brown lesions between spines",
                        detail = "Causes sunken dark rot on fruit rinds, accelerating rapidly as the fruit matures."
                    )
                ),
                stepsHeader = "3-Step Prevention & Treatment Plan",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "Step 1: Canopy Pruning & Field Sanitation",
                        stepSubtitle = "Reduce canopy humidity and eliminate spore reservoirs",
                        bullets = listOf(
                            "<b>Canopy Aeration:</b> Prune dense inner branches after harvest so sunlight and airflow dry the foliage quickly.",
                            "<b>Sanitation:</b> Collect and incinerate diseased leaves and fallen fruit outside the plot — <b>never leave them under the tree</b>.",
                            "<b>Flush Monitoring:</b> Scout closely whenever trees push new leaf flushes."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#0D9488",
                        surfaceColorHex = "#F0FDFA",
                        borderColorHex = "#99F6E4",
                        stepTitle = "Step 2: Biological Protection",
                        stepSubtitle = "Preventive foliar colonization",
                        bullets = listOf(
                            "<b>Trichoderma Foliar Spray:</b> Apply <b>100 g per 20 L water</b> across the canopy in the evening every 7–10 days.",
                            "<b>Bacillus subtilis (BS):</b> Spray preventively during new leaf flush and pre-bloom stages according to label rates."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_chemical_spray,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "Step 3: Targeted Chemical Control",
                        stepSubtitle = "Standard dosage per 20 Liters of water (spray every 5–7 days)",
                        bullets = listOf(
                            "<b>Prochloraz 45% EC:</b> <b>20–30 ml per 20 L water</b> (highly effective eradicant action).",
                            "<b>Azoxystrobin 25% SC:</b> <b>10–20 ml per 20 L water</b> (systemic protection for young foliage).",
                            "<b>Difenoconazole 25% EC:</b> <b>10–15 ml per 20 L water</b>.",
                            "<b>Mancozeb 80% WP:</b> <b>40–50 g per 20 L water</b> (excellent contact protectant during leaf flushing)."
                        )
                    )
                ),
                cautionTitle = "Important Farmer Precautions",
                cautionBullets = listOf(
                    "<b>Alternate fungicide FRAC groups</b> every 2 sprays (e.g., rotate Prochloraz with Azoxystrobin) to prevent resistance.",
                    "<b>Add an agricultural sticker/surfactant</b> when spraying in the rainy season so spray droplets adhere to glossy durian leaves."
                ),
                referenceButtonTitle = "Open Durian Disease Management Guide",
                referenceButtonSubtitle = "Official Source: Dept. of Agricultural Extension (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )

            "Rhizoctonia" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "🦠 Pathogen",
                pathogenValue = "<b>Rhizoctonia solani</b> fungus (Web blight / Leaf fall disease)",
                riskSeasonLabel = "🌧️ High-Risk Condition",
                riskSeasonValue = "<b>Continuous monsoon rains</b>, dense unpruned canopies, and lush new leaf flushes",
                urgentHeader = "⚡ Immediate Farmer Action Required!",
                urgentBody = "<b>Cut off branches with clumped dead leaves and rake all fallen leaves to burn outside the orchard immediately!</b> Fungal sclerotia on fallen leaves survive in soil and trigger rapid re-infection.",
                urgentColorHex = "#D97706",
                urgentBgHex = "#FFFBEB",
                symptomsTitle = "Key Symptoms to Identify",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "Young Leaves",
                        highlight = "Water-soaked scalded lesions expanding rapidly",
                        detail = "Starts as pale water-soaked patches that turn brown and engulf the leaf blade within days."
                    ),
                    SymptomItem(
                        partTag = "Key Sign",
                        highlight = "White cobweb-like fungal threads binding leaves together",
                        detail = "Under high humidity, white mycelial webs glue infected leaves together in matted clusters hanging on twigs."
                    ),
                    SymptomItem(
                        partTag = "Shoots & Twigs",
                        highlight = "Severe defoliation and terminal shoot dieback",
                        detail = "Leaves dry out and shed, leaving bare twigs that progressively die back from the tips."
                    )
                ),
                stepsHeader = "3-Step Prevention & Treatment Plan",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "Step 1: Canopy Pruning & Fertilizer Control",
                        stepSubtitle = "Most critical step to stop web blight spread",
                        bullets = listOf(
                            "<b>Open the Canopy Before Monsoon:</b> Thin out overlapping inner branches so sunlight penetrates and dries the foliage.",
                            "<b>Avoid High-Nitrogen Fertilizer During Outbreaks:</b> Hold off on high-N formulas (e.g., 46-0-0) during heavy rains, as soft lush flushes are highly susceptible.",
                            "<b>Strict Orchard Sanitation:</b> Remove and burn clumped diseased leaves and ground litter outside the orchard."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#0D9488",
                        surfaceColorHex = "#F0FDFA",
                        borderColorHex = "#99F6E4",
                        stepTitle = "Step 2: Preventive Biological Control",
                        stepSubtitle = "Apply 1–2 months before rainy season onset",
                        bullets = listOf(
                            "<b>Trichoderma Application:</b> Spray canopy and broadcast compost-mixed Trichoderma under trees to destroy resting sclerotia in the soil.",
                            "<b>Bacillus subtilis (BS):</b> Spray every 7–10 days during leaf flushing in orchards with prior disease history."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_chemical_spray,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "Step 3: Targeted Chemical Control",
                        stepSubtitle = "Standard dosage per 20 Liters of water (spray every 7 days)",
                        bullets = listOf(
                            "<b>Hexaconazole 5% SC:</b> <b>20–30 ml per 20 L water</b> (systemic triazole highly effective against Rhizoctonia).",
                            "<b>Copper oxychloride 85% WP:</b> <b>30–50 g per 20 L water</b> or <b>Copper hydroxide 77% WP:</b> <b>20–30 g per 20 L water</b>.",
                            "<b>Validamycin 3% SL:</b> <b>30–40 ml per 20 L water</b> or <b>Pencycuron 25% WP:</b> <b>20–30 g per 20 L water</b>."
                        )
                    )
                ),
                cautionTitle = "Important Farmer Precautions",
                cautionBullets = listOf(
                    "<b>Do not spray copper fungicides during open bloom</b> or under intense midday sun to avoid flower drop and leaf scorch.",
                    "Ensure thorough spray coverage on <b>both upper and lower leaf surfaces and inner canopy twigs</b>."
                ),
                referenceButtonTitle = "Open Durian Disease Management Guide",
                referenceButtonSubtitle = "Official Source: Dept. of Agricultural Extension (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )

            "Phomopsis" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "🦠 Pathogen",
                pathogenValue = "<b>Phomopsis spp.</b> fungi (Phomopsis leaf spot)",
                riskSeasonLabel = "🌤️ High-Risk Condition",
                riskSeasonValue = "<b>Physiologically stressed trees / Water or nutrient deficiency</b> or insect feeding wounds",
                urgentHeader = "⚡ Immediate Farmer Action Required!",
                urgentBody = "<b>Rake and destroy fallen spotted leaves</b> and restore tree vigor with balanced irrigation and nutrition, as Phomopsis often remains latent until the tree becomes stressed.",
                urgentColorHex = "#854D0E",
                urgentBgHex = "#FEFCE8",
                symptomsTitle = "Key Symptoms to Identify",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "Leaves",
                        highlight = "Dark brown necrotic spots with a bright yellow halo",
                        detail = "Small circular dark brown lesions surrounded by a distinct chlorotic yellow ring distributed across the blade."
                    ),
                    SymptomItem(
                        partTag = "Lesion Center",
                        highlight = "Greyish-white center with tiny black dots (pycnidia)",
                        detail = "Older spots turn greyish-white in the center and develop minute black pinhead fruiting bodies (pycnidia)."
                    ),
                    SymptomItem(
                        partTag = "Tree Impact",
                        highlight = "Premature leaf yellowing and reduced vigor",
                        detail = "Heavy spotting reduces photosynthetic area and triggers early leaf drop."
                    )
                ),
                stepsHeader = "3-Step Prevention & Treatment Plan",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "Step 1: Tree Vigor & Orchard Hygiene",
                        stepSubtitle = "Strengthen host immunity and reduce spore load",
                        bullets = listOf(
                            "<b>Balanced Irrigation & Nutrition:</b> Provide consistent water and fertilizer after harvest so trees do not suffer stress.",
                            "<b>Protect Young Flushes from Insects:</b> Control thrips and leafhoppers that create entry wounds for fungal spores.",
                            "<b>Sanitation:</b> Collect and burn heavily infected fallen leaves."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#0D9488",
                        surfaceColorHex = "#F0FDFA",
                        borderColorHex = "#99F6E4",
                        stepTitle = "Step 2: Biological Protection",
                        stepSubtitle = "Suppress fungal colonization on foliage",
                        bullets = listOf(
                            "<b>Trichoderma Spray:</b> Apply <b>100 g per 20 L water</b> across the canopy every 10–14 days during humid periods.",
                            "<b>Bacillus subtilis (BS):</b> Apply preventively on expanding leaf flushes."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_chemical_spray,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "Step 3: Targeted Chemical Control",
                        stepSubtitle = "Standard dosage per 20 Liters of water (spray every 7–10 days)",
                        bullets = listOf(
                            "<b>Hexaconazole 5% SC:</b> <b>20–30 ml per 20 L water</b>.",
                            "<b>Difenoconazole 25% EC:</b> <b>10–15 ml per 20 L water</b>.",
                            "<b>Carbendazim 50% SC:</b> <b>20–30 ml per 20 L water</b>.",
                            "<b>Mancozeb 80% WP:</b> <b>40–50 g per 20 L water</b> (use in rotation or tank-mix for broad protection)."
                        )
                    )
                ),
                cautionTitle = "Important Farmer Precautions",
                cautionBullets = listOf(
                    "Yellow-halo spots can resemble micronutrient deficiencies; look for <b>tiny black dots (pycnidia) inside the grey center</b> to confirm Phomopsis.",
                    "Rotate fungicide groups after every 2 applications."
                ),
                referenceButtonTitle = "Open Durian Disease Management Guide",
                referenceButtonSubtitle = "Official Source: Dept. of Agricultural Extension (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )

            "Algal spot" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "🦠 Pathogen",
                pathogenValue = "<b>Cephaleuros virescens</b> parasitic green alga (Algal rust / Red rust)",
                riskSeasonLabel = "🌿 High-Risk Condition",
                riskSeasonValue = "<b>Dense, shaded canopies with poor airflow</b> and high humidity",
                urgentHeader = "⚡ Immediate Farmer Action Required!",
                urgentBody = "<b>Prune overcrowded inner branches immediately to let sunlight into the canopy!</b> Parasitic algae thrive in damp shade and will spread onto twigs, causing bark cracking and twig dieback.",
                urgentColorHex = "#0D9488",
                urgentBgHex = "#F0FDFA",
                symptomsTitle = "Key Symptoms to Identify",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "Early Stage",
                        highlight = "Small raised greenish-grey circular spots",
                        detail = "Slightly raised, circular spots with feathery margins appearing mostly on older shaded leaves."
                    ),
                    SymptomItem(
                        partTag = "Mature Stage",
                        highlight = "Velvety rust-orange or reddish-brown cushions",
                        detail = "When producing sporangia, spots turn rusty orange-red with a distinct velvety, cushion-like texture."
                    ),
                    SymptomItem(
                        partTag = "Twigs & Bark",
                        highlight = "Cracked bark and stunted, drying twigs",
                        detail = "Algal filaments penetrate young bark, causing rough cracks, leaf yellowing, and twig dieback."
                    )
                ),
                stepsHeader = "2-Step Prevention & Treatment Plan",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "Step 1: Canopy Sunlight & Weed Control",
                        stepSubtitle = "Create an environment unfavorable for algal growth",
                        bullets = listOf(
                            "<b>Selective Pruning:</b> Thin out shaded inner branches so sunlight reaches the trunk and interior foliage.",
                            "<b>Weed Management:</b> Keep ground vegetation trimmed to lower orchard humidity.",
                            "<b>Tree Nutrition:</b> Apply balanced fertilizer so trees replace damaged older leaves."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_chemical_spray,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "Step 2: Targeted Algaecide Spray",
                        stepSubtitle = "Standard dosage per 20 Liters of water (spray every 7–10 days)",
                        bullets = listOf(
                            "<b>Copper oxychloride 85% WP:</b> <b>30–50 g per 20 L water</b> sprayed thoroughly onto affected leaves and branches.",
                            "<b>Copper hydroxide 77% WP:</b> <b>20–30 g per 20 L water</b>.",
                            "<b>Application Tip:</b> Mix with a surfactant/sticker and spray thoroughly over <b>inner branches and undersides of leaves</b>."
                        )
                    )
                ),
                cautionTitle = "Important Farmer Precautions",
                cautionBullets = listOf(
                    "Once killed by copper spray, <b>algal spots turn dull grey/brown</b> but remain attached to the old leaf; check that new leaves remain clean.",
                    "Avoid spraying copper during open flowering."
                ),
                referenceButtonTitle = "Open Durian Disease Management Guide",
                referenceButtonSubtitle = "Official Source: Dept. of Agricultural Extension (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )

            "Healthy" -> StructuredDiseaseGuide(
                key = key,
                pathogenLabel = "✅ Leaf Status",
                pathogenValue = "<b>Healthy, vigorous durian foliage</b> — no primary disease detected",
                riskSeasonLabel = "🛡️ Routine Scouting",
                riskSeasonValue = "Inspect orchard every <b>7–14 days</b>, especially during seasonal transitions and new leaf flushes",
                urgentHeader = "🌟 Durian Foliage is Healthy & Strong",
                urgentBody = "<b>Maintain good orchard management practices</b> — balanced irrigation, proper soil pH, and regular canopy pruning to prevent future disease outbreaks.",
                urgentColorHex = "#059669",
                urgentBgHex = "#ECFDF5",
                symptomsTitle = "Characteristics of Healthy Durian Foliage",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "Blade & Cuticle",
                        highlight = "Uniform deep green color with glossy surface",
                        detail = "Smooth, turgid leaf blade free from water-soaked lesions, concentric rings, or rust-colored crusts."
                    ),
                    SymptomItem(
                        partTag = "Photosynthesis",
                        highlight = "Optimal energy production for flowering & fruiting",
                        detail = "Healthy mature leaves store carbohydrates essential for strong bloom and fruit set."
                    )
                ),
                stepsHeader = "Preventive Orchard Care Guide",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "1. Water, Soil & Nutrient Balance",
                        stepSubtitle = "Keep natural plant immunity strong",
                        bullets = listOf(
                            "<b>Irrigation & Drainage:</b> Provide consistent moisture without waterlogging; clear drainage ditches before monsoon rains.",
                            "<b>Secondary & Micronutrients:</b> Supplement organic fertilizer with <b>Calcium-Boron and Magnesium</b> for resilient leaf cell walls.",
                            "<b>Canopy Maintenance:</b> Prune water shoots and dead twigs regularly."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#0D9488",
                        surfaceColorHex = "#F0FDFA",
                        borderColorHex = "#99F6E4",
                        stepTitle = "2. Preventive Bio-Protection",
                        stepSubtitle = "Keep soil pathogens in check year-round",
                        bullets = listOf(
                            "<b>Soil Trichoderma Broadcast:</b> Apply Trichoderma mixed with compost around the root zone every <b>2–3 months</b>.",
                            "<b>Pest Scouting:</b> Check undersides of young flushes for thrips, mites, and psyllids."
                        )
                    )
                ),
                cautionTitle = null,
                cautionBullets = emptyList(),
                referenceButtonTitle = "Open Durian Care & Disease Guide",
                referenceButtonSubtitle = "Official Source: Dept. of Agricultural Extension (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )

            else -> StructuredDiseaseGuide(
                key = "Other",
                pathogenLabel = "🔎 Condition Type",
                pathogenValue = "<b>Other Abnormalities (Non-primary fungal/algal disorder)</b>",
                riskSeasonLabel = "⚠️ Common Causes",
                riskSeasonValue = "<b>Insect pests / Nutrient deficiencies / Environmental stress</b> (sunburn, wind, or salinity)",
                urgentHeader = "⚡ Immediate Field Check Recommended!",
                urgentBody = "<b>Inspect the undersides of leaves for insect pests</b> and check soil moisture, pH, and recent fertilizer applications before applying any chemical sprays.",
                urgentColorHex = "#475569",
                urgentBgHex = "#F8FAFC",
                symptomsTitle = "How to Distinguish Common Non-Disease Issues",
                symptoms = listOf(
                    SymptomItem(
                        partTag = "Insect Pests",
                        highlight = "Leaf curling, stippling, or white waxy secretions",
                        detail = "Caused by <b>Durian Psyllids</b> (white fluff), <b>Thrips</b> (curled bronze leaf edges), or <b>Red Spider Mites</b> (pale dusty upper leaf surface)."
                    ),
                    SymptomItem(
                        partTag = "Nutrient Deficit",
                        highlight = "Symmetrical yellowing patterns such as interveinal chlorosis",
                        detail = "E.g., <b>Magnesium deficiency</b> (yellowing between green veins on older leaves), <b>Zinc deficiency</b> (narrow small yellow leaves), or <b>Nitrogen deficiency</b>."
                    ),
                    SymptomItem(
                        partTag = "Abiotic Stress",
                        highlight = "Dry brown leaf tips/margins from sunburn, drought, or salt burn",
                        detail = "Marginal scorching without fungal spore dots, often appearing uniformly on sun-exposed sides."
                    )
                ),
                stepsHeader = "Targeted Management Steps",
                actionSections = listOf(
                    ActionStepSection(
                        iconRes = R.drawable.ic_cultural_care,
                        badgeColorHex = "#059669",
                        surfaceColorHex = "#F0FDF4",
                        borderColorHex = "#BBF7D0",
                        stepTitle = "1. Diagnose & Treat the Root Cause",
                        stepSubtitle = "Match treatment to the actual disorder",
                        bullets = listOf(
                            "<b>For Insect Damage:</b> Use targeted insecticides (e.g., spirotetramat, abamectin, or imidacloprid) matched to the specific pest.",
                            "<b>For Nutrient Deficiency:</b> Check soil pH (optimal 5.5–6.5) and apply chelated foliar micronutrients.",
                            "<b>For Marginal Scorch:</b> Check irrigation water salinity (EC) and ensure adequate soil moisture."
                        )
                    ),
                    ActionStepSection(
                        iconRes = R.drawable.ic_bio_control,
                        badgeColorHex = "#2563EB",
                        surfaceColorHex = "#EFF6FF",
                        borderColorHex = "#BFDBFE",
                        stepTitle = "2. Consult Local Agricultural Officers",
                        stepSubtitle = "When symptoms spread or remain unclear",
                        bullets = listOf(
                            "<b>Collect leaf samples at various symptom stages</b> in a sealed plastic bag along with photos of the whole tree.",
                            "<b>Consult your District Agricultural Extension Office</b> or regional DOA plant clinic for laboratory diagnosis."
                        )
                    )
                ),
                cautionTitle = "Important Farmer Precautions",
                cautionBullets = listOf(
                    "<b>Avoid spraying fungicides unnecessarily</b> if symptoms are caused by insects or nutrient deficiencies."
                ),
                referenceButtonTitle = "Open Official Durian Care Guide",
                referenceButtonSubtitle = "Official Source: Dept. of Agricultural Extension (DOAE)",
                referenceUrl = URL_DOAE_DURIAN_PDF
            )
        }
    }
}

