package io.alwayszihan.zeroxpv.ui.iptv.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.koin.compose.koinInject
import coil3.ImageLoader

@Composable
fun IptvChannelLogo(
    logoUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (logoUrl.isNotEmpty()) {
        val context = LocalContext.current
        val imageLoader = koinInject<ImageLoader>()
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(logoUrl)
                .crossfade(true)
                .memoryCacheKey(logoUrl)
                .diskCacheKey(logoUrl)
                .build(),
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            modifier = modifier
                .heightIn(max = 28.dp)
                .widthIn(max = 48.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart
        )
    }
}
