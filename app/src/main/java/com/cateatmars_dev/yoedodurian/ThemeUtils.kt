package com.cateatmars_dev.yoedodurian

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat

enum class SeverityLevel(val level: Int) {
    HEALTHY(0),
    MILD(1),
    MODERATE(2),
    CRITICAL(3)
}

object ThemeUtils {

    fun getSeverityColor(context: Context, level: SeverityLevel): Int {
        val colorRes = when (level) {
            SeverityLevel.HEALTHY -> R.color.severity_healthy
            SeverityLevel.MILD -> R.color.severity_mild
            SeverityLevel.MODERATE -> R.color.severity_moderate
            SeverityLevel.CRITICAL -> R.color.severity_critical
        }
        return ContextCompat.getColor(context, colorRes)
    }

    fun getSeverityTint(context: Context, level: SeverityLevel): Int {
        val colorRes = when (level) {
            SeverityLevel.HEALTHY -> R.color.severity_healthy_tint
            SeverityLevel.MILD -> R.color.severity_mild_tint
            SeverityLevel.MODERATE -> R.color.severity_moderate_tint
            SeverityLevel.CRITICAL -> R.color.severity_critical_tint
        }
        return ContextCompat.getColor(context, colorRes)
    }

    fun getSeverityLabel(context: Context, level: SeverityLevel): String {
        val colorRes = when (level) {
            SeverityLevel.HEALTHY -> R.string.severity_healthy
            SeverityLevel.MILD -> R.string.severity_mild
            SeverityLevel.MODERATE -> R.string.severity_moderate
            SeverityLevel.CRITICAL -> R.string.severity_critical
        }
        return context.getString(colorRes)
    }

    fun getPathogenColor(context: Context, label: String): Int {
        val colorRes = when (label) {
            "Phytophtora" -> R.color.pathogen_phytophthora
            "Anthracnose" -> R.color.pathogen_anthracnose
            "Algal spot" -> R.color.pathogen_cephaleuros
            "Phomopsis" -> R.color.pathogen_phomopsis
            "Rhizoctonia" -> R.color.severity_moderate
            "Healthy" -> R.color.colorPrimary
            else -> R.color.pathogen_other
        }
        return ContextCompat.getColor(context, colorRes)
    }

    /**
     * Localized farmer-friendly display label for HUD bounding boxes, summaries, and charts.
     */
    fun getDisplayLabel(context: Context, rawLabel: String): String {
        val resId = when (rawLabel) {
            "Phytophtora" -> R.string.label_short_phytophthora
            "Anthracnose" -> R.string.label_short_anthracnose
            "Rhizoctonia" -> R.string.label_short_rhizoctonia
            "Phomopsis" -> R.string.label_short_phomopsis
            "Algal spot" -> R.string.label_short_algal_spot
            "Healthy" -> R.string.label_short_healthy
            "Other" -> R.string.label_short_other
            else -> 0
        }
        return if (resId != 0) context.getString(resId) else rawLabel
    }

    /**
     * High-visibility Hi-Tech HUD bounding box colors for outdoor sunlight readability.
     */
    fun getHudBoxColor(rawLabel: String): Int {
        return when (rawLabel) {
            "Healthy" -> Color.parseColor("#00F59B")      // Neon Mint (Healthy)
            "Phytophtora" -> Color.parseColor("#FF4D4F")  // Critical Red
            "Anthracnose" -> Color.parseColor("#FF6B6B")  // Coral Red
            "Rhizoctonia" -> Color.parseColor("#FFA940")  // Warning Amber
            "Phomopsis" -> Color.parseColor("#FFC53D")    // Golden Warning
            "Algal spot" -> Color.parseColor("#2DD4BF")   // Tech Teal
            else -> Color.parseColor("#CBD5E1")           // Neutral Slate
        }
    }

    /**
     * Cohesive Agri-Tech chart color for PieChart slices.
     */
    fun getChartColor(rawLabel: String): Int {
        return when (rawLabel) {
            "Healthy" -> Color.parseColor("#10B981")      // Emerald Green
            "Phytophtora" -> Color.parseColor("#EF4444")  // Critical Red
            "Anthracnose" -> Color.parseColor("#F97316")  // Deep Orange
            "Rhizoctonia" -> Color.parseColor("#F59E0B")  // Amber
            "Phomopsis" -> Color.parseColor("#A16207")    // Earth Bronze
            "Algal spot" -> Color.parseColor("#0D9488")   // Teal
            else -> Color.parseColor("#64748B")           // Cool Slate
        }
    }

    /**
     * Estimates the severity level based on the model's bounding box area relative to image/leaf size.
     * Since we might not have the full leaf bounds, we approximate using confidence or relative bbox area.
     */
    fun estimateSeverity(coveragePercent: Float): SeverityLevel {
        return when {
            coveragePercent == 0f -> SeverityLevel.HEALTHY
            coveragePercent < 15f -> SeverityLevel.MILD
            coveragePercent <= 40f -> SeverityLevel.MODERATE
            else -> SeverityLevel.CRITICAL
        }
    }
}
