package com.autodroid.workscripts.ui

import com.autodroid.workscripts.utils.AppiumXmlConverter
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream

class MultiElementTest {

    @Test
    fun testConvertAppiumXmlToAndroidLayout_IncludesAllRequiredElements() {
        // Given: An Appium XML with multiple UI elements
        val appiumXml = """
            <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
            <hierarchy index="0" class="hierarchy" rotation="0" width="1080" height="1812">
                <android.widget.FrameLayout index="0" package="com.tdx.androidCCZQ" class="android.widget.FrameLayout" text="" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[0,0][1080,1920]" displayed="true" a11y-important="true" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843">
                    <!-- EditText element -->
                    <android.widget.EditText index="0" package="com.tdx.androidCCZQ" class="android.widget.EditText" text="" resource-id="searchBox" checkable="false" checked="false" clickable="true" enabled="true" focusable="true" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[126,243][1023,315]" displayed="true" hint="请输入证券代码或简拼" a11y-important="false" input-type="1" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843" />
                    
                    <!-- TextView element -->
                    <android.widget.TextView index="1" package="com.tdx.androidCCZQ" class="android.widget.TextView" text="股票搜索" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[447,99][633,177]" displayed="true" hint="" a11y-important="false" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843" />
                    
                    <!-- Button element -->
                    <android.widget.Button index="2" package="com.tdx.androidCCZQ" class="android.widget.Button" text="搜索" checkable="false" checked="false" clickable="true" enabled="true" focusable="true" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[900,243][1023,315]" displayed="true" hint="" a11y-important="false" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843" />
                    
                    <!-- ImageButton element -->
                    <android.widget.ImageButton index="3" package="com.tdx.androidCCZQ" class="android.widget.ImageButton" text="" checkable="false" checked="false" clickable="true" enabled="true" focusable="true" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[1000,100][1050,150]" displayed="true" hint="" a11y-important="false" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843" />
                    
                    <!-- ImageView element -->
                    <android.widget.ImageView index="4" package="com.tdx.androidCCZQ" class="android.widget.ImageView" text="" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[50,50][150,150]" displayed="true" hint="" a11y-important="false" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843" />
                </android.widget.FrameLayout>
            </hierarchy>
        """.trimIndent()

        // Create an AppiumXmlConverter to test the conversion
        val converter = AppiumXmlConverter()

        // When: Converting the Appium XML to Android layout
        val inputStream = ByteArrayInputStream(appiumXml.toByteArray())
        val result = converter.convertAppiumXmlToAndroidLayout(inputStream)
        
        // Print the result for verification
        println("Generated XML for multiple elements:")
        println(result)

        // Then: The result should contain all required elements
        assertTrue("Generated XML should contain EditText", result.contains("<EditText"))
        assertTrue("Generated XML should contain the hint attribute", result.contains("android:hint=\"请输入证券代码或简拼\""))
        
        assertTrue("Generated XML should contain TextView", result.contains("<TextView"))
        assertTrue("Generated XML should contain the text attribute for TextView", result.contains("android:text=\"股票搜索\""))
        
        assertTrue("Generated XML should contain Button", result.contains("<Button"))
        assertTrue("Generated XML should contain the text attribute for Button", result.contains("android:text=\"搜索\""))
        
        assertTrue("Generated XML should contain ImageButton", result.contains("<ImageButton"))
        
        assertTrue("Generated XML should contain ImageView", result.contains("<ImageView"))
        
        // Verify flat layout structure with GridLayout
        assertTrue("Generated XML should start with GridLayout", result.contains("<GridLayout"))
        assertTrue("Generated XML should have columnCount attribute", result.contains("android:columnCount=\"3\""))
    }
}