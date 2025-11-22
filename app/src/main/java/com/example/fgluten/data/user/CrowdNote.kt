package com.example.fgluten.data.user

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Crowd-sourced note model for restaurant feedback and experiences.
 * 
 * This class represents user-generated content about restaurants,
 * particularly focused on gluten-free experiences and safety.
 * It replaces the current local note system with cloud synchronization.
 * 
 * @property id Unique identifier for the note
 * @property restaurantId Google Places ID of the restaurant
 * @property restaurantName Name of the restaurant for display
 * @property userId ID of the user who created the note
 * @property userDisplayName Display name of the note author
 * @property noteText The actual note content
 * @property noteType Type of note: "experience", "warning", "recommendation", "cross_contamination"
 * @property severity Severity level: "info", "warning", "danger"
 * @property helpfulVotes Count of users who found this note helpful
 * @property reportedCount Count of times this note has been reported
 * @property isVerified Whether this note is from a verified user
 * @property createdAt Timestamp when note was created
 * @property updatedAt Timestamp when note was last updated
 * @property isDeleted Whether the note has been deleted/moderated
 * @property tags List of relevant tags for filtering/searching
 */
@Parcelize
data class CrowdNote(
    val id: String,
    val restaurantId: String,
    val restaurantName: String,
    val userId: String,
    val userDisplayName: String,
    val noteText: String,
    val noteType: NoteType,
    val severity: NoteSeverity = NoteSeverity.INFO,
    val helpfulVotes: Int = 0,
    val reportedCount: Int = 0,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val tags: List<String> = emptyList()
) : Parcelable {

    /**
     * Enumeration of note types for categorization
     */
    enum class NoteType(val displayName: String) {
        EXPERIENCE("My Experience"),
        WARNING("Warning"),
        RECOMMENDATION("Recommendation"),
        CROSS_CONTAMINATION("Cross-Contamination"),
        MENU_ITEM("Specific Menu Item"),
        STAFF_KNOWLEDGE("Staff Knowledge"),
        DEDICATED_FACILITY("Dedicated Facility"),
        OTHER("Other")
    }

    /**
     * Enumeration of note severity levels
     */
    enum class NoteSeverity(val displayName: String, val priority: Int) {
        INFO("Informational", 1),
        WARNING("Caution", 2),
        DANGER("Warning", 3)
    }

    /**
     * Determines if this note should be displayed based on moderation status
     */
    fun isVisible(): Boolean {
        return !isDeleted && reportedCount < 3 // Hide if reported 3+ times
    }

    /**
     * Gets the calculated quality score for this note based on various factors
     */
    fun getQualityScore(): Double {
        var score = 0.0
        
        // Base score from helpful votes
        score += helpfulVotes * 2.0
        
        // Bonus for verification
        if (isVerified) score += 10.0
        
        // Bonus for specific note types that are more valuable
        when (noteType) {
            NoteType.CROSS_CONTAMINATION -> score += 5.0
            NoteType.MENU_ITEM -> score += 3.0
            NoteType.DEDICATED_FACILITY -> score += 4.0
            else -> { /* no bonus */ }
        }
        
        // Penalty for reports
        score -= reportedCount * 3.0
        
        // Bonus for longer, detailed notes (within reason)
        if (noteText.length > 50) score += 2.0
        if (noteText.length > 150) score += 1.0
        
        return maxOf(0.0, score) // Ensure score doesn't go negative
    }
}