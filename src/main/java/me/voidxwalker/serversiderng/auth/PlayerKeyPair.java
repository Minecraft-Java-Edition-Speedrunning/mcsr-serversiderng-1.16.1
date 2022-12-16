package me.voidxwalker.serversiderng.auth;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

public class PlayerKeyPair {
    public PrivateKey privateKey;
    public PublicKeyData playerPublicKey;
    private static final String RSA_PUBLIC_KEY_PREFIX = "-----BEGIN RSA PUBLIC KEY-----";
    private static final String RSA_PUBLIC_KEY_SUFFIX = "-----END RSA PUBLIC KEY-----";
    private static final String RSA_PRIVATE_KEY_PREFIX = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String RSA_PRIVATE_KEY_SUFFIX = "-----END RSA PRIVATE KEY-----";
    private static final String RSA = "RSA";
    public PlayerKeyPair(PrivateKey privateKey, PublicKeyData playerPublicKey){
        this.privateKey =privateKey;
        this.playerPublicKey =playerPublicKey;
    }
    static PlayerKeyPair fetchKeyPair(KeyPairResponse keyPairResponse) throws IOException {
        if (keyPairResponse != null) {
            PublicKeyData publicKeyData = decodeKeyPairResponse(keyPairResponse);
            return new PlayerKeyPair(decodeRsaPrivateKeyPem(keyPairResponse.privateKey), publicKeyData);
        }
        throw new IOException("Could not retrieve profile key pair");
    }

    static PublicKeyData decodeKeyPairResponse(KeyPairResponse keyPairResponse) throws IOException {
        if (Strings.isNullOrEmpty(keyPairResponse.publicKey) || keyPairResponse.publicKeySignature == null || keyPairResponse.publicKeySignature.length == 0) {
            throw new IOException("");
        }
        return new PublicKeyData(
                    Instant.parse(keyPairResponse.expiresAt),
                    decodeRsaPublicKeyPem(keyPairResponse.publicKey),
                    keyPairResponse.publicKeySignature
            );
    }

    static PrivateKey decodeRsaPrivateKeyPem(String string) throws IOException {
        return decodePem(string, RSA_PRIVATE_KEY_PREFIX, RSA_PRIVATE_KEY_SUFFIX, PlayerKeyPair::decodeEncodedRsaPrivateKey);
    }
    static PrivateKey decodeEncodedRsaPrivateKey(byte[] bs) throws IOException {
        try {
            PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(bs);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            return keyFactory.generatePrivate(encodedKeySpec);
        }
        catch (Exception exception) {
            throw new IOException(exception);
        }
    }
    static PublicKey decodeRsaPublicKeyPem(String string) throws IOException {
        return decodePem(string, RSA_PUBLIC_KEY_PREFIX, RSA_PUBLIC_KEY_SUFFIX, PlayerKeyPair::decodeEncodedRsaPublicKey);
    }
    static PublicKey decodeEncodedRsaPublicKey(byte[] bs) throws IOException {
        try {
            X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(bs);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            return keyFactory.generatePublic(encodedKeySpec);
        }
        catch (Exception exception) {
            throw new IOException(exception);
        }
    }
    static <T extends Key> T decodePem(String string, String string2, String string3, KeyDecoder<T> keyDecoder) throws IOException {
        int i = string.indexOf(string2);
        if (i != -1) {
            int j = string.indexOf(string3, i += string2.length());
            string = string.substring(i, j + 1);
        }
        try {
            return keyDecoder.apply(Base64.getMimeDecoder().decode(string));
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new IOException(illegalArgumentException);
        }
    }
    static class PublicKeyData {
        public Instant expirationDate;
        public PublicKey publicKey;
        public byte[] signatureBytes;
        public PublicKeyData(Instant instant, PublicKey publicKey, byte[] signatureBytes){
            this.expirationDate =instant;
            this.publicKey =publicKey;
            this.signatureBytes =signatureBytes;
        }
    }
    interface KeyDecoder<T extends Key> {
        T apply(byte[] var1) throws IOException;
    }
    public static class KeyPairResponse {
        final byte[] publicKeySignature;
        final String expiresAt;
        String privateKey;
        String publicKey;

        KeyPairResponse(String expiresAt,byte[] publicKeySignature,String privateKey,String publicKey){
            this.expiresAt=expiresAt;
            this.publicKeySignature=publicKeySignature;
            this.privateKey=privateKey;
            this.publicKey=publicKey;
        }
        static KeyPairResponse fromJson(JsonObject jsonObject){
            return new KeyPairResponse(
                    jsonObject.get("expiresAt").getAsString(),
                    Base64.getDecoder().decode(jsonObject.get("publicKeySignatureV2").getAsString()),
                    jsonObject.get("keyPair").getAsJsonObject().get("privateKey").getAsString(),
                    jsonObject.get("keyPair").getAsJsonObject().get("publicKey").getAsString()
            );
        }
    }

}
