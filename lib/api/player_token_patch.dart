import 'dart:convert';
import 'dart:typed_data';
import 'package:encrypt/encrypt.dart';

/// Helper class to patch Deezer player tokens to enable HiFi features
/// Based on dzunlock userscript functionality
class PlayerTokenPatch {
  // AES key from dzunlock userscript
  static final List<int> _playerTokenKey = [
    102, 228, 95, 242, 215, 50, 122, 26,
    57, 216, 206, 38, 164, 237, 200, 85
  ];

  /// Decrypts a hex-encoded player token
  static String _decryptHex(String hex) {
    try {
      final bytes = _hexToBytes(hex);
      final key = Key(Uint8List.fromList(_playerTokenKey));
      final encrypter = Encrypter(AES(key, mode: AESMode.ecb));
      
      final encrypted = Encrypted(Uint8List.fromList(bytes));
      final decrypted = encrypter.decrypt(encrypted);
      
      // Remove zero-padding
      return decrypted.replaceAll(RegExp(r'\x00+$'), '');
    } catch (e) {
      return '';
    }
  }

  /// Encrypts a string to hex-encoded format
  static String _encryptHex(String str) {
    try {
      // Add zero-padding if needed
      String padded = str;
      if (str.length % 16 != 0) {
        final padding = 16 - (str.length % 16);
        padded = str + ('\x00' * padding);
      }

      final key = Key(Uint8List.fromList(_playerTokenKey));
      final encrypter = Encrypter(AES(key, mode: AESMode.ecb));
      
      final encrypted = encrypter.encrypt(padded);
      return _bytesToHex(encrypted.bytes);
    } catch (e) {
      return '';
    }
  }

  /// Patches a player token to enable HiFi features
  static String patchPlayerToken(String playerToken) {
    try {
      // Decrypt the token
      final decrypted = _decryptHex(playerToken);
      if (decrypted.isEmpty) return playerToken;

      // Parse JSON
      final tokenData = jsonDecode(decrypted) as Map<String, dynamic>;

      // Patch audio qualities - enable all qualities for wifi streaming
      if (tokenData.containsKey('audio_qualities')) {
        final audioQualities = tokenData['audio_qualities'] as Map<String, dynamic>;
        audioQualities['wifi_streaming'] = ['low', 'standard', 'high', 'lossless'];
      }

      // Disable previews - enable full streaming
      tokenData['streaming'] = true;
      tokenData['limited'] = false;

      // Disable skip limit on mixes/radio
      tokenData['radio_skips'] = 0;

      // Re-encrypt and return
      final patched = jsonEncode(tokenData);
      return _encryptHex(patched);
    } catch (e) {
      // If patching fails, return original token
      return playerToken;
    }
  }

  /// Converts hex string to bytes
  static List<int> _hexToBytes(String hex) {
    final bytes = <int>[];
    for (var i = 0; i < hex.length; i += 2) {
      bytes.add(int.parse(hex.substring(i, i + 2), radix: 16));
    }
    return bytes;
  }

  /// Converts bytes to hex string
  static String _bytesToHex(List<int> bytes) {
    return bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join('');
  }
}
