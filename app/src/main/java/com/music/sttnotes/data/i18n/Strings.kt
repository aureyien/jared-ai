package com.music.sttnotes.data.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    FRENCH("fr", "Français")
}

/**
 * Simple i18n system for the app
 */
object Strings {
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    // Get current strings based on language
    val current: StringResources
        get() = when (_currentLanguage.value) {
            AppLanguage.ENGLISH -> EnglishStrings
            AppLanguage.FRENCH -> FrenchStrings
        }
}

/**
 * All string resources for the app
 */
interface StringResources {
    // Common
    val cancel: String
    val save: String
    val delete: String
    val rename: String
    val search: String
    val back: String
    val settings: String
    val archive: String
    val restore: String
    val export: String
    val copy: String
    val clear: String
    val ok: String
    val none: String
    val yes: String
    val no: String

    // Dashboard
    val dashboardTitle: String
    val notes: String
    val chat: String
    val knowledgeBase: String
    val newNote: String
    val newChat: String
    val searchPlaceholder: String
    val noResults: String

    // Notes
    val notesTitle: String
    val noNotes: String
    val createFirstNote: String
    val untitled: String
    val noteDeleted: String
    val noteArchived: String
    val noArchivedNotes: String
    val archivedNotesAppearHere: String
    val notePermanentlyDeleted: String
    val deletePermantently: String
    val filterByTags: String

    // Chat
    val chatTitle: String
    val newConversation: String
    val noConversations: String
    val startNewConversation: String
    val conversationDeleted: String
    val renameConversation: String
    val newTitle: String
    val thinking: String
    val recording: String
    val transcribing: String
    val conversationCleared: String
    val startConversation: String
    val typeOrDictate: String
    val micPermissionRequired: String
    val emptyTranscription: String
    val transcriptionError: String
    val llmError: String
    val saveError: String
    val llmNotConfigured: String
    val apiKeyMissing: String
    val archiveChat: String
    val unarchiveChat: String
    val archivedChats: String
    val chatArchived: String
    val chatUnarchived: String

    // Chat - Save dialog
    val saveResponse: String
    val filename: String
    val folder: String
    val newFolder: String
    val newFolderName: String
    val saved: String

    // Knowledge Base
    val kbTitle: String
    val noSavedFiles: String
    val saveFromChat: String
    val folderDeleted: String
    val fileDeleted: String
    val deleteFolder: String
    val tryAnotherSearch: String
    val file: String
    val files: String

    // Settings
    val settingsTitle: String
    val transcriptionStt: String
    val transcriptionLanguage: String
    val chatFontSize: String
    val llmProcessing: String
    val apiKeys: String
    val groqApiKey: String
    val openaiApiKey: String
    val xaiApiKey: String
    val anthropicApiKey: String
    val adminKeysForUsageTracking: String
    val openaiAdminKey: String
    val anthropicAdminKey: String
    val freeConsole: String
    val usageStatistics: String
    val last7Days: String
    val last30Days: String
    val refreshUsage: String
    val requiresAdminKey: String
    val llmSystemPrompt: String
    val instructionsForLlm: String
    val resetToDefault: String
    val about: String
    val aboutText: String
    val disabled: String
    val rawTranscription: String
    val freeVeryFast: String
    val preview: String
    val active: String
    val configured: String
    val show: String
    val hide: String
    val appLanguage: String

    // Relative time
    val now: String
    val minutesAgo: String
    val hoursAgo: String
    val yesterday: String
    val daysAgo: String

    // Preview messages
    val previewUserMessage: String
    val previewAssistantMessage: String

    // Note Editor
    val newNoteTitle: String
    val editNote: String
    val title: String
    val addTag: String
    val existingTags: String
    val tags: String
    val startWritingOrRecord: String
    val tapToRecord: String
    val initializing: String
    val dismiss: String
    val edit: String

    // Knowledge Base - additional
    val loading: String
    val emptyFolder: String
    val deleteFile: String
    val copyContent: String

    // Notes List - additional
    val deleteAllArchived: String
    val viewArchives: String
    val gridView: String
    val listView: String
    val note: String
    val exportNote: String
    val message: String
    val messages: String

