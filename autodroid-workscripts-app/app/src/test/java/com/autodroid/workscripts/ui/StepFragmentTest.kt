package com.autodroid.workscripts.ui

import com.autodroid.workscripts.utils.AppiumXmlConverter
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream

class StepFragmentTest {

    @Test
    fun testConvertAppiumXmlToAndroidLayout_IncludesEditTextWithHint() {
        // Given: An Appium XML with an EditText element that has a hint
        val appiumXml = """
            <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
            <hierarchy index="0" class="hierarchy" rotation="0" width="1080" height="1812">
                <android.widget.FrameLayout index="0" package="com.tdx.androidCCZQ" class="android.widget.FrameLayout" text="" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[0,0][1080,1920]" displayed="true" a11y-important="true" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843">
                    <android.view.View index="0" package="com.tdx.androidCCZQ" class="android.view.View" text="" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[0,0][1080,1812]" displayed="true" hint="" a11y-important="false" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843">
                        <android.view.View index="1" package="com.tdx.androidCCZQ" class="android.view.View" text="" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[126,243][1023,315]" displayed="true" hint="" a11y-important="false" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843">
                            <android.widget.EditText index="0" package="com.tdx.androidCCZQ" class="android.widget.EditText" text="" resource-id="searchBox" checkable="false" checked="false" clickable="true" enabled="true" focusable="true" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[126,243][1023,315]" displayed="true" hint="请输入证券代码或简拼" a11y-important="false" input-type="1" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843" />
                        </android.view.View>
                    </android.view.View>
                </android.widget.FrameLayout>
            </hierarchy>
        """.trimIndent()

        // Create an AppiumXmlConverter to test the conversion
        val converter = AppiumXmlConverter()

        // When: Converting the Appium XML to Android layout
        val inputStream = ByteArrayInputStream(appiumXml.toByteArray())
        val result = converter.convertAppiumXmlToAndroidLayout(inputStream, 1080, 1920, 3.0f)
        
        // Print the result for verification
        println("Generated XML:")
        println(result)

        // Then: The result should contain an EditText with the hint
        assertTrue("Generated XML should contain EditText", result.contains("<EditText"))
        assertTrue("Generated XML should contain the hint attribute", result.contains("android:hint=\"请输入证券代码或简拼\""))
    }

    @Test
    fun testConvertAppiumXmlToAndroidLayout_IncludesTextViewWithText() {
        // Given: An Appium XML with a TextView element that has text
        val appiumXml = """
            <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
            <hierarchy index="0" class="hierarchy" rotation="0" width="1080" height="1812">
                <android.widget.FrameLayout index="0" package="com.tdx.androidCCZQ" class="android.widget.FrameLayout" text="" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[0,0][1080,1920]" displayed="true" a11y-important="true" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843">
                    <android.widget.TextView index="0" package="com.tdx.androidCCZQ" class="android.widget.TextView" text="股票搜索" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[447,99][633,177]" displayed="true" hint="" a11y-important="false" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843" />
                </android.widget.FrameLayout>
            </hierarchy>
        """.trimIndent()

        // Create an AppiumXmlConverter to test the conversion
        val converter = AppiumXmlConverter()

        // When: Converting the Appium XML to Android layout
        val inputStream = ByteArrayInputStream(appiumXml.toByteArray())
        val result = converter.convertAppiumXmlToAndroidLayout(inputStream, 1080, 1920, 3.0f)
        
        // Print the result for verification
        println("Generated XML for TextView:")
        println(result)

        // Then: The result should contain a TextView with the text
        assertTrue("Generated XML should contain TextView", result.contains("<TextView"))
        assertTrue("Generated XML should contain the text attribute", result.contains("android:text=\"股票搜索\""))
    }

    @Test
    fun testConvertAppiumXmlToAndroidLayout_IncludesButtonWithText() {
        // Given: An Appium XML with a Button element that has text
        val appiumXml = """
            <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
            <hierarchy index="0" class="hierarchy" rotation="0" width="1080" height="1812">
                <android.widget.FrameLayout index="0" package="com.tdx.androidCCZQ" class="android.widget.FrameLayout" text="" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[0,0][1080,1920]" displayed="true" a11y-important="true" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843">
                    <android.widget.Button index="0" package="com.tdx.androidCCZQ" class="android.widget.Button" text="搜索" checkable="false" checked="false" clickable="true" enabled="true" focusable="true" focused="false" long-clickable="false" password="false" scrollable="false" selected="false" bounds="[900,243][1023,315]" displayed="true" hint="" a11y-important="false" drawing-order="0" showing-hint="false" dismissable="false" a11y-focused="false" live-region="0" context-clickable="false" content-invalid="false" window-id="843" />
                </android.widget.FrameLayout>
            </hierarchy>
        """.trimIndent()

        // Create an AppiumXmlConverter to test the conversion
        val converter = AppiumXmlConverter()

        // When: Converting the Appium XML to Android layout
        val inputStream = ByteArrayInputStream(appiumXml.toByteArray())
        val result = converter.convertAppiumXmlToAndroidLayout(inputStream, 1080, 1920, 3.0f)
        
        // Print the result for verification
        println("Generated XML for Button:")
        println(result)

        // Then: The result should contain a Button with the text
        assertTrue("Generated XML should contain Button", result.contains("<Button"))
        assertTrue("Generated XML should contain the text attribute", result.contains("android:text=\"搜索\""))
    }
}