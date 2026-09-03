package com.rcmiku.music.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rcmiku.music.utils.dataStore
import com.rcmiku.ncmapi.api.playlist.PlaylistApi
import com.rcmiku.ncmapi.model.TopListResponse
import com.rcmiku.ncmapi.utils.json
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreScreenViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val cachedTopListKey = stringPreferencesKey("cachedTopList")

    private val _topList =
        MutableStateFlow<Result<TopListResponse>?>(null)
    val topList: StateFlow<Result<TopListResponse>?> =
        _topList.asStateFlow()

    private fun fetchTopList() {
        viewModelScope.launch {
            val result = PlaylistApi.topList()
            result.onSuccess { response ->
                _topList.value = result
                context.dataStore.edit { it[cachedTopListKey] = json.encodeToString(response) }
            }
            if (result.isFailure && _topList.value == null) _topList.value = result
        }
    }

    init {
        viewModelScope.launch {
            context.dataStore.data.first()[cachedTopListKey]?.let { cached ->
                runCatching { json.decodeFromString<TopListResponse>(cached) }
                    .onSuccess { _topList.value = Result.success(it) }
            }
            fetchTopList()
        }
    }
}
