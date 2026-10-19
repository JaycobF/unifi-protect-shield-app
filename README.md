# Shield Home

A custom home-screen launcher for the NVIDIA Shield TV (and other Android TV / Google TV
devices) with a look that blends **Google TV** and a touch of **Apple tvOS**.

![Home](app/screenshots/home.png)

| Browsing lower rows | All apps | Long-press menu |
| --- | --- | --- |
| ![Rows](app/screenshots/home_row2.png) | ![Apps](app/screenshots/apps.png) | ![Options](app/screenshots/options.png) |

_Screenshots are rendered headlessly with placeholder apps; on a device the tiles show each app's real banner._

## Design

- **Google TV:** greeting + tab bar across the top, a featured "hero" area for the focused
  app that collapses as you browse further down, horizontal rows (Favorites / Apps / Games),
  tabs that switch as soon as they receive focus, and an ambient colour wash that picks up
  the focused app's artwork colour.
- **tvOS:** frosted pill tab bar, large rounded 16:9 tiles that lift and scale on focus with a
  glossy highlight and a light sweep (a nod to tvOS parallax), a "top shelf"-style floating
  artwork, and a context menu on long-press.

## Features

- Lists every launchable app (TV-native apps use their banner; phone apps get a generated tile
  from their icon and dominant colour).
- **Favorites** row, seeded on first run (UniFi Protect, YouTube, Netflix, Plex, Disney+, …
  if installed). Hold **OK** on any tile to add/remove favourites, reorder them, or open App info.
- **Apps** tab with a full grid of everything installed; **Games** detected via the app category.
- Live clock/date, Settings shortcut, and automatic refresh when apps are installed, updated or removed.
- Back never exits the launcher; it returns to the Home tab.

## Build & install

Requires JDK 17+ and the Android SDK (platform 35).

```bash
./gradlew assembleRelease          # app/build/outputs/apk/release/app-release.apk (debug-signed)
adb connect <shield-ip>
adb install -r app/build/outputs/apk/release/app-release.apk
```

Enable **Developer options → Network debugging** on the Shield to use `adb connect`.

### Making it the default home screen

Shield Home registers as a `HOME` launcher, so it can be picked whenever Android asks which
home app to use. Recent Shield / Google TV firmware sends the remote's Home button straight to
the stock launcher, though. Common workarounds:

- Use an app such as **Button Mapper** to remap the Home button to launch Shield Home, or
- Disable the stock launcher over adb (reversible, at your own risk):
  `adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx`
  (re-enable with `pm enable`). On older non-Google-TV Shield builds the stock launcher is
  `com.google.android.tvlauncher`.

Shield Home also appears in the regular app list, so it can be opened like any other app.

## Development

- Kotlin + Jetpack Compose for TV (`androidx.tv:tv-material`).
- Screenshot tests (Robolectric + Roborazzi) render the UI without a device:

  ```bash
  ./gradlew recordRoborazziDebug   # regenerate app/screenshots/*.png
  ```

```
app/src/main/java/com/shieldtv/launcher/
├── MainActivity.kt          # hosts the UI, launches apps, listens for package changes
├── LauncherViewModel.kt     # app list + favourites state
├── data/AppRepository.kt    # queries installed apps, banners, icons, accent colours
├── data/FavoritesStore.kt   # persisted, ordered favourites
└── ui/                      # TopBar, HeroShelf, AppCard, AppOptionsDialog, LauncherScreen
```
