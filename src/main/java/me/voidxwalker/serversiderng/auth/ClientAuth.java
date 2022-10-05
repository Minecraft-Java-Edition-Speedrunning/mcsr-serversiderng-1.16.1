package me.voidxwalker.serversiderng.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.Base64;
import java.util.UUID;

public class ClientAuth {
    final static String SIGNATURE_ALGORITHM = "SHA256withRSA";
    final static String DIGEST_ALGORITHM = "SHA-256";
    final static String PUBLIC_KEY_PREFIX = "-----BEGIN PUBLIC KEY-----\n";
    final static String PUBLIC_KEY_SUFFIX = "\n-----END PUBLIC KEY-----";
    String accessToken;
    Proxy proxy;
    PlayerKeyPair pair;

    public ClientAuth() throws Exception {
        this.accessToken= MinecraftClient.getInstance().getSession().getAccessToken();
        this.proxy= ((YggdrasilMinecraftSessionService)MinecraftClient.getInstance().getSessionService()).getAuthenticationService().getProxy();
        this.pair=PlayerKeyPair.fetchKeyPair(this.post(ClientAuth.constantURL("https://api.minecraftservices.com/player/certificates")));
    }

    public JsonObject createMessageJson( UUID sender, long randomLong) {
        byte[] data = sign(sender,randomLong);
        JsonObject output = new JsonObject();
        output.addProperty("uuid",sender.getMostSignificantBits()+"/"+sender.getLeastSignificantBits());
        output.addProperty("randomLong",""+randomLong);
        output.addProperty("publicKey",PUBLIC_KEY_PREFIX +Base64.getEncoder().encodeToString(pair.playerPublicKey.publicKey.getEncoded()) + PUBLIC_KEY_SUFFIX);
        output.addProperty("instant",pair.playerPublicKey.expirationDate.toEpochMilli());
        output.addProperty("signatureBytes",Base64.getEncoder().encodeToString(pair.playerPublicKey.signatureBytes));
        output.addProperty("data",Base64.getEncoder().encodeToString(data));
        return output;
    }
    public static URL constantURL(final String url) {
        try {
            return new URL(url);
        } catch (final MalformedURLException ex) {
            throw new Error("Couldn't create constant for " + url, ex);
        }
    }

    private byte[] sign(UUID sender, long randomLong) {
        Signature signature;
        try {
            signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(pair.privateKey);
            signature.update((sender.getMostSignificantBits()+"/"+sender.getLeastSignificantBits()).getBytes(StandardCharsets.UTF_8));
            signature.update(Base64.getEncoder().encode(digest(randomLong)));
            return signature.sign();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    static byte[] digest(long randomLong) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            digest.update((randomLong+"").getBytes(StandardCharsets.UTF_8));
            digest.update("70".getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (NoSuchAlgorithmException ignored) {
        }
        return null;

    }
    KeyPairResponse post(final URL url) throws IOException {
        return readInputStream(postInternal(url, new byte[0]));
    }

    KeyPairResponse readInputStream(final HttpURLConnection connection) throws IOException {

        InputStream inputStream = null;
        try {
            final int status = connection.getResponseCode();
            final String result;
            if (status < 400) {
                inputStream = connection.getInputStream();

                result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                return KeyPairResponse.fromJson(new JsonParser().parse(result).getAsJsonObject());
            } else {
                throw new IOException(status+"");

            }
        } catch (final IOException e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    HttpURLConnection postInternal(final URL url, final byte[] postAsBytes) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection(this.proxy);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setUseCaches(false);
        OutputStream outputStream = null;
        try {
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Content-Length", "" + postAsBytes.length);
            connection.setRequestProperty("Authorization", "Bearer " + this.accessToken);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            outputStream = connection.getOutputStream();
            IOUtils.write(postAsBytes, outputStream);
        } catch (IOException io) {
            throw new IOException(io);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
        return connection;

    }

}
