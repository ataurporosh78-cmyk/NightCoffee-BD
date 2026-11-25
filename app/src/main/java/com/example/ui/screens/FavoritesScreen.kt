@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.LiquidGlassShopCard
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.StaggeredEntranceItem
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.FavoriteHeart
import com.example.ui.theme.FavoriteHeartContainer
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeHomeUiState

@Composable
fun FavoritesScreen(
    uiState: CoffeeHomeUiState,
    onToggleFavorite: (String) -> Unit,
    onShopClick: (String) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EspressoRich.copy(alpha = 0.22f),
                        EspressoBlack,
                        EspressoBlack
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.favorites_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = WarmCream
                    )
                    Text(
                        text = stringResource(R.string.favorites_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = ToastedLatte
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = FavoriteHeartContainer.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, FavoriteHeart.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = FavoriteHeart,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = uiState.favoriteShops.size.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = WarmCream,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (uiState.favoriteShops.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = FavoriteHeart,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = stringResource(R.string.empty_favorites_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.empty_favorites_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = ToastedLatte,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onExploreClick,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CaramelGold,
                                    contentColor = DarkEspressoInk
                                ),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .height(48.dp)
                                    .testTag("favorites_explore_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.tab_all_cafes),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("favorites_shops_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 116.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        items = uiState.favoriteShops,
                        key = { _, item -> "fav_${item.shop.id}" },
                        contentType = { _, _ -> "liquid_shop_card" }
                    ) { index, item ->
                        StaggeredEntranceItem(index = index) {
                            LiquidGlassShopCard(
                                item = item,
                                onToggleFavorite = { onToggleFavorite(item.shop.id) },
                                onClick = { onShopClick(item.shop.id) }
                            )
                        }
                    }
                    item(key = "favorites_bottom_spacer") {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}
