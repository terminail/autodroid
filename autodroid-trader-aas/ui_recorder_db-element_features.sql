PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE `element_features` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `package_name` TEXT NOT NULL, `activity_name` TEXT, `element_signature` TEXT NOT NULL, `element_id` TEXT, `element_type` TEXT NOT NULL, `element_text` TEXT, `element_hint` TEXT, `element_content_desc` TEXT, `element_class` TEXT, `parent_hierarchy` TEXT, `sibling_info` TEXT, `common_values` TEXT, `last_used_time` INTEGER NOT NULL, `usage_count` INTEGER NOT NULL, `auto_fill_enabled` INTEGER NOT NULL, `auto_fill_value` TEXT);
COMMIT;