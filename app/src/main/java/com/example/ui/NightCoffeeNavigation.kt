package com.example.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ShopDetailsScreen
import com.example.ui.screens.signOut
import com.example.viewmodel.CoffeeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val ROUTE_HOME = "home"
private const val ROUTE_SHOP_DETAILS = "shop_details"
private const val ARG_SHOP_ID = "shopId"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun NightCoffeeNavigation() {
    val auth = remember { Firebase.auth }
    val currentUser by remember(auth) { auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedNightCoffeeApp(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedNightCoffeeApp(
    currentUserId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val coffeeViewModel: CoffeeViewModel = viewModel(
        key = currentUserId,
        factory = CoffeeViewModel.factory(context)
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, coffeeViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coffeeViewModel.refreshTime()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val uiState by coffeeViewModel.uiState.collectAsStateWithLifecycle()
    val searchInputText by coffeeViewModel.searchInputText.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME
    ) {
        composable(route = ROUTE_HOME) {
            HomeScreen(
                uiState = uiState,
                searchInputText = searchInputText,
                onSearchQueryChange = coffeeViewModel::onSearchQueryChange,
                onClearSearch = coffeeViewModel::clearSearch,
                onSelectTab = coffeeViewModel::selectTab,
                onToggleOpenNow = coffeeViewModel::setOpenNowOnly,
                onToggleFamilyFriendly = coffeeViewModel::setFamilyFriendlyOnly,
                onSelectArea = coffeeViewModel::selectArea,
                onToggleFavorite = coffeeViewModel::toggleFavorite,
                onResetFilters = coffeeViewModel::resetFilters,
                onShopClick = { shopId ->
                    navController.navigate("$ROUTE_SHOP_DETAILS/${Uri.encode(shopId)}")
                },
                onAddShop = coffeeViewModel::addShop,
                onClearError = coffeeViewModel::clearError,
                onSignOutClick = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
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

            ShopDetailsScreen(
                detailState = detailState,
                currentUserId = currentUserId,
                isSubmittingAction = uiState.isSubmittingAction,
                errorMessage = uiState.errorMessage,
                onBack = { navController.popBackStack() },
                onToggleFavorite = coffeeViewModel::toggleFavorite,
                onSubmitReview = { rating, comment ->
                    coffeeViewModel.submitReview(shopId, rating, comment)
                },
                onDeleteReview = coffeeViewModel::deleteReview,
                onClearError = coffeeViewModel::clearError
            )
        }
    }
}
