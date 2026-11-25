package co.edu.univalle.notia.features.organizations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.univalle.notia.core.ui.NotiaPrimaryButton
import co.edu.univalle.notia.core.ui.NotiaSecondaryButton
import co.edu.univalle.notia.core.ui.NotiaTextField
import co.edu.univalle.notia.features.auth.AuthViewModel
import co.edu.univalle.notia.ui.theme.NotiaBorder
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPaper
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaSurface
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import co.edu.univalle.notia.ui.theme.SuccessBg
import co.edu.univalle.notia.ui.theme.SuccessText

@Composable
fun OrganizationsScreen(
    onOrganizationSelected: (OrganizationMembership) -> Unit,
    onLoggedOut: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    viewModel: OrganizationsViewModel = viewModel()
) {
    val uiState by viewModel.state.collectAsState()

    var joinCode by rememberSaveable { mutableStateOf("") }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createName by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.created) {
        if (uiState.created != null) {
            showCreateDialog = false
            createName = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tus organizaciones",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NotiaInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tu rol cambia según la organización.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NotiaTextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable {
                        authViewModel.logout()
                        onLoggedOut()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cerrar sesión",
                    style = MaterialTheme.typography.labelLarge,
                    color = NotiaPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error al cargar la lista (si existe)
        if (uiState.listError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.listError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reintentar",
                    style = MaterialTheme.typography.labelMedium,
                    color = NotiaPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.load() }
                )
            }
        }

        // Tarjeta blanca: Lista de organizaciones
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.loading && uiState.organizations.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = NotiaPrimary,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                    uiState.organizations.isEmpty() -> {
                        Text(
                            text = "Todavía no perteneces a ninguna organización. Únete con un código o crea una.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NotiaTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            uiState.organizations.forEachIndexed { index, org ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = NotiaBorder,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                OrganizationRow(
                                    org = org,
                                    onClick = { onOrganizationSelected(org) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Segunda tarjeta: Únete con un código
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Únete con un código",
                    style = MaterialTheme.typography.titleMedium,
                    color = NotiaInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Entrarás como Estudiante. Coordinación puede cambiar tu rol después.",
                    style = MaterialTheme.typography.labelSmall,
                    color = NotiaTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NotiaTextField(
                        value = joinCode,
                        onValueChange = {
                            joinCode = it.uppercase().replace(" ", "")
                            viewModel.clearJoinError()
                        },
                        label = "Código de invitación",
                        modifier = Modifier.weight(1f),
                        isError = uiState.joinError != null,
                        errorMessage = uiState.joinError
                    )

                    NotiaPrimaryButton(
                        text = "Unirme",
                        onClick = {
                            if (joinCode.trim().isNotEmpty()) {
                                viewModel.join(joinCode) {
                                    joinCode = ""
                                }
                            }
                        },
                        loading = uiState.joinLoading,
                        modifier = Modifier.width(110.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Sección inferior: Crear organización
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NotiaSecondaryButton(
                text = "+ Crear una organización",
                onClick = {
                    createName = ""
                    viewModel.clearCreateError()
                    showCreateDialog = true
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Al crearla quedas con el rol de Coordinación y se genera un código de invitación.",
                style = MaterialTheme.typography.labelSmall,
                color = NotiaTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Diálogo para Crear Organización
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                createName = ""
                viewModel.clearCreateError()
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = NotiaSurface,
            title = {
                Text(
                    text = "Crear una organización",
                    style = MaterialTheme.typography.titleLarge,
                    color = NotiaInk
                )
            },
            text = {
                NotiaTextField(
                    value = createName,
                    onValueChange = {
                        createName = it
                        viewModel.clearCreateError()
                    },
                    label = "Nombre de la organización",
                    isError = uiState.createError != null,
                    errorMessage = uiState.createError
                )
            },
            confirmButton = {
                NotiaPrimaryButton(
                    text = "Crear",
                    onClick = {
                        if (createName.trim().isNotEmpty()) {
                            viewModel.create(createName)
                        }
                    },
                    loading = uiState.createLoading
                )
            },
            dismissButton = {
                NotiaSecondaryButton(
                    text = "Cancelar",
                    onClick = {
                        showCreateDialog = false
                        createName = ""
                        viewModel.clearCreateError()
                    }
                )
            }
        )
    }

    // Diálogo de Éxito al Crear Organización
    if (uiState.created != null) {
        val createdOrg = uiState.created!!
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            shape = RoundedCornerShape(16.dp),
            containerColor = NotiaSurface,
            title = {
                Text(
                    text = "Organización creada",
                    style = MaterialTheme.typography.titleLarge,
                    color = NotiaInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Comparte este código para que otras personas se unan como Estudiantes:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NotiaTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = createdOrg.inviteCode.orEmpty(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NotiaPrimary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    )
                }
            },
            confirmButton = {
                NotiaPrimaryButton(
                    text = "Entrar a la organización",
                    onClick = {
                        viewModel.dismissCreated()
                        onOrganizationSelected(createdOrg)
                    }
                )
            },
            dismissButton = {
                NotiaSecondaryButton(
                    text = "Cerrar",
                    onClick = {
                        viewModel.dismissCreated()
                    }
                )
            }
        )
    }
}

@Composable
private fun OrganizationRow(
    org: OrganizationMembership,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = getAvatarColor(org.orgName),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getInitials(org.orgName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Nombre de la Organización
        Text(
            text = org.orgName,
            style = MaterialTheme.typography.titleMedium,
            color = NotiaInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Chip de Rol
        val (chipBg, chipText) = when (org.role) {
            Roles.COORDINATION -> SuccessBg to SuccessText
            Roles.STUDENT -> Color(0xFFE8ECF7) to NotiaPrimary
            Roles.TEACHER -> Color(0xFFE9E7E0) to NotiaTextSecondary
            else -> Color(0xFFE9E7E0) to NotiaTextSecondary
        }

        Box(
            modifier = Modifier
                .background(color = chipBg, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = org.role,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = chipText
            )
        }
    }
}

private fun getAvatarColor(name: String): Color {
    val colors = listOf(NotiaPrimary, Color(0xFFF4A340), NotiaInk)
    return colors[kotlin.math.abs(name.hashCode()) % colors.size]
}

private fun getInitials(name: String): String {
    val words = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.size == 1 && words[0].length >= 2 -> words[0].take(2).uppercase()
        words.size == 1 -> words[0].uppercase()
        else -> "NO"
    }
}
