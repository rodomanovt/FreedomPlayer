package com.rodomanovt.freedomplayer.helpers

object TrackNameUtils {

    private val KEEP_WORDS = setOf(
        "remix", "remastered", "remaster", "feat", "featuring", "ft",
        "slowed", "reverb", "speed", "sped", "sped up", "lofi", "live",
        "acoustic", "instrumental", "cover", "edit", "extended", "radio"
    )

    private val NOISE_WORDS = setOf(
        "official", "music", "video", "audio", "lyric", "lyrics",
        "premiere", "original", "single", "version", "hd", "hq",
        " - Topic", "TikTok", "visualizer"
    )

    fun getSmartSongName(channel: String?, name: String?): Pair<String, String> {
        val safeName = name ?: "Unknown Title"
        val safeChannel = channel ?: "Unknown Artist"

        // Пытаемся извлечь из name в любом случае
        var (artist, title) = parseName(safeName)

        // Если не удалось — fallback на channel
        if (artist.isBlank() || artist.lowercase().trim() == "unknown") {
            artist = cleanPart(safeChannel, isArtist = true)
        }

        if (title.isBlank() || title.lowercase().trim() == "unknown") {
            title = cleanPart(safeName, isArtist = false)
        }

        artist = removeSpecialChars(artist)
        title = removeSpecialChars(title)

        if (artist.isNotBlank() && !channel.isNullOrBlank()) {
            title = title.replace(artist, "", ignoreCase = true)
                .replace(channel, "", ignoreCase = true)
            title = title.replace('-', ' ')
        }

        // Final cleanup after all replacements
        artist = artist.trim().replace(Regex("\\s+"), " ")
        title = title.trim().replace(Regex("\\s+"), " ")

        if (artist.isBlank()) artist = "Unknown Artist"
        if (title.isBlank()) title = "Unknown Title"

        return Pair(artist, title)
    }

    private fun parseName(name: String): Pair<String, String> {
        val trimmedName = name.trim()

        // Сначала пробуем кавычки
        val quoteRegex = Regex("""[«“"](.+?)[»”"]""")
        val quoteMatch = quoteRegex.find(trimmedName)
        if (quoteMatch != null) {
            val titlePart = quoteMatch.groupValues[1].trim()
            val before = trimmedName.substring(0, quoteMatch.range.first).trim()
            val after = trimmedName.substring(quoteMatch.range.last + 1).trim()

            var artistCandidate = "$before $after".trim()
            if (before.isNotBlank() && looksLikeArtist(before)) {
                artistCandidate = before
            }

            return Pair(
                cleanPart(artistCandidate, isArtist = true),
                cleanPart(titlePart, isArtist = false)
            )
        }

        // Затем пробуем " - "
        if (trimmedName.contains("-")) {
            val parts = trimmedName.split("-", limit = 2)
            if (parts.size >= 2 && looksLikeArtist(parts[0])) {
                return Pair(
                    cleanPart(parts[0], isArtist = true),
                    cleanPart(parts[1], isArtist = false)
                )
            }
        }

        return Pair("", trimmedName)
    }

    private fun cleanPart(text: String, isArtist: Boolean): String {
        if (text.isBlank()) return text

        var cleaned = text

        // Clean brackets
        val bracketRegex = Regex("""[\(\[\{](.*?)[\)\]\}]""")
        cleaned = bracketRegex.replace(cleaned) { matchResult ->
            val content = matchResult.groupValues[1]
            val contentLower = content.lowercase().trim()
            val words = Regex("""\b\w+\b""").findAll(contentLower).map { it.value }.toList()
            if (words.any { it in KEEP_WORDS }) {
                " (${content.trim()})"
            } else {
                ""
            }
        }

        // Noise words
        val noisePattern = "\\b(?i)(" + NOISE_WORDS.joinToString("|") { Regex.escape(it) } + ")\\b"
        cleaned = Regex(noisePattern).replace(cleaned, " ")

        // Hashtags
        cleaned = Regex("""\s*#(?:\w+)""").replace(cleaned, " ")

        // Trim special chars at start/end
        cleaned = cleaned.trim { it.isWhitespace() || it == '-' || it == '_' || it == '.' }

        // Collapse spaces
        cleaned = cleaned.replace(Regex("""\s+"""), " ")

        return if (cleaned.isBlank()) "Unknown" else cleaned
    }

    private fun looksLikeArtist(text: String): Boolean {
        if (text.isBlank()) return false
        val textLower = text.lowercase()
        val badForArtist = setOf(
            "official", "video", "audio", "lyric", "phonk", "music", "remix",
            "slowed", "reverb", "edit", "version", "single", "premiere",
            "title", "song", "track", "hq", "hd"
        )
        return badForArtist.none { it in textLower }
    }

    private fun removeSpecialChars(filename: String): String {
        var result = filename
        for (char in "<>:\"/\\|?*") {
            result = result.replace(char.toString(), "")
        }
        return result.trim(' ', '.')
    }
}
