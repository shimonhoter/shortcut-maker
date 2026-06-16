#!/bin/bash
# =============================================================
# install_shortcutmaker.sh
# הרץ מ-Downloads אחרי הורדת הארכיב מ-Claude
# =============================================================

set -e

DOWNLOADS="$HOME/storage/downloads"
DEST="$HOME/shimon"
ARCHIVE="$DOWNLOADS/ShortcutMaker.tar.gz"

GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}ℹ  $1${NC}"; }
success() { echo -e "${GREEN}✅ $1${NC}"; }

echo ""
echo -e "${CYAN}⚡ ShortcutMaker – התקנה${NC}"
echo ""

# פרוס את הארכיב
if [ -f "$ARCHIVE" ]; then
    info "מחלץ ארכיב..."
    mkdir -p "$HOME/shimon"
    tar xzf "$ARCHIVE" -C "$HOME/shimon/" --strip-components=1
    success "חולץ ל-$DEST"
else
    info "ארכיב לא נמצא – מניח שהקבצים כבר ב-$DEST"
fi

# הגדרת הרשאות הפעלה
chmod +x "$DEST/setup_repo.sh" "$DEST/build.sh" "$DEST/gradlew" 2>/dev/null || true

# הרץ setup
info "מפעיל setup_repo.sh..."
bash "$DEST/setup_repo.sh"
