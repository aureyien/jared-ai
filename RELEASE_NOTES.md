# Jared AI - Release Notes

## Version 1.2.0 - December 2025

### 🌟 Major Features

#### ⭐ Favorites System (NEW)
- **Complete Favorites Implementation** - Mark notes, chat conversations, and KB files as favorites
- **Dedicated Favorites Screen** - View all favorites in one place with filter buttons (All/Notes/KB/Chat)
- **Star Icons Everywhere** - Black star icons displayed on favorited items in all list views
- **Quick Toggle** - Tap star icons in detail views (NoteEditor, ChatScreen, KbDetail) to toggle favorite status
- **Long-press Removal** - Remove items from favorites by long-pressing in the favorites list
- **Reactive Updates** - Favorite status updates immediately across all views with Flow-based state management
- **Dashboard Integration** - Favorites section now a clickable card matching Notes/Chat/KB sections

#### 🤖 Claude Haiku 4.5 Support (NEW)
- **New Model Added** - Claude Haiku 4.5 support (fastest Anthropic model)
- **Multi-Provider Usage Tracking** - Track token usage and costs across OpenAI, Anthropic, and xAI providers
- **Provider-Specific Analytics** - View detailed usage statistics per LLM provider in settings

#### 🏷️ Comprehensive Tag Management
- **Full-Screen Tag Management** - Dedicated screens for managing tags in Notes, Chat, and KB
- **Global Tag Operations** - Long-press filter icons to access global tag management
- **Tag Filtering** - Filter content by tags in all list views with multi-tag selection
- **Tag CRUD** - Create, rename, delete tags with conflict resolution
- **Tag Length Limit** - All tags limited to 20 characters for consistency (enforced in backend + UI)
- **Persistent Tags** - Tags persist even when removed from all items for easy reuse
- **Long-press Delete** - Quick tag deletion via long-press on tag chips
- **Auto-clear Filters** - Tag filters automatically clear when deleting an active filter tag
- **Harmonized Styling** - Consistent tag chip design across all views

### 🎨 UI/UX Improvements

#### Navigation & Layout
- **Bottom Bar Rounded Corners** - Improved visual design with rounded bottom bar matching Boox Palma hardware
- **Aligned Navigation** - Consistent back arrow alignment across all screens
- **Tap to Rename** - Tap conversation title to quickly rename in chat
- **Dashboard Cards** - All sections (Notes, Chat, KB, Favorites) use consistent clickable card design

#### E-Ink Display Optimizations
- **High Contrast Design** - Black icons and borders for optimal e-ink readability
- **No Animations** - Prevents ghosting on e-ink displays
- **Refresh Optimizations** - Reduced unnecessary redraws for better e-ink performance
- **Button Styling** - E-ink optimized buttons with clear visual feedback
- **Custom Components** - EInkButton, EInkCard, EInkChip, EInkTextField, EInkIconButton
- **Boox Palma Specific** - Special optimizations noted for Boox Palma e-ink devices

#### Markdown & Text Display
- **Enhanced Markdown Rendering** - Improved KB markdown display with better paragraph spacing
- **Chat Bubble Styling** - Better visual separation between user and assistant messages
- **Extra Margin** - Added spacing above LLM response bubbles for clarity
- **Toolbar Behavior** - Toolbar appears above keyboard only when typing (prevents layout jumps)

### 🔧 Functionality Enhancements

#### Knowledge Base
- **Markdown Editing** - Edit KB files directly in the app with markdown preview
- **3-Level Navigation** - Browse folders → files → content seamlessly
- **Search & Filter** - Full-text search and tag filtering in KB
- **Folder/File Rename** - Rename KB folders and files with accent and space support (UTF-8)
- **Bottom Bar Actions** - Quick actions accessible via bottom bar
- **Tags Display** - Visual tag chips on KB files with filter and management options
- **16KB Alignment** - Optimized file size alignment for storage efficiency (Android 15+)
- **Tag Management UI** - Full-screen dedicated tag management with CRUD operations
- **Export Functionality** - Share KB files via Android Share Sheet

#### Notes
- **Archive Feature** - Archive notes to declutter main list while keeping them accessible
- **Unarchive** - Restore archived notes back to main list
- **Global Search** - Search across all notes by title, content, or tags (max 5 results per type)
- **Empty Note Fix** - Prevent creation of empty untitled notes
- **Tag Icon Improvements** - Long-press tag filter icon for global tag management
- **Tag Management** - Per-note tag management via context menu
- **Undo Delete** - UndoButton with countdown for accidental deletions

#### Chat
- **Chat Font Size Setting** - Adjust chat message font size (12sp-24sp) for better readability
- **Tag Management** - Tap tag icon for per-conversation tags, long-press for global tags
- **Conversation Rename** - Tap title bar to rename conversation
- **Usage Tracking** - View token usage and costs per provider
- **SSE Streaming** - Real-time response streaming from LLM
- **Save to KB** - Save AI responses directly to Knowledge Base with custom filename

### 🐛 Bug Fixes

