package tp3.grupo1.hci.itba.edu.ar.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import tp3.grupo1.hci.itba.edu.ar.R

private val brandFeatures = listOf(
    R.string.auth_feature_remote_control,
    R.string.auth_feature_routines,
    R.string.auth_feature_energy,
    R.string.auth_feature_alerts,
)

@Composable
private fun BrandLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun AuthBrandPanel(expanded: Boolean, modifier: Modifier = Modifier) {
    if (expanded) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            BrandLogo()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.auth_tagline),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.auth_brand_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                brandFeatures.forEach { featureRes ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                        Text(
                            text = stringResource(featureRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.auth_copyright),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            BrandLogo()
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// MEDIUM usa el mismo layout apilado que COMPACT porque partir 600-840dp en panel + formulario queda apretado y el panel no entra en alto.
// En las variantes apiladas el formulario usa imePadding() para que el teclado no tape los inputs, el peor caso en landscape de teléfono.
@Composable
fun AuthLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    if (windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.EXPANDED) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthBrandPanel(expanded = false, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(28.dp))
            Column(modifier = Modifier.widthIn(max = 440.dp), content = content)
        }
    } else {
        Row(modifier = modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                AuthBrandPanel(expanded = true, modifier = Modifier.padding(32.dp))
            }
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(modifier = Modifier.widthIn(max = 440.dp), content = content)
            }
        }
    }
}
