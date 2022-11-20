package me.voidxwalker.serversiderng.auth;

import com.google.gson.JsonObject;

import java.util.Base64;

public class KeyPairResponse {
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
