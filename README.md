# 🧪 Water Sort Puzzle (Android)

A feature-rich, high-performance casual puzzle game for Android built with native Java and Material Design 3 components.

---

## ✨ Key Features

- **🎮 1000 Progressive Levels**: A carefully balanced campaign scaling from 3 colors (easy) up to 10 colors and 1 empty tube (Grandmaster difficulty).
- **✨ Smooth Custom Animations**: Custom `BoardView` featuring tube tilt rotation (`ValueAnimator`) and flowing liquid streams during pouring.
- **💡 Hint System**: Intelligent helper that suggests valid moves and highlights source and destination tubes with a gold border.
- **💰 Coin Economy & Star Rating**: Earn coins for every completed level, evaluate performance with 1–3 stars (`⭐ ⭐ ⭐`), and spend coins on hints.
- **🎲 Random Gameplay Events**: Dynamic events distributed across levels:
  - **🕵️ Hidden Colors (`?`)**: Bottom water layers are shrouded in mystery until top layers are poured away.
  - **⏱️ Time Limit (Timer)**: High-stakes countdown timers for master levels.
- **🔄 Undo & Extra Tube**: Unlimited undo stack and a one-time use extra tube power-up per level.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Java
- **UI Framework**: Android Views, Custom `Canvas` Rendering, Material Design 3 (`MaterialButton`, `MaterialCardView`, `MaterialAlertDialogBuilder`)
- **Animation**: `ValueAnimator`, `AccelerateDecelerateInterpolator`, `AnimatorListenerAdapter`
- **Data Persistence**: `SharedPreferences` (`Prefs` utility for level unlocks, audio/vibration settings, and coin wallet)

---

## 📁 Project Structure

```text
app/src/main/java/com/example/watersort/
├── BoardView.java          # Custom canvas game board renderer, touch handling, & animations
├── GameActivity.java       # Game loop, timer, move counter, win/loss dialogs
├── LevelSelectActivity.java # Grid-based level selection adapter (3 columns)
├── MainActivity.java       # Home screen, settings, how-to-play dialogs
├── LevelConfig.java        # Random event generator (Hidden colors & Timers)
└── Prefs.java              # Local storage helper for progress, settings, and coins
```

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/Conmeosoma/WaterSort.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and run the app on an Android Emulator or physical device (`minSdkVersion 24+`).

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
