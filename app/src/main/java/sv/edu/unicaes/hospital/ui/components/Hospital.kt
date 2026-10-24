package sv.edu.unicaes.hospital.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.ui.theme.AmbarBorde
import sv.edu.unicaes.hospital.ui.theme.AmbarFondo
import sv.edu.unicaes.hospital.ui.theme.AmbarTexto
import sv.edu.unicaes.hospital.ui.theme.BordeCampo
import sv.edu.unicaes.hospital.ui.theme.BordeSuave
import sv.edu.unicaes.hospital.ui.theme.Error
import sv.edu.unicaes.hospital.ui.theme.ErrorBorde
import sv.edu.unicaes.hospital.ui.theme.ErrorFondo
import sv.edu.unicaes.hospital.ui.theme.ErrorTextoBanner
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.ParColor
import sv.edu.unicaes.hospital.ui.theme.Primario100
import sv.edu.unicaes.hospital.ui.theme.Primario200
import sv.edu.unicaes.hospital.ui.theme.Primario50
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Primario800
import sv.edu.unicaes.hospital.ui.theme.Primario900
import sv.edu.unicaes.hospital.ui.theme.Superficie
import sv.edu.unicaes.hospital.ui.theme.Texto2

// Componentes del sistema de diseño para Android (handoff §5.2 y §6).

/** TopAppBar small: navegación ← (o ✕ en formularios), título y chip "En vivo". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarHospital(
    titulo: String,
    modifier: Modifier = Modifier,
    onVolver: (() -> Unit)? = null,
    cerrar: Boolean = false,
    enVivo: Boolean = false,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = {
            if (onVolver != null) {
                IconoAccion(
                    icono = if (cerrar) R.drawable.ic_close else R.drawable.ic_arrow_back,
                    descripcion = stringResource(if (cerrar) R.string.accion_cerrar else R.string.accion_volver),
                    onClick = onVolver,
                )
            }
        },
        actions = {
            if (enVivo) ChipEnVivo(modifier = Modifier.padding(horizontal = Espacio.xxs))
            acciones()
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun IconoAccion(
    @DrawableRes icono: Int,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painter = painterResource(icono), contentDescription = descripcion)
    }
}

/** Tarjeta: blanco, borde 1 y radio 16. Las prioritarias usan borde de 2,5 del color de estado. */
@Composable
fun TarjetaHospital(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colorBorde: Color = BordeSuave,
    anchoBorde: Dp = 1.dp,
    colorFondo: Color = Superficie,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = MaterialTheme.shapes.large
    val borde = BorderStroke(anchoBorde, colorBorde)
    val interior: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = Espacio.sm),
            verticalArrangement = Arrangement.spacedBy(Espacio.xxs),
            content = contenido,
        )
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = forma,
            color = colorFondo,
            border = borde,
            content = interior,
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = forma,
            color = colorFondo,
            border = borde,
            content = interior,
        )
    }
}

@Composable
fun TituloSeccion(
    texto: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.titleSmall,
            color = Texto2,
            modifier = Modifier.weight(1f),
        )
        if (accion != null) TextButton(onClick = onAccion) { Text(accion) }
    }
}

@Composable
fun Avatar(
    iniciales: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 40.dp,
) {
    Box(
        modifier = modifier.size(tamano).clip(CircleShape).background(Primario100),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iniciales,
            color = Primario800,
            fontWeight = FontWeight.Bold,
            fontSize = (tamano.value * 0.35f).sp,
        )
    }
}

/** Iniciales de un nombre: "María López" → "ML". */
fun iniciales(nombre: String): String =
    nombre
        .split(" ")
        .filter { it.isNotBlank() && it.first().isUpperCase() }
        .take(2)
        .joinToString("") { it.first().toString() }

/** Fila de lista M3: alto mínimo 60, título 16/500 y soporte 14. */
@Composable
fun FilaLista(
    titulo: String,
    modifier: Modifier = Modifier,
    soporte: String? = null,
    onClick: (() -> Unit)? = null,
    resaltada: Boolean = false,
    divisor: Boolean = true,
    inicio: (@Composable () -> Unit)? = null,
    final: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .background(if (resaltada) Primario50 else Color.Transparent)
                    .heightIn(min = 60.dp)
                    .padding(horizontal = Espacio.md, vertical = Espacio.xs),
            horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            inicio?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                if (soporte != null) {
                    Text(
                        text = soporte,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            final?.invoke()
        }
        if (divisor) HorizontalDivider(color = BordeSuave)
    }
}

