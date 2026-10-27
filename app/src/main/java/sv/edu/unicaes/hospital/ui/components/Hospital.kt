package sv.edu.unicaes.hospital.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.ui.theme.AmbarBorde
import sv.edu.unicaes.hospital.ui.theme.AmbarFondo
import sv.edu.unicaes.hospital.ui.theme.AmbarFondoSuave
import sv.edu.unicaes.hospital.ui.theme.AmbarTexto
import sv.edu.unicaes.hospital.ui.theme.AmbarTextoBanner
import sv.edu.unicaes.hospital.ui.theme.Borde
import sv.edu.unicaes.hospital.ui.theme.BordeCampo
import sv.edu.unicaes.hospital.ui.theme.BordeSuave
import sv.edu.unicaes.hospital.ui.theme.Error
import sv.edu.unicaes.hospital.ui.theme.ErrorBorde
import sv.edu.unicaes.hospital.ui.theme.ErrorFondo
import sv.edu.unicaes.hospital.ui.theme.ErrorTextoBanner
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.Neutro700
import sv.edu.unicaes.hospital.ui.theme.ParColor
import sv.edu.unicaes.hospital.ui.theme.Primario100
import sv.edu.unicaes.hospital.ui.theme.Primario200
import sv.edu.unicaes.hospital.ui.theme.Primario50
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Primario800
import sv.edu.unicaes.hospital.ui.theme.Primario900
import sv.edu.unicaes.hospital.ui.theme.Superficie
import sv.edu.unicaes.hospital.ui.theme.Superficie2
import sv.edu.unicaes.hospital.ui.theme.Texto2
import sv.edu.unicaes.hospital.ui.theme.Texto3

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
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = Neutro700,
            ),
    )
}

@Composable
fun IconoAccion(
    @DrawableRes icono: Int,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tinte: Color = Color.Unspecified,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(icono),
            contentDescription = descripcion,
            tint = if (tinte == Color.Unspecified) LocalContentColor.current else tinte,
        )
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
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
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
    // `.sect`: 14/20 500 n600 con 4 de margen superior; con acción la fila crece a 40.
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = Espacio.xxs)
                .then(if (accion != null) Modifier.heightIn(min = 40.dp) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = texto,
            style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
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

// `.lmain`: título 16/22 500 y soporte 14/20 n600, separados 2.
val EstiloFilaTitulo = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
val EstiloFilaSoporte = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)

/** Bloque título + soporte de una fila (`.lmain`). */
@Composable
fun TextosFila(
    titulo: String,
    modifier: Modifier = Modifier,
    soporte: String? = null,
    etiquetaArriba: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (etiquetaArriba && soporte != null) Text(text = soporte, style = EstiloFilaSoporte, color = Texto2)
        Text(text = titulo, style = EstiloFilaTitulo, color = MaterialTheme.colorScheme.onSurface)
        if (!etiquetaArriba && soporte != null) Text(text = soporte, style = EstiloFilaSoporte, color = Texto2)
    }
}

/**
 * Fila de lista (`.lrow`): alto mínimo 60, divisor n200. Dentro de [TarjetaLista] usa margen 14
 * (el resaltado llega al borde de la tarjeta); suelta en pantalla usa margen 16.
 */
@Composable
fun FilaLista(
    titulo: String,
    modifier: Modifier = Modifier,
    soporte: String? = null,
    onClick: (() -> Unit)? = null,
    resaltada: Boolean = false,
    divisor: Boolean = true,
    enTarjeta: Boolean = false,
    margenHorizontal: Dp? = null,
    inicio: (@Composable () -> Unit)? = null,
    final: (@Composable () -> Unit)? = null,
) {
    // margenHorizontal = 0 para filas dentro de una TarjetaHospital, que ya trae su relleno de 14.
    val margen = margenHorizontal ?: if (enTarjeta) 14.dp else Espacio.md
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .background(if (resaltada) Primario50 else Color.Transparent)
                    .heightIn(min = 60.dp)
                    .padding(horizontal = margen, vertical = Espacio.xs),
            horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            inicio?.invoke()
            TextosFila(titulo = titulo, soporte = soporte, modifier = Modifier.weight(1f))
            final?.invoke()
        }
        if (divisor) {
            HorizontalDivider(
                color = BordeSuave,
                modifier = Modifier.padding(horizontal = if (enTarjeta) margen else 0.dp),
            )
        }
    }
}

