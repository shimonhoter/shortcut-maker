#!/bin/bash
# =============================================================
# ShortcutMaker – setup_repo.sh
# הגדרת Git מקומי + חיבור ל-GitHub (הרץ פעם אחת)
# =============================================================

set -e

# ─── הגדרות ──────────────────────────────────────────────────
PROJECT_DIR="$HOME/shimon/ShortcutMaker"
GITHUB_USER="shimonhoter"
REPO_NAME="shortcut-maker"
REMOTE_URL="https://github.com/${GITHUB_USER}/${REPO_NAME}.git"

# ─── צבעים ───────────────────────────────────────────────────
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()    { echo -e "${CYAN}ℹ  $1${NC}"; }
success() { echo -e "${GREEN}✅ $1${NC}"; }
warn()    { echo -e "${YELLOW}⚠  $1${NC}"; }
error()   { echo -e "${RED}❌ $1${NC}"; exit 1; }

echo ""
echo -e "${CYAN}═══════════════════════════════════════════${NC}"
echo -e "${CYAN}  ⚡ ShortcutMaker – הגדרת ריפוזיטורי Git  ${NC}"
echo -e "${CYAN}═══════════════════════════════════════════${NC}"
echo ""

# ─── 1. בדיקת כלים ───────────────────────────────────────────
info "בודק כלים נדרשים..."
command -v git  >/dev/null 2>&1 || error "git לא מותקן. הרץ: pkg install git"
command -v java >/dev/null 2>&1 || error "java לא מותקן. הרץ: pkg install openjdk-17"
success "כלים OK"

# ─── 2. יצירת תיקיית הפרויקט ─────────────────────────────────
info "מגדיר תיקיות..."
mkdir -p "$PROJECT_DIR"

# ─── 3. הגדרת Git גלובלית (אם חסר) ──────────────────────────
if [ -z "$(git config --global user.name 2>/dev/null)" ]; then
    warn "פרטי Git חסרים – מגדיר..."
    git config --global user.name  "Shimon Hoter"
    git config --global user.email "shimon.hoter@gmail.com"
    success "Git user מוגדר"
else
    info "Git user: $(git config --global user.name)"
fi

# ─── 4. Git init מקומי ────────────────────────────────────────
cd "$PROJECT_DIR"

if [ ! -d ".git" ]; then
    info "מאתחל ריפוזיטורי מקומי..."
    git init
    git branch -M main
    success "git init הצליח"
else
    info "ריפוזיטורי מקומי קיים כבר"
fi

# ─── 5. .gitignore ──────────────────────────────────────────────
if [ ! -f ".gitignore" ]; then
    warn ".gitignore חסר – מעתיק מ-Downloads..."
    cp "$HOME/storage/downloads/ShortcutMaker/.gitignore" . 2>/dev/null || \
        warn ".gitignore לא נמצא, ימוסף ידנית"
fi

# ─── 6. הורדת gradle-wrapper.jar ─────────────────────────────
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
    info "מוריד gradle-wrapper.jar..."
    mkdir -p gradle/wrapper
    curl -fLo "$WRAPPER_JAR" \
      "https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar" \
      && success "gradle-wrapper.jar הורד" \
      || warn "הורדה נכשלה – בנה ידנית עם: gradle wrapper"
else
    info "gradle-wrapper.jar קיים"
fi

# ─── 7. Commit ראשון ──────────────────────────────────────────
info "מוסיף קבצים ל-Git..."
git add -A
if git diff --cached --quiet; then
    info "אין שינויים לעשות commit"
else
    git commit -m "feat: initial project setup – ShortcutMaker v1.0

- AndroidManifest + permissions (CALL_PHONE, SEND_SMS, LOCATION)
- Data models: ShortcutConfig, ScheduledTask
- AppRepository (DataStore JSON persistence)
- ShortcutCreator: DIAL / MAPS / WAZE / URL / APP / SMS
- ShortcutActionActivity: runtime permission handling
- SchedulerReceiver: AlarmManager with daily/weekly repeat
- BootReceiver: alarm restoration after reboot
- LocationSharingService: FusedLocation + WhatsApp deep-link
- Jetpack Compose UI: MainScreen + ShortcutDialog + TaskDialog
- Hebrew RTL theme (navy/cyan palette)"
    success "commit ראשון בוצע"
fi

# ─── 8. חיבור ל-GitHub Remote ────────────────────────────────
echo ""
info "מגדיר remote לגיטהאב: $REMOTE_URL"

if git remote get-url origin >/dev/null 2>&1; then
    CURRENT=$(git remote get-url origin)
    if [ "$CURRENT" != "$REMOTE_URL" ]; then
        warn "Remote קיים אבל שונה ($CURRENT) – מעדכן..."
        git remote set-url origin "$REMOTE_URL"
    else
        info "Remote כבר מוגדר נכון"
    fi
else
    git remote add origin "$REMOTE_URL"
    success "Remote נוסף"
fi

# ─── 9. הסבר על פוש ─────────────────────────────────────────
echo ""
echo -e "${YELLOW}══════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  📤 לדחוף ל-GitHub לראשונה:${NC}"
echo -e "${YELLOW}══════════════════════════════════════════════════${NC}"
echo ""
echo "  אפשרות א׳ – Personal Access Token (מומלץ):"
echo ""
echo "    1. היכנס ל: https://github.com/settings/tokens"
echo "    2. צור token עם הרשאת 'repo'"
echo "    3. שמור את ה-token ואז הרץ:"
echo ""
echo "       git push -u origin main"
echo "       # הכנס username: shimonhoter"
echo "       # הכנס password: [הtoken שיצרת]"
echo ""
echo "  אפשרות ב׳ – GitHub CLI (נוח יותר):"
echo ""
echo "    pkg install gh"
echo "    gh auth login"
echo "    gh repo create $REPO_NAME --public --source=. --push"
echo ""
echo -e "${YELLOW}══════════════════════════════════════════════════${NC}"

# ─── 10. סיכום תיקיות ────────────────────────────────────────
echo ""
echo -e "${GREEN}══════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ✅ הגדרה הושלמה!${NC}"
echo -e "${GREEN}══════════════════════════════════════════════════${NC}"
echo ""
echo "  📁 תיקיית פרויקט:  $PROJECT_DIR"
echo "  📁 תיקיית הורדות:  $HOME/storage/downloads"
echo "  📁 תיקיית Termux:  $HOME"
echo ""
echo "  פקודות שימושיות:"
echo "    cd ~/shimon        # כניסה לפרויקט"
echo "    git status         # בדיקת מצב"
echo "    git log --oneline  # היסטוריה"
echo "    ./build.sh         # בנייה + התקנה"
echo ""

# ─── 11. פתיחת תיקיות (אם termux-open זמין) ─────────────────
if command -v termux-open >/dev/null 2>&1; then
    info "פותח את תיקיית הפרויקט בניהול קבצים..."
    termux-open "$HOME/shimon" 2>/dev/null || true
fi

success "הכל מוכן! 🚀"
