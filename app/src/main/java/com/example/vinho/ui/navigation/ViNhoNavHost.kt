package com.example.vinho.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vinho.R
import com.example.vinho.ui.budget.BudgetScreen
import com.example.vinho.ui.form.TransactionFormScreen
import com.example.vinho.ui.login.LoginScreen
import com.example.vinho.ui.overview.OverviewScreen
import com.example.vinho.ui.settings.SettingsScreen
import com.example.vinho.ui.stats.StatsScreen
import com.example.vinho.ui.transactions.TransactionsScreen
import kotlin.reflect.KClass

/**
 * NavHost gốc: Splash → (Đăng nhập | Vỏ chính) → Form giao dịch.
 *
 * Việc điều hướng theo trạng thái đăng nhập nằm ở đây thay vì trong từng màn hình,
 * nhờ vậy đăng xuất ở bất kỳ đâu cũng đưa người dùng về màn Đăng nhập và xoá sạch back stack.
 */
@Composable
fun ViNhoRoot(authViewModel: AuthGateViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthGateViewModel.State.Loading -> Unit
            is AuthGateViewModel.State.SignedOut -> navController.navigateClearingBackStack(LoginRoute)
            is AuthGateViewModel.State.SignedIn -> navController.navigateClearingBackStack(MainRoute)
        }
    }

    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
    ) {
        composable<SplashRoute> { SplashScreen() }

        composable<LoginRoute> {
            LoginScreen()
        }

        composable<MainRoute> {
            MainScreen(
                onAddTransaction = { navController.navigate(TransactionFormRoute()) },
                onEditTransaction = { id -> navController.navigate(TransactionFormRoute(id)) },
            )
        }

        composable<TransactionFormRoute>(
            enterTransition = { slideInHorizontally(tween(260)) { it / 3 } + fadeIn(tween(260)) },
            exitTransition = { slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(220)) },
        ) {
            TransactionFormScreen(onDone = { navController.popBackStack() })
        }
    }
}

private fun NavHostController.navigateClearingBackStack(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            CircularProgressIndicator(Modifier.padding(top = 12.dp))
        }
    }
}

private data class TabItem(
    val route: Any,
    val routeClass: KClass<*>,
    val labelRes: Int,
    val icon: ImageVector,
)

private val TABS = listOf(
    TabItem(OverviewRoute, OverviewRoute::class, R.string.nav_overview, Icons.Filled.Home),
    TabItem(TransactionsRoute, TransactionsRoute::class, R.string.nav_transactions, Icons.AutoMirrored.Filled.List),
    TabItem(BudgetRoute, BudgetRoute::class, R.string.nav_budget, Icons.Filled.Savings),
    TabItem(StatsRoute, StatsRoute::class, R.string.nav_stats, Icons.Filled.BarChart),
    TabItem(SettingsRoute, SettingsRoute::class, R.string.nav_settings, Icons.Filled.Settings),
)

/**
 * Vỏ chính: bottom bar + NavHost con cho 5 tab.
 *
 * Vì sao tách NavHost con: form giao dịch cần che kín bottom bar, còn 5 tab thì dùng chung
 * một bottom bar. Hai NavHost lồng nhau giữ mỗi tầng đúng một trách nhiệm.
 */
@Composable
private fun MainScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute.isRouteOf(tab.routeClass),
                        onClick = {
                            if (!currentRoute.isRouteOf(tab.routeClass)) {
                                tabNavController.navigate(tab.route) {
                                    popUpTo(tabNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = tabNavController,
            startDestination = OverviewRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<OverviewRoute> {
                OverviewScreen(
                    onAddTransaction = onAddTransaction,
                    onTransactionClick = onEditTransaction,
                    onSeeAllTransactions = {
                        tabNavController.navigate(TransactionsRoute) {
                            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenBudget = {
                        tabNavController.navigate(BudgetRoute) {
                            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable<TransactionsRoute> {
                TransactionsScreen(
                    onAddTransaction = onAddTransaction,
                    onTransactionClick = onEditTransaction,
                )
            }
            composable<BudgetRoute> {
                BudgetScreen()
            }
            composable<StatsRoute> {
                StatsScreen()
            }
            composable<SettingsRoute> {
                SettingsScreen()
            }
        }
    }
}

/**
 * So khớp route hiện tại với route type-safe.
 * Navigation sinh route dạng chuỗi bằng qualifiedName của @Serializable object,
 * nên so sánh này ổn định cho các tab (không có tham số).
 */
private fun String?.isRouteOf(routeClass: KClass<*>): Boolean =
    this?.substringBefore('?') == routeClass.qualifiedName
