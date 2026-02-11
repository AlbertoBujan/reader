package com.boaxente.riffle.util

import java.util.regex.Pattern

/**
 * Extension function to extract the first image URL from an HTML string.
 * It looks for the 'src' attribute within an 'img' tag.
 *
 * @return The URL of the first image found, or null if no image is found.
 */
fun String.extractFirstImageUrl(): String? {
    // Regex to match <img ... src="..." ...>
    // Valid for both single and double quotes around the URL.
    // Group 2 will contain the URL if double quotes are used.
    // Group 3 will contain the URL if single quotes are used.
    val imgRegex = "<img\\s+[^>]*src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>"
    val pattern = Pattern.compile(imgRegex, Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(this)

    return if (matcher.find()) {
        matcher.group(1)
    } else {
        null
    }
}
