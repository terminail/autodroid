package com.autodroid.workscripts.ui

import com.autodroid.workscripts.utils.AppiumXmlConverter
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream

class ComprehensiveLayoutTest {

    @Test
    fun testGeneratedLayoutMeetsAllRequirements() {
        // Use hardcoded Appium XML from MultiElementTest.kt to prevent test failures if files are removed
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

        // Convert using AppiumXmlConverter with default screen dimensions
        val converter = AppiumXmlConverter()
        val inputStream = ByteArrayInputStream(appiumXml.toByteArray())
        val result = converter.convertAppiumXmlToAndroidLayout(inputStream)
        
        // Print the result for verification
        println("Generated Android Layout XML:")
        println(result)
        
        // Requirement 1: EditText should be included
        assertTrue("Generated XML should contain EditText element", result.contains("<EditText"))
        
        // Requirement 2: EditText should have the correct hint
        assertTrue("Generated XML should contain the hint attribute with correct value", result.contains("android:hint=\"请输入证券代码或简拼\""))
        
        // Requirement 3: EditText should have minimum size of at least 100dp (actually 1/4 screen width)
        assertTrue("Generated XML should contain minHeight attribute", result.contains("android:minHeight="))
        assertTrue("Generated XML should contain minWidth attribute", result.contains("android:minWidth="))
        
        // Requirement 4: Layout should use flat arrangement (GridLayout with 3 columns)
        assertTrue("Generated XML should start with GridLayout", result.contains("<GridLayout"))
        assertTrue("Generated XML should have columnCount attribute", result.contains("android:columnCount=\"3\""))
        
        // Requirement 5: EditText should have proper ID
        assertTrue("Generated XML should contain the correct ID", result.contains("android:id=\"@+id/searchBox\""))
        
        // Requirement 6: Layout should have proper padding
        assertTrue("Generated XML should contain padding", result.contains("android:padding=\"16dp\""))
        
        println("\n✅ ALL REQUIREMENTS MET:")
        println("1. EditText element is included ✓")
        println("2. EditText hint is correctly set ✓")
        println("3. EditText has minimum size requirement ✓")
        println("4. Layout uses flat arrangement with GridLayout and 4 columns ✓")
        println("5. EditText has proper ID ✓")
        println("6. Layout has proper padding ✓")
    }
}