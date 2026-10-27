package ayx.copy.utils

import ayx.copy.database.Command

/**
 * Pure logic helper for detecting and replacing shortcut commands inside
 * arbitrary text. Kept separate from the AccessibilityService so it can be
 * unit tested without needing an Android instrumentation environment.
 */
object ReplaceHelper {

    /**
     * Scans [currentText] for any command from [commands] that appears as a
     * "word" immediately before the cursor (i.e. at the end of the string,
     * since accessibility text-changed events give us the text as typed so far).
     *
     * A command is only considered a match if it is bounded by whitespace,
     * start-of-string, or common punctuation on the left, and is at the very
     * end of [currentText] (so we don't replace things mid-sentence while
     * still typing).
     *
     * Returns a [MatchResult] with the matched command and the text that
     * should replace it, or null if no match.
     */
    /**
     * Saved ".upi" and typed ",upi" are the SAME command, and vice versa.
     * The first char may be '.' or ',' — the word after it must match exactly.
     * Longer commands win, so ".upi2" is not shadowed by ".upi".
     */
    fun findMatch(currentText: String, commands: List<Command>): MatchResult? {
        if (currentText.isEmpty()) return null
        val sorted = commands.sortedByDescending { it.command.length }
        for (cmd in sorted) {
            val base = cmd.command.removePrefix(".").removePrefix(",")
            if (base.isBlank()) continue
            for (prefix in listOf(".", ",")) {
                val token = prefix + base
                if (currentText.endsWith(token)) {
                    val startIndex = currentText.length - token.length
                    val boundaryOk = startIndex == 0 || isBoundaryChar(currentText[startIndex - 1])
                    if (boundaryOk) {
                        return MatchResult(command = cmd, matchStart = startIndex, matchEnd = currentText.length)
                    }
                }
            }
        }
        return null
    }

    private fun isBoundaryChar(c: Char): Boolean {
        return c.isWhitespace() || c == '\n' || c == ',' || c == '.' && false || c == '('
    }

    /**
     * Builds the resulting full text after replacing the matched command
     * range with its replacement text.
     */
    fun buildReplacedText(fullText: String, match: MatchResult): String {
        val before = fullText.substring(0, match.matchStart)
        val after = fullText.substring(match.matchEnd)
        return before + match.command.replacementText + after
    }

    data class MatchResult(
        val command: Command,
        val matchStart: Int,
        val matchEnd: Int
    )

    // ---------------------------------------------------------------
    // Quick Copy / Quick Paste (v3+)
    //
    // NEW DESIGN — simpler and more practical:
    //
    //  ".." + space or Enter  → copies EVERYTHING in the text field to
    //                           clipboard, then CLEARS the field entirely.
    //                           Use case: you drafted a long message,
    //                           type ".." + space to grab it all.
    //
    //  "..." + space or Enter → pastes whatever is in the clipboard at
    //                           that position, replacing the "..." marker.
    //                           Use case: paste a previously copied text.
    //
    // Both fire at end-of-string OR when followed by a space/newline.
    // ---------------------------------------------------------------

    // Matches ".." (exactly two dots, NOT three) optionally followed by space/newline at end
    private val quickCopyRegex  = Regex("(?<!\\.)\\.\\.(\\.|[ \\n]|$)")
    // Matches "..." (exactly three dots) optionally followed by space/newline at end
    private val quickPasteRegex = Regex("\\.\\.\\.([ \\n]|$)")

    /**
     * Detects the ".." Quick Copy trigger at the end of [currentText].
     * Returns a [QuickCopyMatch] where [QuickCopyMatch.fullTextToCopy] is
     * everything in the field BEFORE the ".." marker.
     */
    fun findQuickCopyMatch(currentText: String): QuickCopyMatch? {
        // quickCopyRegex must not match "..." — the negative lookbehind handles that,
        // but also verify the char after ".." is not another dot.
        val result = quickCopyRegex.find(currentText) ?: return null
        // Ensure match is at the end of the string
        if (result.range.last + 1 != currentText.length) return null
        val afterDots = result.groupValues[1]
        // If the char right after ".." is another dot → it's "...", not ".."
        if (afterDots == ".") return null

        val matchStart = result.range.first
        val fullTextBefore = currentText.substring(0, matchStart).trim()
        // Nothing before the ".." → nothing to copy
        if (fullTextBefore.isEmpty()) return null

        return QuickCopyMatch(
            fullTextToCopy = fullTextBefore,
            matchStart = matchStart
        )
    }

    /**
     * Detects the "..." Quick Paste trigger at the end of [currentText].
     */
    fun findQuickPasteMatch(currentText: String): QuickPasteMatch? {
        val result = quickPasteRegex.find(currentText) ?: return null
        if (result.range.last + 1 != currentText.length) return null
        val delimiter = result.groupValues[1]
        return QuickPasteMatch(
            matchStart = result.range.first,
            matchEnd   = result.range.last + 1,
            delimiter  = delimiter
        )
    }

    /**
     * After Quick Copy fires: the field should be completely cleared
     * (the whole content was copied to clipboard).
     */
    fun buildAfterQuickCopy(): String = ""

    /**
     * After Quick Paste fires: replace the "..." marker with [clipboardText].
     */
    fun buildAfterQuickPaste(fullText: String, match: QuickPasteMatch, clipboardText: String): String {
        val before = fullText.substring(0, match.matchStart)
        val after  = fullText.substring(match.matchEnd)
        return before + clipboardText + match.delimiter + after
    }

    data class QuickCopyMatch(
        val fullTextToCopy: String,  // everything in the field before ".."
        val matchStart: Int          // index of the ".." in the original string
    )

    data class QuickPasteMatch(
        val matchStart: Int,
        val matchEnd: Int,
        val delimiter: String
    )
}
