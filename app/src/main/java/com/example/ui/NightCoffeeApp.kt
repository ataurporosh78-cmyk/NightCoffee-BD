package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object NightCoffeeRoutes {
    const val HOME = "home"
    const val SHOP_DETAILS = "shop_details/{shopId}"

    fun shopDetailsRoute(shopId: String): String = "shop_details/$shopId"
}

@Composable
fun NightCoffeeApp(
    modifier: Modifier = Modifier,
    viewModel: NightCoffeeViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = NightCoffeeRoutes.HOME,
        modifier = modifier
    ) {
        composable(route = NightCoffeeRoutes.HOME) {
            HomeScreen(
                uiState = uiState,
                onToggleOpenNow = viewModel::setOpenNowOnly,
                onSelectArea = viewModel::selectArea,
                onResetFilters = viewModel::resetFilters,
                onShopClick = { shopId ->
                    navController.navigate(NightCoffeeRoutes.shopDetailsRoute(shopId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = NightCoffeeRoutes.SHOP_DETAILS,
            arguments = listOf(navArgument("shopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
            val shopItem = viewModel.getShopUiItem(shopId)
            if (shopItem != null) {
                ShopDetailsScreen(
                    shopItem = shopItem,
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}
