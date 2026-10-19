package co.edu.univalle.notia.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Pantalla temporal: cada integrante la reemplaza por su pantalla real. */
@Composable
fun PlaceholderScreen(
    title: String,
    requirements: String,
    actions: List<Pair<String, () -> Unit>> = emptyList(),
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("Pendiente: $requirements", style = MaterialTheme.typography.bodyMedium)
        actions.forEach { (label, onClick) -> Button(onClick = onClick) { Text(label) } }
    }
}
