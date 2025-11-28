package com.example.fgluten.data.ai

/**
 * Data model for AI-powered menu analysis results
 * 
 * This class represents the outcome of analyzing restaurant menus for gluten-free options.
 * It provides detailed classification of menu items with confidence scores and reasoning.
 * 
 * @property overallScore Overall confidence that this restaurant offers good GF options
 * @property analyzedItems List of analyzed menu items with their classifications
 * @property confidenceScore Overall confidence in the analysis (0.0 to 1.0)
 * @property reasoning Human-readable explanation of the analysis
 * @property lastUpdated Timestamp when this analysis was performed
 * @property sourceType Source of the menu data (website, photo, manual)
 */
data class MenuAnalysisResult(
    val overallScore: GFSafetyLevel,
    val analyzedItems: List<AnalyzedMenuItem>,
    val confidenceScore: Float,
    val reasoning: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val sourceType: AnalysisSource
) {
    
    /**
     * Get summary statistics for quick UI display
     */
    fun getSummary(): AnalysisSummary {
        val totalItems = analyzedItems.size
        val gfSafeItems = analyzedItems.count { it.classification == GFClassification.GF_SAFE }
        val likelyGFItems = analyzedItems.count { it.classification == GFClassification.LIKELY_GF }
        val warningItems = analyzedItems.count { it.classification == GFClassification.MAY_CONTAIN_GLUTEN }
        
        return AnalysisSummary(
            totalItemsAnalyzed = totalItems,
            gfSafeCount = gfSafeItems,
            likelyGFCount = likelyGFItems,
            warningCount = warningItems,
            confidence = confidenceScore
        )
    }
}

/**
 * Represents a single menu item that was analyzed by AI
 * 
 * @property name Name of the menu item
 * @property description Original description from menu
 * @property classification AI classification of gluten-free status
 * @property confidence Confidence score for this specific item (0.0 to 1.0)
 * @property reasoning Explanation for the classification
 * @property keywords Found keywords that influenced the decision
 * @property warnings Potential gluten-containing ingredients detected
 */
data class AnalyzedMenuItem(
    val name: String,
    val description: String,
    val classification: GFClassification,
    val confidence: Float,
    val reasoning: String,
    val keywords: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Summary statistics for quick display in UI
 */
data class AnalysisSummary(
    val totalItemsAnalyzed: Int,
    val gfSafeCount: Int,
    val likelyGFCount: Int,
    val warningCount: Int,
    val confidence: Float
) {
    /**
     * Calculate percentage of items that are likely safe
     */
    fun getSafetyPercentage(): Float {
        return if (totalItemsAnalyzed > 0) {
            ((gfSafeCount + likelyGFCount).toFloat() / totalItemsAnalyzed) * 100f
        } else {
            0f
        }
    }
}

/**
 * Classification levels for gluten-free safety
 */
enum class GFSafetyLevel(val displayName: String, val description: String) {
    EXCELLENT("Excellent GF Options", "Restaurant has confirmed gluten-free options with high confidence"),
    GOOD("Good GF Options", "Restaurant appears to have reliable gluten-free choices"),
    LIMITED("Limited GF Options", "Restaurant has some gluten-free options but may be limited"),
    POOR("Poor GF Options", "Very limited or uncertain gluten-free options"),
    UNKNOWN("Unknown", "Unable to assess gluten-free options")
}

/**
 * Classification for individual menu items
 */
enum class GFClassification(val displayName: String, val displayIcon: String) {
    GF_SAFE("GF Safe", "✅"),
    LIKELY_GF("Likely GF", "🟡"),
    MAY_CONTAIN_GLUTEN("May Contain Gluten", "⚠️"),
    NOT_GF("Not Gluten-Free", "❌"),
    UNCLEAR("Unclear", "❓")
}

/**
 * Source of menu analysis data
 */
enum class AnalysisSource(val displayName: String) {
    WEBSITE("Restaurant Website"),
    PHOTO("Menu Photo"),
    MANUAL("Manual Entry"),
    CACHED("Cached Analysis")
}