# Jared AI - Release v1.0.0

## Overview

**Jared AI** is an offline-first Android speech-to-text application powered by whisper.cpp with integrated LLM capabilities. Built for e-ink devices (Boox Palma 2 Pro, Viwoods AIPaper), it provides a distraction-free environment for voice notes, AI chat, and knowledge management.

## Key Features

### 🎤 Speech-to-Text
- **Offline STT**: Local whisper.cpp integration with native French support
- **Cloud STT**: Groq Whisper API ($0.04/hr)
- **Model support**: ggml-small-q8_0 (264MB, optimized for 8GB RAM devices)
- **Performance**: ~6s transcription for 16s audio
- **Native French**: No translation, direct transcription

### 💬 AI Chat Integration
- **Multi-provider LLM**: OpenAI GPT-5-mini, Groq Llama 3.3 70B, xAI Grok
- **SSE Streaming**: Real-time response streaming
- **Conversation management**: Full chat history with tags
- **Context-aware**: Messages maintain conversation context

### 📝 Notes System
- **Rich text editor**: Markdown support with live preview
- **Archive system**: Move notes to archive, restore anytime
- **Tag management**: Organize notes with custom tags
- **Search**: Full-text search across all notes

### 📚 Knowledge Base
- **Folder organization**: Hierarchical file structure
- **Markdown rendering**: GFM tables, code blocks, formatting
- **Export**: Share files externally via Android Share Sheet
- **Tag filtering**: Filter files by tags across folders

### 🏷️ Universal Tag System
- **Persistent tags**: Tags survive removal from all items
- **CRUD operations**: Create, read, update (rename), delete
- **20-character limit**: Enforced at backend + UI validation
- **Tag filtering**: Filter by multiple tags simultaneously
- **Global management**: Dedicated screens for tag overview
- **Context menus**: Long-press items → Rename/Manage Tags/Delete

### 🎨 E-Ink Optimized UI
- **No animations**: Prevents ghosting on e-ink displays
- **High contrast**: Black/white color scheme
- **Custom components**: EInkButton, EInkCard, EInkChip, EInkTextField
- **Rounded corners**: 24-40dp border radius matching Palma 2 Pro hardware
- **Material 3**: Jetpack Compose with modern Android design

### 🌐 Internationalization
- **French & English**: Full i18n support
- **Language toggle**: Settings → Language selection
- **Persistent**: Language preference saved in DataStore

### 🔧 Technical Highlights
- **Architecture**: MVVM with Hilt dependency injection
- **Storage**: DataStore for settings, JSON for structured data
- **Threading**: Kotlin Coroutines + Flows for reactive UI
- **Audio**: 16kHz mono PCM recording, float32 normalization
- **NDK**: whisper.cpp JNI integration with CMake
- **16KB page alignment**: Android 15+ compatibility

## Installation

### Requirements
- Android 15+ (API 26+)
- 4GB+ RAM (8GB recommended)
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
./gradlew assembleDebug      # ~12-23s
./gradlew assembleRelease    # ~2m (first build)
```

## Configuration

### API Keys
Settings → API Configuration:
- **STT Provider**: Local (whisper.cpp) or Cloud (Groq)
- **LLM Provider**: OpenAI, Groq, xAI
- **API Keys**: Securely stored in DataStore
- **System Prompt**: Customize LLM behavior

### Language
Settings → Language:
- French (Français)
- English

## Usage

### Dashboard
- **Quick Actions**: New Note, New Chat
- **Global Search**: Search across Notes, Conversations, Knowledge Base (max 15 results)
- **Recent Items**: Quick access to recent notes/chats

### Recording Flow
1. Tap microphone icon (Chat/Notes)
2. Record audio
3. Stop → STT transcription
4. (Chat only) Send to LLM → Receive response
5. (Chat only) Save response to Knowledge Base (optional)

### Tag Management UX
- **Chat**: Tap conversation tag icon → manage tags | Long-press tag filter → global tags
- **Notes**: Long-press tag filter → global tags | Context menu → Manage Tags
- **KB**: Long-press tag filter → global tags | Context menu → Manage Tags

### Keyboard Shortcuts
- Tap outside input fields → Dismiss keyboard
- System bars: Respected (status bar + navigation bar)

## Recent Updates (v1.0.0)

### Tag System Enhancements
- ✅ Full CRUD tag management across Chat, Notes, KB
- ✅ 20-character limit with backend + UI enforcement
- ✅ Persistent tags (survive removal from all items)
- ✅ Auto-clear filters when deleting active filter tag
- ✅ Dual-mode tag screens (global read-only + per-item CRUD)

### UX Improvements
- ✅ Unified tag icon behavior (tap/long-press patterns)
- ✅ Cleaner Notes top bar (removed redundant tag icon)
- ✅ KB/Notes tag filter icon long-press → global management

### Localization
- ✅ Accents & spaces in KB folder/file names (was ASCII-only)
- ✅ Sanitization preserves UTF-8, blocks only dangerous chars

### Build Optimizations
- ✅ Release signing with debug keystore (personal builds)
- ✅ R8 shrinking: 7% smaller APK (274→254 MB)
- ✅ 10-30% performance improvement in release builds

## Known Limitations

### Performance
- **Local STT**: 2+ min for 22s audio on Palma 2 Pro (use Groq cloud STT)
- **Model size**: Medium model overheats device (use small-q8_0)
- **First inference**: ~3s warm-up (kernel compilation + KV cache init)

### Platform
- **Android only**: No iOS support
- **ARM only**: x86 emulators untested
- **API 26+**: Minimum Android 8.0

### Features
- **No cloud sync**: All data stored locally
- **No multi-device**: Each device has independent data
- **No backup**: Manual export required

## Architecture

### Modules
- `app`: Main application (Kotlin + Compose)
- `whisper`: Native whisper.cpp library (C/C++ + JNI)

### Key Components
- **WhisperManager**: Singleton STT service
- **SttManager**: Unified local/cloud STT interface
- **LlmService**: SSE streaming with OpenAI-compatible API
- **Repositories**: ChatHistory, Notes, LlmOutput (KB)
- **ViewModels**: ChatList, Chat, NotesList, KnowledgeBase

### Data Flow
```
Audio → AudioRecorder (16kHz PCM)
      → WhisperManager (local) OR CloudSttService (Groq)
      → ChatViewModel
      → LlmService (streaming SSE)
      → ChatHistoryRepository (save)
      → LlmOutputRepository (KB export)
```

## Credits

Built with:
- **whisper.cpp**: https://github.com/ggml-org/whisper.cpp
- **Jetpack Compose**: Modern Android UI toolkit
- **Hilt**: Dependency injection
- **markdown-renderer**: mikepenz/multiplatform-markdown-renderer
- **richeditor-compose**: mohamedrejeb/compose-rich-editor

## License

[Your License Here]

## Contributing

Contributions welcome! Please open issues for bugs or feature requests.

## Roadmap

- [ ] Cloud sync (optional)
- [ ] Multi-language UI (Spanish, German, etc.)
- [ ] Voice activity detection (auto-start/stop)
- [ ] Custom whisper model support
- [ ] Offline LLM integration (Llama.cpp)
- [ ] Export/import conversation history
- [ ] Backup/restore functionality

---

**Generated with Claude Code**
Version: v1.0.0
Release Date: December 2025
