package io.vertx.ext.auth.webauthn.impl;

import com.fasterxml.jackson.core.JsonParser;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jwt.JWK;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * FIDO2 User Info.
 * This class decodes the buffer into a parsable object
 */
public class AuthenticatorData {

  private static final Base64.Decoder B64DEC = Base64.getUrlDecoder();

  public static final int USER_PRESENT = 0x01;
  public static final int USER_VERIFIED = 0x04;
  public static final int ATTESTATION_DATA = 0x40;
  public static final int EXTENSION_DATA = 0x80;

  private final static char[] HEX = "0123456789abcdef".toCharArray();

  private static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX[v >>> 4];
      hexChars[j * 2 + 1] = HEX[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * the hash of the rpId which is basically the effective domain or host.
   * For example: “https://example.com” effective domain is “example.com”
   */
  private byte[] rpIdHash;
  /**
   * 8bit flag that defines the state of the authenticator during the authentication.
   * Bits 0 and 2 are User Presence and User Verification flags.
   * Bit 6 is AT(Attested Credential Data).
   * Must be set when attestedCredentialData is presented.
   * Bit 7 must be set if extension data is presented.
   */
  private byte flags;
  /**
   * Signature counter unsigned 32 bits.
   */
  private long signCounter;
  /**
   * authenticator attestation identifier — a unique identifier of authenticator model
   */
  private byte[] aaguid;
  private String aaguidString;

  /**
   * Credential Identifier. The length is defined by credIdLen. Must be the same as id/rawId.
   */
  private byte[] credentialId;

  /**
   * attested credential data (if present). WebAuthN spec §6.4.1 Attested Credential Data for details.
   * Its length depends on the length of the credential ID and credential public key being attested.
   */
  private byte[] credentialPublicKey;

  private JWK credentialJWK;

  private byte[] extensions;
  private JsonObject extensionsData;

  public AuthenticatorData(byte[] buffer) {
    this(Buffer.buffer(buffer));
  }

  public AuthenticatorData(String base64) {
    this(B64DEC.decode(base64));
  }

  public AuthenticatorData(Buffer buffer) {
    // 37 sum of all required field lengths
    if (buffer.length() < 37) {
      throw new IllegalArgumentException("Authenticator Data must be at least 37 bytes long!");
    }
    int pos = 0;

    rpIdHash = buffer.getBytes(pos, pos + 32);
    pos += 32;

    flags = buffer.getByte(pos);
    pos += 1;

    signCounter = buffer.getUnsignedInt(pos);
    pos += 4;

    // Attested Data is present
    if ((flags & ATTESTATION_DATA) != 0) {
      // 148 sum of all field lengths
      if (buffer.length() < 148) {
        throw new IllegalArgumentException("It seems as the Attestation Data flag is set, but the data is smaller than 148 bytes. You might have set AT flag for the assertion response.");
      }

      aaguid = buffer.getBytes(pos, pos + 16);
      pos += 16;

      String tmp = bytesToHex(aaguid);
      aaguidString = tmp.substring(0, 8) + "-" + tmp.substring(8, 12)+ "-" + tmp.substring(12, 16) + "-" + tmp.substring(16, 20) + "-" + tmp.substring(20);

      int credIDLen = buffer.getUnsignedShort(pos);
      pos += 2;

      credentialId = buffer.getBytes(pos, pos + credIDLen);
      pos += credIDLen;

      byte[] bytes = buffer.getBytes(pos, buffer.length());

      try (JsonParser parser = CBOR.cborParser(bytes)) {
        // the decoded credential primary as a JWK
        this.credentialJWK = COSE.toJWK(CBOR.parse(parser));
        int credentialPublicKeyLen = (int) parser.getCurrentLocation().getByteOffset();
        this.credentialPublicKey = buffer.getBytes(pos, pos + credentialPublicKeyLen);
        pos += credentialPublicKeyLen;
      } catch (IOException e) {
        throw new IllegalArgumentException("Invalid CBOR message");
      }
    }

    if ((flags & EXTENSION_DATA) != 0) {

      byte[] bytes = buffer.getBytes(pos, buffer.length());

      try (JsonParser parser = CBOR.cborParser(bytes)) {
        // the decoded credential primary as a JWK
        this.extensionsData = new JsonObject(CBOR.<Map>parse(parser));
        int extensionsDataLen = (int) parser.getCurrentLocation().getByteOffset();
        this.extensions = buffer.getBytes(pos, pos + extensionsDataLen);
        pos += extensionsDataLen;
      } catch (IOException e) {
        throw new IllegalArgumentException("Invalid CBOR message");
      }
    }

    if(buffer.length() > pos) {
      throw new IllegalArgumentException("Failed to decode authData! Leftover bytes been detected!");
    }
  }

  public boolean is(int flag) {
    return (flags & flag) != 0;
  }

  public byte[] getRpIdHash() {
    return rpIdHash;
  }

  public byte getFlags() {
    return flags;
  }

  public long getSignCounter() {
    return signCounter;
  }

  public byte[] getAaguid() {
    return aaguid;
  }

  public String getAaguidString() {
    return aaguidString;
  }

  public byte[] getCredentialId() {
    return credentialId;
  }

  public byte[] getCredentialPublicKey() {
    return credentialPublicKey;
  }

  public JWK getCredentialJWK() {
    return credentialJWK;
  }

  public byte[] getExtensions() {
    return extensions;
  }
}
