package r.r.refreezer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper class to patch Deezer player tokens to enable HiFi features
 * Based on dzunlock userscript functionality
 */
public class PlayerTokenPatch {
    private static final String TAG = "PlayerTokenPatch";
    
    // AES key from dzunlock userscript
    private static final byte[] PLAYER_TOKEN_KEY = {
        102, (byte) 228, 95, (byte) 242, (byte) 215, 50, 122, 26,
        57, (byte) 216, (byte) 206, 38, (byte) 164, (byte) 237, (byte) 200, 85
    };

    /**
     * Decrypts a hex-encoded player token
     */
    private static String decryptHex(String hex) {
        try {
            byte[] bytes = hexToBytes(hex);
            
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec key = new SecretKeySpec(PLAYER_TOKEN_KEY, "AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            
            byte[] decrypted = cipher.doFinal(bytes);
            String result = new String(decrypted, StandardCharsets.UTF_8);
            
            // Remove zero-padding
            return result.replaceAll("\\x00+$", "");
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting player token: " + e.getMessage());
            return "";
        }
    }

    /**
     * Encrypts a string to hex-encoded format
     */
    private static String encryptHex(String str) {
        try {
            // Add zero-padding if needed
            String padded = str;
            if (str.length() % 16 != 0) {
                int padding = 16 - (str.length() % 16);
                padded = str + new String(new byte[padding], StandardCharsets.UTF_8);
            }

            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec key = new SecretKeySpec(PLAYER_TOKEN_KEY, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            
            byte[] encrypted = cipher.doFinal(padded.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encrypted);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting player token: " + e.getMessage());
            return "";
        }
    }

    /**
     * Patches a player token to enable HiFi features
     */
    public static String patchPlayerToken(String playerToken) {
        try {
            // Decrypt the token
            String decrypted = decryptHex(playerToken);
            if (decrypted.isEmpty()) {
                return playerToken;
            }

            // Parse JSON
            JSONObject tokenData = new JSONObject(decrypted);

            // Patch audio qualities - enable all qualities for wifi streaming
            if (tokenData.has("audio_qualities")) {
                JSONObject audioQualities = tokenData.getJSONObject("audio_qualities");
                JSONArray qualities = new JSONArray();
                qualities.put("low");
                qualities.put("standard");
                qualities.put("high");
                qualities.put("lossless");
                audioQualities.put("wifi_streaming", qualities);
            }

            // Disable previews - enable full streaming
            tokenData.put("streaming", true);
            tokenData.put("limited", false);

            // Disable skip limit on mixes/radio
            tokenData.put("radio_skips", 0);

            // Re-encrypt and return
            String patched = tokenData.toString();
            return encryptHex(patched);
        } catch (Exception e) {
            Log.e(TAG, "Error patching player token: " + e.getMessage());
            // If patching fails, return original token
            return playerToken;
        }
    }

    /**
     * Converts hex string to bytes
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    /**
     * Converts bytes to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Patches track data to enable HiFi quality selection (dzunlock equivalent)
     * Sets FILESIZE_MP3_320 and FILESIZE_FLAC to '1' for non-user-uploaded tracks
     */
    public static void patchTrackData(JSONObject trackData) {
        try {
            if (trackData.has("SNG_ID")) {
                int id = Integer.parseInt(trackData.getString("SNG_ID"));
                // Only patch non-user-uploaded tracks (id >= 0)
                if (id >= 0) {
                    trackData.put("FILESIZE_MP3_320", "1");
                    trackData.put("FILESIZE_FLAC", "1");
                }
            }
        } catch (Exception e) {
            // Silently ignore patching errors
        }
    }
}
