package io.alwayszihan.zeroxpv.ui.iptv.component

import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
@Composable
fun IptvBackButton(onClick: () -> Unit) {
    IconButton(
        modifier = Modifier,
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
        )
    }
}
