# Implementation Plan - Professional Video Panel & Native Focus Navigation

This plan aims to transform the "Detected Videos" panel into a professional, large-scale UI and solve the interaction conflict between the virtual cursor and the video list.

## User Review Required

> [!IMPORTANT]
> **Mode Switching**: When the video panel appears, the virtual cursor will be **disabled**. You will be able to navigate the video list using the standard remote control highlight (Focus). To return to the web browser and the cursor, you can press the **Left** arrow or the **Back** button.

## Proposed Changes

### 1. High-Fidelity UI Redesign
- **`DetectedVideosPanel`**:
    - Increased width and height to match the reference image professional look.
    - Background: Deep black with high opacity and a subtle white border.
    - Header: Blue play icon next to "Detected Videos".
    - Domain Toggle: A dedicated card showing "Enable pop-up for [domain]" with a Material 3 `Switch`.
- **`VideoCard`**:
    - **Thumbnails**: Larger placeholder area with a centered play icon and a bottom-aligned timestamp.
    - **Typography**: Clearer titles and a stylized format tag (e.g., "M3U8" in a small bordered box).
    - **Focus Effect**: Professional white background with black text when the card is focused by the remote.

### 2. Smart Mode Switching (Cursor vs. Focus)
- **Automatic Focus**: When a video is first detected, the app will automatically hide the virtual cursor and move focus to the side panel.
- **D-pad Integration**:
    - **Up/Down**: Move between video cards.
    - **Left**: Exit the panel and return to "Cursor Mode" for the web.
    - **Back**: Close the panel (or just return focus to the web).
- **State Management**: Add a `isPanelFocused` state to manage which navigation system is active.

### 3. Click Reliability
- Since we are using native Compose focus for the panel, clics on video cards will be 100% reliable using standard `onClick` handlers.

## Verification Plan

### Automated Tests
- Build verification: `./gradlew :app:assembleDebug`.

### Manual Verification
- **Video Detection**: Load a video site. Confirm the panel appears and the cursor disappears.
- **Navigation**: Verify you can move through the list of detected videos with the D-pad.
- **Exiting**: Verify that pressing **Left** on the remote brings back the cursor and allows web navigation.
- **Visuals**: Compare the new panel against the reference image for accuracy in scale and styling.
