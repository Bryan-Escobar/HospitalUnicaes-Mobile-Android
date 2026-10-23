package sv.edu.unicaes.hospital.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

data class Pestana(
    @param:StringRes val titulo: Int,
    @param:DrawableRes val icono: Int,
)

/**
 * Barra inferior (o riel en pantallas grandes) con las pestañas de un rol.
 * Cada pestaña tendrá luego su propio grafo de navegación.
 */
@Composable
fun HomeConPestanas(
    pestanas: List<Pestana>,
    contenido: @Composable (pestana: Pestana, modifier: Modifier) -> Unit,
) {
    var seleccionada by rememberSaveable { mutableIntStateOf(0) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            pestanas.forEachIndexed { indice, pestana ->
                item(
                    icon = { Icon(painterResource(pestana.icono), contentDescription = null) },
                    label = { Text(stringResource(pestana.titulo)) },
                    selected = indice == seleccionada,
                    onClick = { seleccionada = indice },
                )
            }
        },
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            contenido(pestanas[seleccionada], Modifier.padding(innerPadding))
        }
    }
}
