package com.example.riffle.utils

import android.util.Xml
import com.example.riffle.data.local.entity.SourceEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.io.StringWriter

object OpmlUtility {

    data class OpmlFolder(val title: String, val children: List<OpmlSource>)
    data class OpmlSource(val title: String, val xmlUrl: String, val htmlUrl: String?)

    sealed class OpmlItem {
        data class Folder(val title: String, val children: List<Source>) : OpmlItem()
        data class Source(val title: String, val xmlUrl: String, val htmlUrl: String?) : OpmlItem()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): List<OpmlItem> {
        inputStream.use { stream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, null)
            parser.nextTag()
            return readOpml(parser)
        }
    }

    private fun readOpml(parser: XmlPullParser): List<OpmlItem> {
        val items = mutableListOf<OpmlItem>()
        parser.require(XmlPullParser.START_TAG, null, "opml")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "body") {
                items.addAll(readBody(parser))
            } else {
                skip(parser)
            }
        }
        return items
    }

    private fun readBody(parser: XmlPullParser): List<OpmlItem> {
        val items = mutableListOf<OpmlItem>()
        parser.require(XmlPullParser.START_TAG, null, "body")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "outline") {
                items.add(readOutline(parser))
            } else {
                skip(parser)
            }
        }
        return items
    }

    private fun readOutline(parser: XmlPullParser): OpmlItem {
        parser.require(XmlPullParser.START_TAG, null, "outline")
        val title = parser.getAttributeValue(null, "title") ?: parser.getAttributeValue(null, "text") ?: ""
        val type = parser.getAttributeValue(null, "type")
        val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
        val htmlUrl = parser.getAttributeValue(null, "htmlUrl")

        if (xmlUrl != null) {
            // It's a source
            skip(parser) // Move to end tag
            return OpmlItem.Source(title, xmlUrl, htmlUrl)
        } else {
            // It might be a folder or just a container
            val children = mutableListOf<OpmlItem.Source>()
            
            // Check if it has children (nested outlines)
            // But we must check if it's an empty tag or not. 
            // XmlPullParser handling of empty tags <outline ... /> depends on implementation, usually next() goes to END_TAG if empty?
            // Safer to check logic inside loop.
            
            // Actually, if it has no xmlUrl, likely a folder. We iterate until end tag.
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                if (parser.name == "outline") {
                    val child = readOutline(parser)
                    if (child is OpmlItem.Source) {
                        children.add(child)
                    }
                    // We flatten nested folders for now as our model is 1-level deep? 
                    // Or we just ignore nested folders inside folders if our model doesn't support it.
                    // The app supports 1 level of folders.
                } else {
                    skip(parser)
                }
            }
            return OpmlItem.Folder(title, children)
        }
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) throw IllegalStateException()
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    fun generate(sources: List<SourceEntity>): String {
        val writer = StringWriter()
        val xmlSerializer = Xml.newSerializer()
        xmlSerializer.setOutput(writer)
        xmlSerializer.startDocument("UTF-8", true)
        xmlSerializer.text("\n")
        xmlSerializer.startTag(null, "opml")
        xmlSerializer.attribute(null, "version", "1.0")
        xmlSerializer.text("\n  ")
        
        xmlSerializer.startTag(null, "head")
        xmlSerializer.text("\n    ")
        xmlSerializer.startTag(null, "title")
        xmlSerializer.text("Riffle Export")
        xmlSerializer.endTag(null, "title")
        xmlSerializer.text("\n  ")
        xmlSerializer.endTag(null, "head")
        
        xmlSerializer.text("\n  ")
        xmlSerializer.startTag(null, "body")

        // Group by folder
        val grouped = sources.groupBy { it.folderName }
        
        // 1. Folders
        grouped.filterKeys { it != null }.forEach { (folderName, folderSources) ->
            xmlSerializer.text("\n    ")
            xmlSerializer.startTag(null, "outline")
            xmlSerializer.attribute(null, "text", folderName!!);
            xmlSerializer.attribute(null, "title", folderName);
            
            folderSources.forEach { source ->
                xmlSerializer.text("\n      ")
                writeSourceOutline(xmlSerializer, source)
            }
            xmlSerializer.text("\n    ")
            xmlSerializer.endTag(null, "outline")
        }

        // 2. Orphan sources
        grouped[null]?.forEach { source ->
            xmlSerializer.text("\n    ")
            writeSourceOutline(xmlSerializer, source)
        }

        xmlSerializer.text("\n  ")
        xmlSerializer.endTag(null, "body")
        xmlSerializer.text("\n")
        xmlSerializer.endTag(null, "opml")
        xmlSerializer.endDocument()
        
        return writer.toString()
    }

    private fun writeSourceOutline(serializer: org.xmlpull.v1.XmlSerializer, source: SourceEntity) {
        serializer.startTag(null, "outline")
        serializer.attribute(null, "type", "rss")
        serializer.attribute(null, "text", source.title)
        serializer.attribute(null, "title", source.title)
        serializer.attribute(null, "xmlUrl", source.url)
        // We don't have htmlUrl stored in SourceEntity, can omit or duplicate xmlUrl/title if mostly RSS
        serializer.endTag(null, "outline")
    }
}
