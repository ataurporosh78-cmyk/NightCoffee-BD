package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CoffeeShop
import com.example.model.MenuItem
import com.example.ui.theme.CaramelContainer
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.CremaGold
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import kotlinx.coroutines.launch

@Composable
fun ShopDetailsScreen(
    shopItem: CoffeeShopUiItem,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shop = shopItem.shop
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val noMapsMessage = stringResource(R.string.error_no_maps_app)
    val noDialerMessage = stringResource(R.string.error_no_dialer_app)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EspressoBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            DetailsBottomActionBar(
                onViewOnMaps = {
                    val opened = openShopInGoogleMaps(context, shop)
                    if (!opened) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(noMapsMessage) }
                    }
                },
                onCallShop = {
                    val dialed = dialShopPhone(context, shop.phoneNumber)
                    if (!dialed) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(noDialerMessage) }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(EspressoBlack),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    DetailsHeroHeader(
                        shop = shop,
                        isOpenNow = shopItem.isOpenNow,
                        onBackClick = onBackClick
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AreaBadge(areaName = shop.area.displayName)
                                OpenStatusBadge(isOpenNow = shopItem.isOpenNow)
                                if (shop.isFamilyFriendly) {
                                    FamilyFriendlyBadge()
                                }
                            }

                            Text(
                                text = shop.name,
                                style = MaterialTheme.typography.displaySmall,
                                color = WarmCream
                            )

                            StarRatingRow(
                                rating = shop.rating,
                                showOutOfFive = true
                            )

                            Text(
                                text = shop.ambianceNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ToastedLatte
                            )
                        }

                        AddressAndHoursCard(shop = shop)

                        PopularMenuSection(menu = shop.popularMenu)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsHeroHeader(
    shop: CoffeeShop,
    isOpenNow: Boolean,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        Image(
            painter = painterResource(id = shop.imageRes),
            contentDescription = shop.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EspressoBlack.copy(alpha = 0.52f),
                            Color.Transparent,
                            EspressoBlack
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = EspressoBlack.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, CremaGold.copy(alpha = 0.35f))
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.details_back_desc),
                        tint = WarmCream
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressAndHoursCard(shop: CoffeeShop) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MochaCardSurface,
        border = BorderStroke(1.dp, CocoaBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoastedBeanElevated,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = CremaGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.details_hours_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedCinnamon
                    )
                    Text(
                        text = shop.hoursDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmCream
                    )
                }
            }

            HorizontalDivider(color = CocoaBorder.copy(alpha = 0.6f))

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoastedBeanElevated,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = CremaGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.details_address_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedCinnamon
                    )
                    Text(
                        text = shop.fullAddress,
                        style = MaterialTheme.typography.bodyLarge,
                        color = WarmCream
                    )
                }
            }
        }
    }
}

@Composable
private fun PopularMenuSection(menu: List<MenuItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalCafe,
                    contentDescription = null,
                    tint = CremaGold,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.details_menu_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = WarmCream
                )
            }

            Text(
                text = stringResource(R.string.details_menu_currency),
                style = MaterialTheme.typography.labelSmall,
                color = MutedCinnamon
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MochaCardSurface,
            border = BorderStroke(1.dp, CocoaBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                menu.forEachIndexed { index, item ->
                    MenuItemRow(item = item)
                    if (index < menu.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = CocoaBorder.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemRow(item: MenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmCream
                )
                if (item.isHouseFavorite) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CaramelContainer,
                        border = BorderStroke(1.dp, CremaGold.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = stringResource(R.string.signature_highlight),
                            style = MaterialTheme.typography.labelSmall,
                            color = CremaGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = ToastedLatte
            )
        }

        Text(
            text = "৳${item.priceBdt}",
            style = MaterialTheme.typography.titleMedium,
            color = CremaGold,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailsBottomActionBar(
    onViewOnMaps: () -> Unit,
    onCallShop: () -> Unit
) {
    Surface(
        color = DeepRoastSurface,
        border = BorderStroke(1.dp, CocoaBorder.copy(alpha = 0.8f)),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onViewOnMaps,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CremaGold.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = WarmCream
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("view_on_maps_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Map,
                    contentDescription = null,
                    tint = CremaGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_view_on_maps),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }

            Button(
                onClick = onCallShop,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CremaGold,
                    contentColor = DarkEspressoInk
                ),
                modifier = Modifier
                    .weight(0.85f)
                    .height(52.dp)
                    .testTag("call_shop_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_call_shop),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

private fun openShopInGoogleMaps(context: Context, shop: CoffeeShop): Boolean {
    val query = Uri.encode("${shop.name}, ${shop.fullAddress}")
    val geoUri = Uri.parse("geo:${shop.latitude},${shop.longitude}?q=$query")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    return try {
        context.startActivity(mapIntent)
        true
    } catch (_: ActivityNotFoundException) {
        val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

private fun dialShopPhone(context: Context, phoneNumber: String): Boolean {
    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
    return try {
        context.startActivity(dialIntent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
