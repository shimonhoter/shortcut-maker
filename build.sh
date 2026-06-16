#!/bin/bash
# =============================================================
# ShortcutMaker - Build Script for Termux
# =============================================================
set -e

PROJECT_DIR="$HOME/shimon"
DOWNLOADS="$HOME/storage/downloads"

echo "📦 מעתיק פרויקט אם נדרש..."
if [ ! -d "$PROJECT_DIR" ]; then
    mkdir -p "$PROJECT_DIR"
fi

echo "🔧 בונה APK..."
cd "$PROJECT_DIR"
chmod +x gradlew 2>/dev/null || true
./gradlew assembleDebug --no-daemon 2>&1

APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    cp "$APK_PATH" "$DOWNLOADS/ShortcutMaker.apk"
    echo ""
    echo "✅ הצלחה! APK נשמר ב: $DOWNLOADS/ShortcutMaker.apk"
    echo "📲 להתקנה:"
    echo "   termux-open $DOWNLOADS/ShortcutMaker.apk"
else
    echo "❌ הבנייה נכשלה"
    exit 1
fi
