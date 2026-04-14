package com.example.d_linkmobilymanagement.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

@Composable
fun ModernBottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    
                    // تحريك وزن العنصر ليعطي مساحة أكبر للمختار
                    val animatedWeight by animateFloatAsState(
                        targetValue = if (isSelected) 1.5f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "weight"
                    )
                    
                    ModernNavItemIndicator(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onItemSelected(item.route) },
                        modifier = Modifier.weight(animatedWeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernNavItemIndicator(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // زيادة عرض المؤشر ليتناسب مع المساحة الأكبر
    val indicatorWidth by animateDpAsState(
        targetValue = if (isSelected) 64.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicatorWidth"
    )
    
    Column(
        modifier = modifier
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. مؤشر الاختيار العلوي
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
        
        Spacer(modifier = Modifier.height(22.dp))
        
        // 2. الأيقونة الكبيرة
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        
        // 3. العنوان (بأقصى مساحة ممكنة في سطر واحد)
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(21.dp))
        } else {
            Spacer(modifier = Modifier.height(25.dp))
        }
    }
}
