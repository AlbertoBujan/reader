# Riffle - Modern RSS Reader

**Riffle** is a modern, minimalist, and powerful RSS reader built for Android using Jetpack Compose. It focuses on a clean reading experience, enhanced by AI summaries and intuitive feed management.

![Riffle Banner](https://via.placeholder.com/1200x400.png?text=Riffle+RSS+Reader) *<!-- Replace with actual screenshot/banner if available -->*

## ✨ Features

### 📖 **Distraction-Free Reading**
*   **Clean UI**: A clutter-free interface designed for reading.
*   **Article View**: Read articles directly within the app with a simplified layout.
*   **Custom Typography**: Carefully selected fonts (**Manrope** for titles, **Merriweather** for body) for optimal legibility.
*   **Themes**: Beautiful "Soft Green" branded theme with full **Dark Mode** support.

### 🤖 **AI-Powered Summaries**
*   **Smart Summaries**: Get concise AI-generated summaries of articles using **Google Gemini** (Flash & Flash Lite models).
*   **Context Aware**: Summaries appear directly above the article content for quick consumption.

### ⚡ **Efficient Feed Management**
*   **Unified Discovery**: Search for feeds or add URLs directly from a single, unified dialog.
*   **Organization**: Group feeds into **Folders** to keep your reading list searching.
*   **Drag & Drop**: Intuitive drag-and-drop gestures to organize feeds into folders or reorder them.
*   **Swipe Actions**:
    *   **Swipe Right**: Share articles instantly.
    *   **Swipe Left**: Save to "Read Later".
    *   **Haptic Feedback**: Satisfying tactile feedback when swipe actions are triggered.
*   **Read Later**: Save interesting articles to a dedicated list for offline or later reading.

### 🛠 **Power User Tools**
*   **Mark as Read on Scroll**: Automatically mark articles as read as you scroll past them (configurable).
*   **Model Stats**: Monitor AI model usage and status.
*   **Customization**: Rename feeds and folders to suit your preferences.

## 🏗️ Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture principles
*   **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
*   **Local Database**: [Room](https://developer.android.com/training/data-storage/room)
*   **Networking**: [Retrofit](https://square.github.io/retrofit/) & OkHttp
*   **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
*   **AI Integration**: [Google AI Client SDK for Android](https://ai.google.dev/docs/android_quickstart) (Gemini)

## 🚀 Getting Started

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/riffle.git
    ```
2.  **Open in Android Studio**:
    Open the project folder in Android Studio (Ladybug or newer recommended).
3.  **Build and Run**:
    Run the app on an emulator or physical device.

## 🤝 Contributing

Contributions are welcome! Whether it's reporting a bug, suggesting a feature, or writing code.

1.  Fork the project.
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---
*Built with ❤️ by Alberto*