/** Tarjeta que agrupa filas (`.card` con padding 4/14): las filas ponen su propio margen. */
@Composable
fun <T> TarjetaLista(
    elementos: List<T>,
    modifier: Modifier = Modifier,
    colorBorde: Color = BordeSuave,
    anchoBorde: Dp = 1.dp,
    colorFondo: Color = Superficie,
    fila: @Composable (elemento: T, ultimo: Boolean) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colorFondo,
        border = BorderStroke(anchoBorde, colorBorde),
    ) {
        Column(modifier = Modifier.padding(vertical = Espacio.xxs)) {
            elementos.forEachIndexed { indice, elemento -> fila(elemento, indice == elementos.lastIndex) }
        }
    }
}

/** Texto de acción en fila ("Agendar", "Ver"): 14/20 500 teal. */
@Composable
fun AccionTexto(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = texto,
        style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        color = Primario700,
        modifier = modifier.clickable(onClick = onClick).padding(vertical = Espacio.xs),
    )
}

@Composable
fun Chevron() {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = Texto3,
        modifier = Modifier.size(16.dp),
    )
}

// `.chip`: alto 36, radio 8, borde n400, 14/18 500 n700 sobre blanco; elegido en primary-100.
private val estiloChip = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)

@Composable
private fun coloresChip() =
    FilterChipDefaults.filterChipColors(
        containerColor = Superficie,
        labelColor = Neutro700,
        iconColor = Texto2,
        selectedContainerColor = Primario100,
        selectedLabelColor = Primario900,
        disabledContainerColor = Superficie2,
        disabledLabelColor = BordeCampo,
    )

/** Fila de chips (`.chips`): separación 8 y salto de línea cuando no caben. */
@Composable
fun FilaChips(
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) { contenido() }
}

/**
 * Chips de filtro (alto 36, radio 8); el seleccionado lleva "✓" en negrita. También sirve como
 * selector de días. [antes] permite anteponer chips desplegables ("Área: Todas ▾").
 */
@Composable
fun ChipsFiltro(
    opciones: List<String>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
    antes: @Composable () -> Unit = {},
) {
    FilaChips(modifier = modifier) {
        antes()
        opciones.forEachIndexed { indice, opcion ->
            ChipOpcion(texto = opcion, elegida = indice == seleccionada, onClick = { onSeleccion(indice) })
        }
    }
}

/** Chip de opción (`.chip`): "✓" en negrita si está elegido; deshabilitado se ve gris y tachado. */
@Composable
fun ChipOpcion(
    texto: String,
    elegida: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitada: Boolean = true,
) {
    FilterChip(
        selected = elegida,
        enabled = habilitada,
        onClick = onClick,
        label = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (elegida) Text(text = "✓", style = estiloChip.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = texto,
                    style =
                        if (habilitada) estiloChip else estiloChip.copy(textDecoration = TextDecoration.LineThrough),
                )
            }
        },
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = coloresChip(),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = habilitada,
                selected = elegida,
                borderColor = BordeCampo,
                selectedBorderColor = Primario700,
                disabledBorderColor = BordeSuave,
                selectedBorderWidth = 1.dp,
            ),
    )
}

/**
 * Pestañas del diseño (`.mtabs`): alto 48, ancho igual, 14/20 500 n600; la activa en n900 y
 * negrita con subrayado teal de 3. Línea inferior n200.
 */
@Composable
fun PestanasHospital(
    titulos: List<String>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            titulos.forEachIndexed { indice, titulo ->
                val activa = indice == seleccionada
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .selectable(selected = activa, role = Role.Tab, onClick = { onSeleccion(indice) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = titulo,
                        style =
                            TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = if (activa) FontWeight.Bold else FontWeight.Medium,
                            ),
                        color = if (activa) MaterialTheme.colorScheme.onSurface else Texto2,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = Espacio.xs),
                    )
                    if (activa) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Primario700),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = BordeSuave)
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
        label = { Text(text = texto, style = estiloChip) },
        trailingIcon = {
            Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null, modifier = Modifier.size(18.dp))
        },
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = coloresChip(),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = BordeCampo),
    )
}

// `.btn.and`: 16/22 500
private val estiloBoton = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)

