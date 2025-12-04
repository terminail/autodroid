// 这个脚本用于强制 Java 语言服务器重新导入项目
// 在 VSCode 中按 F1 打开命令面板，然后运行 "Java: Force Java Language Server to reload project"

// 或者手动执行以下步骤：
// 1. 关闭 VSCode
// 2. 删除以下目录：
//    - %USERPROFILE%\.vscode\extensions\redhat.java-*
//    - %USERPROFILE%\.vscode\server\*
//    - d:\git\autodroid\.classpath
//    - d:\git\autodroid\.settings
// 3. 重新打开 VSCode
// 4. 等待 Java 语言服务器重新初始化

console.log("请按照以下步骤操作：");
console.log("1. 在 VSCode 中按 Ctrl+Shift+P");
console.log("2. 输入 'Java: Clean Java Language Server Workspace'");
console.log("3. 选择并执行该命令");
console.log("4. 重新加载 VSCode 窗口");
console.log("5. 等待 Java 语言服务器重新初始化");