    // Chat Tags
    val manageTags: String
    val addTagToConversation: String
    val searchTags: String
    val allTags: String
    val selectedTags: String
    val selected: String
    val deleteTag: String
    val deleteTagConfirmation: String
    val deleteTagWarning: String
    val removeTagFromAll: String
    val removeTagFromAllConfirmation: String
    val confirm: String

    // KB Folder
    val renameFolder: String

    // Favorites
    val favorites: String
    val allFavorites: String
    val filterNotes: String
    val filterChat: String
    val filterKb: String
    val noFavoritesYet: String
    val addToFavorites: String
    val removeFromFavorites: String

    // KB Merge
    val merge: String
    val mergeFiles: String
    val mergeFilesConfirmation: String
    val newFilename: String
    val selectFile: String
    val selectToMerge: String

    // Chat Summary
    val summarize: String
    val conversationSummary: String
    val saveToKb: String
    val folderName: String
    val fileName: String
    val close: String

    // Share feature
    val shareArticle: String
    val shareUrl: String
    val sharing: String
    val shareFeature: String
    val enableSharing: String
    val shareApiToken: String
    val expirationDays: String
    val expiresOn: String
    val hiddenFeatureHint: String

    // Whisper Model Download
    val whisperModels: String
    val downloadModel: String
    val deleteModel: String
    val modelDownloaded: String
    val modelNotDownloaded: String
    val downloading: String
    val selectModel: String
    val modelSelected: String
    val download: String

    // Volume Button Scrolling
    val volumeScrollTitle: String
    val volumeScrollEnable: String
    val volumeScrollDescription: String
    val volumeScrollDistance: String
}

object EnglishStrings : StringResources {
    // Common
    override val cancel = "Cancel"
    override val save = "Save"
    override val delete = "Delete"
    override val rename = "Rename"
    override val search = "Search"
    override val back = "Back"
    override val settings = "Settings"
    override val archive = "Archive"
    override val restore = "Restore"
    override val export = "Export / Share"
    override val copy = "Copy"
    override val clear = "Clear"
    override val ok = "OK"
    override val none = "None"
    override val yes = "Yes"
    override val no = "No"

    // Dashboard
    override val dashboardTitle = "Dashboard"
    override val notes = "Notes"
    override val chat = "Chat"
    override val knowledgeBase = "KB"
    override val newNote = "New Note"
    override val newChat = "New Chat"
    override val searchPlaceholder = "Search..."
    override val noResults = "No results"

    // Notes
    override val notesTitle = "Notes"
    override val noNotes = "No notes yet"
    override val createFirstNote = "Create your first note"
    override val untitled = "Untitled"
    override val noteDeleted = "Note deleted"
    override val noteArchived = "Note archived"
    override val noArchivedNotes = "No archived notes"
    override val archivedNotesAppearHere = "Archived notes will appear here"
    override val notePermanentlyDeleted = "Note permanently deleted"
    override val deletePermantently = "Delete permanently"
    override val filterByTags = "Filter by tags"

    // Chat
    override val chatTitle = "Chat"
    override val newConversation = "New conversation"
    override val noConversations = "No conversations"
    override val startNewConversation = "Start a new conversation with the AI"
    override val conversationDeleted = "Conversation deleted"
    override val renameConversation = "Rename conversation"
    override val newTitle = "New title"
    override val thinking = "Thinking..."
    override val recording = "Recording..."
    override val transcribing = "Transcribing..."
    override val conversationCleared = "Conversation cleared"
    override val startConversation = "Start a conversation"
    override val typeOrDictate = "Type a message or use the mic to dictate"
    override val micPermissionRequired = "Microphone permission required"
    override val emptyTranscription = "Empty transcription"
    override val transcriptionError = "Transcription error"
    override val llmError = "LLM error"
    override val saveError = "Save error"
    override val llmNotConfigured = "LLM not configured. Go to Settings."
    override val apiKeyMissing = "API key missing. Configure it in Settings."
    override val archiveChat = "Archive"
    override val unarchiveChat = "Unarchive"
    override val archivedChats = "Archived Chats"
    override val chatArchived = "Chat archived"
    override val chatUnarchived = "Chat unarchived"

    // Chat - Save dialog
    override val saveResponse = "Save response"
    override val filename = "Filename"
    override val folder = "Folder"
    override val newFolder = "+ New"
    override val newFolderName = "New folder name"
    override val saved = "Saved"

