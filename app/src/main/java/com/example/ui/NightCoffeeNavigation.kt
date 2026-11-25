@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.NotificationDeepLink
import com.example.R
import com.example.data.FirebaseSafeAccess
import com.example.data.LocationProvider
import com.example.model.DigitalMenuItemDoc
import com.example.model.ExploreCategory
import com.example.model.ReservationDoc
import com.example.model.ShopTimeParser
import com.example.ui.components.LiquidGlassFloatingBottomBar
import com.example.ui.components.MainNavTab
import com.example.ui.components.StartupSplashOverlay
import com.example.ui.screens.AddShopSheet
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.DigitalMenuScreen
import com.example.ui.screens.EventsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NearbyMapScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OffersScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReservationsScreen
import com.example.ui.screens.ReviewsScreen
import com.example.ui.screens.ShopDetailsScreen
import com.example.ui.screens.signOut
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.WarmCream
import com.example.util.PlatformLogic
import com.example.viewmodel.CoffeeViewModel
import com.example.viewmodel.PlatformViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private const val ROUTE_MAIN_TABS = "main_tabs"
private const val ROUTE_SHOP_DETAILS = "shop_details"
private const val ROUTE_DIGITAL_MENU = "digital_menu"
private const val ROUTE_OFFERS = "offers"
private const val ROUTE_NEARBY_MAP = "nearby_map"
private const val ROUTE_REVIEWS = "reviews"
private const val ROUTE_RESERVATIONS = "reservations"
private const val ROUTE_COMPARE = "compare"
private const val ROUTE_EVENTS = "events"
private const val ROUTE_NOTIFICATIONS = "notifications"
private const val ROUTE_HELP_SUPPORT = "help_support"
private const val ROUTE_ADMIN_DASHBOARD = "admin_dashboard"

private const val ARG_SHOP_ID = "shopId"
private const val ARG_SHOP_NAME = "shopName"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun NightCoffeeNavigation(
    pendingDeepLink: NotificationDeepLink? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = remember(context) {
        if (FirebaseSafeAccess.ensureInitialized(context)) {
            runCatching { FirebaseAuth.getInstance() }.getOrNull()
        } else {
            null
        }
    }
    val currentUser by remember(auth) {
        auth?.authStateFlow() ?: flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = auth?.currentUser)

    var showSplashForAuthenticatedUser by rememberSaveable { mutableStateOf(auth?.currentUser != null) }
    var authRefreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            showSplashForAuthenticatedUser = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val signedInUser = currentUser
        if (signedInUser == null) {
            AuthScreen(
                onAuthSuccess = {
                    authRefreshTick++
                }
            )
        } else {
            val resolvedDisplayName = remember(signedInUser.uid, authRefreshTick, signedInUser.displayName, signedInUser.email) {
                signedInUser.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: signedInUser.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            }
            key(signedInUser.uid) {
                NightCoffeeAppContent(
                    currentUserId = signedInUser.uid,
                    displayName = resolvedDisplayName,
                    isSignedIn = true,
                    pendingDeepLink = pendingDeepLink,
                    onConsumeDeepLink = onConsumeDeepLink,
                    onRequireSignIn = { }
                )
            }
            if (showSplashForAuthenticatedUser) {
                StartupSplashOverlay(onFinished = { showSplashForAuthenticatedUser = false })
            }
        }
    }
}

