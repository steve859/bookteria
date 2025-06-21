# Bookteria - Android E-Book Reader

A modern Android e-book reader application built with Jetpack Compose, following MVVM architecture pattern.

## Features

### ✅ Completed Features
- **Modern UI/UX**: Material 3 design with custom fonts (Lora, PT Serif)
- **Book Discovery**: Browse and search books from Gutenberg Project API
- **Search Functionality**: Real-time search with debouncing
- **Pagination**: Efficient loading with infinite scroll
- **Navigation**: Bottom navigation with 4 main tabs
- **Welcome Screen**: Beautiful onboarding experience
- **Network Handling**: Proper error handling and offline support
- **Database**: Room database for local storage (Library & Progress)

### 🏗️ Architecture
- **MVVM Pattern**: Clean separation of concerns
- **Jetpack Compose**: Modern declarative UI
- **Hilt**: Dependency injection
- **Room**: Local database
- **Retrofit/OkHttp**: Network requests
- **Navigation Compose**: Type-safe navigation

### 📱 Screens
1. **Welcome Screen**: Onboarding with beautiful background
2. **Home Screen**: Book discovery with search and pagination
3. **Categories Screen**: Browse books by categories
4. **Library Screen**: Personal book collection
5. **Settings Screen**: App preferences and configuration

### 🔧 Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Repository Pattern
- **Dependency Injection**: Hilt
- **Database**: Room
- **Networking**: OkHttp + Kotlinx Serialization
- **Navigation**: Navigation Compose
- **State Management**: Compose State

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Kotlin 1.8+
- Android SDK 24+

### Installation
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on device or emulator

## API Integration
The app integrates with the Gutenberg Project API for book data:
- Base URL: `https://gutenberg-backend-o7ffixyxs-steve859s-projects.vercel.app/books`
- Features: Search, pagination, category filtering

## Database Schema
- **Library**: Store user's book collection
- **Progress**: Track reading progress for each book

## Future Enhancements
- [ ] Book detail screen
- [ ] EPUB reader implementation
- [ ] Offline reading capability
- [ ] Reading progress tracking
- [ ] Bookmarks and annotations
- [ ] Dark/Light theme toggle
- [ ] Push notifications
- [ ] User preferences sync

## Contributing
Feel free to submit issues and enhancement requests!

## License
This project is licensed under the MIT License.
