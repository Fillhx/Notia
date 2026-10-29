package co.edu.univalle.notia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import co.edu.univalle.notia.core.navigation.NotiaNavHost
import co.edu.univalle.notia.ui.theme.NotiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotiaTheme {
                NotiaNavHost()
            }
        }
    }
}
