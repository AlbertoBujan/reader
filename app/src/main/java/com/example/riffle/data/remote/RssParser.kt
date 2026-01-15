package com.example.riffle.data.remote

import android.util.Xml
import com.example.riffle.data.local.entity.ArticleEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

class RssParser {

    private val ns: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream, sourceUrl: String): List<ArticleEntity> {
        inputStream.use {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(it, null)
            parser.nextTag()
            return readFeed(parser, sourceUrl)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser, sourceUrl: String): List<ArticleEntity> {
        val entries = mutableListOf<ArticleEntity>()

        parser.require(XmlPullParser.START_TAG, ns, "rss")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Look for the <channel> tag
            if (parser.name == "channel") {
                 entries.addAll(readChannel(parser, sourceUrl))
            } else {
                skip(parser)
            }
        }
        return entries
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readChannel(parser: XmlPullParser, sourceUrl: String): List<ArticleEntity> {
        val entries = mutableListOf<ArticleEntity>()
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (parser.name == "item") {
                entries.add(readEntry(parser, sourceUrl))
            } else {
                skip(parser)
            }
        }
        return entries
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
                "title" -> title = readText(parser, "title")
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
}