    // Knowledge Base
    override val kbTitle = "Knowledge Base"
    override val noSavedFiles = "No saved files"
    override val saveFromChat = "Save AI responses from Chat"
    override val folderDeleted = "Folder deleted"
    override val fileDeleted = "File deleted"
    override val deleteFolder = "Delete folder"
    override val tryAnotherSearch = "Try another search"
    override val file = "file"
    override val files = "files"

    // Settings
    override val settingsTitle = "Settings"
    override val transcriptionStt = "Transcription (STT)"
    override val transcriptionLanguage = "Transcription language"
    override val chatFontSize = "Chat font size"
    override val llmProcessing = "LLM Processing"
    override val apiKeys = "API Keys"
    override val groqApiKey = "Groq API Key"
    override val openaiApiKey = "OpenAI API Key"
    override val xaiApiKey = "xAI API Key"
    override val anthropicApiKey = "Anthropic API Key"
    override val adminKeysForUsageTracking = "Admin Keys (Usage Tracking)"
    override val openaiAdminKey = "OpenAI Admin Key (Billing API)"
    override val anthropicAdminKey = "Anthropic Admin Key (sk-ant-admin...)"
    override val freeConsole = "Get API key: console.groq.com"
    override val usageStatistics = "Usage Statistics"
    override val last7Days = "Last 7 days"
    override val last30Days = "Last 30 days"
    override val refreshUsage = "Refresh Usage"
    override val requiresAdminKey = "⚠️ Requires Admin API key (sk-ant-admin...)"
    override val llmSystemPrompt = "LLM System Prompt"
    override val instructionsForLlm = "Instructions for the LLM"
    override val resetToDefault = "Reset to default"
    override val about = "About"
    override val aboutText = """
        • Local: Whisper.cpp (offline, model included)
        • Groq: Whisper v3 Turbo
        • LLM: Formats and enhances transcriptions
    """.trimIndent()
    override val disabled = "Disabled"
    override val rawTranscription = "Raw transcription"
    override val freeVeryFast = "Free, very fast"
    override val preview = "Preview"
    override val active = "active"
    override val configured = "configured"
    override val show = "Show"
    override val hide = "Hide"
    override val appLanguage = "App language"

    // Relative time
    override val now = "now"
    override val minutesAgo = "min ago"
    override val hoursAgo = "h ago"
    override val yesterday = "yesterday"
    override val daysAgo = "d ago"

    // Preview messages
    override val previewUserMessage = "Hello, how are you?"
    override val previewAssistantMessage = "I'm doing great, thanks!"

    // Note Editor
    override val newNoteTitle = "New Note"
    override val editNote = "Edit Note"
    override val title = "Title"
    override val addTag = "Add tag..."
    override val existingTags = "Existing tags:"
    override val tags = "Tags"
    override val startWritingOrRecord = "Start writing or record voice..."
    override val tapToRecord = "Tap to record"
    override val initializing = "Initializing..."
    override val dismiss = "Dismiss"
    override val edit = "Edit"

    // Knowledge Base - additional
    override val loading = "Loading..."
    override val emptyFolder = "Empty folder"
    override val deleteFile = "Delete file"
    override val copyContent = "Copy content"

    // Notes List - additional
    override val deleteAllArchived = "Delete all archived"
    override val viewArchives = "View archives"
    override val gridView = "Grid view"
    override val listView = "List view"
    override val note = "Note"
    override val exportNote = "Export Note"
    override val message = "message"
    override val messages = "messages"

    // Chat Tags
    override val manageTags = "Manage tags"
    override val addTagToConversation = "Add tag..."
    override val searchTags = "Search tags..."
    override val allTags = "All tags"
    override val selectedTags = "Selected tags"
    override val selected = "Selected"
    override val deleteTag = "Delete tag"
    override val deleteTagConfirmation = "Are you sure you want to delete the tag"
    override val deleteTagWarning = "This will remove the tag from all conversations."
    override val removeTagFromAll = "Remove from all files"
    override val removeTagFromAllConfirmation = "Remove this tag from all files?"
    override val confirm = "Confirm"

    // KB Folder
    override val renameFolder = "Rename folder"

