package com.yogesh.gallery

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ViewerScreen(vm: GalleryVM, s: Screen.Viewer) {
    val items = remember(s.ids, vm.byId) { s.ids.mapNotNull { vm.byId[it] } }
    LaunchedEffect(items.isEmpty(), vm.loaded) { if (items.isEmpty() && vm.loaded) vm.pop() }
    if (items.isEmpty()) return
    val pager = rememberPagerState(initialPage = s.index.coerceIn(0, items.lastIndex)) { items.size }
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val cur = items[pager.currentPage.coerceIn(0, items.lastIndex)]
    LaunchedEffect(pager.currentPage) { zoomed = false }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pager,
            userScrollEnabled = !zoomed,
            beyondBoundsPageCount = 1,
            key = { items.getOrNull(it)?.id ?: it },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val m = items.getOrNull(page) ?: return@HorizontalPager
            val isCur = pager.currentPage == page
            if (m.isVideo) VideoPage(m, isCur)
            else ZoomImage(m.uri, isCur, onTap = { chrome = !chrome }, onZoomed = { z -> if (isCur) zoomed = z })
        }

        val show = chrome || cur.isVideo

        AnimatedVisibility(show, Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                    .statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton({ vm.pop() }) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(cur.name, color = Color.White, maxLines = 1, fontSize = 15.sp)
                    Text(
                        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                            .format(Instant.ofEpochMilli(cur.date).atZone(ZoneId.systemDefault())),
                        color = Color(0xCCFFFFFF), fontSize = 12.sp
                    )
                }
                val fav = cur.id in vm.favorites
                IconButton({ vm.toggleFav(cur.id) }) {
                    Icon(
                        if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite",
                        tint = if (fav) Color(0xFFFF4081) else Color.White
                    )
                }
            }
        }

        AnimatedVisibility(show, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .navigationBarsPadding().padding(vertical = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                IconButton({ vm.share(listOf(cur)) }) { Icon(Icons.Rounded.Share, "Share", tint = Color.White) }
                if (cur.isVideo) IconButton({ vm.push(Screen.Trim(cur.id)) }) {
                    Icon(Icons.Rounded.ContentCut, "Trim", tint = Color.White)
                } else IconButton({ vm.push(Screen.Editor(cur.id)) }) {
                    Icon(Icons.Rounded.Edit, "Edit", tint = Color.White)
                }
                IconButton({ vm.trash(listOf(cur)) }) { Icon(Icons.Rounded.Delete, "Delete", tint = Color.White) }
                IconButton({ vm.infoTarget = cur }) { Icon(Icons.Rounded.Info, "Details", tint = Color.White) }
                Box {
                    IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "More", tint = Color.White) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { vm.renameTarget = cur; menu = false })
                        DropdownMenuItem(
                            text = { Text("Copy to album") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                            onClick = { vm.albumAction = AlbumAction(listOf(cur), false); menu = false })
                        DropdownMenuItem(
                            text = { Text("Move to album") }, leadingIcon = { Icon(Icons.Rounded.DriveFileMove, null) },
                            onClick = { vm.albumAction = AlbumAction(listOf(cur), true); menu = false })
                        DropdownMenuItem(
                            text = { Text("Hide in private vault") }, leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                            onClick = { menu = false; vm.authenticate("Use private vault") { vm.hide(listOf(cur)) } })
                        if (!cur.isVideo) {
                            DropdownMenuItem(
                                text = { Text("Rotate 90°") }, leadingIcon = { Icon(Icons.Rounded.RotateRight, null) },
                                onClick = { vm.rotate(cur); menu = false })
                            DropdownMenuItem(
                                text = { Text("Set as wallpaper") }, leadingIcon = { Icon(Icons.Rounded.Wallpaper, null) },
                                onClick = { vm.setWallpaper(cur); menu = false })
                            DropdownMenuItem(
                                text = { Text("Scan text / QR") }, leadingIcon = { Icon(Icons.Rounded.QrCodeScanner, null) },
                                onClick = { vm.scanText(cur); menu = false })
                        }
                        DropdownMenuItem(
                            text = { Text("Slideshow") }, leadingIcon = { Icon(Icons.Rounded.Slideshow, null) },
                            onClick = {
                                menu = false
                                val photos = items.filter { !it.isVideo }
                                vm.startSlideshow(photos.map { it.id }, photos.indexOfFirst { it.id == cur.id }.coerceAtLeast(0))
                            })
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomImage(uri: Uri, isCurrent: Boolean, onTap: () -> Unit, onZoomed: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(isCurrent) { if (!isCurrent) { scale = 1f; offset = Offset.Zero } }
    val req = remember(uri) {
        ImageRequest.Builder(ctx).data(uri).size(Size(2048, 2048))
            .decoderFactory(ImageDecoderDecoder.Factory()).build()
    }
    Box(
        Modifier.fillMaxSize().onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.01f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                        onZoomed(scale > 1.01f)
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val ev = awaitPointerEvent(PointerEventPass.Main)
                        val multi = ev.changes.size > 1
                        if (multi || scale > 1.01f) {
                            val ns = (scale * ev.calculateZoom()).coerceIn(1f, 6f)
                            val maxX = box.width * (ns - 1f) / 2f
                            val maxY = box.height * (ns - 1f) / 2f
                            var no = offset + ev.calculatePan()
                            no = Offset(no.x.coerceIn(-maxX, maxX), no.y.coerceIn(-maxY, maxY))
                            if (ns <= 1.01f) no = Offset.Zero
                            scale = ns; offset = no
                            onZoomed(ns > 1.01f)
                            ev.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (ev.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                translationX = offset.x; translationY = offset.y
            }
    ) {
        AsyncImage(req, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

@Composable
fun VideoPage(m: Media, isCurrent: Boolean) {
    if (!isCurrent) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncImage(m.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(64.dp), tint = Color(0xCCFFFFFF))
        }
    } else VideoPlayer(m.uri, autoPlay = true, withVolume = true, modifier = Modifier.fillMaxSize())
}

/** ExoPlayer with play/pause, seek bar and a volume slider. */
@Composable
fun VideoPlayer(uri: Uri, autoPlay: Boolean, withVolume: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val exo = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(ExoItem.fromUri(uri)); prepare(); playWhenReady = autoPlay
        }
    }
    DisposableEffect(exo) { onDispose { exo.release() } }
    var volume by remember { mutableFloatStateOf(1f) }
    Box(modifier) {
        AndroidView(
            factory = { c -> PlayerView(c).apply { this.player = exo; useController = true } },
            modifier = Modifier.fillMaxSize().padding(top = 64.dp, bottom = 60.dp)
        )
        if (withVolume) Row(
            Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 12.dp)
                .clip(RoundedCornerShape(50)).background(Color(0x88000000)).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.VolumeUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Slider(volume, { volume = it; exo.volume = it }, Modifier.width(120.dp))
        }
    }
}
