# ⚡ ShortcutMaker

אפליקציית אנדרואיד ל**קיצורי דרך מותאמים אישית** ו**תזמון משימות אוטומטיות** – פותחה ב-Kotlin + Jetpack Compose, נבנית ישירות מ-Termux.

---

## תכונות

### קיצורי דרך למסך הבית
| סוג | תיאור |
|-----|--------|
| 📞 חיוג ישיר | לחיצה אחת → מתחיל שיחה לאיש קשר |
| 🗺 ניווט Google Maps | פתיחת ניווט מפנה לנקודת יעד קבועה |
| 🚗 ניווט Waze | פתיחת Waze ישירות למסלול |
| 🌐 פתיחת URL | פתיחת כתובת אינטרנט בדפדפן |
| 📱 הפעלת אפליקציה | הפעלת כל אפליקציה לפי Package Name |
| ✉️ שליחת SMS | שליחת הודעה מוגדרת בלחיצה |

### תזמון משימות אוטומטיות
| סוג | תיאור |
|-----|--------|
| SMS מתוזמן | שליחת SMS בשעה מוגדרת |
| הודעת WhatsApp | פתיחת צ'אט עם טקסט מוכן |
| שיתוף מיקום | שליחת מיקום GPS נוכחי לוואטסאפ |

**מצבי חזרה:** פעם אחת / כל יום / ימים נבחרים בשבוע

---

## מבנה הפרויקט

```
ShortcutMaker/
├── app/src/main/
│   ├── AndroidManifest.xml          # הרשאות + רכיבי מערכת
│   └── java/com/shimon/shortcutmaker/
│       ├── MainActivity.kt           # נקודת כניסה
│       ├── data/
│       │   ├── Models.kt             # ShortcutConfig, ScheduledTask
│       │   └── AppRepository.kt      # DataStore persistence
│       ├── shortcuts/
│       │   ├── ShortcutCreator.kt    # [ShortcutLogic] יצירת קיצורים
│       │   └── ShortcutActionActivity.kt  # טיפול בהרשאות runtime
│       ├── receiver/
│       │   ├── SchedulerReceiver.kt  # [SchedulerReceiver] AlarmManager
│       │   └── BootReceiver.kt       # שחזור אלארמים אחרי ריבוט
│       ├── service/
│       │   └── LocationSharingService.kt  # [LocationService] GPS + WhatsApp
│       └── ui/
│           ├── theme/Theme.kt        # צבעים + טיפוגרפיה
│           └── screens/
│               ├── MainScreen.kt     # ממשק ראשי + טאבים
│               ├── ShortcutDialog.kt # דיאלוג יצירת קיצור
│               └── TaskDialog.kt     # דיאלוג יצירת משימה
├── setup_repo.sh    # הגדרת Git + GitHub בפעם הראשונה
├── build.sh         # בנייה והתקנה
└── .gitignore
```

---

## דרישות מערכת

- **Termux** עם `openjdk-17`, `gradle`, `git`
- **אנדרואיד** 8.0+ (API 26)
- **Google Play Services** (לשירותי מיקום)

---

## התקנה ובנייה מ-Termux

### פעם ראשונה – שכפול והגדרה

```bash
# שכפל את הריפוזיטורי
git clone https://github.com/shimonhoter/shortcut-maker.git ~/shimon
cd ~/shimon

# הורד gradle wrapper jar (נדרש פעם אחת)
wget -O gradle/wrapper/gradle-wrapper.jar \
  https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar

# בנה והתקן
chmod +x build.sh && ./build.sh
```

### בנייה רגילה

```bash
cd ~/shimon
./build.sh
```

### בנייה ידנית

```bash
cd ~/shimon
./gradlew assembleDebug --no-daemon
termux-open app/build/outputs/apk/debug/app-debug.apk
```

---

## פרוטוקול עבודה מול Claude (Merge V1.2)

הקוד מסומן בתגיות לעדכון קל:

| תגית | קובץ |
|------|------|
| `[START: AndroidManifest]` | `AndroidManifest.xml` |
| `[START: ShortcutLogic]` | `ShortcutCreator.kt` |
| `[START: SchedulerReceiver]` | `SchedulerReceiver.kt` |
| `[START: LocationService]` | `LocationSharingService.kt` |
| `[START: MainActivityLayout]` | `MainActivity.kt` |

---

## הרשאות נדרשות

| הרשאה | שימוש |
|--------|--------|
| `CALL_PHONE` | חיוג ישיר |
| `SEND_SMS` | שליחת SMS |
| `ACCESS_FINE_LOCATION` | GPS מדויק |
| `ACCESS_BACKGROUND_LOCATION` | שיתוף מיקום ברקע |
| `SCHEDULE_EXACT_ALARM` | תזמון מדויק |
| `RECEIVE_BOOT_COMPLETED` | שחזור אחרי ריבוט |
| `POST_NOTIFICATIONS` | התראות (Android 13+) |

---

## רישיון

MIT © Shimon Hoter
