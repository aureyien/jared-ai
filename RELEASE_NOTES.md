# Release Notes

## Latest Release - Share Functionality for Chat & Notes

### 🔗 New Share Features

#### Chat Conversation Sharing
- **Share Button** in chat 3-dot menu (⋮)
  - Enabled when conversation has messages
  - Formats messages as markdown: **User:** / **Assistant:**
  - Preserves full conversation context
  - Uses existing Share API settings (expiration, burn-after-read)

#### Note Sharing
- **Share IconButton** in note top bar
  - Only visible for saved notes (not new notes)
  - Shares note title and content
  - Fallback to "Note" if title is blank
  - Same Share API configuration as KB articles

#### Share Modal
- **QR Code Display**: Locally generated via ZXing (512x512)
- **Share URL**: Ready to copy and send
- **Consistent Experience**: Same modal used for KB, Chat, and Notes
- **Settings Integration**: Respects API token, expiration days, burn-after-read preference

### 🎨 Localization
- **Added "share" string**: English "Share", French "Partager"
- Used consistently across Chat and Notes screens

---

### Technical Details

**Files Modified**: 6 files changed, 203 insertions(+), 22 deletions(-)

**Key Components**:
- ChatViewModel: `shareConversation()` method, StateFlows for share state
- NoteEditorViewModel: `shareNote()` method, ShareService integration
- ChatScreen: Share menu item, ShareResultModal display
- NoteEditorScreen: Share button, modal integration
- NavGraph: Callback wiring for both chat and note sharing
- Strings: Added `share` property with EN/FR translations

**Architecture**:
- Reuses existing ShareService and ShareModal infrastructure from KB
- Chat messages formatted as markdown for readability
- StateFlow-based reactive UI for modal display
- Proper error handling and API token validation

**Build Status**: ✅ BUILD SUCCESSFUL in 13s (76 tasks: 13 executed, 63 up-to-date)

---

## Previous Release - Dashboard Improvements & UI Refinements

### 🎯 Dashboard Enhancements

#### Knowledge Base Display
- **Improved Preview Extraction**: Robust YAML frontmatter parsing with line-by-line separator detection
  - Prevents false matches with markdown horizontal rules in content
  - Properly removes "## Original transcription" section
  - Handles edited articles that don't follow standard structure
  - More reliable content extraction for previews

#### Configurable Article Count
- **New Setting**: Choose how many KB articles to display on dashboard (1-10, default: 5)
  - Slider control in Settings with live value display
  - Persisted preference across app restarts
  - International support (English/French)

#### UI Polish
- **Swapped Chat and Notes**: Chat section now appears on the left, Notes on the right
- **Removed "View All" button**: Cleaner KB section without redundant button
- **Optimized for E-Ink**:
  - Reduced KB preview font size to 0.5f (50% of base size)
  - Reduced Chat preview font to 11sp
  - Reduced all section titles to 13sp
  - Uniform 78dp card height for all KB articles
  - Tighter spacing: 4dp between items, 10dp padding
  - All text properly sized for e-ink display clarity

### 💬 Chat Improvements

#### Bulk Delete Archived Chats
- **New Feature**: Delete all archived chats at once with undo functionality
  - Trash icon button appears when viewing archived chats
  - Click to delete all → Undo button appears with countdown
  - Cancel anytime before timeout to restore all chats
  - Safe, reversible bulk operation

#### Navigation Fix
- **Fixed crash** when opening chat conversations from Favorites screen
  - Safe ViewModel retrieval with fallback
  - Handles navigation from multiple entry points (ChatList, Favorites, Dashboard)

### ✏️ Markdown Enhancements

#### New Separator Button
- **Horizontal Rule Button** added to markdown toolbar
  - Located after the checkbox button
  - Inserts `\n\n---\n\n` with proper spacing
  - HorizontalRule icon (three horizontal lines)
  - Ensures separators are always visible in markdown preview

#### Improved Rendering
- **Markdown Spacing**: Added vertical padding to horizontal rule component
  - 16dp spacing above and below separators
  - Better visual separation in rendered markdown

### 🎨 UI Polish

#### Note Editor
- **Removed redundant title** from top bar
  - Title only shown once in the editor body
  - Cleaner, less cluttered interface
  - More space for action buttons

---

### Technical Details

**Files Modified**: 12 files changed, 358 insertions(+), 165 deletions(-)

**Key Components**:
- Dashboard: KB preview extraction, layout optimization, configurable settings
- Chat: Bulk delete with undo, navigation crash fix
- Markdown: Toolbar button, spacing improvements
- Settings: New KB article count preference with UI control

**Architecture Improvements**:
- Robust YAML frontmatter parsing (line-by-line instead of regex)
- Safe ViewModel retrieval for multi-entry-point navigation
- Proper spacing for markdown components
- DataStore integration for new preferences

---

**Build Status**: ✅ All changes compiled and tested successfully

**Compatibility**: Optimized for e-ink displays with smaller fonts and tighter spacing
