package co.edu.univalle.notia.features.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.univalle.notia.features.organizations.OrganizationMembership
import co.edu.univalle.notia.features.organizations.OrganizationRepository
import co.edu.univalle.notia.features.organizations.Roles

/** Carga de organizaciones de producción. Ajustar si OrganizationRepository necesita argumentos o es un object. */
internal suspend fun loadMyOrganizations(): Result<List<OrganizationMembership>> =
    OrganizationRepository().myOrganizations()

/**
 * Organización activa + selector. Solo se puede elegir una organización donde el rol sea Estudiante;
 * las demás se muestran con su rol pero deshabilitadas.
 */
@Composable
fun OrganizationSwitcher(
    active: OrganizationMembership?,
    state: StudentLoadState<List<OrganizationMembership>>,
    onSelect: (OrganizationMembership) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Organización activa",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = state is StudentLoadState.Success && state.data.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(active?.orgName ?: "Selecciona una organización", modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                (state as? StudentLoadState.Success)?.data?.forEach { organization ->
                    val selectable = organization.role == Roles.STUDENT
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(organization.orgName)
                                Text(
                                    if (selectable) "Rol: ${organization.role}"
                                    else "Rol: ${organization.role} · no disponible aquí",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        leadingIcon = if (organization.orgId == active?.orgId) {
                            { Icon(Icons.Filled.Done, contentDescription = "Organización activa") }
                        } else {
                            null
                        },
                        enabled = selectable,
                        onClick = {
                            expanded = false
                            onSelect(organization)
                        },
                    )
                }
            }
        }

        when (state) {
            is StudentLoadState.Loading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Cargando organizaciones…", style = MaterialTheme.typography.bodySmall)
            }

            is StudentLoadState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MessageRow(Icons.Filled.Warning, state.message)
                OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reintentar")
                }
            }

            is StudentLoadState.Success -> if (state.data.isEmpty()) {
                MessageRow(Icons.Filled.Info, "No perteneces a ninguna organización.")
            }
        }
    }
}
