package co.edu.univalle.notia.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = NotiaPrimary,
    onPrimary = Color.White,
    background = NotiaPaper,
    onBackground = NotiaInk,
    surface = NotiaSurface,
    onSurface = NotiaInk,
    onSurfaceVariant = NotiaTextSecondary,
    outline = NotiaBorder,
    secondary = NotiaAccent,
    error = Color(0xFFC62828)
)

@Composable
fun NotiaTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}
