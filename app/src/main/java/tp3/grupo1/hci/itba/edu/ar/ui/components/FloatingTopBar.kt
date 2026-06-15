package tp3.grupo1.hci.itba.edu.ar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * App bar flotante con el look del prototipo: una píldora redondeada que "flota"
 * sobre el contenido (margen, esquinas redondeadas y sombra) y acompaña al
 * usuario en todas las pantallas.
 *
 * Mantiene los mismos slots que [androidx.compose.material3.TopAppBar]
 * (`title` / `navigationIcon` / `actions`), de modo que cada pantalla sigue
 * mostrando su título contextual y sus acciones propias (RNF2): solo cambia el
 * aspecto visual. Se usa dentro del slot `topBar` de un `Scaffold`, igual que el
 * `TopAppBar` que reemplaza.
 */
@Composable
fun FloatingTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            navigationIcon()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                    title()
                }
            }
            actions()
        }
    }
}
