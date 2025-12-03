@echo off
echo Setting up Android Studio project...

echo.
echo 1. Cleaning and rebuilding the project...
call gradlew clean
call gradlew build

echo.
echo 2. Project setup complete!
echo.
echo To fix the empty Module dropdown in Android Studio:
echo 1. Close Android Studio if it's open
echo 2. Open Android Studio
echo 3. Select "Open an Existing Project"
echo 4. Navigate to this directory: %CD%
echo 5. Click "OK"
echo 6. Wait for the project to sync with Gradle
echo 7. The Module dropdown should now show "app" module
echo.
pause