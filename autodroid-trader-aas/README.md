# Autodroid AAS (Accessibility Automation Service)

Autodroid AAS is an Android Accessibility Service application that records user interactions in Android apps and stores UI element characteristics in a local database. The service monitors user operations such as clicks, inputs, and selections in specified apps, capturing control information features for automation testing and UI analysis.

## 🚀 Features

- **Comprehensive UI Event Monitoring**: Captures various UI events including clicks, text inputs, selections, scrolls, long clicks, focus changes, and window state changes
- **Detailed Element Feature Extraction**: Records comprehensive element information including ID, type, text, content description, class, bounds, parent hierarchy, and sibling information
- **Flexible Recording Configuration**: Supports recording for specific apps or all apps with configurable settings
- **Local Database Storage**: Uses Room database to store UI events and element features with full CRUD operations
- **Screenshot Support**: Optional screenshot capture for each recorded event
- **Floating Control Panel**: Provides real-time control and monitoring of the service

## 📋 Prerequisites

- Android API Level 21+ (Android 5.0)
- Accessibility Service permission (user must enable in Settings)
- Storage permission (for screenshot functionality)

## 🛠️ Architecture

### Core Components

```
UIRecorderAccessibilityService - Main accessibility service class
├── UIEventProcessor - Event processing and feature extraction
├── ElementAnalyzer - Element analysis engine
├── ScreenshotHelper - Screenshot capture utility
├── UIRecorderDatabase - Room database implementation
├── UIEventDao - UI event data access object
├── ElementFeatureDao - Element feature data access object
└── AppConfigDao - Application configuration data access object
```

### Data Models

- **UIEvent**: Stores UI interaction events with detailed metadata
- **ElementFeature**: Captures UI element characteristics for automation
- **AppConfig**: Configuration settings for specific applications

## 🏗️ Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/autodroid-trader-aas.git
   ```

2. Open the project in Android Studio

3. Build and install the APK on your Android device

4. Enable the accessibility service:
   - Go to Settings > Accessibility
   - Find "Autodroid AAS" and enable it
   - Grant necessary permissions

## 📱 Usage

### Starting the Service
1. Launch the Autodroid AAS app
2. Tap "Start Service" (if accessibility permission isn't granted, you'll be prompted to enable it)
3. The service will begin monitoring specified apps (or all apps if no restrictions are set)

### Viewing Records
1. Use the main interface to view recent events
2. Navigate to the "Records" screen to see all captured events
3. Access the "Element Features" screen to review extracted UI element characteristics

### Configuration
- Configure which apps to monitor through the app settings
- Enable/disable specific event types (clicks, inputs, selections, etc.)
- Configure screenshot capture settings
- Set up automatic value filling for frequently used inputs

## 🔧 Configuration Options

- **Target Applications**: Specify which apps to monitor
- **Event Types**: Enable/disable specific event types (clicks, inputs, scrolls, etc.)
- **Screenshot Capture**: Enable/disable screenshot capture for each event
- **Screenshot Quality**: Configure screenshot quality (1-100)
- **Auto-fill Features**: Enable automatic value filling based on recorded features

## 📊 Data Storage

All recorded data is stored in a local Room database with the following tables:

- **ui_events**: Stores all UI interaction events
- **element_features**: Stores extracted element characteristics
- **app_configs**: Stores application-specific configurations

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

If you encounter any issues or have questions, please file an issue in the repository.

## 🔄 Updates

- **v1.0.0**: Initial release with core functionality
- **v1.1.0**: Added floating control panel and improved element analysis
- **v1.2.0**: Enhanced database schema and added auto-fill capabilities