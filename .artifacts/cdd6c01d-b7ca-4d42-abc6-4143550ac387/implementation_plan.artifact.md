# Implementation Plan - Fix White Splash Screen

Fix the white screen flicker during app startup by implementing a dark splash theme.

## Proposed Changes

### Resources

#### [NEW] [splash_background.xml](file:///C:/Users/maria/AndroidStudioProjects/BrowseProPlayer/app/src/main/res/drawable/splash_background.xml)
- Create a `layer-list` drawable with a black background and the app icon centered.

#### [MODIFY] [themes.xml](file:///C:/Users/maria/AndroidStudioProjects/BrowseProPlayer/app/src/main/res/values/themes.xml)
- Update `Theme.BrowseProPlayer` to set `android:windowBackground` to black to ensure consistent dark UI during transitions.
- Add a new style `Theme.BrowseProPlayer.Splash` that uses `@drawable/splash_background` as its window background.

### Android Manifest

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/maria/AndroidStudioProjects/BrowseProPlayer/app/src/main/AndroidManifest.xml)
- Change the `theme` of `MainActivity` to `Theme.BrowseProPlayer.Splash`.

### Main Activity

#### [MODIFY] [MainActivity.kt](file:///C:/Users/maria/AndroidStudioProjects/BrowseProPlayer/app/src/main/java/com/example/browseproplayer/MainActivity.kt)
- In `onCreate`, call `setTheme(R.style.Theme_BrowseProPlayer)` before `super.onCreate` to switch from the splash theme to the main theme once the activity starts.

## Verification Plan

### Manual Verification
- Deploy the app to a TV/Emulator.
- Close the app completely (force stop).
- Launch the app and verify that a black screen with the app icon appears instead of a white screen.
