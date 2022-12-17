package me.voidxwalker.serversiderng.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import me.voidxwalker.serversiderng.ServerSideRNG;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ClientAuth {
    final static String SIGNATURE_ALGORITHM = "SHA256withRSA";
    final static String DIGEST_ALGORITHM = "SHA-256";
    final static String CERTIFICATE_URL = "https://api.minecraftservices.com/player/certificates";

    String accessToken;
    public UUID uuid;
    Proxy proxy;
    PlayerKeyPair pair;

    public static CompletableFuture<ClientAuth> clientAuthCompletableFuture;
    private static ClientAuth instance;
    /**
     * Tries to update the {@link ClientAuth#instance} with the {@link ClientAuth} created by the {@link ClientAuth#clientAuthCompletableFuture} and returns it.
     * If the {@link ClientAuth#clientAuthCompletableFuture} is null however, or an error occurs, it tries to update and return the {@link ClientAuth#instance} synchronously using {@link ClientAuth#createClientAuth()}
     * @see ClientAuth#createClientAuth()
     * @return A {@link ClientAuth} {@code Object} that has been initialized and is ready for requests.
     */
    public static ClientAuth getInstance() {
        if (ClientAuth.instance == null) {
            if (ClientAuth.clientAuthCompletableFuture != null) {
                try {
                    ClientAuth.instance = clientAuthCompletableFuture.get();
                    return ClientAuth.instance;
                } catch (ExecutionException | InterruptedException ignored) {
                }
            }
            ClientAuth.instance = ClientAuth.createClientAuth();
        }
        return ClientAuth.instance;
    }


    ClientAuth() throws IOException {
        this.accessToken = MinecraftClient.getInstance().getSession().getAccessToken();
        this.uuid = MinecraftClient.getInstance().getSession().getProfile().getId();
        this.proxy = ((YggdrasilMinecraftSessionService)MinecraftClient.getInstance().getSessionService()).getAuthenticationService().getProxy();
        this.pair = PlayerKeyPair.fetchKeyPair(readInputStream(postInternal(ClientAuth.constantURL(), new byte[0])));
    }

    public static ClientAuth createClientAuth() {
        try {
            return new ClientAuth();
        } catch (Exception e) {
            ServerSideRNG.LOGGER.warn("Failed to create Authentication: ");
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Creates a signature {@link JsonObject} that authenticates that the player with {@code UUID} {@link ClientAuth#uuid} is the owner their account.
     * @return A {@link ClientAuth} {@code Object} that has been initialized and is ready for requests.
     */

    public JsonObject createMessageJson() {
        try {
            long randomLong = new SecureRandom().nextLong();
            byte[] data = sign(this.uuid,randomLong);
            JsonObject output = new JsonObject();
            output.addProperty("uuid",this.uuid.getMostSignificantBits()+"/"+this.uuid.getLeastSignificantBits());
            output.addProperty("randomLong",""+randomLong);
            output.addProperty("publicKey",Base64.getEncoder().encodeToString(pair.playerPublicKey.publicKey.getEncoded()));
            output.addProperty("instant",pair.playerPublicKey.expirationDate.toEpochMilli());
            output.addProperty("signatureBytes",Base64.getEncoder().encodeToString(pair.playerPublicKey.signatureBytes));
            output.addProperty("data",Base64.getEncoder().encodeToString(data));
            return output;
        } catch (Exception e) {
            ServerSideRNG.LOGGER.log(Level.WARN,"Failed to sign authentication message JSON: ");
            e.printStackTrace();
            return null;
        }
    }
    static URL constantURL() {
        try {
            return new URL(CERTIFICATE_URL);
        } catch (final MalformedURLException ex) {
            throw new Error("Couldn't create constant for " + CERTIFICATE_URL, ex);
        }
    }

    byte[] sign(UUID sender, long randomLong) throws GeneralSecurityException {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(pair.privateKey);
        signature.update(
            (sender.getMostSignificantBits() + "/" + sender.getLeastSignificantBits())
                .getBytes(StandardCharsets.UTF_8));
        signature.update(Base64.getEncoder().encode(digest(randomLong)));
        return signature.sign();
    }
    static byte[] digest(long randomLong) {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            digest.update((randomLong + "").getBytes(StandardCharsets.UTF_8));
            digest.update("70".getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }
    }

    PlayerKeyPair.KeyPairResponse readInputStream(final HttpURLConnection connection) throws IOException {

        InputStream inputStream = null;
        try {
            final int status = connection.getResponseCode();
            final String result;
            if (status < 400) {
                inputStream = connection.getInputStream();

                result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                return PlayerKeyPair.KeyPairResponse.fromJson(new JsonParser().parse(result).getAsJsonObject());
            } else {
                throw new IOException(status+"");
            }
        }
        finally {
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
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
        return connection;
    }
}
