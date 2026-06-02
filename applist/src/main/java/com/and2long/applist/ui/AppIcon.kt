package com.and2long.applist.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

@Composable
fun AppIcon(drawable: Drawable?, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) {
        drawable?.toBitmap(width = 96, height = 96)?.asImageBitmap()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            content = {}
        )
    }
}
