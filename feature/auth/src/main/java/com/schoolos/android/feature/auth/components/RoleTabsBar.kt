package com.schoolos.android.feature.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@Composable
fun RoleTabsBar(
    roleTabs: List<RoleTabConfig>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CosmicNavy.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            roleTabs.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index
                val tabBackground by animateColorAsState(
                    targetValue = if (isSelected) tab.accentColor.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = tween(250),
                    label = "tabBg",
                )
                val tabBorderColor by animateColorAsState(
                    targetValue = if (isSelected) tab.accentColor else Color.Transparent,
                    animationSpec = tween(250),
                    label = "tabBorder",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tabBackground)
                        .border(1.2.dp, tabBorderColor, RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) tab.accentColor.copy(alpha = 0.2f) else CosmicSurface)
                                .border(
                                    0.8.dp,
                                    if (isSelected) tab.accentColor.copy(alpha = 0.6f) else Color.Transparent,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) tab.accentColor else TextTertiary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) tab.accentColor else TextSecondary,
                        )
                        if (tab.subtitle.isNotEmpty()) {
                            Text(
                                text = tab.subtitle,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) tab.accentColor.copy(alpha = 0.85f) else TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
