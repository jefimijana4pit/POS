package com.fourpit.marketpos.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fourpit.R
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.UserRole

@Composable
fun LoginHeroBanner(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_pos_hero),
            contentDescription = strings.appName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                        )
                    )
                )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                modifier = Modifier.size(52.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_marketpos_logo),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun RoleSelectorRow(
    selectedRole: UserRole,
    onSelectRole: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RoleChip(
            selected = selectedRole == UserRole.MARKET_WORKER,
            label = strings.roleMarketWorker,
            icon = Icons.Default.PointOfSale,
            onClick = { onSelectRole(UserRole.MARKET_WORKER) },
            modifier = Modifier
                .weight(1f)
                .testTag("role_market_chip")
        )
        RoleChip(
            selected = selectedRole == UserRole.BAR_WORKER,
            label = strings.roleBarWorker,
            icon = Icons.Default.LocalBar,
            onClick = { onSelectRole(UserRole.BAR_WORKER) },
            modifier = Modifier
                .weight(1f)
                .testTag("role_bar_chip")
        )
        RoleChip(
            selected = selectedRole == UserRole.ADMIN,
            label = strings.roleAdmin,
            icon = Icons.Default.AdminPanelSettings,
            onClick = { onSelectRole(UserRole.ADMIN) },
            modifier = Modifier
                .weight(1f)
                .testTag("role_admin_chip")
        )
    }
}

@Composable
private fun RoleChip(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        modifier = modifier
    )
}
