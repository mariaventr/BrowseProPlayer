# Implementation Plan - Fix "Browser Cracked" and "Long Error" UI Issues

Improve WebView stability and error handling to prevent renderer crashes and "unresponsive" error states.

## User Review Required

> [!IMPORTANT]
> The "browser cracked" error likely stems from the WebView's rendering process crashing or being detected as a bot by the website. We will implement robust error handling and mimic a standard browser to avoid this.

## Proposed Changes

### Dependencies
- Add `androidx.webkit:webkit:1.12.0` to `libs.versions.toml` and `app/build.gradle.kts`.

### Main Activity

#### [MODIFY] [MainActivity.kt](file:///C:/Users/maria/AndroidStudioProjects/BrowseProPlayer/app/src/main/java/com/example/browseproplayer/MainActivity.kt)
- **WebView Setup**:
    - Set a standard Chrome User-Agent.
    - Enable `databaseEnabled` and `javaScriptCanOpenWindowsAutomatically`.
- **WebViewClient Improvements**:
    - Implement `onRenderProcessGone` to handle renderer crashes by reloading the page.
    - Implement `onReceivedError` to show a user-friendly message instead of the default error page.
    - Implement `onReceivedHttpError` for better error tracking.
- **Video Detection Panel**:
    - Improve title extraction logic to truncate long names and provide fallback titles.
    - Add a "Clear" button to the panel to allow users to dismiss it.

### Player Activity

#### [MODIFY] [PlayerActivity.kt](file:///C:/Users/maria/AndroidStudioProjects/BrowseProPlayer/app/src/main/java/com/example/browseproplayer/PlayerActivity.kt)
- **Error Handling**: Truncate long error messages in the `Toast` to avoid UI clutter.

## Verification Plan

### Automated Tests
- Build the project: `gradle_build(":app:assembleDebug")`.
- Analyze files for syntax errors: `analyze_file`.

### Manual Verification
- Launch the app and browse to `https://www.playhubmax.com/`.
- Verify that the "browser cracked" error no longer appears (or is handled gracefully).
- Verify that if a network error occurs, a short and friendly message is displayed.
- Verify the video detection panel works as expected with various video URLs.
