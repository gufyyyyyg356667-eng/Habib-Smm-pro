# Lia AI — Setup Guide

## Step 1: Add font files

Put these font files in `app/src/main/res/font/`:

- `manrope_variable.ttf` — download free from Google Fonts: https://fonts.google.com/specimen/Manrope
- `instrument_serif_regular.ttf` — download free from Google Fonts: https://fonts.google.com/specimen/Instrument+Serif

Rename the downloaded files to exactly those names.

## Step 2: Add launcher icons

Put icon files in `app/src/main/res/`:
- `mipmap-hdpi/ic_launcher.png`
- `mipmap-xhdpi/ic_launcher.png`
- `mipmap-xxhdpi/ic_launcher.png`
- `mipmap-xxxhdpi/ic_launcher.png`
- Same for `ic_launcher_round.png`

Or use Android Studio: Right-click `res` → New → Image Asset.

## Step 3: Open in Android Studio

1. Open Android Studio (Hedgehog or newer)
2. File → Open → select the `LiaAI` folder
3. Wait for Gradle sync to finish
4. Select build variant: `directDebug` or `playDebug`
5. Run on device or emulator

## Step 4: Get a Gemini API Key

1. Go to https://aistudio.google.com/apikey
2. Create a free API key
3. In the app: Settings → Gemini API Key → paste it → Save Key

## Step 5: Build commands

```bash
# Debug builds
./gradlew assembleDirectDebug
./gradlew assemblePlayDebug

# Run unit tests
./gradlew testDirectDebugUnitTest
./gradlew testPlayDebugUnitTest

# Release (needs keystore.properties)
./gradlew assembleDirectRelease
./gradlew bundlePlayRelease
```

## Step 6: Release signing (optional)

Create `keystore.properties` in the project root (gitignored):

```properties
storeFile=../my-release.jks
storePassword=yourpassword
keyAlias=youralias
keyPassword=yourpassword
```

Generate keystore:
```bash
keytool -genkeypair -v -keystore my-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias youralias
```

## Build flavors

| Flavor | Features | Distribution |
|--------|----------|--------------|
| `direct` | All features including screen reading, social agent, accessibility | Website / sideload |
| `play` | Voice, chat, tools (no screen control) | Google Play Store |

## Architecture overview

```
voice/         → Gemini Live WebSocket, session manager, audio I/O, echo guard
data/          → API key store, preferences, personality repository
action/        → App launcher, contact resolver, tool executor (flavor-specific)
forge/         → Website builder: SSE streaming, stage detection, HTML utilities
ui/theme/      → Colors, typography, depth modifiers
ui/components/ → Reusable Compose components
ui/fx/         → Orb animations, edge glow
ui/screens/    → One package per screen
src/direct/    → Accessibility service, screen tools, social agent stubs
src/play/      → Stripped stubs, no accessibility
```

## Troubleshooting

**Font error at build**: Make sure font files are in `res/font/` with exact names.

**WebSocket never connects**: Check your Gemini API key is pasted correctly in Settings.

**Voice session silent**: Gemini Live requires `speechConfig` with a voice name — this is already set (Aoede).

**App crashes on launch**: Check logcat for missing resources. Most common cause: missing font files.