@Composable
private fun NightCoffeeAppContent(
    currentUserId: String,
    displayName: String?,
    isSignedIn: Boolean,
    pendingDeepLink: NotificationDeepLink?,
    onConsumeDeepLink: () -> Unit,
    onRequireSignIn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }
    val navController = rememberNavController()
    val coffeeViewModel: CoffeeViewModel = viewModel(
        key = currentUserId,
        factory = CoffeeViewModel.factory(context)
    )
    val platformViewModel: PlatformViewModel = viewModel(
        key = "platform_$currentUserId",
        factory = PlatformViewModel.factory(context, currentUserId)
    )

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(MainNavTab.HOME) }
    var showAddShopSheet by rememberSaveable { mutableStateOf(false) }

    val shopPublishedMessage = stringResource(R.string.add_shop_success)
    val locationDeniedMsg = stringResource(R.string.location_permission_denied)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        coffeeViewModel.refreshLocation(context)
        if (granted) {
            coffeeViewModel.selectCategory(ExploreCategory.NEAR_ME)
            if (navController.currentBackStackEntry?.destination?.route == ROUTE_MAIN_TABS) {
                selectedTab = MainNavTab.EXPLORE
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(locationDeniedMsg)
            }
        }
    }

    val requestLocationAndFocusNearMe: () -> Unit = {
        if (LocationProvider.hasCoarseLocationPermission(context)) {
            coffeeViewModel.refreshLocation(context)
            coffeeViewModel.selectCategory(ExploreCategory.NEAR_ME)
            selectedTab = MainNavTab.EXPLORE
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val requestLocationOnly: () -> Unit = {
        if (LocationProvider.hasCoarseLocationPermission(context)) {
            coffeeViewModel.refreshLocation(context)
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, coffeeViewModel) {
        coffeeViewModel.refreshLocation(context)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coffeeViewModel.refreshTime()
                coffeeViewModel.refreshLocation(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val uiState by coffeeViewModel.uiState.collectAsStateWithLifecycle()
    val searchInputText by coffeeViewModel.searchInputText.collectAsStateWithLifecycle()

    val isOnline by platformViewModel.isOnline.collectAsStateWithLifecycle()
    val pendingSyncCount by platformViewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val platformBusy by platformViewModel.isBusy.collectAsStateWithLifecycle()
    val userRole by platformViewModel.userRole.collectAsStateWithLifecycle()
    val shopOverlays by platformViewModel.shopOverlays.collectAsStateWithLifecycle()
    val statusReports by platformViewModel.statusReports.collectAsStateWithLifecycle()
    val crowdReports by platformViewModel.crowdReports.collectAsStateWithLifecycle()
    val coupons by platformViewModel.coupons.collectAsStateWithLifecycle()
    val couponsOfflineCached by platformViewModel.couponsOfflineCached.collectAsStateWithLifecycle()
    val myCouponRedemptions by platformViewModel.myCouponRedemptions.collectAsStateWithLifecycle()
    val photoReviews by platformViewModel.photoReviews.collectAsStateWithLifecycle()
    val myReviewVotes by platformViewModel.myReviewVotes.collectAsStateWithLifecycle()
    val myReservations by platformViewModel.myReservations.collectAsStateWithLifecycle()
    val adminReservations by platformViewModel.adminReservations.collectAsStateWithLifecycle()
    val events by platformViewModel.events.collectAsStateWithLifecycle()
    val eventsOfflineCached by platformViewModel.eventsOfflineCached.collectAsStateWithLifecycle()
    val myEventRegistrations by platformViewModel.myEventRegistrations.collectAsStateWithLifecycle()
    val notifications by platformViewModel.notifications.collectAsStateWithLifecycle()
    val userPreferences by platformViewModel.userPreferences.collectAsStateWithLifecycle()
    val compareShopIds by platformViewModel.compareShopIds.collectAsStateWithLifecycle()
    val adminVerificationRequests by platformViewModel.adminVerificationRequests.collectAsStateWithLifecycle()
    val mySupportTickets by platformViewModel.mySupportTickets.collectAsStateWithLifecycle()
    val adminSupportTickets by platformViewModel.adminSupportTickets.collectAsStateWithLifecycle()

    val staffReservations by remember(userRole.staffShopIds, platformViewModel) {
        if (userRole.staffShopIds.isEmpty()) {
            flowOf(emptyList<ReservationDoc>())
        } else {
            combine(userRole.staffShopIds.map { platformViewModel.observeShopReservations(it) }) { arrays ->
                arrays.flatMap { it }
            }
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val manageableReservations = remember(userRole.isAdmin, adminReservations, staffReservations) {
        if (userRole.isAdmin) adminReservations else staffReservations
    }

    val comparedIdSet = remember(compareShopIds) { compareShopIds.toSet() }
    val unreadNotificationsCount = remember(notifications) { notifications.count { !it.read } }
    val allCatalogShops = remember(uiState.allCatalogItems) {
        uiState.allCatalogItems.map { it.shop }
    }
    val shopById = remember(allCatalogShops) {
        allCatalogShops.associateBy { it.id }
    }

    val toggleCompareWithFeedback: (String) -> Unit = { shopId ->
        scope.launch {
            val changed = platformViewModel.toggleCompareShop(shopId)
            if (!changed) {
                snackbarHostState.showSnackbar("You can compare up to 3 shops at once")
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        coffeeViewModel.clearError()
    }

    LaunchedEffect(pendingDeepLink) {
        val deepLink = pendingDeepLink ?: return@LaunchedEffect
        navigateToDeepLink(
            navController = navController,
            targetRoute = deepLink.route,
            targetId = deepLink.targetId
        )
        onConsumeDeepLink()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isMainTabsRoute = navBackStackEntry?.destination?.route == ROUTE_MAIN_TABS

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = EspressoBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = if (isMainTabsRoute) Modifier.padding(bottom = 92.dp) else Modifier
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = RoastedBeanElevated,
                        contentColor = WarmCream,
                        actionColor = CaramelGold
                    )
                }
            }
        ) { _ ->
            NavHost(
                navController = navController,
                startDestination = ROUTE_MAIN_TABS,
                enterTransition = { fadeIn(tween(240)) + slideInHorizontally { it / 10 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally { it / 10 } }
            ) {
                composable(route = ROUTE_MAIN_TABS) {
                    if (selectedTab != MainNavTab.HOME) {
                        BackHandler {
                            selectedTab = MainNavTab.HOME
                        }
                    }
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(160))
                        },
                        label = "mainTabTransition"
                    ) { tab ->
                        val minuteNow = ShopTimeParser.currentDeviceMinuteOfDay()
                        when (tab) {
                            MainNavTab.HOME -> HomeScreen(
                                uiState = uiState,
                                onSelectArea = coffeeViewModel::selectArea,
                                onToggleFavorite = coffeeViewModel::toggleFavorite,
                                onShopClick = { shopId ->
                                    navController.navigate("$ROUTE_SHOP_DETAILS/${Uri.encode(shopId)}")
                                },
                                onNavigateToExplore = { category ->
                                    coffeeViewModel.selectCategory(category)
                                    selectedTab = MainNavTab.EXPLORE
                                },
                                onRequestLocation = requestLocationAndFocusNearMe,
                                isOnline = isOnline,
                                pendingSyncCount = pendingSyncCount,
                                onSyncNow = platformViewModel::triggerOfflineSyncNow,
                                compareCount = compareShopIds.size,
                                comparedIds = comparedIdSet,
                                overlays = shopOverlays,
                                resolveStatus = { item ->
                                    PlatformLogic.resolveShopStatus(
                                        shop = item.shop,
                                        overlay = shopOverlays[item.shop.id],
                                        reports = statusReports,
                                        minuteOfDay = minuteNow
                                    )
                                },
                                resolveCrowd = { shopId ->
                                    PlatformLogic.aggregateCrowdReports(shopId, crowdReports)
                                },
                                onToggleCompare = toggleCompareWithFeedback,
                                onOpenMap = { navController.navigate(ROUTE_NEARBY_MAP) },
                                onOpenOffers = { navController.navigate(ROUTE_OFFERS) },
                                onOpenEvents = { navController.navigate(ROUTE_EVENTS) },
                                onOpenCompare = { navController.navigate(ROUTE_COMPARE) },
                                onOpenReservations = { navController.navigate(ROUTE_RESERVATIONS) },
                                onOpenSupport = { navController.navigate(ROUTE_HELP_SUPPORT) }
                            )

                            MainNavTab.EXPLORE -> ExploreScreen(
                                uiState = uiState,
                                searchInputText = searchInputText,
                                onSearchQueryChange = coffeeViewModel::onSearchQueryChange,
                                onClearSearch = coffeeViewModel::clearSearch,
                                onSelectCategory = coffeeViewModel::selectCategory,
                                onToggleFamilyFriendly = coffeeViewModel::setFamilyFriendlyOnly,
                                onSelectArea = coffeeViewModel::selectArea,
                                onToggleFavorite = coffeeViewModel::toggleFavorite,
                                onResetFilters = coffeeViewModel::resetFilters,
                                onShopClick = { shopId ->
                                    navController.navigate("$ROUTE_SHOP_DETAILS/${Uri.encode(shopId)}")
                                },
                                onNearMeClick = requestLocationAndFocusNearMe,
                                comparedIds = comparedIdSet,
                                overlays = shopOverlays,
                                resolveStatus = { item ->
                                    PlatformLogic.resolveShopStatus(
                                        shop = item.shop,
                                        overlay = shopOverlays[item.shop.id],
                                        reports = statusReports,
                                        minuteOfDay = minuteNow
                                    )
                                },
                                resolveCrowd = { shopId ->
                                    PlatformLogic.aggregateCrowdReports(shopId, crowdReports)
                                },
                                onToggleCompare = toggleCompareWithFeedback,
                                onOpenMap = { navController.navigate(ROUTE_NEARBY_MAP) },
                                onOpenCompare = { navController.navigate(ROUTE_COMPARE) }
                            )

                            MainNavTab.FAVORITES -> FavoritesScreen(
                                uiState = uiState,
                                onToggleFavorite = coffeeViewModel::toggleFavorite,
                                onShopClick = { shopId ->
                                    navController.navigate("$ROUTE_SHOP_DETAILS/${Uri.encode(shopId)}")
                                },
                                onExploreClick = {
                                    coffeeViewModel.selectCategory(ExploreCategory.ALL)
                                    selectedTab = MainNavTab.EXPLORE
                                }
                            )

                            MainNavTab.PROFILE -> ProfileScreen(
                                uiState = uiState,
                                isSignedIn = isSignedIn,
                                displayName = displayName,
                                onAddShopClick = {
                                    if (isSignedIn) {
                                        showAddShopSheet = true
                                    } else {
                                        onRequireSignIn()
                                    }
                                },
                                onEnableLocationClick = requestLocationAndFocusNearMe,
                                onSignInClick = onRequireSignIn,
                                onSignOutClick = {
                                    signOut(
                                        context = context,
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                },
                                userRole = userRole,
                                unreadNotificationsCount = unreadNotificationsCount,
                                compareCount = compareShopIds.size,
                                onOpenReservations = { navController.navigate(ROUTE_RESERVATIONS) },
                                onOpenOffers = { navController.navigate(ROUTE_OFFERS) },
                                onOpenEvents = { navController.navigate(ROUTE_EVENTS) },
                                onOpenCompare = { navController.navigate(ROUTE_COMPARE) },
                                onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) },
                                onOpenSupport = { navController.navigate(ROUTE_HELP_SUPPORT) },
                                onOpenAdminConsole = { navController.navigate(ROUTE_ADMIN_DASHBOARD) }
                            )
                        }
                    }
                }

                composable(
                    route = "$ROUTE_SHOP_DETAILS/{$ARG_SHOP_ID}",
                    arguments = listOf(navArgument(ARG_SHOP_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val rawId = backStackEntry.arguments?.getString(ARG_SHOP_ID).orEmpty()
                    val shopId = Uri.decode(rawId)
                    val detailFlow = remember(shopId, coffeeViewModel) {
                        coffeeViewModel.observeShopDetail(shopId)
                    }
                    val detailState by detailFlow.collectAsStateWithLifecycle(initialValue = null)
                    val shop = detailState?.item?.shop
                    val overlay = shopOverlays[shopId]
                    val minuteNow = ShopTimeParser.currentDeviceMinuteOfDay()
                    val resolvedStatus = remember(shop, overlay, statusReports, minuteNow) {
                        shop?.let {
                            PlatformLogic.resolveShopStatus(
                                shop = it,
                                overlay = overlay,
                                reports = statusReports,
                                minuteOfDay = minuteNow
                            )
                        }
                    }
                    val crowdStatus = remember(shopId, crowdReports) {
                        PlatformLogic.aggregateCrowdReports(shopId, crowdReports)
                    }
                    val latestActiveCrowdId = remember(shopId, crowdReports) {
                        val now = System.currentTimeMillis()
                        crowdReports
                            .filter { r ->
                                r.shopId == shopId &&
                                    r.verificationState != "DISMISSED" &&
                                    (r.expiresAt?.toDate()?.time?.let { it > now } ?: true)
                            }
                            .maxByOrNull { it.createdAt?.toDate()?.time ?: 0L }
                            ?.id
                    }

                    ShopDetailsScreen(
                        detailState = detailState,
                        currentUserId = if (isSignedIn) currentUserId else null,
                        isSignedIn = isSignedIn,
                        isSubmittingAction = uiState.isSubmittingAction,
                        errorMessage = uiState.errorMessage,
                        onBack = { navController.popBackStack() },
                        onToggleFavorite = coffeeViewModel::toggleFavorite,
                        onSubmitReview = { rating, comment ->
                            coffeeViewModel.submitReview(shopId, rating, comment)
                        },
                        onDeleteReview = coffeeViewModel::deleteReview,
                        onClearError = coffeeViewModel::clearError,
                        onRequireSignIn = onRequireSignIn,
                        resolvedStatus = resolvedStatus,
                        isVerifiedBusiness = overlay?.verified == true,
                        crowdStatus = crowdStatus,
                        latestActiveCrowdReportId = latestActiveCrowdId,
                        isCompared = shopId in comparedIdSet,
                        canManageShop = userRole.canManageShop(shopId),
                        isBusy = platformBusy,
                        onOpenDigitalMenu = {
                            navController.navigate("$ROUTE_DIGITAL_MENU/${Uri.encode(shopId)}")
                        },
                        onOpenPhotoReviews = {
                            navController.navigate("$ROUTE_REVIEWS/${Uri.encode(shopId)}")
                        },
                        onBookTable = {
                            navController.navigate("$ROUTE_RESERVATIONS?$ARG_SHOP_ID=${Uri.encode(shopId)}")
                        },
                        onToggleCompare = { toggleCompareWithFeedback(shopId) },
                        onReportIssue = {
                            val name = shop?.name.orEmpty()
                            navController.navigate(
                                "$ROUTE_HELP_SUPPORT?$ARG_SHOP_ID=${Uri.encode(shopId)}&$ARG_SHOP_NAME=${Uri.encode(name)}"
                            )
                        },
                        onSubmitStatusReport = { statusType, note ->
                            platformViewModel.submitStatusReport(shopId, statusType, note)
                        },
                        onOfficialStatusUpdate = { liveStatus ->
                            val current = shop
                            if (current == null) {
                                Result.failure(IllegalStateException("Shop not loaded"))
                            } else {
                                platformViewModel.updateOfficialShopStatus(current, liveStatus)
                            }
                        },
                        onSubmitCrowdReport = { level, note ->
                            platformViewModel.submitCrowdReport(shopId, level, note)
                        },
                        onFlagLatestCrowdReport = { reportId, reason ->
                            platformViewModel.flagCrowdReport(reportId, reason)
                        },
                        onSubmitVerificationRequest = { ownerName, phone, note ->
                            platformViewModel.submitVerificationRequest(
                                shopId = shopId,
                                shopName = shop?.name ?: shopId,
                                businessOwnerName = ownerName,
                                businessPhone = phone,
                                registrationNote = note
                            )
                        }
                    )
                }

                composable(
                    route = "$ROUTE_DIGITAL_MENU/{$ARG_SHOP_ID}",
                    arguments = listOf(navArgument(ARG_SHOP_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val shopId = Uri.decode(backStackEntry.arguments?.getString(ARG_SHOP_ID).orEmpty())
                    val menuPair by remember(shopId, platformViewModel) {
                        platformViewModel.observeMenuForShop(shopId)
                    }.collectAsStateWithLifecycle(initialValue = emptyList<DigitalMenuItemDoc>() to false)

                    DigitalMenuScreen(
                        shop = shopById[shopId],
                        menuItems = menuPair.first,
                        isOfflineCached = menuPair.second || !isOnline,
                        canManage = userRole.canManageShop(shopId),
                        isBusy = platformBusy,
                        onBack = { navController.popBackStack() },
                        onSaveItem = { existingId, category, name, desc, imageUrl, priceBdt, available, special ->
                            platformViewModel.upsertMenuItem(
                                existingId = existingId,
                                shopId = shopId,
                                category = category,
                                name = name,
                                description = desc,
                                imageUrl = imageUrl,
                                priceBdt = priceBdt,
                                available = available,
                                special = special
                            )
                        },
                        onDeleteItem = platformViewModel::deleteMenuItem
                    )
                }

                composable(route = ROUTE_OFFERS) {
                    OffersScreen(
                        coupons = coupons,
                        redemptions = myCouponRedemptions,
                        isOfflineCached = couponsOfflineCached || !isOnline,
                        isSignedIn = isSignedIn,
                        canCreateOffers = userRole.hasAnyManagementAccess,
                        shops = allCatalogShops,
                        isBusy = platformBusy,
                        onBack = { navController.popBackStack() },
                        onRequireSignIn = onRequireSignIn,
                        onSaveCoupon = platformViewModel::saveCoupon,
                        onRedeemCoupon = platformViewModel::redeemCoupon,
                        onCreateCoupon = { shopId, shopName, code, title, desc, type, value, minSpend, maxLimit, terms, validDays ->
                            platformViewModel.createCoupon(
                                shopId = shopId,
                                shopName = shopName,
                                code = code,
                                title = title,
                                description = desc,
                                discountType = type,
                                discountValue = value,
                                minSpendBdt = minSpend,
                                maxUsageLimit = maxLimit,
                                terms = terms,
                                validDays = validDays
                            )
                        },
                        onOpenShop = { shopId ->
                            navController.navigate("$ROUTE_SHOP_DETAILS/${Uri.encode(shopId)}")
                        }
                    )
                }

                composable(route = ROUTE_NEARBY_MAP) {
                    val mapItems = remember(uiState.allCatalogItems, uiState.selectedArea) {
                        if (uiState.selectedArea == com.example.model.DhakaArea.ALL) {
                            uiState.allCatalogItems
                        } else {
                            uiState.allCatalogItems.filter { it.shop.area == uiState.selectedArea }
                        }
                    }
                    NearbyMapScreen(
                        shops = mapItems.ifEmpty { uiState.shops },
                        selectedArea = uiState.selectedArea,
                        hasLocationPermission = uiState.hasLocationPermission,
                        onSelectArea = coffeeViewModel::selectArea,
                        onRequestLocation = requestLocationOnly,
                        onShopClick = { shopId ->
                            navController.navigate("$ROUTE_SHOP_DETAILS/${Uri.encode(shopId)}")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "$ROUTE_REVIEWS/{$ARG_SHOP_ID}",
                    arguments = listOf(navArgument(ARG_SHOP_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val shopId = Uri.decode(backStackEntry.arguments?.getString(ARG_SHOP_ID).orEmpty())
                    val shopReviews = remember(photoReviews, shopId) {
                        photoReviews.filter { it.shopId == shopId }
                    }
                    ReviewsScreen(
                        shop = shopById[shopId],
                        reviews = shopReviews,
                        myVotedReviewIds = myReviewVotes,
                        currentUserId = if (isSignedIn) currentUserId else null,
                        isSignedIn = isSignedIn,
                        isBusy = platformBusy,
                        onBack = { navController.popBackStack() },
                        onRequireSignIn = onRequireSignIn,
                        onSubmitReview = { rating, comment, uris ->
                            platformViewModel.submitPhotoReview(shopId, rating, comment, uris)
                        },
                        onToggleHelpful = platformViewModel::toggleHelpfulVote,
                        onReportReview = platformViewModel::reportReview,
                        onDeleteReview = platformViewModel::deletePhotoReview
                    )
                }

                composable(
                    route = "$ROUTE_RESERVATIONS?$ARG_SHOP_ID={$ARG_SHOP_ID}",
                    arguments = listOf(
                        navArgument(ARG_SHOP_ID) {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val preselectedShopId = Uri.decode(backStackEntry.arguments?.getString(ARG_SHOP_ID).orEmpty())
                    ReservationsScreen(
                        preselectedShop = shopById[preselectedShopId],
                        allShops = allCatalogShops,
                        myReservations = myReservations,
                        manageableReservations = manageableReservations,
                        userRole = userRole,
                        defaultGuestName = displayName.orEmpty(),
                        isSignedIn = isSignedIn,
                        isBusy = platformBusy,
                        onBack = { navController.popBackStack() },
                        onRequireSignIn = onRequireSignIn,
                        onCreateReservation = platformViewModel::createReservation,
                        onCancelReservation = platformViewModel::cancelReservation,
                        onStaffUpdateStatus = platformViewModel::updateReservationStatus
                    )
                }

                composable(route = ROUTE_COMPARE) {
                    val minuteNow = ShopTimeParser.currentDeviceMinuteOfDay()
                    CompareScreen(
                        allItems = uiState.allCatalogItems,
                        compareShopIds = compareShopIds,
                        overlays = shopOverlays,
                        resolveStatus = { item ->
                            PlatformLogic.resolveShopStatus(
                                shop = item.shop,
                                overlay = shopOverlays[item.shop.id],
                                reports = statusReports,
                                minuteOfDay = minuteNow
                            )
                        },
                        onToggleCompare = platformViewModel::toggleCompareShop,
                        onClearAll = platformViewModel::clearCompareShops,
                        onOpenShop = { shopId ->
                            navController.navigate("$ROUTE_SHOP_DETAILS/${Uri.encode(shopId)}")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(route = ROUTE_EVENTS) {
                    EventsScreen(
                        events = events,
                        myRegistrations = myEventRegistrations,
                        isOfflineCached = eventsOfflineCached || !isOnline,
                        isSignedIn = isSignedIn,
                        canCreateEvents = userRole.hasAnyManagementAccess,
                        isBusy = platformBusy,
                        onBack = { navController.popBackStack() },
                        onRequireSignIn = onRequireSignIn,
                        onToggleRegistration = platformViewModel::toggleEventRegistration,
                        onCreateEvent = platformViewModel::createEvent
                    )
                }

                composable(route = ROUTE_NOTIFICATIONS) {
                    NotificationsScreen(
                        notifications = notifications,
                        preferences = userPreferences,
                        isSignedIn = isSignedIn,
                        onBack = { navController.popBackStack() },
                        onSavePreferences = platformViewModel::saveNotificationPreferences,
                        onNotificationClick = { item ->
                            scope.launch {
                                if (!item.read) {
                                    platformViewModel.markNotificationRead(item)
                                }
                            }
                            if (item.targetRoute.isNotBlank()) {
                                navigateToDeepLink(
                                    navController = navController,
                                    targetRoute = item.targetRoute,
                                    targetId = item.targetId
                                )
                            }
                        }
                    )
                }

                composable(
                    route = "$ROUTE_HELP_SUPPORT?$ARG_SHOP_ID={$ARG_SHOP_ID}&$ARG_SHOP_NAME={$ARG_SHOP_NAME}",
                    arguments = listOf(
                        navArgument(ARG_SHOP_ID) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument(ARG_SHOP_NAME) {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val preShopId = Uri.decode(backStackEntry.arguments?.getString(ARG_SHOP_ID).orEmpty())
                        .ifBlank { null }
                    val preShopName = Uri.decode(backStackEntry.arguments?.getString(ARG_SHOP_NAME).orEmpty())
                        .ifBlank { null }
                    HelpSupportScreen(
                        preselectedShopId = preShopId,
                        preselectedShopName = preShopName,
                        myTickets = mySupportTickets,
                        isSignedIn = isSignedIn,
                        isBusy = platformBusy,
                        onBack = { navController.popBackStack() },
                        onRequireSignIn = onRequireSignIn,
                        onSubmitTicket = platformViewModel::submitSupportTicket
                    )
                }

                composable(route = ROUTE_ADMIN_DASHBOARD) {
                    AdminDashboardScreen(
                        userRole = userRole,
                        verificationRequests = adminVerificationRequests,
                        statusReports = statusReports,
                        crowdReports = crowdReports,
                        photoReviews = photoReviews,
                        supportTickets = adminSupportTickets,
                        reservations = manageableReservations,
                        isBusy = platformBusy,
                        onBack = { navController.popBackStack() },
                        onReviewVerification = platformViewModel::reviewVerificationRequest,
                        onVerifyStatusReport = platformViewModel::verifyStatusReport,
                        onDismissCrowdReport = platformViewModel::dismissCrowdReport,
                        onModerateReview = platformViewModel::moderateReview,
                        onUpdateTicket = platformViewModel::updateSupportTicketAdmin,
                        onUpdateReservation = platformViewModel::updateReservationStatus,
                        onSendAnnouncement = platformViewModel::sendPlatformAnnouncement
                    )
                }
            }
        }

        if (isMainTabsRoute) {
            LiquidGlassFloatingBottomBar(
                selectedTab = selectedTab,
                favoritesCount = uiState.favoritesCount,
                onSelectTab = { selectedTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showAddShopSheet) {
        AddShopSheet(
            isSubmitting = uiState.isSubmittingAction,
            onDismiss = { showAddShopSheet = false },
            onSubmit = { input ->
                scope.launch {
                    val result = coffeeViewModel.addShop(input)
                    if (result.isSuccess) {
                        showAddShopSheet = false
                        snackbarHostState.showSnackbar(shopPublishedMessage)
                    }
                }
            }
        )
    }
}

private fun navigateToDeepLink(
    navController: NavHostController,
    targetRoute: String,
    targetId: String
) {
    val cleanRoute = targetRoute.trim()
    val cleanId = targetId.trim()
    when {
        cleanRoute.startsWith("$ROUTE_SHOP_DETAILS/") -> {
            val id = cleanRoute.removePrefix("$ROUTE_SHOP_DETAILS/")
            if (id.isNotBlank()) navController.navigate("$ROUTE_SHOP_DETAILS/${Uri.encode(id)}")
        }
        cleanRoute == ROUTE_SHOP_DETAILS && cleanId.isNotBlank() -> {
            navController.navigate("$ROUTE_SHOP_DETAILS/${Uri.encode(cleanId)}")
        }
        cleanRoute == ROUTE_DIGITAL_MENU && cleanId.isNotBlank() -> {
            navController.navigate("$ROUTE_DIGITAL_MENU/${Uri.encode(cleanId)}")
        }
        cleanRoute == ROUTE_REVIEWS && cleanId.isNotBlank() -> {
            navController.navigate("$ROUTE_REVIEWS/${Uri.encode(cleanId)}")
        }
        cleanRoute == ROUTE_RESERVATIONS -> {
            val dest = if (cleanId.isNotBlank()) "$ROUTE_RESERVATIONS?$ARG_SHOP_ID=${Uri.encode(cleanId)}" else ROUTE_RESERVATIONS
            navController.navigate(dest)
        }
        cleanRoute == ROUTE_OFFERS -> navController.navigate(ROUTE_OFFERS)
        cleanRoute == ROUTE_EVENTS -> navController.navigate(ROUTE_EVENTS)
        cleanRoute == ROUTE_NEARBY_MAP -> navController.navigate(ROUTE_NEARBY_MAP)
        cleanRoute == ROUTE_COMPARE -> navController.navigate(ROUTE_COMPARE)
        cleanRoute == ROUTE_NOTIFICATIONS -> navController.navigate(ROUTE_NOTIFICATIONS)
        cleanRoute == ROUTE_HELP_SUPPORT -> navController.navigate(ROUTE_HELP_SUPPORT)
        cleanRoute == ROUTE_ADMIN_DASHBOARD -> navController.navigate(ROUTE_ADMIN_DASHBOARD)
    }
}
