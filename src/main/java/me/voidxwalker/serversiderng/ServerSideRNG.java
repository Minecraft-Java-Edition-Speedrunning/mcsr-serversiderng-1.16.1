package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;
import me.voidxwalker.serversiderng.auth.ClientAuth;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ServerSideRNG implements ClientModInitializer {
    final static String BASE_URL="https://serverside-rng-website-fputsekrmq-uc.a.run.app";
    final static String START_RUN_URL = BASE_URL+"/startRun";
    final static String GET_RANDOM_URL = BASE_URL+"/getRandom";
    final static String UPLOAD_HASH_URL = BASE_URL+"/uploadHash";
    final static String HASH_ALG = "MD5";
    final static String READ_ME_NAME = "readme.txt";
    final static String READ_ME= "Submit the Verification Zip File with the name the world you played your run in alongside your speedrun.com submission.\nMake sure not to alter the ZIP in any way, as that may lead your run becoming unverifiable.\nFor more information read this: https://github.com/VoidXWalker/serverSideRNG/blob/master/README.md.\nIf you have any problems or unanswered questions feel free to open a help thread in the Minecraft Java Edition Speedrunning Discord: https://discord.com/invite/jmdFn3C.";
    final static File verificationFolder=new File("verification-zips");

    public static final Logger LOGGER = LogManager.getLogger("ServerSideRNG");

    @Override
    public void onInitializeClient() {
        CompletableFuture.runAsync(IOUtils::prepareVerificationFolder);
        ClientAuth.clientAuthCompletableFuture=CompletableFuture.supplyAsync(ClientAuth::createClientAuth);
        RNGSession.rngSessionCompletableFuture = CompletableFuture.supplyAsync(RNGSession::createRNGSessionOrNull);
    }


    /**
     * Packs the current world folder and the latest.log file into a {@code ZIP} file named "verification-[worldFileName].zip" in the {@link ServerSideRNG#verificationFolder} using  {@link IOUtils#packZipFile(String, String, String)}
     * It then converts the {@code ZIP-File} into a {@code Hash} using {@link IOUtils#zipToHash(File)}
     * and sends it to the {@code Verification-Server} using  {@link ServerSideRNG#uploadHashToken(long, String)}
     * @see  IOUtils#zipToHash(File)
     * @see IOUtils#packZipFile(String, String, String)
     * @see ServerSideRNG#uploadHashToken(long, String) )
     * @author Void_X_Walker
     */
    public static void getAndUploadHash(File worldFile){
        try {
            File logsFile = new File(MinecraftClient.getInstance().runDirectory,"logs/latest.log");
            File zipFile=  new File(ServerSideRNG.verificationFolder, "verification-" + worldFile.getName()+".zip");
            IOUtils.packZipFile(zipFile.getPath(),worldFile.getPath(),logsFile.getPath());
            String hash= IOUtils.zipToHash(zipFile);
            ServerSideRNG.uploadHashToken(RNGSession.getInstance().runId,hash);
            LOGGER.log(Level.INFO,"Successfully uploaded File Hash!");
        } catch (Exception e) {
            LOGGER.log(Level.WARN,"Failed to uploaded File Hash: ");
            e.printStackTrace();
        }
    }
    /**
     * Sends a {@code startRunRequest} to the {@code Verification-Server} via the {@link ServerSideRNG#START_RUN_URL},
     * and returns a {@link JsonObject} containing the {@code runId} of the session and the {@code random}.
     * Automatically grabs the {@code UUID} of the current {@link com.mojang.authlib.GameProfile} for the upload.
     * This method should be called asynchronous due to the delay associated with the request.
     * @return a {@link JsonObject} with the  {@code runId} as the {@code Long} value for the {@code "runId"} property
     * and the {@code random} as the {@code Long} value for the {@code "random"} property
     * @throws IOException: If an error occurred when making the request
     * @see  IOUtils#makeRequest(JsonObject, String)
     * @see ServerSideRNG#START_RUN_URL
     * @author Void_X_Walker
     */
    static JsonObject getStartRunToken() throws IOException {
        JsonObject json = new JsonObject();
        json.add("auth", ClientAuth.getInstance().createMessageJson());
        json.addProperty("uuid",ClientAuth.getInstance().uuid.toString());
        return IOUtils.makeRequest(json,START_RUN_URL);
    }
    /**
     * Sends a {@code getRandomRequest} with the {@code runId} to the {@code Verification-Server} via the {@link ServerSideRNG#GET_RANDOM_URL},
     * and returns a {@link JsonObject} containing the {@code random}.
     * Automatically grabs the {@code UUID} of the current {@link com.mojang.authlib.GameProfile} for the upload.
     * This method should be called asynchronous due to the delay associated with the request.
     * @param runId the {@code Long} {@code runId} for the {@link RNGSession} associated with the request.
     * @return a {@link JsonObject} with the {@code random} as the {@code Long} value for the {@code "random"} property
     * @throws IOException: If an error occurred when making the request
     * @see  IOUtils#makeRequest(JsonObject, String)
     * @see ServerSideRNG#GET_RANDOM_URL
     * @author Void_X_Walker
     */
    static JsonObject getGetRandomToken(long runId) throws IOException {
        JsonObject json = new JsonObject();
        json.add("auth", ClientAuth.getInstance().createMessageJson());
        json.addProperty("uuid",ClientAuth.getInstance().uuid.toString());
        json.addProperty("runId",runId);
        return IOUtils.makeRequest(json, ServerSideRNG.GET_RANDOM_URL);
    }
    /**
     * Uploads a {@code Hash} together with the {@code runId} to the {@code Verification-Server} via the {@link ServerSideRNG#UPLOAD_HASH_URL}.
     * Automatically grabs the {@code UUID} of the current {@link com.mojang.authlib.GameProfile} for the upload.
     * This method should be called asynchronous due to the delay associated with the request.
     * @param runId the {@code Long} {@code runId} for the {@link RNGSession} associated with the {code Hash}.
     * @param hash the {@code Hash} that should be uploaded to the {@code Verification-Server}
     * @throws IOException: If an error occurred when making the request
     * @see  IOUtils#makeRequest(JsonObject, String)
     * @see ServerSideRNG#UPLOAD_HASH_URL
     * @author Void_X_Walker
     */
    static void uploadHashToken(long runId, String hash) throws IOException {
        JsonObject json = new JsonObject();
        json.add("auth", ClientAuth.getInstance().createMessageJson());
        json.addProperty("uuid",ClientAuth.getInstance().uuid.toString());
        json.addProperty("hash",hash);
        json.addProperty("runId",runId);
        IOUtils.makeRequest(json, UPLOAD_HASH_URL);
    }
}
