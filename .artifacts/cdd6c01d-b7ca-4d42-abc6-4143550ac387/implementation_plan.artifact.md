# Implementation Plan - Fix Cuevana Redirects and Refine AdBlocker

Address the redirection issue on `cuevana3i.you` by refining the pop-up blocking and script filtering logic.

## User Review Required

> [!IMPORTANT]
> The redirection to the home page occurs because the website detects that its attempts to open a pop-up window were blocked. I will change the blocker to "silent mode" to trick the website into thinking the pop-up was successful, while still preventing the ad from appearing.

## Proposed Changes

### Main Activity

#### [MODIFY] [MainActivity.kt](file:///C:/Users/maria/AndroidStudioProjects/BrowseProPlayer/app/src/main/java/com/example/browseproplayer/MainActivity.kt)
- **WebChromeClient Improvement**:
    - Update `onCreateWindow` to return `true` instead of `false`.
    - This tells the browser engine that we "handled" the new window request, so it doesn't trigger error fallbacks on the page, but since we don't actually create a new WebView, nothing happens on screen.
- **WebViewClient Refinement**:
    - **Safe-list Main Domain**: Update `shouldInterceptRequest` to explicitly **allow** all requests originating from the main site's domain (e.g., if it contains `cuevana3i.you`), even if the path contains "ads" or "pop".
    - **Refined Script Filter**: Only block scripts containing "ads" or "pop" if they are coming from a different domain than the current page.
    - **URL Navigation Interception**: Implement `shouldOverrideUrlLoading` to ensure that any redirect happening in the main frame that leads to a known ad domain is blocked before it replaces the current page.
- **Improved Ad-Hiding**:
    - Enhance the JavaScript injection to run every second for a short period after the page loads, catching ads that are inserted late by dynamic scripts.

## Verification Plan

### Automated Tests
- `gradle_build(":app:assembleDebug")`.
- `analyze_file`.

### Manual Verification
- Go to `https://cuevana3i.you/`.
- Click on a server selection button.
- Verify that the page stays on the movie/server selection and DOES NOT redirect to the home page.
- Confirm that no ad pop-ups are visible.
- Verify that the video is still detected and can be played.
