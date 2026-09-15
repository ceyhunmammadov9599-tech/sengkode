package com.hjinlabs.sengkode.feature.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Phase 0 scaffold for the QR studio. Phase 1 replaces the body with the
 * live content editor + QR preview (the studio experience); the module
 * boundary, navigation entry and theme integration are settled here.
 */
@Composable
fun GeneratorScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "QR Studio",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "The creation experience arrives in Phase 1.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
