package com.example.sellerhelperebay.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.sellerhelperebay.ui.ebay.EbayAuthScreen
import com.example.sellerhelperebay.ui.ebay.EbayPushScreen
import com.example.sellerhelperebay.ui.entrydetail.EntryDetailScreen
import com.example.sellerhelperebay.ui.entrylist.EntryListScreen
import com.example.sellerhelperebay.ui.review.MismatchReviewScreen
import com.example.sellerhelperebay.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object EntryListRoute

@Serializable
data class EntryDetailRoute(val entryId: Long)

@Serializable
data class MismatchReviewRoute(val entryId: Long)

@Serializable
data class EbayPushRoute(val entryId: Long)

@Serializable
object SettingsRoute

@Serializable
object EbayAuthRoute

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = EntryListRoute) {
        composable<EntryListRoute> {
            EntryListScreen(
                onEntryClick = { id -> navController.navigate(EntryDetailRoute(id)) },
                onSettings = { navController.navigate(SettingsRoute) }
            )
        }
        composable<EntryDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EntryDetailRoute>()
            EntryDetailScreen(
                entryId = route.entryId,
                onBack = { navController.popBackStack() },
                onReviewPhotos = { navController.navigate(MismatchReviewRoute(route.entryId)) },
                onPushToEbay = { navController.navigate(EbayPushRoute(route.entryId)) }
            )
        }
        composable<MismatchReviewRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<MismatchReviewRoute>()
            MismatchReviewScreen(
                entryId = route.entryId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<EbayPushRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EbayPushRoute>()
            EbayPushScreen(
                entryId = route.entryId,
                onBack = { navController.popBackStack() },
                onConnect = { navController.navigate(EbayAuthRoute) }
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onConnect = { navController.navigate(EbayAuthRoute) }
            )
        }
        composable<EbayAuthRoute> {
            EbayAuthScreen(onDone = { navController.popBackStack() })
        }
    }
}
