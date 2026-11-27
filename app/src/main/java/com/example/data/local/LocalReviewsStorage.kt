package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.PhotoReviewDoc
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.reviewsDataStore by preferencesDataStore(name = "nightcoffee_local_reviews")

/** Device-local review and helpful-vote storage used when Firestore is not configured. */
class LocalReviewsStorage(private val context: Context) {
    private val reviewsKey = stringPreferencesKey("reviews_v1")
    private val votesKey = stringPreferencesKey("helpful_votes_v1")

    val reviews: Flow<List<PhotoReviewDoc>> = context.reviewsDataStore.data.map { prefs ->
        decodeReviews(prefs[reviewsKey].orEmpty())
    }
    val votes: Flow<Set<String>> = context.reviewsDataStore.data.map { prefs ->
        runCatching { JSONArray(prefs[votesKey].orEmpty()).let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() } }
            .getOrDefault(emptySet())
    }

    suspend fun add(review: PhotoReviewDoc) {
        context.reviewsDataStore.edit { prefs ->
            val list = decodeReviews(prefs[reviewsKey].orEmpty()).filterNot { it.id == review.id } + review
            prefs[reviewsKey] = encodeReviews(list)
        }
    }

    suspend fun toggleVote(reviewId: String, voted: Boolean) {
        context.reviewsDataStore.edit { prefs ->
            val votes = runCatching {
                JSONArray(prefs[votesKey].orEmpty()).let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() }
            }.getOrDefault(emptySet()).toMutableSet()
            if (voted) votes.remove(reviewId) else votes.add(reviewId)
            prefs[votesKey] = JSONArray(votes.toList()).toString()
            val updated = decodeReviews(prefs[reviewsKey].orEmpty()).map { review ->
                if (review.id == reviewId) review.copy(helpfulCount = (review.helpfulCount + if (voted) -1 else 1).coerceAtLeast(0)) else review
            }
            prefs[reviewsKey] = encodeReviews(updated)
        }
    }

    suspend fun report(reviewId: String) {
        context.reviewsDataStore.edit { prefs ->
            prefs[reviewsKey] = encodeReviews(decodeReviews(prefs[reviewsKey].orEmpty()).map {
                if (it.id == reviewId) it.copy(reportCount = it.reportCount + 1) else it
            })
        }
    }

    suspend fun setVisibility(reviewId: String, visibility: String) {
        context.reviewsDataStore.edit { prefs ->
            prefs[reviewsKey] = encodeReviews(decodeReviews(prefs[reviewsKey].orEmpty()).map {
                if (it.id == reviewId) it.copy(visibility = visibility) else it
            })
        }
    }

    suspend fun delete(reviewId: String) {
        context.reviewsDataStore.edit { prefs ->
            prefs[reviewsKey] = encodeReviews(decodeReviews(prefs[reviewsKey].orEmpty()).filterNot { it.id == reviewId })
        }
    }

    private fun encodeReviews(reviews: List<PhotoReviewDoc>): String = JSONArray().apply {
        reviews.forEach { review ->
            put(JSONObject().apply {
                put("id", review.id); put("shopId", review.shopId); put("userId", review.userId)
                put("authorName", review.authorName); put("rating", review.rating); put("comment", review.comment)
                put("photoUrls", JSONArray(review.photoUrls)); put("helpfulCount", review.helpfulCount)
                put("reportCount", review.reportCount); put("visibility", review.visibility)
                put("createdAt", review.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            })
        }
    }.toString()

    private fun decodeReviews(raw: String): List<PhotoReviewDoc> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val photos = item.optJSONArray("photoUrls") ?: JSONArray()
            PhotoReviewDoc(
                id = item.optString("id"), shopId = item.optString("shopId"), userId = item.optString("userId"),
                authorName = item.optString("authorName", "Coffee Lover"), rating = item.optLong("rating", 5),
                comment = item.optString("comment"), photoUrls = (0 until photos.length()).map { photos.getString(it) },
                helpfulCount = item.optLong("helpfulCount"), reportCount = item.optLong("reportCount"),
                visibility = item.optString("visibility", "public"),
                createdAt = Timestamp(Date(item.optLong("createdAt", System.currentTimeMillis())))
            )
        }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }.getOrDefault(emptyList())
}
