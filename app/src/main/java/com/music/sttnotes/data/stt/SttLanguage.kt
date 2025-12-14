package com.music.sttnotes.data.stt

enum class SttLanguage(val code: String, val displayName: String) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String): SttLanguage {
            return entries.find { it.code == code } ?: FRENCH
        }
    }
}