@Composable
fun Chevron() {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Chips de filtro (alto 36, radio 8); el seleccionado lleva "✓". También sirve como selector de días. */
@Composable
fun ChipsFiltro(
    opciones: List<String>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Espacio.md),
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        opciones.forEachIndexed { indice, opcion ->
            val elegida = indice == seleccionada
            FilterChip(
                selected = elegida,
                onClick = { onSeleccion(indice) },
                label = { Text(if (elegida) "✓ $opcion" else opcion) },
                shape = RoundedCornerShape(8.dp),
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primario100,
                        selectedLabelColor = Primario900,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = elegida,
                        borderColor = BordeCampo,
                        selectedBorderColor = Primario700,
                        selectedBorderWidth = 1.dp,
                    ),
            )
        }
    }
}

/** Chip con desplegable ("Doctor: Todos ▾"). El menú se conecta cuando existan los filtros reales. */
@Composable
fun ChipDesplegable(
    texto: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text("$texto ▾") },
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = BordeCampo),
    )
}

/** Botón primario Filled: alto 52, pill (radio 26). */
@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    destructivo: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors =
            if (destructivo) {
                ButtonDefaults.buttonColors(containerColor = Error, contentColor = Superficie)
            } else {
                ButtonDefaults.buttonColors()
            },
    ) { Text(text = texto, style = MaterialTheme.typography.titleMedium) }
}

/** Botón secundario Outlined: alto 48, radio 24, borde neutro. */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    destructivo: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (destructivo) Error else BordeCampo),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (destructivo) Error else Primario700),
    ) { Text(texto) }
}

/** OutlinedTextField con radio 12 (coherente con la marca) y mensajes de ayuda/error. */
@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    onValor: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    ayuda: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    soloLectura: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValor,
        modifier = modifier.fillMaxWidth(),
        label = { Text(etiqueta) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        readOnly = soloLectura,
        shape = MaterialTheme.shapes.medium,
    )
}

enum class TipoBanner { ERROR, INFO, AVISO }

/** Banner con texto + glifo y acción opcional (Reintentar, Abrir ficha, Confirmar…). */
@Composable
fun Banner(
    tipo: TipoBanner,
    texto: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: () -> Unit = {},
) {
    val (colores, glifo) =
        when (tipo) {
            TipoBanner.ERROR -> ParColor(ErrorTextoBanner, ErrorFondo, ErrorBorde) to "!"
            TipoBanner.INFO -> ParColor(Primario900, Primario50, Primario200) to "★"
            TipoBanner.AVISO -> ParColor(AmbarTexto, AmbarFondo, AmbarBorde) to "⚠"
        }
    val forma = MaterialTheme.shapes.medium
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(forma)
                .background(colores.fondo)
                .border(1.dp, colores.borde, forma)
                .padding(start = Espacio.sm, end = Espacio.xxs, top = Espacio.xs, bottom = Espacio.xs),
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = glifo, color = colores.texto, fontWeight = FontWeight.Bold)
        Text(
            text = texto,
            color = colores.texto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(vertical = Espacio.xxs),
        )
        if (accion != null) {
            TextButton(onClick = onAccion) { Text(text = accion, color = colores.texto, fontWeight = FontWeight.Bold) }
        }
    }
}

enum class EstadoPaso { HECHO, ACTUAL, PENDIENTE }

data class PasoLinea(
    val titulo: String,
    val detalle: String,
    val estado: EstadoPaso,
)

/** Línea de tiempo de estado (laboratorio, solicitud de ambulancia). */
@Composable
fun LineaTiempo(
    pasos: List<PasoLinea>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        pasos.forEachIndexed { indice, paso ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                val circulo = Modifier.size(22.dp).clip(CircleShape)
                when (paso.estado) {
                    EstadoPaso.HECHO -> {
                        Box(circulo.background(Primario700), contentAlignment = Alignment.Center) {
                            Text("✓", color = Superficie, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    EstadoPaso.ACTUAL -> {
                        Box(circulo.border(2.dp, Primario700, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${indice + 1}", color = Primario700, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    EstadoPaso.PENDIENTE -> {
                        Box(circulo.border(2.dp, BordeCampo, CircleShape))
                    }
                }
                Column {
                    Text(text = paso.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = paso.detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (indice < pasos.lastIndex) {
                Box(
                    Modifier
                        .padding(start = 10.dp)
                        .width(2.dp)
                        .height(Espacio.md)
                        .background(if (paso.estado == EstadoPaso.HECHO) Primario700 else BordeSuave),
                )
            }
        }
    }
}

/** Diálogo de confirmación M3 (radio 28). Las acciones destructivas usan el color de error. */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    texto: String,
    confirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    destructivo: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(text = confirmar, color = if (destructivo) Error else Primario700)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) } },
    )
}
