package com.rcmiku.music.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.music.utils.dataStore
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.recommend.RecommendApi
import com.rcmiku.ncmapi.model.DailySongsResponse
import com.rcmiku.ncmapi.model.RecommendPlaylistResponse
import com.rcmiku.ncmapi.utils.json
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {

    private val cachedRecommendSongsKey = stringPreferencesKey("cachedRecommendSongs")
    private val cachedRecommendPlaylistKey = stringPreferencesKey("cachedRecommendPlaylist")

    private val _recommendSongs =
        MutableStateFlow<Result<DailySongsResponse>?>(null)
    val recommendSongs: StateFlow<Result<DailySongsResponse>?> =
        _recommendSongs.asStateFlow()
    private val _recommendPlaylist =
        MutableStateFlow<Result<RecommendPlaylistResponse>?>(null)
    val recommendPlaylist: StateFlow<Result<RecommendPlaylistResponse>?> =
        _recommendPlaylist.asStateFlow()

    private fun fetchRecommendSongs() {
        viewModelScope.launch {
            val result = RecommendApi.recommendSongs()
            result.onSuccess { response ->
                _recommendSongs.value = result
                context.dataStore.edit { preferences ->
                    preferences[cachedRecommendSongsKey] = json.encodeToString(response)
                }
            }
            if (result.isFailure && _recommendSongs.value == null) {
                _recommendSongs.value = result
            }
        }
    }

    private fun fetchFavoriteSongIds() {
        viewModelScope.launch {
            AccountApi.favoriteSongIds().getOrNull()?.ids?.let {
                FavoriteSongIdsUtil.updateSongIds(context, it)
            }
        }
    }

    private fun fetchRecommendPlaylist() {
        viewModelScope.launch {
            val result = RecommendApi.recommendPlaylist()
            result.onSuccess { response ->
                _recommendPlaylist.value = result
                context.dataStore.edit { preferences ->
                    preferences[cachedRecommendPlaylistKey] = json.encodeToString(response)
                }
            }
            if (result.isFailure && _recommendPlaylist.value == null) {
                _recommendPlaylist.value = result
            }
        }
    }

    private suspend fun loadCachedData() {
        context.dataStore.data.first().let { preferences ->
            preferences[cachedRecommendSongsKey]?.let { cached ->
                runCatching { json.decodeFromString<DailySongsResponse>(cached) }
                    .onSuccess { _recommendSongs.value = Result.success(it) }
            }
            preferences[cachedRecommendPlaylistKey]?.let { cached ->
                runCatching { json.decodeFromString<RecommendPlaylistResponse>(cached) }
                    .onSuccess { _recommendPlaylist.value = Result.success(it) }
            }
        }
    }

    init {
        viewModelScope.launch {
            loadCachedData()
            fetchRecommendSongs()
            fetchRecommendPlaylist()
            fetchFavoriteSongIds()
        }
    }

    fun refresh() {
        fetchRecommendSongs()
        fetchRecommendPlaylist()
        fetchFavoriteSongIds()
    }

}
