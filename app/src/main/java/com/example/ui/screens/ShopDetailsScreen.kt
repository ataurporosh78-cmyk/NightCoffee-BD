@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MenuItem
import com.example.model.ShopReview
import com.example.ui.components.AreaBadge
import com.example.ui.components.CommunityBadge
import com.example.ui.components.FamilyFriendlyBadge
import com.example.ui.components.FavoriteHeartButton
import com.example.ui.components.InteractiveStarSelector
import com.example.ui.components.OpenStatusBadge
import com.example.ui.components.StarRatingRow
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeDetailState
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShopDetailsScreen(
    detailState: CoffeeDetailState?,
    currentUserId: String?,
    isSubmittingAction: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSubmitReview: suspend (rating: Int, comment: String) -> Result<Unit>,
    onDeleteReview: suspend (reviewId: String) -> Result<Unit>,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    val noMapsAppMsg = stringResource(R.string.error_no_maps_app)
    val noDialerAppMsg = stringResource(R.string.error_no_dialer_app)
    val reviewPostedMsg = stringResource(R.string.reviews_posted_toast)

    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onClearError()
    }

    val openMapsAction: () -> Unit = {
        val shop = detailState?.item?.shop
        if (shop != null) {
            val encodedQuery = Uri.encode("${shop.name}, ${shop.address}, Dhaka")
            val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encodedQuery"))
            try {
                context.startActivity(geoIntent)
            } catch (_: ActivityNotFoundException) {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                )
                try {
                    context.startActivity(webIntent)
                } catch (_: ActivityNotFoundException) {
                    coroutineScope.launch { snackbarHostState.showSnackbar(noMapsAppMsg) }
                }
            }
        }
    }

    val callShopAction: () -> Unit = {
        val phone = detailState?.item?.shop?.phone.orEmpty()
        if (phone.isNotBlank()) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            try {
                context.startActivity(dialIntent)
            } catch (_: ActivityNotFoundException) {
                coroutineScope.launch { snackbarHostState.showSnackbar(noDialerAppMsg) }
            }
        }
    }

    val shareShopAction: () -> Unit = {
        val item = detailState?.item
        if (item != null) {
            val shop = item.shop
            val shareText = buildString {
                append("${shop.name} (${shop.areaLabel})\n")
                append("Hours: ${shop.hoursDisplay}\n")
                append("Address: ${shop.address}\n")
                if (shop.priceRange.isNotBlank()) append("Prices: ${shop.priceRange}\n")
                if (shop.popularItems.isNotEmpty()) {
                    append("Popular: ${shop.popularItems.joinToString(", ")}\n")
                }
                append("Found on NightCoffee BD")
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.details_share_action)))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EspressoBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = RoastedBeanElevated,
                    contentColor = WarmCream,
                    actionColor = CaramelGold
                )
            }
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EspressoBlack,
                    titleContentColor = WarmCream,
                    navigationIconContentColor = WarmCream
                ),
                title = {
                    Text(
                        text = detailState?.item?.shop?.name ?: stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.details_back_desc)
                        )
                    }
                },
                actions = {
                    if (detailState != null) {
                        IconButton(
                            onClick = shareShopAction,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("details_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.details_share_desc),
                                tint = WarmCream
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        FavoriteHeartButton(
                            isFavorite = detailState.item.isFavorite,
                            onToggle = { onToggleFavorite(detailState.item.shop.id) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("details_favorite_button")
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (detailState != null) {
                val hasPhone = detailState.item.shop.phone.isNotBlank()
                Surface(
                    color = DeepRoastSurface,
                    border = BorderStroke(1.dp, CocoaBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = openMapsAction,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CaramelGold,
                                    contentColor = DarkEspressoInk
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("directions_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Directions,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.details_directions_action),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (hasPhone) {
                                OutlinedButton(
                                    onClick = callShopAction,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CaramelGold),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("call_shop_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Call,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.details_call_action),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = shareShopAction,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CaramelGold),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("share_shop_bottom_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.details_share_action),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EspressoBlack)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (detailState == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CaramelGold)
                }
            } else {
                val item = detailState.item
                val shop = item.shop
                val menuLabels: List<String> = remember(shop.popularItems, shop.popularMenu) {
                    if (shop.popularItems.isNotEmpty()) {
                        shop.popularItems
                    } else {
                        shop.popularMenu.map { menuItem: MenuItem ->
                            if (menuItem.priceBdt > 0) {
                                "${menuItem.name} (৳${menuItem.priceBdt})"
                            } else {
                                menuItem.name
                            }
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "header_card") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MochaCardSurface,
                            border = BorderStroke(1.dp, CocoaBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AreaBadge(areaName = shop.areaLabel)
                                    OpenStatusBadge(isOpenNow = item.isOpenNow)
                                    if (shop.isFamilyFriendly) {
                                        FamilyFriendlyBadge()
                                    }
                                    if (shop.isCommunitySubmitted) {
                                        CommunityBadge()
                                    }
                                }

                                Text(
                                    text = shop.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = WarmCream
                                )

                                StarRatingRow(
                                    rating = item.effectiveRating,
                                    reviewCount = item.reviewCount,
                                    showOutOfFive = true
                                )
                            }
                        }
                    }

                    item(key = "info_card") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MochaCardSurface,
                            border = BorderStroke(1.dp, CocoaBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                DetailInfoRow(
                                    icon = Icons.Rounded.Schedule,
                                    label = stringResource(R.string.details_hours_label),
                                    value = shop.hoursDisplay
                                )
                                HorizontalDivider(color = CocoaBorder.copy(alpha = 0.5f))
                                DetailInfoRow(
                                    icon = Icons.Rounded.LocationOn,
                                    label = stringResource(R.string.details_address_label),
                                    value = shop.address
                                )
                                if (shop.priceRange.isNotBlank()) {
                                    HorizontalDivider(color = CocoaBorder.copy(alpha = 0.5f))
                                    DetailInfoRow(
                                        icon = Icons.Rounded.Payments,
                                        label = stringResource(R.string.details_price_range_label),
                                        value = shop.priceRange
                                    )
                                }
                                if (shop.phone.isNotBlank()) {
                                    HorizontalDivider(color = CocoaBorder.copy(alpha = 0.5f))
                                    DetailInfoRow(
                                        icon = Icons.Rounded.Call,
                                        label = stringResource(R.string.add_shop_phone_label),
                                        value = shop.phone
                                    )
                                }
                            }
                        }
                    }

                    if (menuLabels.isNotEmpty()) {
                        item(key = "popular_items_card") {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MochaCardSurface,
                                border = BorderStroke(1.dp, CocoaBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.details_popular_items_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WarmCream
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        menuLabels.forEach { drink ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = RoastedBeanElevated,
                                                border = BorderStroke(1.dp, CocoaBorder)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.LocalCafe,
                                                        contentDescription = null,
                                                        tint = CaramelGold,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = drink,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = WarmCream,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "reviews_composer") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MochaCardSurface,
                            border = BorderStroke(1.dp, CocoaBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.reviews_section_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WarmCream
                                    )
                                    Text(
                                        text = stringResource(R.string.reviews_count_label, detailState.reviews.size),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CaramelGold
                                    )
                                }

                                Text(
                                    text = stringResource(R.string.reviews_your_rating),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ToastedLatte
                                )

                                InteractiveStarSelector(
                                    selectedRating = selectedRating,
                                    onSelectRating = { selectedRating = it }
                                )

                                OutlinedTextField(
                                    value = reviewComment,
                                    onValueChange = { if (it.length <= 500) reviewComment = it },
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.reviews_comment_hint),
                                            color = MutedCinnamon
                                        )
                                    },
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DeepRoastSurface,
                                        unfocusedContainerColor = DeepRoastSurface,
                                        focusedBorderColor = CaramelGold,
                                        unfocusedBorderColor = CocoaBorder,
                                        focusedTextColor = WarmCream,
                                        unfocusedTextColor = WarmCream,
                                        cursorColor = CaramelGold
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("review_comment_input")
                                )

                                Button(
                                    onClick = {
                                        val cleanComment = reviewComment.trim()
                                        if (cleanComment.isNotEmpty() && !isSubmittingAction) {
                                            coroutineScope.launch {
                                                val result = onSubmitReview(selectedRating, cleanComment)
                                                if (result.isSuccess) {
                                                    reviewComment = ""
                                                    selectedRating = 5
                                                    snackbarHostState.showSnackbar(reviewPostedMsg)
                                                }
                                            }
                                        }
                                    },
                                    enabled = reviewComment.isNotBlank() && !isSubmittingAction,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CaramelGold,
                                        contentColor = DarkEspressoInk,
                                        disabledContainerColor = RoastedBeanElevated,
                                        disabledContentColor = MutedCinnamon
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("submit_review_button")
                                ) {
                                    if (isSubmittingAction) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = DarkEspressoInk,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.reviews_submit_button),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (detailState.reviews.isEmpty()) {
                        item(key = "reviews_empty") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DeepRoastSurface,
                                border = BorderStroke(1.dp, CocoaBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.reviews_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ToastedLatte,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(
                            items = detailState.reviews,
                            key = { it.id }
                        ) { review ->
                            ReviewCard(
                                review = review,
                                isOwnedByCurrentUser = currentUserId != null && review.userId == currentUserId,
                                onDelete = {
                                    coroutineScope.launch {
                                        onDeleteReview(review.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CaramelGold,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedCinnamon
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = WarmCream
            )
        }
    }
}

@Composable
private fun ReviewCard(
    review: ShopReview,
    isOwnedByCurrentUser: Boolean,
    onDelete: () -> Unit
) {
    val dateLabel = remember(review.createdAt) {
        review.createdAt?.toDate()?.let { date ->
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        }
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MochaCardSurface,
        border = BorderStroke(1.dp, CocoaBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.authorName.ifBlank { "Dhaka Coffee Lover" },
                        style = MaterialTheme.typography.titleSmall,
                        color = WarmCream,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!dateLabel.isNullOrBlank()) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedCinnamon
                        )
                    }
                }

                if (isOwnedByCurrentUser) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_review_${review.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.reviews_delete_desc),
                            tint = ToastedLatte
                        )
                    }
                }
            }

            StarRatingRow(rating = review.rating.toDouble())

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = ToastedLatte
            )
        }
    }
}
