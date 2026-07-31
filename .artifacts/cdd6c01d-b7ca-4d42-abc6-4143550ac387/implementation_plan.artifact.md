# Implementation Plan - Replace Logo with Loading Spinner and Fix White Flash

Remove the splash logo, add an animated loading spinner, and eliminate the white flicker that occurs while the WebView is initializing.

## Proposed Changes

### Resources

#### [MODIFY] [splash_background.xml](file:///C:/Users/maria/AndroidStudioProjects/BrowseProPlayer/app/src/main/res/drawable/splash_background.xml)
- Remove the `<item>` with the bitmap logo. Keep only the black background for a seamless transition.

### Main Activity

#### [MODIFY] [MainActivity.kt](file:///C:/Users/maria/AndroidStudioProjects/BrowseProPlayer/app/src/main/java/com/example/browseproplayer/MainActivity.kt)
- **WebView Initialization**:
    - Set `webView.setBackgroundColor(android.graphics.Color.BLACK)` to prevent the default white rendering before content loads.
- **Loading State**:
    - Add `var isPageLoading by remember { mutableStateOf(true) }`.
    - Update `WebViewClient` to set `isPageLoading = false` in `onPageFinished`.
- **UI Structure**:
    - Wrap the `AndroidView` in a `Box` with a `background(Color.Black)`.
    - Add a `CircularProgressIndicator` (the "rueda") centered in the `Box`, visible only when `isPageLoading` is true.
    - Use `AnimatedVisibility` or simple `if` to transition between the spinner and the WebView content if needed, though keeping the WebView visible behind the spinner is usually smoother.

## Verification Plan

### Manual Verification
1.  **Cold Start**: Force stop and launch the app.
2.  **Transition**: Verify the screen goes from Black (Splash) -> Black with Spinner -> Website.
3.  **No White Flash**: Confirm there is zero white flicker during the entire process.
4.  **Completion**: Ensure the spinner disappears exactly when the website content becomes visible.
