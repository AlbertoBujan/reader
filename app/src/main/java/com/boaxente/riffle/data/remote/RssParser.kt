package com.boaxente.riffle.data.remote

import android.util.Xml
import com.boaxente.riffle.data.local.entity.ArticleEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import com.boaxente.riffle.util.RiffleLogger

data class ParsedFeed(
    val articles: List<ArticleEntity>,
    val imageUrl: String? = null,
    val siteUrl: String? = null
)

class RssParser {

    private val ns: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream, sourceUrl: String): ParsedFeed {
        inputStream.use {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(it, null)
            parser.nextTag()
            return readFeed(parser, sourceUrl)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser, sourceUrl: String): ParsedFeed {
        var entries = listOf<ArticleEntity>()
        var imageUrl: String? = null
        var siteUrl: String? = null

        // Flexible root tag check
        if (parser.name == "rss") {
             while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                if (parser.name == "channel") {
                     val result = readChannel(parser, sourceUrl)
                     entries = result.articles
                     imageUrl = result.imageUrl
                     siteUrl = result.siteUrl
                } else {
                    skip(parser)
                }
            }
        } else if (parser.name == "feed") {
            // Basic Atom support just in case, though structure is different.
             val result = readChannel(parser, sourceUrl) // Reusing readChannel/readEntry logic best effort
             entries = result.articles
             imageUrl = result.imageUrl
        } else {
            // Try to proceed if possible or throw
        }

        return ParsedFeed(entries, imageUrl, siteUrl)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readChannel(parser: XmlPullParser, sourceUrl: String): ParsedFeed {
        val entries = mutableListOf<ArticleEntity>()
        var imageUrl: String? = null
        var siteUrl: String? = null
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "item" -> entries.add(readEntry(parser, sourceUrl))
                "image" -> imageUrl = readImage(parser)
                "link" -> siteUrl = readText(parser, "link")
                else -> skip(parser)
            }
        }
        return ParsedFeed(entries, imageUrl, siteUrl)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readImage(parser: XmlPullParser): String? {
        parser.require(XmlPullParser.START_TAG, ns, "image")
        var url: String? = null
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "url") {
                url = readText(parser, "url")
            } else {
                skip(parser)
            }
        }
        return url
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEntry(parser: XmlPullParser, sourceUrl: String): ArticleEntity {
        parser.require(XmlPullParser.START_TAG, ns, "item")
        var title: String? = null
        var link: String? = null
        var description: String? = null
        var pubDateStr: String? = null
        var imageUrl: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "title" -> title = cleanHtmlTags(readText(parser, "title"))
                "link" -> link = readText(parser, "link")
                "description" -> description = readText(parser, "description")
                "pubDate" -> pubDateStr = readText(parser, "pubDate")
                "media:content" -> {
                     imageUrl = parser.getAttributeValue(null, "url")
                     skip(parser) 
                }
                "enclosure" -> {
                    val type = parser.getAttributeValue(null, "type")
                    if (type != null && type.startsWith("image/")) {
                        imageUrl = parser.getAttributeValue(null, "url")
                    }
                    skip(parser)
                }
                else -> skip(parser)
            }
        }
        
        val safeTitle = title ?: "No Title"
        val safeLink = link ?: ""
        val safeDesc = description ?: ""
        val safeDate = parseDate(pubDateStr)

        return ArticleEntity(
            link = safeLink,
            title = safeTitle,
            description = safeDesc,
            pubDate = safeDate,
            sourceUrl = sourceUrl,
            imageUrl = imageUrl
        )
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser, tagName: String): String {
        parser.require(XmlPullParser.START_TAG, ns, tagName)
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            // Truncate if too long to avoid CursorWindow errors
            if (result.length > 100000) {
                result = result.substring(0, 100000)
            }
            parser.nextTag()
        }
        parser.require(XmlPullParser.END_TAG, ns, tagName)
        return result
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null) return System.currentTimeMillis()
        val formats = arrayOf(
            SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US),
            SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US)
        )
        
        for (format in formats) {
            try {
                return format.parse(dateStr)?.time ?: continue
            } catch (e: Exception) {
                RiffleLogger.recordException(e)
            }
        }
        return System.currentTimeMillis()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun cleanHtmlTags(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").trim()
    }
}