/** Botón primario Filled: alto 52, pill (radio 26). */
@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    destructivo: Boolean = false,
    @DrawableRes icono: Int? = null,
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
                // `.btn.dis`: n200 con texto n500
                ButtonDefaults.buttonColors(disabledContainerColor = BordeSuave, disabledContentColor = Texto3)
            },
    ) {
        if (icono != null) {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Espacio.xs))
        }
        Text(text = texto, style = estiloBoton)
    }
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
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (destructivo) Error else BordeCampo),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (destructivo) Error else Primario700),
    ) { Text(text = texto, style = estiloBoton) }
}

private val estiloCampo = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)

/**
 * Campo del diseño (`.field.and`): etiqueta 12/16 arriba, caja de 56 con borde 1,5 y radio 12.
 * Enfocado o con error el borde pasa a 2 (teal o rojo). Los multilínea miden 96 de alto mínimo.
 */
@Composable
fun CampoTexto(
    etiqueta: String?,
    valor: String,
    onValor: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    ayuda: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    soloLectura: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    final: (@Composable () -> Unit)? = null,
) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    val forma = MaterialTheme.shapes.medium
    val (anchoBorde, colorBorde) =
        when {
            error != null -> 2.dp to Error
            enfocado && !soloLectura -> 2.dp to Primario700
            else -> 1.5.dp to BordeCampo
        }
    val multilinea = !singleLine || minLines > 1

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (etiqueta != null) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp),
                color = if (error != null) Error else Texto2,
            )
        }
        BasicTextField(
            value = valor,
            onValueChange = onValor,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = etiqueta ?: placeholder.orEmpty() },
            readOnly = soloLectura,
            textStyle = estiloCampo.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = singleLine,
            minLines = minLines,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaccion,
            cursorBrush = SolidColor(Primario700),
            decorationBox = { campo ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (multilinea) 96.dp else 56.dp)
                            .background(Superficie, forma)
                            .border(anchoBorde, colorBorde, forma)
                            .padding(horizontal = 14.dp, vertical = if (multilinea) 14.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
                    verticalAlignment = if (multilinea) Alignment.Top else Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (valor.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = estiloCampo, color = Texto3)
                        }
                        campo()
                    }
                    final?.invoke()
                }
            },
        )
        when {
            error != null -> Text(text = error, style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp), color = Error)
            ayuda != null -> Text(text = ayuda, style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp), color = Texto2)
        }
    }
}

/**
 * Botón segmentado del diseño (`.segbtn`): alto 48, píldora con borde n400; el segmento activo
 * en primary-100 con "✓". Texto 14/20 500.
 */
