package sv.edu.unicaes.hospital.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

data class Pestana(
    @param:StringRes val titulo: Int,
    @param:DrawableRes val icono: Int,
    val ruta: Any,
)

/**
 * Barra inferior (o riel en pantallas grandes) con las pestañas de un rol. La pestaña elegida
 * se guarda aparte de la pila para que siga marcada al abrir pantallas de detalle.
 */
@Composable
fun HomeConPestanas(
    pestanas: List<Pestana>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit,
    contenido: @Composable (modifier: Modifier) -> Unit,
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            pestanas.forEachIndexed { indice, pestana ->
                item(
                    icon = { Icon(painterResource(pestana.icono), contentDescription = null) },
                    label = { Text(stringResource(pestana.titulo)) },
                    selected = indice == seleccionada,
                    onClick = { onSeleccion(indice) },
                )
            }
        },
    ) {
        contenido(Modifier.fillMaxSize())
    }
}

/** Cambia de pestaña conservando el estado de cada una (patrón de Navigation Compose). */
fun NavHostController.irAPestana(ruta: Any) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
