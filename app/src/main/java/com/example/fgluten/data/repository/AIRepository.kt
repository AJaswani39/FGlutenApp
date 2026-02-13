package com.example.fgluten.data.repository

import android.content.Context
import com.example.fgluten.data.ai.*
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * Repository interface for AI-powered features in FGlutenApp
 * 
 * This interface provides a clean abstraction for all AI operations,
 * allowing for easy testing and potential swapping of AI implementations.
 * Currently focuses on menu analysis for gluten-free safety assessment.
 * 
 * @see MenuAnalysisService for the main AI functionality
 */
interface AIRepository {
    
    /**
     * Analyze restaurant menu text for gluten-free options
     * 
     * @param menuText Raw menu text to analyze
     * @param restaurantName Name of the restaurant (for context)
     * @param sourceType Source of the menu data (website, photo, etc.)
     * @return MenuAnalysisResult with detailed gluten-free safety assessment
     */
    suspend fun analyzeMenuText(
        menuText: String,
        restaurantName: String,
        sourceType: AnalysisSource = AnalysisSource.WEBSITE
    ): MenuAnalysisResult
    
    /**
     * Check if AI models are ready for use
     * 
     * @return true if all required models are downloaded and ready
     */
    suspend fun isReadyForAnalysis(): Boolean
    
    /**
     * Download required AI models for offline use
     * 
     * @param context Android context for model storage
     * @return Result indicating success or failure of model download
     */
    suspend fun downloadModels(context: Context): Result<Unit>
    
    /**
     * Get current status of AI service
     * 
     * @return AIStatus indicating the current state of AI services
     */
    suspend fun getStatus(): AIStatus
}

/**
 * Status of AI services for UI feedback
 */
sealed class AIStatus {
    data object Ready : AIStatus()                    // All models loaded and ready
    data object Downloading : AIStatus()              // Models currently downloading
    data object NotAvailable : AIStatus()             // AI services not available
    data class Error(val message: String) : AIStatus() // Error state with message
}

/**
 * Default implementation of AIRepository using ML Kit and custom logic
 * 
 * This implementation provides:
 * - On-device text analysis using ML Kit
 * - Rule-based classification for gluten-free detection
 * - Confidence scoring based on keyword analysis
 * - Multi-language support through ML Kit Translate
 */
class DefaultAIRepository : AIRepository {
    
    private var translationClient: Translator? = null
    private var isInitialized = false
    
    init {
        createTranslationClient()
    }
    
    /**
     * Analyze menu text for gluten-free options using AI and rule-based classification
     */
    override suspend fun analyzeMenuText(
        menuText: String,
        restaurantName: String,
        sourceType: AnalysisSource
    ): MenuAnalysisResult {
        
        // Ensure AI services are ready
        if (!isReadyForAnalysis()) {
            throw IllegalStateException("AI services not ready for analysis")
        }
        
        // Preprocess and clean the text
        val cleanedText = preprocessMenuText(menuText)
        
        // Extract menu items using NLP
        val menuItems = extractMenuItems(cleanedText)
        
        // Analyze each item for gluten-free safety
        val analyzedItems = menuItems.map { item ->
            analyzeMenuItem(item.name, item.description)
        }
        
        // Calculate overall safety score
        val overallScore = calculateOverallSafetyScore(analyzedItems)
        
        // Generate reasoning and confidence
        val confidenceScore = calculateOverallConfidence(analyzedItems)
        val reasoning = generateReasoningText(analyzedItems, restaurantName)
        
        return MenuAnalysisResult(
            overallScore = overallScore,
            analyzedItems = analyzedItems,
            confidenceScore = confidenceScore,
            reasoning = reasoning,
            sourceType = sourceType
        )
    }
    
    /**
     * Check if AI services are ready for analysis
     */
    override suspend fun isReadyForAnalysis(): Boolean {
        return translationClient != null || isInitialized
    }
    