@Composable
fun SegmentosHospital(
    opciones: List<String>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(24.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(forma)
                .border(1.dp, BordeCampo, forma),
    ) {
        opciones.forEachIndexed { indice, opcion ->
            val activa = indice == seleccionada
            if (indice > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(BordeCampo))
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (activa) Primario100 else Color.Transparent)
                        .selectable(selected = activa, role = Role.RadioButton, onClick = { onSeleccion(indice) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (activa) "✓ $opcion" else opcion,
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                    color = if (activa) Primario900 else Neutro700,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Fila etiqueta–valor (`.kv`): 15/20, etiqueta n600 y valor 600 a la derecha; divisor n100. */
@Composable
fun FilaDato(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    divisor: Boolean = true,
) {
    val estilo = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            Text(text = etiqueta, style = estilo, color = Texto2, modifier = Modifier.weight(1f))
            Text(
                text = valor,
                style = estilo.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (divisor) HorizontalDivider(color = Superficie2)
    }
}

/** Avatar de 56 con nombre 22/28 y una línea de soporte (ficha, alta). */
@Composable
fun CabeceraPersona(
    nombre: String,
    soporte: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(iniciales(nombre), tamano = 56.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(nombre, style = TextStyle(fontSize = 22.sp, lineHeight = 28.sp))
            Text(text = soporte, style = EstiloFilaSoporte, color = Texto2)
        }
    }
}

enum class TipoBanner { ERROR, INFO, AVISO }

/**
 * Banner (`.banner`): radio 12, padding 12/14, icono de 20 y texto 14/20. Acepta texto con
 * negritas (AnnotatedString) y una acción opcional (Reintentar, Abrir ficha…).
 */
@Composable
fun Banner(
    tipo: TipoBanner,
    texto: AnnotatedString,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    accion: String? = null,
    onAccion: () -> Unit = {},
) {
    val (colores, tinte, iconoTipo) =
        when (tipo) {
            TipoBanner.ERROR -> {
                Triple(ParColor(ErrorTextoBanner, ErrorFondo, ErrorBorde), Error, R.drawable.ic_alerta)
            }

            TipoBanner.INFO -> {
                Triple(ParColor(Primario900, Primario50, Primario200), Primario700, R.drawable.ic_info)
            }

            TipoBanner.AVISO -> {
                Triple(ParColor(AmbarTextoBanner, AmbarFondoSuave, AmbarBorde), AmbarTexto, R.drawable.ic_alerta)
            }
        }
    val forma = MaterialTheme.shapes.medium
    val estilo = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(forma)
                .background(colores.fondo)
                .border(1.dp, colores.borde, forma)
                .padding(horizontal = 14.dp, vertical = Espacio.sm),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(icono ?: iconoTipo),
            contentDescription = null,
            tint = tinte,
            modifier = Modifier.size(20.dp),
        )
        Text(text = texto, color = colores.texto, style = estilo, modifier = Modifier.weight(1f))
        if (accion != null) {
            Text(
                text = accion,
                color = colores.texto,
                style = estilo.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.clickable(onClick = onAccion),
            )
        }
    }
}

@Composable
fun Banner(
    tipo: TipoBanner,
    texto: String,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    accion: String? = null,
    onAccion: () -> Unit = {},
) {
    Banner(tipo, AnnotatedString(texto), modifier, icono, accion, onAccion)
}

/** FAB extendido del diseño (`.fab`): alto 56, radio 16, primary-100 con texto 15/20 500. */
@Composable
fun FabHospital(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int = R.drawable.ic_add,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = Primario100,
        contentColor = Primario900,
        icon = { Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(22.dp)) },
        text = {
            Text(text = texto, style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium))
        },
    )
}

/** Botón destructivo relleno (`.btn.and.dan`): rojo, píldora; alto 48 o 52. */
@Composable
fun BotonPeligro(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    alto: Dp = 48.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(alto),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Error, contentColor = Superficie),
    ) {
        if (icono != null) {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Espacio.xs))
        }
        Text(text = texto, style = estiloBoton)
    }
}

enum class EstadoPaso { HECHO, ACTUAL, PENDIENTE }

data class PasoLinea(
    val titulo: String,
    val detalle: String,
    val estado: EstadoPaso,
)

/**
 * Línea de tiempo (`.timeline`): círculos de 28 (hecho en teal con ✓, actual con borde teal,
 * pendiente con borde n400), título 15/20 600 y detalle 13/18; conector de 2 en primary-200 (n300
 * hacia un paso pendiente).
 */
@Composable
fun LineaTiempo(
    pasos: List<PasoLinea>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        pasos.forEachIndexed { indice, paso ->
            val ultimo = indice == pasos.lastIndex
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val circulo = Modifier.size(28.dp).clip(CircleShape)
                    when (paso.estado) {
                        EstadoPaso.HECHO -> {
                            Box(circulo.background(Primario700), contentAlignment = Alignment.Center) {
                                Text("✓", color = Superficie, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        EstadoPaso.ACTUAL -> {
                            Box(
                                circulo.background(Superficie).border(2.dp, Primario700, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${indice + 1}",
                                    color = Primario700,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        EstadoPaso.PENDIENTE -> {
                            Box(circulo.background(Superficie).border(2.dp, BordeCampo, CircleShape))
                        }
                    }
                    if (!ultimo) {
                        val siguiente = pasos[indice + 1].estado
                        Box(
                            Modifier
                                .padding(vertical = 2.dp)
                                .width(2.dp)
                                .weight(1f)
                                .background(if (siguiente == EstadoPaso.PENDIENTE) Borde else Primario200),
                        )
                    }
                }
                Column(modifier = Modifier.padding(bottom = if (ultimo) 0.dp else 18.dp)) {
                    Text(
                        text = paso.titulo,
                        style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
                        color =
                            if (paso.estado ==
                                EstadoPaso.PENDIENTE
                            ) {
                                Texto2
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                    if (paso.detalle.isNotEmpty()) {
                        Text(
                            text = paso.detalle,
                            style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                            color = Texto2,
                        )
                    }
                }
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
