package com.autodroid.trader.ime;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TraderImeService extends AccessibilityService {
    private static final String TAG = "TraderAccessibility";
    private static final String PREFS_NAME = "TraderAccessibilityPrefs";
    private static final String LAST_DUMP_PATH = "last_dump_path";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Log accessibility events to ensure the service is working
        Log.d(TAG, "Received accessibility event: " + event.getEventType());

        // This is needed to ensure the service is properly initialized
        // but we don't want to dump on every event, only when requested
    }

    private void saveAccessibilityNodeInfoToFile() {
        // 使用新的API获取更完整的窗口内容（如果API级别支持）
        AccessibilityNodeInfo root = null;
        root = getRootInActiveWindow();
        if (root == null) {
            // 如果getRootInActiveWindow返回null，尝试通过当前活动获取
            root = findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        }

        if (root != null) {
            try {
                String jsonString = convertAccessibilityNodeToJson(root);
                String fileName = "trader_ime_dump.json";

                // 使用应用的内部存储空间
                File dumpDir = new File(getFilesDir(), "autodroid_dumps");
                if (!dumpDir.exists()) {
                    dumpDir.mkdirs();
                }

                // 清理旧的 dump 文件，避免文件堆积
                cleanupOldDumpFiles(dumpDir);

                File dumpFile = new File(dumpDir, fileName);
                FileWriter writer = new FileWriter(dumpFile);
                writer.write(jsonString);
                writer.close();

                // 保存最后导出的文件路径到 SharedPreferences
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(LAST_DUMP_PATH, dumpFile.getAbsolutePath());
                editor.apply();

                Log.d(TAG, "Accessibility node info saved to: " + dumpFile.getAbsolutePath());
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error saving accessibility node info: " + e.getMessage());
            } finally {
                root.recycle();
            }
        } else {
            Log.w(TAG, "Could not get root node for accessibility dump");
        }
    }

    private void cleanupOldDumpFiles(File dumpDir) {
        // 获取目录中的所有文件
        File[] files = dumpDir.listFiles();
        if (files != null) {
            for (File file : files) {
                // 删除所有 .json 文件，但保留当前正在使用的 trader_ime_dump.json
                if (file.getName().endsWith(".json") && !file.getName().equals("trader_ime_dump.json")) {
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old dump file: " + file.getName());
                    } else {
                        Log.w(TAG, "Failed to delete old dump file: " + file.getName());
                    }
                }
            }
        }
    }

    private String convertAccessibilityNodeToJson(AccessibilityNodeInfo node) throws JSONException {
        if (node == null) {
            return "null";
        }

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");

        // 添加节点的基本属性 (使用与XML相同的属性名，only if they have meaningful values)
        appendJsonProperty(jsonBuilder, "class", node.getClassName() != null ? node.getClassName().toString() : "");
        appendJsonProperty(jsonBuilder, "text", node.getText() != null ? node.getText().toString() : "");
        appendJsonProperty(jsonBuilder, "content-desc",
                node.getContentDescription() != null ? node.getContentDescription().toString() : "");
        appendJsonProperty(jsonBuilder, "resource-id",
                node.getViewIdResourceName() != null ? node.getViewIdResourceName() : "");
        appendJsonProperty(jsonBuilder, "package",
                node.getPackageName() != null ? node.getPackageName().toString() : "");

        // Add all boolean properties (to match XML format), but only if not default
        // values
        appendJsonBooleanPropertyFull(jsonBuilder, "checkable", node.isCheckable(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "checked", node.isChecked(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "clickable", node.isClickable(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "enabled", node.isEnabled(), true);
        appendJsonBooleanPropertyFull(jsonBuilder, "focusable", node.isFocusable(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "focused", node.isFocused(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "scrollable", node.isScrollable(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "long-clickable", node.isLongClickable(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "password", node.isPassword(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "selected", node.isSelected(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "editable", node.isEditable(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "multiline", node.isMultiLine(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "dismissable", node.isDismissable(), false);
        appendJsonBooleanPropertyFull(jsonBuilder, "visibleToUser", node.isVisibleToUser(), true);

        jsonBuilder.append("\"index\":0,"); // 默认索引

        // 添加边界信息 - 使用与XML相同的格式: [left,top][right,bottom]
        Rect boundsRect = new Rect();
        node.getBoundsInScreen(boundsRect);
        jsonBuilder.append("\"bounds\":\"[").append(boundsRect.left).append(",").append(boundsRect.top)
                .append("][").append(boundsRect.right).append(",").append(boundsRect.bottom).append("]\",");

        // 添加子节点
        jsonBuilder.append("\"children\":[");
        boolean hasChildren = false;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (hasChildren) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append(convertAccessibilityNodeToJson(child));
                hasChildren = true;
                // Recycle child after processing to free memory
                child.recycle();
            }
        }
        jsonBuilder.append("]");

        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    private void appendJsonProperty(StringBuilder builder, String key, String value) {
        if (value != null && !value.isEmpty()) {
            builder.append("\"").append(escapeJsonString(key)).append("\":\"")
                    .append(escapeJsonString(value)).append("\",");
        }
    }

    private void appendJsonBooleanProperty(StringBuilder builder, String key, boolean value) {
        if (value) { // Only add if true to reduce size
            builder.append("\"").append(escapeJsonString(key)).append("\":").append(value).append(",");
        }
    }

    private void appendJsonBooleanPropertyFull(StringBuilder builder, String key, boolean value, boolean defaultValue) {
        if (value != defaultValue) { // Only add if different from default to reduce size
            builder.append("\"").append(escapeJsonString(key)).append("\":").append(value).append(",");
        }
    }

    private String escapeJsonString(String str) {
        if (str == null)
            return "";
        // Use a more robust approach that handles Unicode characters properly
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    // Check if the character needs Unicode escaping
                    if (ch < ' ' || ch > 127) { // Non-ASCII characters
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    private static TraderImeService instance;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility service connected");
        instance = this;

        // 配置服务参数
        AccessibilityServiceInfo config = getServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        config.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        config.notificationTimeout = 100;
        setServiceInfo(config);

        Log.d(TAG, "Service connected, creating test file to verify functionality");
        // Create a test file to verify the service is working
        try {
            File testFile = new File(getFilesDir(), "service_connected_test.txt");
            FileWriter writer = new FileWriter(testFile);
            writer.write("Service connected at: " + new Date().toString());
            writer.close();
            Log.d(TAG, "Test file created successfully: " + testFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error creating test file: " + e.getMessage());
        }

        // Check if there's a pending dump request
        if (pendingDumpRequest) {
            Log.d(TAG, "Processing pending dump request");
            pendingDumpRequest = false;
            saveAccessibilityNodeInfoToFile();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "Accessibility service destroyed");
    }

    private static boolean pendingDumpRequest = false;

    public static void requestAccessibilityDump(Context context) {
        Log.d(TAG, "requestAccessibilityDump called statically");
        if (instance != null) {
            Log.d(TAG, "Calling saveAccessibilityNodeInfoToFile on instance");
            instance.saveAccessibilityNodeInfoToFile();
        } else {
            Log.w(TAG, "Service instance is null, setting pending request flag");
            pendingDumpRequest = true;
            // Start the service to ensure it gets connected
            Intent intent = new Intent(context, TraderImeService.class);
            // The service should be started by the system when accessibility events occur
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }
}