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
    fun `mergeWishlist appends only new games and never removes or reorders`() {
        val third = WishlistEntry(gameId = 3, title = "Sample Game Three")
        val (merged, added) = mergeWishlist(
            existing = listOf(second, first),
            incoming = listOf(first.copy(title = "Renamed on site"), third, third, WishlistEntry(gameId = 0, title = "No Id"))
        )

        assertEquals(listOf(2, 1, 3), merged.map { it.gameId })
        assertEquals(1, added)
        // An existing entry is left exactly as the user had it.
        assertEquals("Sample Game One", merged.first { it.gameId == 1 }.title)
    }

    @Test
    fun `addAll merges into the stored wishlist and reports how many were new`() = runTest {
        val repository = repo()
        repository.toggle(first)

        val addedFirst = repository.addAll(listOf(first, second))
        val addedAgain = repository.addAll(listOf(first, second))

        assertEquals(1, addedFirst)
        assertEquals(0, addedAgain)
        assertEquals(listOf(1, 2), repository.wishlistFlow.first().map { it.gameId })
    }

    @Test
    fun `corrupt or blank stored data decodes to an empty list`() {
        assertTrue(decodeWishlist(null).isEmpty())
        assertTrue(decodeWishlist("").isEmpty())
        assertTrue(decodeWishlist("{not json").isEmpty())
    }
}