    // Favorites
    override val favorites = "Favorites"
    override val allFavorites = "All"
    override val filterNotes = "Notes"
    override val filterChat = "Chat"
    override val filterKb = "KB"
    override val noFavoritesYet = "No favorites yet"
    override val addToFavorites = "Add to favorites"
    override val removeFromFavorites = "Remove from favorites"

    // KB Merge
    override val merge = "Merge"
    override val mergeFiles = "Merge files"
    override val mergeFilesConfirmation = "Merge"
    override val newFilename = "New filename"
    override val selectFile = "Select"
    override val selectToMerge = "Select to merge"

    // Chat Summary
    override val summarize = "Summarize"
    override val conversationSummary = "Conversation Summary"
    override val saveToKb = "Save to KB"
    override val folderName = "Folder name"
    override val fileName = "File name"
    override val close = "Close"

    // Share feature
    override val shareArticle = "Share Article"
    override val shareUrl = "Share URL"
    override val sharing = "Sharing"
    override val shareFeature = "Share Feature"
    override val enableSharing = "Enable Sharing"
    override val shareApiToken = "Share API Token"
    override val expirationDays = "Expiration (days)"
    override val expiresOn = "Expires on"
    override val hiddenFeatureHint = "Long-press section titles to unlock hidden features"

    // Whisper Model Download
    override val whisperModels = "Whisper Models"
    override val downloadModel = "Download Model"
    override val deleteModel = "Delete Model"
    override val modelDownloaded = "Downloaded"
    override val modelNotDownloaded = "Not Downloaded"
    override val downloading = "Downloading"
    override val selectModel = "Select Model"
    override val modelSelected = "Selected"
    override val download = "Download"

    // Volume Button Scrolling
    override val volumeScrollTitle = "Volume Button Scrolling (E-Ink)"
    override val volumeScrollEnable = "Enable volume button scrolling"
    override val volumeScrollDescription = "Optimized for e-ink: instant jumps, no ghosting"
    override val volumeScrollDistance = "Scroll distance (100% = full screen, 50% = half screen with 50% overlap)"
}

object FrenchStrings : StringResources {
    // Common
    override val cancel = "Annuler"
    override val save = "Sauvegarder"
    override val delete = "Supprimer"
    override val rename = "Renommer"
    override val search = "Rechercher"
    override val back = "Retour"
    override val settings = "Paramètres"
    override val archive = "Archiver"
    override val restore = "Restaurer"
    override val export = "Exporter / Partager"
    override val copy = "Copier"
    override val clear = "Effacer"
    override val ok = "OK"
    override val none = "Aucun"
    override val yes = "Oui"
    override val no = "Non"

    // Dashboard
    override val dashboardTitle = "Accueil"
    override val notes = "Notes"
    override val chat = "Chat"
    override val knowledgeBase = "KB"
    override val newNote = "Nouvelle note"
    override val newChat = "Nouveau chat"
    override val searchPlaceholder = "Rechercher..."
    override val noResults = "Aucun résultat"

    // Notes
    override val notesTitle = "Notes"
    override val noNotes = "Aucune note"
    override val createFirstNote = "Créez votre première note"
    override val untitled = "Sans titre"
    override val noteDeleted = "Note supprimée"
    override val noteArchived = "Note archivée"
    override val noArchivedNotes = "Aucune note archivée"
    override val archivedNotesAppearHere = "Les notes archivées apparaîtront ici"
    override val notePermanentlyDeleted = "Note supprimée définitivement"
    override val deletePermantently = "Supprimer définitivement"
    override val filterByTags = "Filtrer par tags"

