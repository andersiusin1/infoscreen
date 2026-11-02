# Infoscreen for Android TV

A small Android TV app that shows the infoscreen player full screen in a WebView. It is sideloaded with `adb`. It is not on the Play Store.

- Loads `https://infoscreen-1bc.pages.dev` by default. You can change the URL on the TV or over adb.
- Shows up in the Android TV launcher as **Infoscreen**.
- Keeps the screen on while the app is open.
- JavaScript, DOM storage (`localStorage`) and autoplay without a user gesture are on, so the player behaves the same as in a desktop browser.
- If the page fails to load (for example, no network right after boot), it retries every 15 s.

Package: `no.andersius.infoscreen`. minSdk 21, target/compile SDK 36. No dependencies beyond the Kotlin stdlib.

## Remote control

| Key | What it does |
|---|---|
| OK, ←, → | Passed to the page. The player uses them for next and previous. |
| Hold OK (about 2 s) | Opens the feed URL screen |
| Menu (if the remote has one) | Opens the feed URL screen |
| Back | Goes back one page in the WebView if it can. Otherwise it closes the app. |

## Build a debug APK

You need JDK 17 or newer and the Android SDK, either from Android Studio or the command-line tools.

1. Tell Gradle where the SDK is. Android Studio does this for you. From the command line, set it yourself:
   ```sh
   export ANDROID_HOME=$HOME/Android/Sdk          # Linux
   # export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
   ```
   You can also put `sdk.dir=/path/to/sdk` in `android/local.properties`.

2. **One time only: finish the Gradle wrapper.** The repo has `gradlew` and `gradlew.bat`, but not `gradle/wrapper/gradle-wrapper.jar` or `gradle/wrapper/gradle-wrapper.properties`. With any Gradle installed (`brew install gradle`, `sdk install gradle`, …), run:
   ```sh
   cd android
   gradle wrapper --gradle-version 8.14.3
   ```
   Then commit the generated `gradle/wrapper/*` files. Opening `android/` in Android Studio also works, because it offers to create the wrapper.

3. Build:
   ```sh
   cd android
   ./gradlew assembleDebug
   ```
   The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Install on the Sharp Android TV

1. On the TV, turn on developer options: **Settings → Device Preferences → About**, then press OK on **Build** seven times. Menu names vary a little between models.
2. Under **Settings → Device Preferences → Developer options**, turn on **USB debugging**. On models that have it, also turn on **Network debugging** / **ADB over network**.
3. Find the TV's IP address under **Settings → Network & Internet → (your network)**.
4. From your computer, on the same network:
   ```sh
   adb connect 192.168.1.50:5555          # use the TV's IP
   # Accept the "Allow USB debugging?" prompt on the TV (tick "Always allow")
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n no.andersius.infoscreen/.MainActivity
   ```
   `-r` replaces an installed copy and keeps its saved URL. Run the same command again to update the app.

On first start the app shows the feed URL screen with the default URL filled in and **Save** focused. Press OK to accept it.

Uninstall: `adb uninstall no.andersius.infoscreen`

## Change the feed URL

**On the TV:** hold OK for about 2 seconds, or press Menu. Edit the URL and choose **Save**. **Use default** puts back `https://infoscreen-1bc.pages.dev`. If you leave out `https://`, it is added for you.

**Over adb** (no remote needed):
```sh
adb shell am start -n no.andersius.infoscreen/.MainActivity --es url https://example.pages.dev
```
The URL is saved and the page reloads right away.

## Kiosk tips

- Turn off the TV's screensaver/ambient mode and "auto power off".
- Debug builds allow remote inspection. Open `chrome://inspect` in desktop Chrome while connected over adb to see the page's console.
- The app does not start on boot by itself. If you need that, set it up with the TV's own "start app on power on" setting where available, or ask for a boot receiver to be added.
