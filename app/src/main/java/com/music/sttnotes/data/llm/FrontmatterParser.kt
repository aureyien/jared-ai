package com.music.sttnotes.data.llm

/**
 * Data class representing frontmatter metadata for KB files
 */
data class KbFileMeta(
    val created: String = "",
    val folder: String = "",
    val tags: List<String> = emptyList()
)

/**
 * Parser for YAML frontmatter in KB markdown files
 */
object FrontmatterParser {

    /**
     * Parse frontmatter from markdown content
     * Returns pair of (metadata, content without frontmatter)
     */
    fun parse(content: String): Pair<KbFileMeta, String> {
        val lines = content.lines()
        if (lines.isEmpty() || lines.first().trim() != "---") {
            return KbFileMeta() to content
        }

        val endIndex = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (endIndex < 0) {
            return KbFileMeta() to content
        }

        val frontmatterLines = lines.subList(1, endIndex + 1)
        val bodyContent = lines.drop(endIndex + 2).joinToString("\n")

        var created = ""
        var folder = ""
        var tags = emptyList<String>()

        frontmatterLines.forEach { line ->
            when {
                line.startsWith("created:") -> created = line.substringAfter(":").trim()
                line.startsWith("folder:") -> folder = line.substringAfter(":").trim()
                line.startsWith("tags:") -> {
                    val tagsStr = line.substringAfter(":").trim()
                    tags = parseTags(tagsStr)
                }
            }
        }

        return KbFileMeta(created, folder, tags) to bodyContent
    }

    /**
     * Parse tags from YAML array format [tag1, tag2] or comma-separated
     */
    private fun parseTags(tagsStr: String): List<String> {
        return if (tagsStr.startsWith("[") && tagsStr.endsWith("]")) {
            tagsStr.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } else {
            tagsStr.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }

    /**
     * Build frontmatter string from metadata
     */
    fun buildFrontmatter(meta: KbFileMeta): String {
        return buildString {
            appendLine("---")
            if (meta.created.isNotEmpty()) appendLine("created: ${meta.created}")
            if (meta.folder.isNotEmpty()) appendLine("folder: ${meta.folder}")
            if (meta.tags.isNotEmpty()) appendLine("tags: [${meta.tags.joinToString(", ")}]")
            appendLine("---")
            appendLine()
        }
    }

    /**
     * Combine frontmatter with content
     */
    fun combine(meta: KbFileMeta, content: String): String {
        return buildFrontmatter(meta) + content
    }
}