    // Chat
    override val chatTitle = "Chat"
    override val newConversation = "Nouvelle conversation"
    override val noConversations = "Aucune conversation"
    override val startNewConversation = "Commencez une nouvelle discussion avec l'IA"
    override val conversationDeleted = "Conversation supprimée"
    override val renameConversation = "Renommer la conversation"
    override val newTitle = "Nouveau titre"
    override val thinking = "Réflexion..."
    override val recording = "Enregistrement en cours..."
    override val transcribing = "Transcription en cours..."
    override val conversationCleared = "Conversation effacée"
    override val startConversation = "Commencez une conversation"
    override val typeOrDictate = "Tapez un message ou utilisez le micro pour dicter"
    override val micPermissionRequired = "Permission micro requise"
    override val emptyTranscription = "Transcription vide"
    override val transcriptionError = "Erreur de transcription"
    override val llmError = "Erreur LLM"
    override val saveError = "Erreur de sauvegarde"
    override val llmNotConfigured = "LLM non configuré. Allez dans Paramètres."
    override val apiKeyMissing = "Clé API manquante. Configurez-la dans Paramètres."
    override val archiveChat = "Archiver"
    override val unarchiveChat = "Désarchiver"
    override val archivedChats = "Chats archivés"
    override val chatArchived = "Chat archivé"
    override val chatUnarchived = "Chat désarchivé"

    // Chat - Save dialog
    override val saveResponse = "Sauvegarder la réponse"
    override val filename = "Nom du fichier"
    override val folder = "Dossier"
    override val newFolder = "+ Nouveau"
    override val newFolderName = "Nom du nouveau dossier"
    override val saved = "Sauvegardé"

    // Knowledge Base
    override val kbTitle = "Base de connaissances"
    override val noSavedFiles = "Aucun fichier sauvegardé"
    override val saveFromChat = "Sauvegardez des réponses IA depuis le Chat"
    override val folderDeleted = "Dossier supprimé"
    override val fileDeleted = "Fichier supprimé"
    override val deleteFolder = "Supprimer le dossier"
    override val tryAnotherSearch = "Essayez une autre recherche"
    override val file = "fichier"
    override val files = "fichiers"

    // Settings
    override val settingsTitle = "Paramètres"
    override val transcriptionStt = "Transcription (STT)"
    override val transcriptionLanguage = "Langue de transcription"
    override val chatFontSize = "Taille de police du chat"
    override val llmProcessing = "Traitement LLM"
    override val apiKeys = "Clés API"
    override val groqApiKey = "Clé API Groq"
    override val openaiApiKey = "Clé API OpenAI"
    override val xaiApiKey = "Clé API xAI"
    override val anthropicApiKey = "Clé API Anthropic"
    override val adminKeysForUsageTracking = "Clés Admin (Suivi d'Utilisation)"
    override val openaiAdminKey = "Clé Admin OpenAI (API Facturation)"
    override val anthropicAdminKey = "Clé Admin Anthropic (sk-ant-admin...)"
    override val freeConsole = "Obtenir une clé API: console.groq.com"
    override val usageStatistics = "Statistiques d'utilisation"
    override val last7Days = "7 derniers jours"
    override val last30Days = "30 derniers jours"
    override val refreshUsage = "Actualiser"
    override val requiresAdminKey = "⚠️ Nécessite une clé API Admin (sk-ant-admin...)"
    override val llmSystemPrompt = "Prompt système LLM"
    override val instructionsForLlm = "Instructions pour le LLM"
    override val resetToDefault = "Réinitialiser par défaut"
    override val about = "À propos"
    override val aboutText = """
        • Local: Whisper.cpp (hors-ligne, modèle inclus)
        • Groq: Whisper v3 Turbo (gratuit 8h/jour)
        • LLM: Formate et améliore les transcriptions
    """.trimIndent()
    override val disabled = "Désactivé"
    override val rawTranscription = "Transcription brute"
    override val freeVeryFast = "Gratuit, très rapide"
    override val preview = "Aperçu"
    override val active = "actif"
    override val configured = "configuré"
    override val show = "Afficher"
    override val hide = "Masquer"
    override val appLanguage = "Langue de l'application"

    // Relative time
    override val now = "maintenant"
    override val minutesAgo = "min"
    override val hoursAgo = "h"
    override val yesterday = "hier"
    override val daysAgo = "j"

    // Preview messages
    override val previewUserMessage = "Bonjour, comment ça va ?"
    override val previewAssistantMessage = "Je vais très bien, merci !"

    // Note Editor
    override val newNoteTitle = "Nouvelle note"
    override val editNote = "Modifier la note"
    override val title = "Titre"
    override val addTag = "Ajouter un tag..."
    override val existingTags = "Tags existants :"
    override val tags = "Tags"
    override val startWritingOrRecord = "Commencez à écrire ou enregistrez..."
    override val tapToRecord = "Appuyez pour enregistrer"
    override val initializing = "Initialisation..."
    override val dismiss = "Fermer"
    override val edit = "Modifier"

