# Android Studio Project Setup

## Issue: Empty Module Dropdown

If you're experiencing an empty "Module:" dropdown in Android Studio, follow these steps:

### Quick Fix

1. Close Android Studio if it's currently open
2. Run the setup script:
   ```
   setup-project.bat
   ```
3. Reopen Android Studio
4. Select "Open an Existing Project"
5. Navigate to this directory (autodroid-app)
6. Click "OK"
7. Wait for the project to sync with Gradle
8. The Module dropdown should now show the "app" module

### Manual Fix

If the script doesn't work, try these manual steps:

1. Close Android Studio
2. Delete the `.idea` directory:
   ```
   rm -rf .idea
   ```
3. Delete the `.gradle` directory:
   ```
   rm -rf .gradle
   ```
4. Clean and rebuild the project:
   ```
   ./gradlew clean
   ./gradlew build
   ```
5. Reopen Android Studio and import the project again

### Why This Happens

This issue typically occurs when:
- The `.idea` directory becomes corrupted
- Gradle sync fails to complete properly
- The project structure is not correctly recognized by Android Studio

By deleting the `.idea` and `.gradle` directories and rebuilding, you force Android Studio to recreate these files with the correct configuration.

### Project Structure

This is a standard Android project with:
- Root project: autodroid-app
- App module: app

The correct configuration should show "app" in the Module dropdown in Android Studio.