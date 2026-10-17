package com.kungal.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kungal.app.KungalApp
import com.kungal.app.ui.login.LoginScreen
import com.kungal.app.ui.topic.TopicDetailScreen
import com.kungal.app.ui.topic.TopicListScreen

object Routes {
    const val HOME = "home"
    const val LOGIN = "login"
    const val TOPIC = "topic/{topicId}"
    fun topic(id: String) = "topic/$id"
}

@Composable
fun AppNav(app: KungalApp) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            TopicListScreen(
                app = app,
                onOpenTopic = { id -> nav.navigate(Routes.topic(id)) },
            )
        }
        composable(
            route = Routes.TOPIC,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType }),
        ) { entry ->
            val topicId = entry.arguments?.getString("topicId").orEmpty()
            TopicDetailScreen(
                app = app,
                topicId = topicId,
                onBack = { nav.popBackStack() },
                onRequireLogin = { nav.navigate(Routes.LOGIN) },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                app = app,
                onDone = { nav.popBackStack() },
                onCancel = { nav.popBackStack() },
            )
        }
    }
}