    // Knowledge Base - additional
    override val loading = "Chargement..."
    override val emptyFolder = "Dossier vide"
    override val deleteFile = "Supprimer le fichier"
    override val copyContent = "Copier le contenu"

    // Notes List - additional
    override val deleteAllArchived = "Tout supprimer"
    override val viewArchives = "Voir les archives"
    override val gridView = "Grille"
    override val listView = "Liste"
    override val note = "Note"
    override val exportNote = "Exporter la note"
    override val message = "message"
    override val messages = "messages"

    // Chat Tags
    override val manageTags = "Gérer les tags"
    override val addTagToConversation = "Ajouter un tag..."
    override val searchTags = "Rechercher des tags..."
    override val allTags = "Tous les tags"
    override val selectedTags = "Tags sélectionnés"
    override val selected = "Sélectionné"
    override val deleteTag = "Supprimer le tag"
    override val deleteTagConfirmation = "Êtes-vous sûr de vouloir supprimer le tag"
    override val deleteTagWarning = "Cela supprimera le tag de toutes les conversations."
    override val removeTagFromAll = "Retirer de tous les fichiers"
    override val removeTagFromAllConfirmation = "Retirer ce tag de tous les fichiers ?"
    override val confirm = "Confirmer"

    // KB Folder
    override val renameFolder = "Renommer le dossier"

    // Favorites
    override val favorites = "Favoris"
    override val allFavorites = "Tous"
    override val filterNotes = "Notes"
    override val filterChat = "Chat"
    override val filterKb = "KB"
    override val noFavoritesYet = "Aucun favori pour le moment"
    override val addToFavorites = "Ajouter aux favoris"
    override val removeFromFavorites = "Retirer des favoris"

    // KB Merge
    override val merge = "Fusionner"
    override val mergeFiles = "Fusionner les fichiers"
    override val mergeFilesConfirmation = "Fusionner"
    override val newFilename = "Nouveau nom de fichier"
    override val selectFile = "Sélectionner"
    override val selectToMerge = "Sélectionner pour fusionner"

    // Chat Summary
    override val summarize = "Résumer"
    override val conversationSummary = "Résumé de la conversation"
    override val saveToKb = "Sauvegarder dans KB"
    override val folderName = "Nom du dossier"
    override val fileName = "Nom du fichier"
    override val close = "Fermer"

    // Share feature
    override val shareArticle = "Partager l'article"
    override val shareUrl = "URL de partage"
    override val sharing = "Partage"
    override val shareFeature = "Fonction de partage"
    override val enableSharing = "Activer le partage"
    override val shareApiToken = "Jeton API de partage"
    override val expirationDays = "Expiration (jours)"
    override val expiresOn = "Expire le"
    override val hiddenFeatureHint = "Appuyez longuement sur les titres de section pour déverrouiller les fonctionnalités cachées"

    // Whisper Model Download
    override val whisperModels = "Modèles Whisper"
    override val downloadModel = "Télécharger le modèle"
    override val deleteModel = "Supprimer le modèle"
    override val modelDownloaded = "Téléchargé"
    override val modelNotDownloaded = "Non téléchargé"
    override val downloading = "Téléchargement"
    override val selectModel = "Sélectionner le modèle"
    override val modelSelected = "Sélectionné"
    override val download = "Télécharger"

    // Volume Button Scrolling
    override val volumeScrollTitle = "Défilement par boutons de volume (E-Ink)"
    override val volumeScrollEnable = "Activer le défilement par boutons de volume"
    override val volumeScrollDescription = "Optimisé pour e-ink : sauts instantanés, pas de ghosting"
    override val volumeScrollDistance = "Distance (100% = plein écran, 50% = moitié avec 50% de chevauchement)"
}

/**
 * Composable helper to get current strings with recomposition on language change
 */
@Composable
fun rememberStrings(): StringResources {
    val language by Strings.currentLanguage.collectAsState()
    return when (language) {
        AppLanguage.ENGLISH -> EnglishStrings
        AppLanguage.FRENCH -> FrenchStrings
    }
}
