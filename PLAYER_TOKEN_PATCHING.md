# Player Token Patching Implementation

This document describes the implementation of dzunlock-style player token patching in ReFreezer, which enables HiFi audio features.

## Overview

The player token patching functionality is inspired by the [dzunlock userscript](https://git.uhwot.cf/uhwot/dzunlock) and implements equivalent features to unlock high-quality audio streaming and remove limitations.

## Features

### 1. Player Token Decryption and Patching

The player token received from Deezer's API is encrypted using AES-128 ECB mode. The implementation:

- Decrypts the player token using the same AES key as dzunlock
- Modifies the token to enable all audio quality options:
  - `low` (MP3 64kbps)
  - `standard` (MP3 128kbps)
  - `high` (MP3 320kbps)
  - `lossless` (FLAC)
- Enables full streaming (disables preview mode)
- Removes skip limits on radio/mixes
- Re-encrypts and returns the patched token

### 2. Track Metadata Patching

For each track returned by the API, the implementation:

- Sets `FILESIZE_MP3_320` to `'1'` to indicate 320kbps MP3 availability
- Sets `FILESIZE_FLAC` to `'1'` to indicate FLAC availability
- Only patches non-user-uploaded tracks (SNG_ID >= 0)

This allows the player to request these quality levels even if the user's subscription wouldn't normally allow it.

### 3. API Response Modifications

The implementation also patches various API responses to:

- Disable ads (`ads_display` and `ads_audio` set to `false`)
- Set premium offer ID to 600 (simulates premium subscription)
- Remove upgrade popup entry points
- Remove marketing push notifications

## Implementation Details

### Dart Implementation (lib/api/player_token_patch.dart)

The Dart implementation is used for the Flutter UI and API interactions:

```dart
class PlayerTokenPatch {
  static final List<int> _playerTokenKey = [
    102, 228, 95, 242, 215, 50, 122, 26,
    57, 216, 206, 38, 164, 237, 200, 85
  ];
  
  static String patchPlayerToken(String playerToken) { ... }
}
```

### Java Implementation (android/app/src/main/java/r/r/refreezer/PlayerTokenPatch.java)

The Java implementation is used for the Android download service:

```java
public class PlayerTokenPatch {
  private static final byte[] PLAYER_TOKEN_KEY = {
    102, (byte) 228, 95, (byte) 242, (byte) 215, 50, 122, 26,
    57, (byte) 216, (byte) 206, 38, (byte) 164, (byte) 237, (byte) 200, 85
  };
  
  public static String patchPlayerToken(String playerToken) { ... }
  public static void patchTrackData(JSONObject trackData) { ... }
}
```

## API Methods Patched

The patching is applied to the following Deezer API methods:

1. **deezer.getUserData**: Patches player token, disables ads, sets premium offer
2. **log.listen**: Patches player token in response
3. **song.getListData**: Patches track metadata
4. **song.getSearchTrackMix**: Patches track metadata
5. **smart.getSmartRadio**: Patches track metadata
6. **radio.getUserRadio**: Patches track metadata
7. **tracklist.getShuffledCollection**: Patches track metadata
8. **deezer.pageAlbum**: Patches track metadata in album songs
9. **deezer.pagePlaylist**: Patches track metadata in playlist songs
10. **deezer.pageSmartTracklist**: Patches track metadata
11. **deezer.pageSearch**: Patches track metadata in search results
12. **page.get**: Patches track metadata in homepage sections

## Security Considerations

The AES encryption key used is the same as in the dzunlock userscript and is publicly available. This implementation does not bypass Deezer's content protection or encryption - it only modifies the player token to enable features that may be restricted based on subscription level.

The actual track decryption still uses Deezer's Blowfish encryption which is handled separately by the `DeezerDecryptor` class.

## Compatibility

This implementation is fully compatible with both:
- Free Deezer accounts (enables high-quality streaming that would normally require premium)
- Premium Deezer accounts (maintains existing functionality)

The patching happens transparently without requiring any user configuration.

## Differences from dzunlock

While inspired by dzunlock, this implementation differs in some ways:

1. **No WebSocket blocking**: The userscript blocks play action tracking via WebSocket. This implementation does not currently block WebSocket connections, as the native app may not use the same WebSocket-based tracking.

2. **No alternative media server**: The userscript can redirect media requests to an alternative server. This implementation does not include that feature yet, but it could be added in the future.

3. **Native implementation**: This is a native Dart/Java implementation rather than a JavaScript userscript, making it more efficient and better integrated with the app.

## Future Enhancements

Potential future improvements:

1. Add configurable alternative media server support (similar to dzunlock's `media_server` setting)
2. Add option to disable/enable patching via settings
3. Add logging/debugging options to track which patches are being applied
4. Monitor and adapt to any changes in Deezer's API or token format
