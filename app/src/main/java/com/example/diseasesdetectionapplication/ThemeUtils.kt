package com.example.diseasesdetectionapplication

import android.content.Context
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

    fun getSeverityLabel(level: SeverityLevel): String {
        return when (level) {
            SeverityLevel.HEALTHY -> "ปกติ (Healthy)"
            SeverityLevel.MILD -> "เริ่มต้น (Mild)"
            SeverityLevel.MODERATE -> "ปานกลาง (Moderate)"
            SeverityLevel.CRITICAL -> "รุนแรง (Critical)"
        }
    }

    fun getPathogenColor(context: Context, label: String): Int {
        val colorRes = when (label) {
            "Phytophtora" -> R.color.pathogen_phytophthora
            "Anthracnose" -> R.color.pathogen_anthracnose
            "Algal spot" -> R.color.pathogen_cephaleuros
            "Phomopsis" -> R.color.pathogen_phomopsis
            else -> R.color.pathogen_other
        }
        return ContextCompat.getColor(context, colorRes)
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
