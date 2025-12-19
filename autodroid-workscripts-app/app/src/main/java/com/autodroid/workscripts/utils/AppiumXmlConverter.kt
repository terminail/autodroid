package com.autodroid.workscripts.utils

import android.graphics.Rect
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.InputStream
import java.io.StringWriter

class AppiumXmlConverter {

    /**
     * Converts Appium XML to Android layout XML with default screen dimensions (1080x1920, density 3.0)
     * This is for backward compatibility with existing tests
     */
    fun convertAppiumXmlToAndroidLayout(inputStream: InputStream): String {
        // Default values for testing
        val defaultScreenWidth = 1080
        val defaultScreenHeight = 1920
        val defaultDensity = 3.0f
        return convertAppiumXmlToAndroidLayout(inputStream, defaultScreenWidth, defaultScreenHeight, defaultDensity)
    }

    /**
     * Converts Appium XML to Android layout XML with specified screen dimensions
     */
    fun convertAppiumXmlToAndroidLayout(inputStream: InputStream, screenWidth: Int, screenHeight: Int, density: Float): String {
        // Try to create parser, fallback to test environment if needed
        val parser = try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            factory.newPullParser()
        } catch (e: Exception) {
            try {
                // Fallback to default newInstance() method
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                factory.newPullParser()
            } catch (fallbackException: Exception) {
                // In test environment, we might not have Android framework available
                throw RuntimeException("Failed to create XML parser. In test environment, make sure xmlpull dependency is available.", e)
            }
        }
        parser.setInput(inputStream, null)
        parser.nextTag()

        // Try to create serializer, fallback to test environment if needed
        val serializer = try {
            val factory = XmlPullParserFactory.newInstance()
            factory.newSerializer()
        } catch (e: Exception) {
            try {
                // Fallback to default newInstance() method
                val factory = XmlPullParserFactory.newInstance()
                factory.newSerializer()
            } catch (fallbackException: Exception) {
                // In test environment, we might not have Android framework available
                throw RuntimeException("Failed to create XML serializer. In test environment, make sure xmlpull dependency is available.", e)
            }
        }
        val writer = StringWriter()
        serializer.setOutput(writer)

        // Start document with proper XML declaration
        serializer.startDocument("UTF-8", null)
        serializer.startTag(null, "GridLayout")
        serializer.attribute(null, "xmlns:android", "http://schemas.android.com/apk/res/android")
        serializer.attribute(null, "android:layout_width", "match_parent")
        serializer.attribute(null, "android:layout_height", "match_parent")
        serializer.attribute(null, "android:padding", "16dp")
        serializer.attribute(null, "android:columnCount", "3")

        // Parse the hierarchy and convert to Android layout with flat arrangement
        parseAndConvertElementsFlat(parser, serializer, screenWidth, screenHeight, density)

        // End the root GridLayout
        serializer.endTag(null, "GridLayout")
        serializer.endDocument()

