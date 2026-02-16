package com.example.fgluten.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fgluten.R
import com.example.fgluten.data.recommendation.RecommendedRestaurant
import com.example.fgluten.databinding.ItemRecommendedRestaurantBinding
import kotlin.math.roundToInt

/**
 * RecyclerView adapter for displaying recommended restaurants in a horizontal scrollable list
 *
 * Each item shows the restaurant name, recommendation score, reason, rating, and distance.
 */
class RecommendedRestaurantAdapter(
    private val onRestaurantClick: (RecommendedRestaurant) -> Unit
) : RecyclerView.Adapter<RecommendedRestaurantAdapter.RecommendationViewHolder>() {

    private var recommendations: List<RecommendedRestaurant> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val binding = ItemRecommendedRestaurantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecommendationViewHolder(binding, onRestaurantClick)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(recommendations[position])
    }

    override fun getItemCount(): Int = recommendations.size

    /**
     * Update the list of recommendations
     */
    fun submitList(newRecommendations: List<RecommendedRestaurant>) {
        recommendations = newRecommendations
        notifyDataSetChanged()
    }

    // ========== VIEW HOLDER ==========

    class RecommendationViewHolder(
        private val binding: ItemRecommendedRestaurantBinding,
        private val onRestaurantClick: (RecommendedRestaurant) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recommendation: RecommendedRestaurant) {
            val restaurant = recommendation.restaurant
            val context = binding.root.context

            // Restaurant name
            binding.tvRestaurantName.text = restaurant.name

            // Score badge
            binding.tvScore.text = recommendation.score.roundToInt().toString()

            // GF indicator (check if has gluten-free options)
            if (restaurant.hasGlutenFreeOptions()) {
                binding.ivGFIndicator.visibility = android.view.View.VISIBLE
            } else {
                binding.ivGFIndicator.visibility = android.view.View.GONE
            }

            // Recommendation reason chip
            binding.chipReason.text = recommendation.reason

            // Address
            binding.tvAddress.text = restaurant.address

            // Rating
            if (restaurant.rating != null && restaurant.rating > 0) {
                binding.ratingBar.rating = restaurant.rating.toFloat()
                binding.ratingBar.visibility = android.view.View.VISIBLE
            } else {
                binding.ratingBar.visibility = android.view.View.GONE
            }

            // Distance
            val distanceText = formatDistance(restaurant.distanceMeters, context)
            binding.tvDistance.text = distanceText

            // Click handler
            binding.root.setOnClickListener {
                onRestaurantClick(recommendation)
            }
        }

        /**
         * Format distance in km or m based on device preferences
         */
        private fun formatDistance(distanceMeters: Double, context: android.content.Context): String {
            return if (distanceMeters < 1000) {
                "${distanceMeters.roundToInt()} m"
            } else {
                val km = distanceMeters / 1000
                "%.1f km".format(km)
            }
        }
    }
}
