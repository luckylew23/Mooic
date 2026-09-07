package com.rcmiku.music.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rcmiku.music.R
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.ui.icons.Artist
import com.rcmiku.music.utils.getItemShape
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.Artist
import com.rcmiku.ncmapi.model.Song
import kotlinx.coroutines.launch

/**
 * 歌手信息弹窗：列出当前歌曲的歌手，支持关注/取关，点击歌手进入歌手页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistInfoSheet(
    song: Song,
    onClick: (Artist) -> Unit,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ncmCookie by rememberPreference(ncmCookieKey, "")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var subbedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var togglingId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(openBottomSheet, song.id) {
        if (openBottomSheet) {
            bottomSheetState.show()
            subbedIds = if (ncmCookie.isNotEmpty()) {
                AccountApi.artistSublist().getOrNull()?.map { it.id }?.toSet() ?: emptySet()
            } else {
                emptySet()
            }
        } else {
            bottomSheetState.hide()
        }
    }

    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState
        ) {
            LazyColumn(
                Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(song.ar) { index, artist ->
                    val shape = getItemShape(
                        prevItem = song.ar.getOrNull(index - 1),
                        nextItem = song.ar.getOrNull(index + 1),
                        corner = 16.dp,
                        subCorner = 4.dp,
                    )
                    Card(
                        shape = shape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    onDismiss()
                                    onClick(artist)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Artist,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            val subbed = artist.id in subbedIds
                            val toggling = togglingId == artist.id
                            if (subbed) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            togglingId = artist.id
                                            AccountApi.artistSub(artist.id, sub = false)
                                                .onSuccess {
                                                    subbedIds = subbedIds - artist.id
                                                    Toast.makeText(
                                                        context,
                                                        R.string.unfollowed,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .onFailure {
                                                    Toast.makeText(
                                                        context,
                                                        R.string.operate_failed,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            togglingId = null
                                        }
                                    },
                                    enabled = !toggling,
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Text(text = stringResource(R.string.followed))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            togglingId = artist.id
                                            AccountApi.artistSub(artist.id, sub = true)
                                                .onSuccess {
                                                    subbedIds = subbedIds + artist.id
                                                    Toast.makeText(
                                                        context,
                                                        R.string.followed_success,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .onFailure {
                                                    Toast.makeText(
                                                        context,
                                                        R.string.operate_failed,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            togglingId = null
                                        }
                                    },
                                    enabled = !toggling,
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Text(text = stringResource(R.string.follow))
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