        return writer.toString()
    }

    private fun parseAndConvertElementsFlat(parser: XmlPullParser, serializer: XmlSerializer, screenWidth: Int, screenHeight: Int, density: Float) {
        var eventType = parser.eventType
        var elementIndex = 0

        // Calculate half screen width and quarter screen width in dp
        val halfScreenWidthDp = "${(screenWidth / 2 / density).toInt()}dp"
        val quarterScreenWidthDp = "${(screenWidth / 4 / density).toInt()}dp"

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    val className = parser.getAttributeValue(null, "class")
                    val bounds = parser.getAttributeValue(null, "bounds")
                    val text = parser.getAttributeValue(null, "text")
                    val hint = parser.getAttributeValue(null, "hint")
                    val resourceId = parser.getAttributeValue(null, "resource-id")

                    // Skip the hierarchy element as it's just a container
                    if (tagName == "hierarchy") {
                        eventType = parser.next()
                        continue
                    }

                    // Convert to Android view class name
                    val androidViewName = convertToAndroidViewClass(className)

                    // Only process key UI elements that users interact with
                    if (androidViewName != null && isKeyUIElement(androidViewName)) {
                        // Check if element has meaningful content
                        val hasText = !text.isNullOrEmpty()
                        val hasHint = !hint.isNullOrEmpty()

                        // Create element if it has text, hint, or is an ImageButton/ImageView
                        if (hasText || hasHint || androidViewName in listOf("ImageButton", "ImageView")) {
                            // Start the tag for the element
                            serializer.startTag(null, androidViewName)

                            // Set basic layout parameters
                            // Maximum width should be half the screen width
                            serializer.attribute(null, "android:layout_width", halfScreenWidthDp)
                            serializer.attribute(null, "android:layout_height", "wrap_content")
                            serializer.attribute(null, "android:layout_marginBottom", "8dp")

                            // Ensure minimum size for visibility (at least 1/4 screen width)
                            serializer.attribute(null, "android:minHeight", quarterScreenWidthDp)
                            serializer.attribute(null, "android:minWidth", quarterScreenWidthDp)

                            // Parse bounds to get actual dimensions
                            val boundsArray = parseBounds(bounds)
                            val width = boundsArray[2] - boundsArray[0]  // right - left
                            val height = boundsArray[3] - boundsArray[1] // bottom - top

                            // Set ID if available
                            if (!resourceId.isNullOrEmpty()) {
                                val androidId = if (resourceId.contains("/")) {
                                    resourceId.split("/")[1]
                                } else {
                                    resourceId
                                }
                                // Fix invalid resource IDs that start with numbers
                                val validId = if (androidId.matches(Regex("^[0-9].*"))) {
                                    "id_$androidId"
                                } else {
                                    androidId
                                }
                                serializer.attribute(null, "android:id", "@+id/$validId")
                            }

                            // Set text if available
                            if (hasText && androidViewName in listOf("TextView", "Button", "EditText")) {
                                serializer.attribute(null, "android:text", text)
                                serializer.attribute(null, "android:textColor", "#000000")
                                serializer.attribute(null, "android:textSize", "16sp")
                            }

                            // Set hint for EditText if available
                            if (androidViewName == "EditText" && hasHint) {
                                serializer.attribute(null, "android:hint", hint)
                                // Ensure EditText is visible even without text
                                serializer.attribute(null, "android:background", "#F0F0F0")
                                serializer.attribute(null, "android:padding", "12dp")
                            }

                            // Add contentDescription for ImageButton
                            if (androidViewName == "ImageButton") {
                                serializer.attribute(null, "android:contentDescription", text ?: "Image button")
                            }

                            // Set background for better visibility
                            if (androidViewName != "EditText") {
                                serializer.attribute(null, "android:background", "#FFFFFF")
                            }

                            // Set padding for better appearance
                            if (androidViewName !in listOf("EditText", "ImageButton")) {
                                serializer.attribute(null, "android:padding", "12dp")
                            }

                            // Close the tag
                            serializer.endTag(null, androidViewName)

                            elementIndex++
                        }
                    }

                    eventType = parser.next()
                }
                else -> {
                    eventType = parser.next()
                }
            }
        }
    }

    private fun isKeyUIElement(androidViewName: String): Boolean {
        // Focus only on key UI elements that users interact with
        return androidViewName in listOf("TextView", "Button", "EditText", "ImageButton", "ImageView")
    }

    private fun convertToAndroidViewClass(className: String?): String? {
        return when {
            className.isNullOrEmpty() -> null
            className == "android.view.View" -> "View"
            className == "android.widget.TextView" -> "TextView"
            className == "android.widget.Button" -> "Button"
            className == "android.widget.EditText" -> "EditText"
            className == "android.widget.ImageView" -> "ImageView"
            className == "android.widget.Image" -> "ImageView"  // Handle android.widget.Image as ImageView
            className == "android.widget.ImageButton" -> "ImageButton"
            else -> null  // Ignore other elements for simplicity
        }
    }

    private fun parseBounds(bounds: String?): IntArray {
        if (bounds.isNullOrEmpty()) {
            return intArrayOf(0, 0, 100, 100)
        }

        // Bounds format: [left,top][right,bottom]
        val regex = Regex("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]")
        val matchResult = regex.find(bounds)

        if (matchResult != null) {
            val (left, top, right, bottom) = matchResult.destructured
            return intArrayOf(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        }

        return intArrayOf(0, 0, 100, 100)
    }

    // Entity class for UI elements
    data class UiElement(
        val className: String,
        val text: String?,
        val hint: String? = null,
        val bounds: Rect,
        val children: MutableList<UiElement> = mutableListOf()
    )
}