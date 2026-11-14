package sv.edu.unicaes.hospital.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import sv.edu.unicaes.hospital.ui.theme.Primario100
import sv.edu.unicaes.hospital.ui.theme.Primario900
import sv.edu.unicaes.hospital.ui.theme.Superficie2
import sv.edu.unicaes.hospital.ui.theme.Texto1
import sv.edu.unicaes.hospital.ui.theme.Texto2

data class Pestana(
    @param:StringRes val titulo: Int,
    @param:DrawableRes val icono: Int,
    val ruta: Any,
)

// `.and-nav`: fondo n100, píldora primary-100 (56×32), etiqueta 12/16 500; activa n900, inactiva n600.
private val estiloDestino = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

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
    val colores =
        NavigationSuiteDefaults.itemColors(
            navigationBarItemColors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = Primario900,
                    selectedTextColor = Texto1,
                    indicatorColor = Primario100,
                    unselectedIconColor = Texto2,
                    unselectedTextColor = Texto2,
                ),
            navigationRailItemColors =
                NavigationRailItemDefaults.colors(
                    selectedIconColor = Primario900,
                    selectedTextColor = Texto1,
                    indicatorColor = Primario100,
                    unselectedIconColor = Texto2,
                    unselectedTextColor = Texto2,
                ),
        )
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            pestanas.forEachIndexed { indice, pestana ->
                item(
                    icon = { Icon(painterResource(pestana.icono), contentDescription = null) },
                    label = { Text(text = stringResource(pestana.titulo), style = estiloDestino) },
                    selected = indice == seleccionada,
                    onClick = { onSeleccion(indice) },
                    colors = colores,
                )
            }
        },
        navigationSuiteColors =
            NavigationSuiteDefaults.colors(
                navigationBarContainerColor = Superficie2,
                navigationRailContainerColor = Superficie2,
            ),
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

/**
 * Marca la pestaña cuando se llega a su ruta sin tocar la barra (por ejemplo "Ver citas" desde
 * Inicio). En las pantallas de detalle no cambia: sigue marcada la pestaña desde la que se abrió.
 */
@Composable
fun SincronizarPestana(
    navController: NavHostController,
    pestanas: List<Pestana>,
    onPestana: (Int) -> Unit,
) {
    val entrada by navController.currentBackStackEntryAsState()
    LaunchedEffect(entrada) {
        val destino = entrada?.destination ?: return@LaunchedEffect
        val indice = pestanas.indexOfFirst { destino.hasRoute(it.ruta::class) }
        if (indice >= 0) onPestana(indice)
    }
}