- **KB Folder Toggle** - Fixed KB folder expand/collapse not updating UI
- **Tag Filter Clear** - Tag filters now properly clear when deleting a tag
- **Tag Management Dismissal** - Fixed Notes tag management screen dismissing immediately
- **KB Tag Long-press** - Fixed KB tag icon long-press to open full-screen management
- **Accents in Filenames** - Allow accents and spaces in KB folder/file names (was ASCII-only)
- **Status Text** - Changed "Envoi au LLM..." to "Sending..." for consistency
- **Chat Tag Icon** - Fixed chat tag icon interaction issues
- **Favorites Filter** - Fixed filter buttons not working with reactive remember()
- **Favorites Refresh** - Fixed favorites list not refreshing after removing items

### 🌐 Internationalization

#### Language Support
- **i18n System** - Complete internationalization infrastructure
- **English/French Support** - Full EN/FR translations for entire app
- **Language Selector** - Switch languages from settings
- **Persistent Preference** - Language choice saved in DataStore
- **UTF-8 Support** - Full Unicode support throughout app

### 🛠️ Build & Infrastructure

- **Release Build Configuration** - Automated release builds with signing
- **Auto-signing** - Streamlined release process with automatic APK signing (debug keystore for personal builds)
- **R8 Code Shrinking** - 7% smaller APK size (274MB → 254MB)
- **Performance Boost** - 10-30% performance improvement in release builds
- **16KB Page Alignment** - Android 15+ compatibility

### 📚 Documentation

- **Updated README** - Comprehensive documentation of all features
- **Feature Guides** - Detailed explanations of KB navigation, search, filtering
- **Boox Palma Notes** - Special optimization guidance for Boox Palma devices
- **Release Notes** - This comprehensive changelog

### 🔄 Refactoring & Code Quality

- **Tag Icon Refactor** - Standardized tag icon behavior (tap vs long-press) across app
- **State Management** - Improved reactive state management with Flow.combine for auto-refresh
- **Code Organization** - Better separation of concerns in ViewModels and Repositories
- **Undo System** - Reusable UndoButton component across all delete operations with countdown
- **Favorites Architecture** - Clean implementation with repository pattern and reactive updates
- **Memory Optimization** - Better memory management for large datasets

---

## What's Changed Since v1.0.0

**38 commits** with major improvements to favorites, tags, LLM providers, and UX refinements.

### Highlights:
1. ⭐ **Favorites System** - Complete implementation across Notes, Chat, and KB
2. 🤖 **Claude Haiku 4.5** - New fastest Anthropic model support
3. 🏷️ **Tag Management** - Full CRUD operations with persistent tags
4. 🎨 **UI Polish** - E-ink optimizations and consistent design
5. 🌐 **i18n** - Full French/English support
6. 🐛 **Bug Fixes** - 10+ bug fixes for better stability

---

## Installation

### Requirements
- Android 8.0+ (API 26+)
- 4GB+ RAM (8GB recommended for local STT)
- ARM64-v8a or ARMv7 device

### APK
- **Debug**: `app/build/outputs/apk/debug/app-debug.apk` (~274 MB)
- **Release**: `app/build/outputs/apk/release/app-release.apk` (~254 MB, 10-30% faster)

### Build from Source
```bash
git clone https://github.com/aureyien/jared-ai.git
cd jared-ai

# Create local.properties
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# Build
./gradlew assembleDebug      # ~15-23s
./gradlew assembleRelease    # ~2m (first build)
```

---

## Configuration

### API Keys
Settings → API Configuration:
- **STT Provider**: Local (whisper.cpp) or Cloud (Groq)
- **LLM Provider**: OpenAI (GPT-4o-mini), Anthropic (Claude Haiku/Sonnet), xAI (Grok)
- **API Keys**: Securely stored in DataStore
- **System Prompt**: Customize LLM behavior
- **Chat Font Size**: Adjust readability (12sp-24sp)

### Language
Settings → Language:
- French (Français)
- English

---

## Known Issues

- **Local STT Performance**: 2+ min for 22s audio on Boox Palma 2 Pro (use Groq cloud STT for better performance)
- **Model Size**: Medium whisper model causes overheating (use small-q8_0)
- **No Cloud Sync**: All data stored locally only
- **No Backup**: Manual export required

---

## Coming Soon

- Cloud sync functionality
- Export/import features
- More LLM provider integrations
- Voice playback for AI responses
- Offline LLM integration (Llama.cpp)
- Voice activity detection (auto-start/stop)

---

## Credits

Built with:
- **whisper.cpp**: https://github.com/ggml-org/whisper.cpp
- **Jetpack Compose**: Modern Android UI toolkit
- **Hilt**: Dependency injection
- **markdown-renderer**: mikepenz/multiplatform-markdown-renderer
- **richeditor-compose**: mohamedrejeb/compose-rich-editor

---

**Full Changelog**: https://github.com/aureyien/jared-ai/compare/6014bf0...8a0b90b

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Version: v1.2.0
Release Date: December 17, 2025