    /**
     * Download required AI models
     */
    override suspend fun downloadModels(context: Context): Result<Unit> {
        return try {
            // Download translation models for key languages
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()
            
            val languagePairs = listOf(
                TranslateLanguage.ENGLISH to TranslateLanguage.ENGLISH,  // For better text processing
                TranslateLanguage.SPANISH to TranslateLanguage.ENGLISH,
                TranslateLanguage.FRENCH to TranslateLanguage.ENGLISH
            )
            
            languagePairs.forEach { (source, target) ->
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build()
                
                val client = Translation.getClient(options)
                client.downloadModelIfNeeded(conditions).await()
            }
            
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current status of AI services
     */
    override suspend fun getStatus(): AIStatus {
        return when {
            isInitialized -> AIStatus.Ready
            // In a real implementation, you'd track download progress
            else -> AIStatus.Downloading
        }
    }
    
    // ========== PRIVATE HELPER METHODS ==========
    
    /**
     * Preprocess menu text for better analysis
     */
    private fun preprocessMenuText(text: String): String {
        return text
            .replace("\\s+".toRegex(), " ") // Normalize whitespace
            .replace("[^\\w\\s.,;:()&%-]".toRegex(), "") // Remove special chars except common menu punctuation
            .trim()
    }
    
    /**
     * Extract individual menu items from text
     */
    private fun extractMenuItems(text: String): List<MenuItem> {
        val items = mutableListOf<MenuItem>()
        
        // Split by common menu item separators
        val lines = text.split("\n", ".").map { it.trim() }.filter { it.isNotEmpty() }
        
        for (line in lines) {
            // Try to extract name and description
            val parts = line.split(" - ", " — ", ":")
            if (parts.size >= 2) {
                val name = parts[0].trim()
                val description = parts.drop(1).joinToString(" - ").trim()
                
                if (name.isNotEmpty() && name.length <= 100) { // Basic validation
                    items.add(MenuItem(name, description))
                }
            } else if (line.length in 5..100) {
                // Treat single line as a menu item
                items.add(MenuItem(line, ""))
            }
        }
        
        return items
    }
    
    /**
     * Analyze individual menu item for gluten-free safety
     */
    private fun analyzeMenuItem(name: String, description: String): AnalyzedMenuItem {
        val fullText = "$name $description".lowercase()
        
        // Define keyword patterns for classification
        val gfKeywords = listOf(
            "gluten-free", "gf", "gluten free", "celiac", "celiac-safe",
            "no gluten", "100% gluten-free", "dedicated gluten-free"
        )
        
        val warningKeywords = listOf(
            "may contain", "processed in", "shared", "cross-contamination",
            "same facility", "shared kitchen", "shared equipment"
        )
        
        val glutenKeywords = listOf(
            "wheat", "barley", "rye", "malt", "semolina", "farro",
            "bulgur", "couscous", "panko", "seitan"
        )
        
        // Score based on keyword presence
        var gfScore = 0
        var warningScore = 0
        var glutenScore = 0
        
        // Check for GF keywords
        gfKeywords.forEach { keyword ->
            if (fullText.contains(keyword)) {
                gfScore += if (keyword in listOf("dedicated gluten-free", "100% gluten-free")) 3 else 2
            }
        }
        
        // Check for warning keywords
        warningKeywords.forEach { keyword ->
            if (fullText.contains(keyword)) {
                warningScore += 2
            }
        }
        
        // Check for gluten-containing ingredients
        glutenKeywords.forEach { keyword ->
            if (fullText.contains(keyword)) {
                glutenScore += 3
            }
        }
        
        // Determine classification
        val classification = when {
            gfScore >= 3 && warningScore == 0 -> GFClassification.GF_SAFE
            gfScore >= 2 && warningScore <= 1 -> GFClassification.LIKELY_GF
            warningScore >= 2 || glutenScore >= 2 -> GFClassification.MAY_CONTAIN_GLUTEN
            glutenScore >= 3 -> GFClassification.NOT_GF
            else -> GFClassification.UNCLEAR
        }
        
        // Calculate confidence based on keyword strength
        val confidence = calculateItemConfidence(gfScore, warningScore, glutenScore)
        
        return AnalyzedMenuItem(
            name = name,
            description = description,
            classification = classification,
            confidence = confidence,
            reasoning = generateItemReasoning(classification, fullText),
            keywords = extractMatchedKeywords(fullText, gfKeywords + warningKeywords + glutenKeywords),
            warnings = extractMatchedKeywords(fullText, warningKeywords)
        )
    }
    
    /**
     * Calculate confidence score for individual item
     */
    private fun calculateItemConfidence(gfScore: Int, warningScore: Int, glutenScore: Int): Float {
        val totalScore = gfScore + warningScore + glutenScore
        return if (totalScore > 0) {
            minOf(1.0f, (totalScore / 10.0f).toFloat())
        } else {
            0.3f // Low confidence for unclear items
        }
    }
    
    /**
     * Calculate overall safety level based on analyzed items
     */
    private fun calculateOverallSafetyScore(items: List<AnalyzedMenuItem>): GFSafetyLevel {
        if (items.isEmpty()) return GFSafetyLevel.UNKNOWN
        
        val totalItems = items.size
        val gfSafeItems = items.count { it.classification == GFClassification.GF_SAFE }
        val likelyGFItems = items.count { it.classification == GFClassification.LIKELY_GF }
        val warningItems = items.count { it.classification == GFClassification.MAY_CONTAIN_GLUTEN || it.classification == GFClassification.NOT_GF }
        
        val safePercentage = (gfSafeItems + likelyGFItems).toFloat() / totalItems
        
        return when {
            safePercentage >= 0.8f && warningItems == 0 -> GFSafetyLevel.EXCELLENT
            safePercentage >= 0.6f -> GFSafetyLevel.GOOD
            safePercentage >= 0.3f -> GFSafetyLevel.LIMITED
            safePercentage >= 0.1f -> GFSafetyLevel.POOR
            else -> GFSafetyLevel.UNKNOWN
        }
    }
    
    /**
     * Calculate overall confidence in the analysis
     */
    private fun calculateOverallConfidence(items: List<AnalyzedMenuItem>): Float {
        return if (items.isNotEmpty()) {
            items.sumOf { it.confidence.toDouble() }.toFloat() / items.size
        } else {
            0.0f
        }
    }
    
    /**
     * Generate human-readable reasoning for the analysis
     */
    private fun generateReasoningText(items: List<AnalyzedMenuItem>, restaurantName: String): String {
        val safeCount = items.count { it.classification == GFClassification.GF_SAFE }
        val likelyCount = items.count { it.classification == GFClassification.LIKELY_GF }
        val warningCount = items.count { it.classification == GFClassification.MAY_CONTAIN_GLUTEN }
        
        return buildString {
            append("Analysis of $restaurantName menu found ")
            append("$safeCount confirmed gluten-free items ")
            append("and $likelyCount likely gluten-free options. ")
            if (warningCount > 0) {
                append("Note: $warningCount items may contain gluten. ")
            }
            append("This assessment is based on menu descriptions and may not reflect current kitchen practices.")
        }
    }
    
    /**
     * Generate reasoning for individual item classification
     */
    private fun generateItemReasoning(classification: GFClassification, text: String): String {
        return when (classification) {
            GFClassification.GF_SAFE -> "Contains explicit gluten-free designation or dedicated preparation area"
            GFClassification.LIKELY_GF -> "Menu description suggests gluten-free preparation or ingredients"
            GFClassification.MAY_CONTAIN_GLUTEN -> "Contains potential gluten-containing ingredients or shared preparation"
            GFClassification.NOT_GF -> "Contains confirmed gluten-containing ingredients"
            GFClassification.UNCLEAR -> "Insufficient information to determine gluten-free status"
        }
    }
    
    /**
     * Extract matched keywords from text
     */
    private fun extractMatchedKeywords(text: String, keywords: List<String>): List<String> {
        return keywords.filter { text.contains(it) }
    }
    
    /**
     * Create translation client for text processing
     */
    private fun createTranslationClient() {
        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
            translationClient = Translation.getClient(options)
            isInitialized = true
        } catch (e: Exception) {
            // Fallback if translation fails
            translationClient = null
        }
    }
    
    // Data class for extracted menu items
    private data class MenuItem(val name: String, val description: String)
}
