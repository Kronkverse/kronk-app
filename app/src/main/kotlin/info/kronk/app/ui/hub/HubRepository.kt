package info.kronk.app.ui.hub

import info.kronk.core.network.api.KornersApi
import info.kronk.core.network.dto.KornerDto
import info.kronk.feature.auth.data.AuthStorage

// Fetches the manifest registry for the Hub grid. Bearer is passed
// through when the user is signed in so per-user fields (tuned_in,
// unread_count) come back populated; without a token the endpoint
// still returns the manifest catalogue.

class HubRepository(
    private val korners: KornersApi,
    private val storage: AuthStorage,
) {
    suspend fun listKorners(): List<KornerDto> {
        val token = storage.currentToken()
        return korners.index(bearer = token?.let { "Bearer $it" })
    }
}
