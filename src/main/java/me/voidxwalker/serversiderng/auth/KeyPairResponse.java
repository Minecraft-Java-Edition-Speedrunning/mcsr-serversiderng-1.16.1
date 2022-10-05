package me.voidxwalker.serversiderng.auth;

import com.google.gson.JsonObject;

import java.nio.ByteBuffer;
import java.util.Base64;

public class KeyPairResponse {
    public final byte[] publicKeySignature;
    public final String expiresAt;
    public String privateKey;
    public String publicKey;

    public KeyPairResponse(String expiresAt,byte[] publicKeySignature,String privateKey,String publicKey){
        this.expiresAt=expiresAt;
        this.publicKeySignature=publicKeySignature;
        this.privateKey=privateKey;
        this.publicKey=publicKey;
    }
    public static KeyPairResponse fromJson(JsonObject jsonObject){
        return new KeyPairResponse(
                jsonObject.get("expiresAt").getAsString(),
                Base64.getDecoder().decode(jsonObject.get("publicKeySignatureV2").getAsString()),
                jsonObject.get("keyPair").getAsJsonObject().get("privateKey").getAsString(),
                jsonObject.get("keyPair").getAsJsonObject().get("publicKey").getAsString()
        );
    }
}
