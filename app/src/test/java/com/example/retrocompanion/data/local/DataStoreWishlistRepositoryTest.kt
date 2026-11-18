package com.example.retrocompanion.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.retrocompanion.data.model.WishlistEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreWishlistRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val first = WishlistEntry(gameId = 1, title = "Sample Game One", consoleName = "Sample Console", imageIcon = "/Images/000001.png")
    private val second = WishlistEntry(gameId = 2, title = "Sample Game Two")

    private fun repo(file: File = File(tempFolder.root, "wishlist.preferences_pb")) =
        DataStoreWishlistRepository(PreferenceDataStoreFactory.create(produceFile = { file }))

    @Test
    fun `starts empty`() = runTest {
        assertTrue(repo().wishlistFlow.first().isEmpty())
    }

    @Test
    fun `toggle adds a game and keeps its display data`() = runTest {
        val repository = repo()
        repository.toggle(first)
        assertEquals(listOf(first), repository.wishlistFlow.first())
    }

    @Test
    fun `toggle on an existing game removes it`() = runTest {
        val repository = repo()
        repository.toggle(first)
        repository.toggle(first)
        assertTrue(repository.wishlistFlow.first().isEmpty())
    }

    @Test
    fun `most recently added game comes first`() = runTest {
        val repository = repo()
        repository.toggle(first)
        repository.toggle(second)
        assertEquals(listOf(2, 1), repository.wishlistFlow.first().map { it.gameId })
    }

    @Test
    fun `encode then decode round-trips entries`() {
        val entries = listOf(first, second)
        assertEquals(entries, decodeWishlist(encodeWishlist(entries)))
    }

    @Test
    fun `corrupt or blank stored data decodes to an empty list`() {
        assertTrue(decodeWishlist(null).isEmpty())
        assertTrue(decodeWishlist("").isEmpty())
        assertTrue(decodeWishlist("{not json").isEmpty())
    }
}
