package com.music.sttnotes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.music.sttnotes.ui.screens.chat.ChatListScreen
import com.music.sttnotes.ui.screens.chat.ChatScreen
import com.music.sttnotes.ui.screens.chat.TagManagementScreen
import com.music.sttnotes.ui.screens.home.DashboardScreen
import com.music.sttnotes.ui.screens.knowledgebase.KnowledgeBaseDetailScreen
import com.music.sttnotes.ui.screens.knowledgebase.KnowledgeBaseFolderScreen
import com.music.sttnotes.ui.screens.knowledgebase.KnowledgeBaseScreen
import com.music.sttnotes.ui.screens.notes.NoteEditorScreen
import com.music.sttnotes.ui.screens.notes.NotesListScreen
import com.music.sttnotes.ui.screens.notes.TagManagementScreenForNotes
import com.music.sttnotes.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object NotesList : Screen("notes_list")
    data object NoteEditor : Screen("note_editor/{noteId}?autoRecord={autoRecord}") {
        fun createRoute(noteId: String?, autoRecord: Boolean = false) =
            "note_editor/${noteId ?: "new"}?autoRecord=$autoRecord"
    }
    data object Settings : Screen("settings")
    data object ChatList : Screen("chat_list")
    data object Chat : Screen("chat/{conversationId}?startRecording={startRecording}") {
        fun createRoute(conversationId: String? = null, startRecording: Boolean = false) =
            "chat/${conversationId ?: "new"}?startRecording=$startRecording"
    }
    data object TagManagement : Screen("tag_management/{conversationId}") {
        fun createRoute(conversationId: String) = "tag_management/$conversationId"
    }
    data object TagManagementNote : Screen("tag_management_note/{noteId}") {
        fun createRoute(noteId: String) = "tag_management_note/$noteId"
    }
    data object KnowledgeBase : Screen("knowledge_base")
    data object KnowledgeBaseFolder : Screen("knowledge_base/folder/{folderName}") {
        fun createRoute(folderName: String) = "knowledge_base/folder/$folderName"
    }
    data object KnowledgeBaseDetail : Screen("knowledge_base/{folder}/{filename}") {
        fun createRoute(folder: String, filename: String) =
            "knowledge_base/$folder/$filename"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        // Dashboard - new home screen
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNotesClick = { navController.navigate(Screen.NotesList.route) },
                onNewNote = { navController.navigate(Screen.NoteEditor.createRoute(null)) },
                onNewNoteWithRecording = { navController.navigate(Screen.NoteEditor.createRoute(null, autoRecord = true)) },
                onConversationsClick = { navController.navigate(Screen.ChatList.route) },
                onNewConversation = { navController.navigate(Screen.Chat.createRoute(null)) },
                onNewConversationWithRecording = { navController.navigate(Screen.Chat.createRoute(null, startRecording = true)) },
                onKnowledgeBaseClick = { navController.navigate(Screen.KnowledgeBase.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onNoteClick = { noteId -> navController.navigate(Screen.NoteEditor.createRoute(noteId)) },
                onConversationClick = { convId -> navController.navigate(Screen.Chat.createRoute(conversationId = convId)) },
                onKbFileClick = { folder, filename -> navController.navigate(Screen.KnowledgeBaseDetail.createRoute(folder, filename)) }
            )
        }

        // Notes list
        composable(Screen.NotesList.route) {
            NotesListScreen(
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteEditor.createRoute(noteId))
                },
                onAddNote = {
                    navController.navigate(Screen.NoteEditor.createRoute(null))
                },
                onNavigateBack = { navController.popBackStack() },
                onManageTags = { noteId ->
                    navController.navigate(Screen.TagManagementNote.createRoute(noteId))
                }
            )
        }

        // Chat list screen
        composable(Screen.ChatList.route) {
            ChatListScreen(
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId = conversationId))
                },
                onNewConversation = {
                    navController.navigate(Screen.Chat.createRoute(conversationId = null))
                },
                onNavigateBack = { navController.popBackStack() },
                onManageTags = { conversationId ->
                    navController.navigate(Screen.TagManagement.createRoute(conversationId))
                }
            )
        }

        // Chat screen with conversation support
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                    defaultValue = "new"
                },
                navArgument("startRecording") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            val startRecording = backStackEntry.arguments?.getBoolean("startRecording") ?: false
            ChatScreen(
                conversationId = if (conversationId == "new") null else conversationId,
                startRecording = startRecording,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Tag Management
        composable(
            route = Screen.TagManagement.route,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            TagManagementScreen(
                conversationId = conversationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    defaultValue = "new"
                },
                navArgument("autoRecord") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            val autoRecord = backStackEntry.arguments?.getBoolean("autoRecord") ?: false
            NoteEditorScreen(
                noteId = if (noteId == "new") null else noteId,
                autoRecord = autoRecord,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Tag Management for Notes
        composable(
            route = Screen.TagManagementNote.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
            TagManagementScreenForNotes(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Knowledge Base screens
        composable(Screen.KnowledgeBase.route) {
            KnowledgeBaseScreen(
                onFolderClick = { folderName ->
                    navController.navigate(Screen.KnowledgeBaseFolder.createRoute(folderName))
                },
                onNavigateBack = { navController.popBackStack() },
                onNewNote = { navController.navigate(Screen.NoteEditor.createRoute(null)) },
                onNewNoteWithRecording = { navController.navigate(Screen.NoteEditor.createRoute(null, autoRecord = true)) },
                onNewChat = { navController.navigate(Screen.Chat.createRoute(null)) },
                onNewChatWithRecording = { navController.navigate(Screen.Chat.createRoute(null, startRecording = true)) }
            )
        }

        composable(
            route = Screen.KnowledgeBaseFolder.route,
            arguments = listOf(
                navArgument("folderName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName") ?: ""
            KnowledgeBaseFolderScreen(
                folderName = folderName,
                onFileClick = { filename ->
                    navController.navigate(Screen.KnowledgeBaseDetail.createRoute(folderName, filename))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.KnowledgeBaseDetail.route,
            arguments = listOf(
                navArgument("folder") { type = NavType.StringType },
                navArgument("filename") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folder = backStackEntry.arguments?.getString("folder") ?: ""
            val filename = backStackEntry.arguments?.getString("filename") ?: ""
            KnowledgeBaseDetailScreen(
                folder = folder,
                filename = filename,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
