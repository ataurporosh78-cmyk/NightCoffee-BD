package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalAuthRepository
import com.example.data.local.LocalReviewsStorage
import com.example.data.DataStoreFavoritesStorage
import com.example.model.PhotoReviewDoc
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalAuthRepositoryTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `email account login session local profile guest and sign out persist locally`() = runBlocking {
        val auth = LocalAuthRepository(context)
        val created = auth.createAccount("Coffee Fan", "fan@example.com", "password123".toCharArray())
        assertEquals("fan@example.com", created.email)
        assertEquals(created, LocalAuthRepository(context).session.first())

        auth.signOut()
        assertNull(auth.session.first())
        val signedIn = auth.signIn("FAN@example.com", "password123".toCharArray())
        assertEquals(created.id, signedIn.id)
        assertEquals("Coffee Fan", signedIn.displayName)

        auth.signOut()
        assertTrue(auth.continueWithLocalProfile().statusLabel.contains("not a verified Google sign-in"))
        assertEquals("local-profile", LocalAuthRepository(context).session.first()?.id)
        auth.signOut()
        assertEquals("GUEST", auth.continueAsGuest().kind.name)
        auth.signOut()
        assertNull(auth.session.first())
    }

    @Test
    fun `favorites persist locally across storage instances`() = runBlocking {
        val first = DataStoreFavoritesStorage(context)
        first.toggleFavorite("shop-local")
        assertTrue(DataStoreFavoritesStorage(context).favoriteIds.first().contains("shop-local"))
        DataStoreFavoritesStorage(context).toggleFavorite("shop-local")
        assertTrue(first.favoriteIds.first().isEmpty())
    }

    @Test
    fun `local reviews can be written listed voted and deleted without cloud`() = runBlocking {
        val storage = LocalReviewsStorage(context)
        val review = PhotoReviewDoc(
            id = "test-review", shopId = "shop-1", userId = "local-test", authorName = "Coffee Fan",
            rating = 5, comment = "Great late-night coffee", createdAt = Timestamp(Date())
        )
        storage.add(review)
        assertEquals("Great late-night coffee", storage.reviews.first().single().comment)
        storage.toggleVote(review.id, voted = false)
        assertTrue(review.id in storage.votes.first())
        assertEquals(1L, storage.reviews.first().single().helpfulCount)
        storage.delete(review.id)
        assertTrue(storage.reviews.first().isEmpty())
    }
}
