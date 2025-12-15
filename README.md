# Jared AI

> **Jared** = **Jarvis** + **Alfred** — Your personal AI assistant combining the intelligence of Jarvis with the reliability of Alfred.

A privacy-focused Android note-taking app with **offline voice transcription** powered by [whisper.cpp](https://github.com/ggerganov/whisper.cpp) and optional **AI-powered chat** integration.

> **Note**: This entire codebase was generated exclusively by [Claude Code](https://claude.ai/claude-code) (Anthropic's AI coding assistant).

## Overview

Whisper Notes is designed for users who want fast, private voice-to-text capabilities without relying on cloud services. The app features:

- **Offline Speech-to-Text**: Uses whisper.cpp running locally on-device
- **Rich Text Notes**: Markdown-based editor with formatting toolbar
- **AI Chat Integration**: Optional LLM chat with multiple providers (Groq, OpenAI, xAI)
- **Knowledge Base**: Save and organize AI responses for future reference
- **E-Ink Optimized UI**: High-contrast black & white design perfect for e-ink devices

## Features

### Voice Transcription

| Feature | Description |
|---------|-------------|
| **Local STT** | On-device transcription using whisper.cpp (no internet required) |
| **Cloud STT** | Optional cloud transcription via Groq or OpenAI Whisper API |
| **Multi-language** | Support for French and English |
| **Quick Record** | Long-press on Note or Chat buttons to start recording immediately |

### Note Editor

- **Rich Text Formatting**: Bold, italic, underline, strikethrough, headers
- **Lists**: Bulleted, numbered, and checkbox lists
- **Code Blocks**: Inline code formatting
- **Tags**: Organize notes with custom tags (20-character limit)
- **Tag Management**: Full-screen tag manager with search and filtering
- **Preview Mode**: Toggle between edit and preview with rendered markdown
- **Voice Dictation**: Record and transcribe directly into notes

### AI Chat

- **Multiple Providers**: Support for Groq (Llama), OpenAI (GPT), and xAI (Grok)
- **Conversation History**: Persistent chat conversations with rename/delete
- **Voice Input**: Dictate messages using STT
- **Save Responses**: Export AI responses to Knowledge Base
- **Customizable System Prompt**: Configure AI behavior and language
- **Tag Organization**: Add tags to conversations for easy filtering
- **Tag Management**: Full-screen tag manager with long-press delete

### Knowledge Base

- **3-Level Navigation**: Folders → Files → Detail view
- **Organized Storage**: Save AI responses in folders (rename folders via menu)
- **Search & Filter**: Full-text search + tag filtering at both folder and file level
- **Tag Management**: Full-screen tag manager with search, filtering, and long-press actions
- **Tag Actions**: Long-press tags to delete or remove from all files
- **Persistent Tags**: Tags remain available even when not assigned to any files
- **Markdown Editor**: Edit saved content with rich text toolbar
- **Preview Mode**: View formatted markdown content
- **Clipboard Integration**: Copy content with one tap

### Dashboard

- **Global Search**: Search across Notes, Conversations, and Knowledge Base
- **Quick Access**: Recently accessed items appear first
- **Search Results**: Max 5 results per category with type-specific icons
- **Direct Navigation**: Tap any result to jump directly to that item

## Architecture

```
whisper-notes/
├── app/                          # Main Android application
│   └── src/main/java/com/music/sttnotes/
│       ├── data/
│       │   ├── api/              # API services (LLM, Cloud STT)
│       │   ├── chat/             # Chat conversation persistence
│       │   ├── i18n/             # Internationalization (EN/FR)
│       │   ├── llm/              # LLM output storage
│       │   ├── notes/            # Notes repository
│       │   └── stt/              # Speech-to-text components
│       └── ui/
│           ├── components/       # Reusable UI components
│           ├── navigation/       # Navigation graph
│           ├── screens/          # App screens
│           └── theme/            # E-Ink optimized theme
└── whisper/                      # whisper.cpp Android module
    └── src/main/
        ├── cpp/                  # Native C++ whisper.cpp code
        └── java/                 # JNI bindings
```

### Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Dependency Injection**: Hilt
- **Data Persistence**: DataStore Preferences + JSON files
- **Networking**: OkHttp
- **Rich Text**: Compose Rich Editor
- **Native Code**: whisper.cpp via JNI

## Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 35 (compile) / SDK 26+ (minimum)
- NDK (for building whisper.cpp)

### Clone and Build

```bash
git clone https://github.com/yourusername/whisper-notes.git
cd whisper-notes
```

### Download Whisper Models

The app requires a whisper.cpp model file. Download one of the following:

```bash
# Small model (466 MB) - Good balance of speed/accuracy
mkdir -p app/src/main/assets/models
curl -L -o app/src/main/assets/models/ggml-small-q8_0.bin \
  https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q8_0.bin

# Or tiny model (75 MB) - Fastest, lower accuracy
curl -L -o app/src/main/assets/models/ggml-tiny.bin \
  https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin

# Or base model (142 MB) - Good for most use cases
curl -L -o app/src/main/assets/models/ggml-base.bin \
  https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin
```

### Build the App

**Debug build** (faster builds, no optimization):
```bash
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk` (~274 MB)

**Release build** (optimized for production):
```bash
./gradlew assembleRelease
```
APK: `app/build/outputs/apk/release/app-release.apk` (~254 MB, 7% smaller)

Release builds include:
- R8 code shrinking and obfuscation
- Resource optimization
- 10-30% faster execution
- Lower CPU and memory usage
- Better battery life

## Configuration

### STT Provider Settings

Navigate to **Settings** to configure:

| Setting | Options | Description |
|---------|---------|-------------|
| **STT Provider** | Local, Groq, OpenAI | Choose transcription backend |
| **Language** | French, English | Transcription language |

### App Language

The app supports multiple UI languages:

| Language | Code |
|----------|------|
| English | EN |
| French | FR |

Change the UI language in **Settings** → **App Language**.

### LLM Provider Settings

| Setting | Options | Description |
|---------|---------|-------------|
| **LLM Provider** | Groq, OpenAI, xAI, None | Choose AI chat provider |
| **API Key** | Your API key | Required for cloud providers |
| **System Prompt** | Custom text | Configure AI behavior |

### API Keys

To use cloud services, you'll need API keys:

- **Groq**: Get a free key at [console.groq.com](https://console.groq.com)
- **OpenAI**: Get a key at [platform.openai.com](https://platform.openai.com)
- **xAI**: Get a key at [console.x.ai](https://console.x.ai)

## Usage

### Creating Notes

1. Tap the **Note** button to create a new note
2. Enter a title and start typing
3. Use the formatting toolbar for rich text
4. Tap the **tag icon** to add tags
5. Tap the **mic icon** to dictate content

### Voice Recording

1. Tap the **Mic** button to start recording
2. Speak your content
3. Tap **Stop** to end recording and transcribe
4. Text is automatically inserted at cursor position

**Tip**: Long-press the **Note** button to create a new note and start recording immediately.

### AI Chat

1. Tap the **Chat** button to open conversations
2. Create a new conversation or select existing
3. Type or dictate your message
4. View AI responses with markdown formatting
5. Long-press a response to save to Knowledge Base

**Tip**: Long-press the **Chat** button to start a new conversation with voice recording.

### Knowledge Base

1. Tap **KB** to access saved content
2. Browse folders organized by date
3. Use the **search bar** to find content or filter by tags
4. Tap a folder to see its files
5. Tap a file to view/edit content
6. Use the **tag icon** to filter by tags
7. Long-press to copy or delete content

### Tag Management

**Access tag management:**
- **Notes**: Tap the tag icon in the note list to open full-screen tag manager
- **Chat**: Tap the tag icon in conversation list to manage conversation tags
- **Knowledge Base**: Long-press the tag icon in the top bar for global tag management, or tap in file detail for file-specific tags

**Tag operations:**
- **Add tags**: Type in the input field (max 20 characters) and tap the + button
- **Toggle tags**: Tap tags to add/remove from the current item
- **Delete tags**: Long-press any tag to delete it or remove it from all items
- **Filter view**: Tap tag chips to filter by tags (long-press chips for actions)
- **Search tags**: Use the search bar to find specific tags

**Tag features:**
- Tags are persistent - they remain available even when not assigned to any items
- 20-character limit prevents overly long tag names
- Input validation prevents typing beyond the character limit
- Tag visibility can be toggled in list views

### Undo Deletions

When deleting items (notes, chats, KB files), an **UNDO button** appears in the top bar with a 5-dot countdown timer. Click it to restore the item before permanent deletion. The countdown provides visual feedback showing time remaining.

## Project Structure

### Key Files

| File | Description |
|------|-------------|
| `WhisperManager.kt` | Manages whisper.cpp model loading and inference |
| `AudioRecorder.kt` | Handles microphone input and WAV encoding |
| `SttManager.kt` | Coordinates local/cloud STT switching |
| `LlmService.kt` | Handles LLM API calls (Groq/OpenAI/xAI) |
| `ChatViewModel.kt` | Chat screen state management with tag operations |
| `NoteEditorViewModel.kt` | Note editor state management |
| `EInkComponents.kt` | E-Ink optimized Compose components |
| `TagManagementScreen.kt` | Full-screen tag manager for Chat conversations |
| `TagManagementScreenForNotes.kt` | Full-screen tag manager for Notes |
| `TagManagementScreenForKB.kt` | Dual-mode tag manager for Knowledge Base (global/file-specific) |
| `DashboardViewModel.kt` | Global search across all content types |
| `KnowledgeBaseFolderScreen.kt` | KB folder files list with search and tag filter |
| `KnowledgeBaseDetailScreen.kt` | KB file viewer/editor with markdown support |

### Build Flavors

The project includes a `palma` build flavor optimized for Palma e-ink devices with:
- Medium whisper model (better accuracy)
- E-ink display optimizations

## Performance

### Whisper Model Comparison

| Model | Size | Speed* | Accuracy |
|-------|------|--------|----------|
| Tiny | 75 MB | ~2x realtime | Basic |
| Base | 142 MB | ~1.5x realtime | Good |
| Small | 466 MB | ~1x realtime | Very Good |
| Medium | 1.5 GB | ~0.5x realtime | Excellent |

*Speed measured on mid-range Android device (Snapdragon 7 Gen 1)

### Memory Usage

- App baseline: ~50 MB
- With tiny model: ~150 MB
- With small model: ~600 MB
- During transcription: +100-200 MB

## Troubleshooting

### Model Loading Fails

1. Ensure model file is in `app/src/main/assets/models/`
2. Check model filename matches expected format
3. Verify file is not corrupted (re-download if needed)

### Transcription is Slow

1. Try a smaller model (tiny or base)
2. Ensure device has sufficient free RAM
3. Close other apps during transcription

### No Audio Input

1. Check microphone permission is granted
2. Ensure no other app is using the microphone
3. Test microphone with another app

### Cloud API Errors

1. Verify API key is correct in Settings
2. Check internet connection
3. Ensure API account has available credits

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [whisper.cpp](https://github.com/ggerganov/whisper.cpp) - C/C++ port of OpenAI's Whisper
- [Compose Rich Editor](https://github.com/MohamedRejworker/compose-rich-editor) - Rich text editor for Compose
- [Groq](https://groq.com) - Fast LLM inference API
