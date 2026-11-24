@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AggregatedCrowdStatus
import com.example.model.CrowdLevel
import com.example.model.LiveShopStatus
import com.example.model.MenuItem
import com.example.model.ReportedStatusType
import com.example.model.ResolvedShopStatus
import com.example.model.ShopReview
import com.example.ui.components.AreaBadge
import com.example.ui.components.BusinessVerificationDialog
import com.example.ui.components.CommunityBadge
import com.example.ui.components.CrowdReportDialog
import com.example.ui.components.DistanceBadge
import com.example.ui.components.FacilityPillBadge
import com.example.ui.components.FavoriteHeartButton
import com.example.ui.components.InteractiveStarSelector
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LiveCrowdStatusCard
import com.example.ui.components.LiveShopStatusCard
import com.example.ui.components.OfficialStatusDialog
import com.example.ui.components.OpenStatusBadge
import com.example.ui.components.QuickPlatformTile
import com.example.ui.components.ShopImageResolver
import com.example.ui.components.StarRatingRow
import com.example.ui.components.StatusReportDialog
import com.example.ui.components.VerifiedShopBadge
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.GlassBottomNavFill
import com.example.ui.theme.GlassHighlightBorder
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeDetailState
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
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
    modifier: Modifier = Modifier,
    isSignedIn: Boolean = true,
    onRequireSignIn: () -> Unit = {},
    resolvedStatus: ResolvedShopStatus? = null,
    isVerifiedBusiness: Boolean = false,
    crowdStatus: AggregatedCrowdStatus = AggregatedCrowdStatus(null, 0, null, false),
    latestActiveCrowdReportId: String? = null,
    isCompared: Boolean = false,
    canManageShop: Boolean = false,
    isBusy: Boolean = false,
    onOpenDigitalMenu: () -> Unit = {},
    onOpenPhotoReviews: () -> Unit = {},
    onBookTable: () -> Unit = {},
    onToggleCompare: () -> Unit = {},
    onReportIssue: () -> Unit = {},
    onSubmitStatusReport: suspend (ReportedStatusType, String) -> Result<Boolean> = { _, _ -> Result.success(false) },
    onOfficialStatusUpdate: suspend (LiveShopStatus) -> Result<Unit> = { Result.success(Unit) },
    onSubmitCrowdReport: suspend (CrowdLevel, String) -> Result<Boolean> = { _, _ -> Result.success(false) },
    onFlagLatestCrowdReport: suspend (String, String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    onSubmitVerificationRequest: suspend (String, String, String) -> Result<Unit> = { _, _, _ -> Result.success(Unit) }
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var showStatusReportDialog by remember { mutableStateOf(false) }
    var showOfficialStatusDialog by remember { mutableStateOf(false) }
    var showCrowdReportDialog by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }

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
            context.startActivity(
                Intent.createChooser(sendIntent, context.getString(R.string.details_share_action))
            )
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
        bottomBar = {
            if (detailState != null) {
                val hasPhone = detailState.item.shop.phone.isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val barShape = RoundedCornerShape(28.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .clip(barShape)
                    ) {
                        val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(18.dp)
                        } else {
                            Modifier
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .then(blurModifier)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            EspressoRich.copy(alpha = 0.45f),
                                            GlassBottomNavFill,
                                            EspressoBlack.copy(alpha = 0.90f)
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(GlassHighlightBorder, CocoaBorder)
                                    ),
                                    shape = barShape
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = openMapsAction,
                                shape = RoundedCornerShape(20.dp),
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
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.65f)),
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
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.65f)),
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
                .background(EspressoBlack),
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
                val heroDrawable = remember(shop.id, shop.imageRes) {
                    ShopImageResolver.resolveDrawable(shop)
                }
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
                    contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "hero_banner") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp)
                        ) {
                            Image(
                                painter = painterResource(id = heroDrawable),
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
                                                EspressoBlack.copy(alpha = 0.65f),
                                                Color.Transparent,
                                                EspressoBlack.copy(alpha = 0.96f)
                                            )
                                        )
                                    )
                            )

                            // Floating translucent glass Top Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EspressoBlack.copy(alpha = 0.68f),
                                    border = BorderStroke(1.dp, CocoaBorder),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    IconButton(
                                        onClick = onBack,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("details_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = stringResource(R.string.details_back_desc),
                                            tint = WarmCream
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = EspressoBlack.copy(alpha = 0.68f),
                                        border = BorderStroke(1.dp, CocoaBorder),
                                        modifier = Modifier.size(48.dp)
                                    ) {
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
                                    }

                                    FavoriteHeartButton(
                                        isFavorite = item.isFavorite,
                                        onToggle = { onToggleFavorite(shop.id) },
                                        modifier = Modifier.testTag("details_favorite_button")
                                    )
                                }
                            }

                            // Hero Status Overlay
                            FlowRow(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AreaBadge(areaName = shop.areaLabel)
                                OpenStatusBadge(isOpenNow = resolvedStatus?.effectiveOpenNow ?: item.isOpenNow)
                                if (isVerifiedBusiness) {
                                    VerifiedShopBadge()
                                }
                                item.distanceKm?.let { km ->
                                    DistanceBadge(distanceKm = km)
                                }
                                if (shop.isCommunitySubmitted) {
                                    CommunityBadge()
                                }
                            }
                        }
                    }

                    item(key = "header_card") {
                        LiquidGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(26.dp),
                            accentGlow = true
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = shop.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = WarmCream
                                )
                                Text(
                                    text = shop.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ToastedLatte
                                )
                                StarRatingRow(
                                    rating = item.effectiveRating,
                                    reviewCount = item.reviewCount,
                                    showOutOfFive = true
                                )
                            }
                        }
                    }

                    item(key = "quick_action_hub") {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickPlatformTile(
                                icon = Icons.Rounded.RestaurantMenu,
                                label = stringResource(R.string.menu_screen_title),
                                onClick = onOpenDigitalMenu,
                                modifier = Modifier.testTag("details_menu_button")
                            )
                            QuickPlatformTile(
                                icon = Icons.Rounded.AddPhotoAlternate,
                                label = "Photo Reviews",
                                onClick = onOpenPhotoReviews,
                                modifier = Modifier.testTag("details_photo_reviews_button")
                            )
                            QuickPlatformTile(
                                icon = Icons.Rounded.EventSeat,
                                label = "Book Table",
                                onClick = onBookTable,
                                modifier = Modifier.testTag("details_book_table_button")
                            )
                            QuickPlatformTile(
                                icon = Icons.Rounded.CompareArrows,
                                label = if (isCompared) "Compared" else "Compare",
                                badge = if (isCompared) "✓" else null,
                                onClick = onToggleCompare,
                                modifier = Modifier.testTag("details_compare_button")
                            )
                            QuickPlatformTile(
                                icon = Icons.Rounded.SupportAgent,
                                label = "Report Info",
                                onClick = onReportIssue,
                                modifier = Modifier.testTag("details_report_issue_button")
                            )
                            if (!isVerifiedBusiness) {
                                QuickPlatformTile(
                                    icon = Icons.Rounded.VerifiedUser,
                                    label = "Verify Shop",
                                    onClick = {
                                        if (!isSignedIn) {
                                            onRequireSignIn()
                                        } else {
                                            showVerificationDialog = true
                                        }
                                    },
                                    modifier = Modifier.testTag("details_verify_shop_button")
                                )
                            }
                        }
                    }

                    if (resolvedStatus != null) {
                        item(key = "live_status_card") {
                            LiveShopStatusCard(
                                status = resolvedStatus,
                                isVerifiedBusiness = isVerifiedBusiness,
                                canManageShop = canManageShop,
                                onReportStatusClick = {
                                    if (!isSignedIn) onRequireSignIn() else showStatusReportDialog = true
                                },
                                onOfficialUpdateClick = {
                                    if (!isSignedIn) onRequireSignIn() else showOfficialStatusDialog = true
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    item(key = "live_crowd_card") {
                        LiveCrowdStatusCard(
                            crowd = crowdStatus,
                            canFlag = latestActiveCrowdReportId != null && isSignedIn,
                            onReportCrowdClick = {
                                if (!isSignedIn) onRequireSignIn() else showCrowdReportDialog = true
                            },
                            onFlagIncorrectClick = {
                                val reportId = latestActiveCrowdReportId ?: return@LiveCrowdStatusCard
                                coroutineScope.launch {
                                    val res = onFlagLatestCrowdReport(reportId, "Crowd level does not match current cafe seating")
                                    snackbarHostState.showSnackbar(
                                        if (res.isSuccess) "Crowd report flagged for review" else (res.exceptionOrNull()?.message ?: "Unable to flag report")
                                    )
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item(key = "facilities_card") {
                        LiquidGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.details_facilities_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WarmCream
                                )
                                if (shop.ambianceNote.isNotBlank()) {
                                    Text(
                                        text = shop.ambianceNote,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ToastedLatte
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    shop.resolvedFacilities.forEach { facility ->
                                        FacilityPillBadge(facility = facility)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "info_card") {
                        LiquidGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(26.dp)
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
                                HorizontalDivider(color = CocoaBorder)
                                DetailInfoRow(
                                    icon = Icons.Rounded.LocationOn,
                                    label = stringResource(R.string.details_address_label),
                                    value = shop.address
                                )
                                if (shop.priceRange.isNotBlank()) {
                                    HorizontalDivider(color = CocoaBorder)
                                    DetailInfoRow(
                                        icon = Icons.Rounded.Payments,
                                        label = stringResource(R.string.details_price_range_label),
                                        value = shop.priceRange
                                    )
                                }
                                if (shop.phone.isNotBlank()) {
                                    HorizontalDivider(color = CocoaBorder)
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
                            LiquidGlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(26.dp)
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
                                                shape = RoundedCornerShape(14.dp),
                                                color = EspressoRich.copy(alpha = 0.35f),
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
                        LiquidGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(26.dp)
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
                                    shape = RoundedCornerShape(18.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DeepRoastSurface,
                                        unfocusedContainerColor = DeepRoastSurface,
                                        focusedBorderColor = CaramelGold.copy(alpha = 0.7f),
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
                                        if (!isSignedIn) {
                                            onRequireSignIn()
                                        } else {
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
                                        }
                                    },
                                    enabled = if (!isSignedIn) {
                                        !isSubmittingAction
                                    } else {
                                        reviewComment.isNotBlank() && !isSubmittingAction
                                    },
                                    shape = RoundedCornerShape(18.dp),
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
                                            text = stringResource(
                                                if (isSignedIn) {
                                                    R.string.reviews_submit_button
                                                } else {
                                                    R.string.auth_sign_in_to_review
                                                }
                                            ),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (detailState.reviews.isEmpty()) {
                        item(key = "reviews_empty") {
                            LiquidGlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(22.dp)
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
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    val currentShop = detailState?.item?.shop
    if (showStatusReportDialog && currentShop != null) {
        StatusReportDialog(
            isBusy = isBusy || isSubmittingAction,
            onDismiss = { showStatusReportDialog = false },
            onSubmit = { statusType, note ->
                coroutineScope.launch {
                    val res = onSubmitStatusReport(statusType, note)
                    if (res.isSuccess) {
                        showStatusReportDialog = false
                        val publishedLive = res.getOrDefault(true)
                        snackbarHostState.showSnackbar(
                            if (publishedLive) "Status report submitted (marked unverified)"
                            else "Status report queued offline — will sync automatically"
                        )
                    } else {
                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to submit status report")
                    }
                }
            }
        )
    }

    if (showOfficialStatusDialog && currentShop != null) {
        OfficialStatusDialog(
            isBusy = isBusy || isSubmittingAction,
            onDismiss = { showOfficialStatusDialog = false },
            onSubmit = { liveStatus ->
                coroutineScope.launch {
                    val res = onOfficialStatusUpdate(liveStatus)
                    if (res.isSuccess) {
                        showOfficialStatusDialog = false
                        snackbarHostState.showSnackbar("Official live status updated")
                    } else {
                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Unable to update official status")
                    }
                }
            }
        )
    }

    if (showCrowdReportDialog && currentShop != null) {
        CrowdReportDialog(
            isBusy = isBusy || isSubmittingAction,
            onDismiss = { showCrowdReportDialog = false },
            onSubmit = { level, note ->
                coroutineScope.launch {
                    val res = onSubmitCrowdReport(level, note)
                    if (res.isSuccess) {
                        showCrowdReportDialog = false
                        val publishedLive = res.getOrDefault(true)
                        snackbarHostState.showSnackbar(
                            if (publishedLive) "Crowd level updated (expires in 2h)"
                            else "Crowd report queued offline — will sync automatically"
                        )
                    } else {
                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Unable to submit crowd report")
                    }
                }
            }
        )
    }

    if (showVerificationDialog && currentShop != null) {
        BusinessVerificationDialog(
            shopName = currentShop.name,
            isBusy = isBusy || isSubmittingAction,
            onDismiss = { showVerificationDialog = false },
            onSubmit = { ownerName, phone, note ->
                coroutineScope.launch {
                    val res = onSubmitVerificationRequest(ownerName, phone, note)
                    if (res.isSuccess) {
                        showVerificationDialog = false
                        snackbarHostState.showSnackbar("Verification request sent to platform admins")
                    } else {
                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Unable to submit verification request")
                    }
                }
            }
        )
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
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = remember(review.createdAt) {
        review.createdAt?.toDate()?.let { date ->
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        }
    }
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
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
