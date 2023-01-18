package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import me.voidxwalker.serversiderng.auth.ClientAuth;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.FileNameUtil;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ServerSideRNG implements ClientModInitializer {
    final static String HASH_ALG = "MD5";
    final static File verificationFolder = new File("verification-zips");

    static final Logger LOGGER = LogManager.getLogger("ServerSideRNG");
    static AtomicBoolean impendingUpload;
    public static boolean needsUpload(){
        return impendingUpload.getAndSet(false);
    }
    public static File lastWorldFile;
    /**
     * Adds an event to the {@code onComplete} listener of <a href="https://github.com/RedLime/SpeedRunIGT">SpeedRunIGT</a> that fires at run completion.
     * It will save the world and upload its Hash after a delay of one second.
     * @see ServerSideRNG#getAndUploadHash(File)  )
     * @author Void_X_Walker
     */
    @Override
    public void onInitializeClient() {
        ServerSideRNGConfig.init();
        try {
            if(FabricLoader.getInstance().isModLoaded("speedrunigt")){
                Class.forName("com.redlimerl.speedrunigt.timer.InGameTimer")
                        .getMethod("onComplete", Consumer.class)
                        .invoke(null,(Consumer<Object>) o -> new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if(RNGSession.inSession()){
                                   impendingUpload.set(true);
                                }
                            }
                        },1000));
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LOGGER.warn("Failed to connect to TimerMod interface: ");
            e.printStackTrace();
        }
        CompletableFuture.runAsync(IOUtils::prepareVerificationFolder);
        ClientAuth.clientAuthCompletableFuture = CompletableFuture.supplyAsync(ClientAuth::createClientAuth);
        RNGSession.rngSessionCompletableFuture = CompletableFuture.supplyAsync(RNGSession::createRNGSessionOrNull);
    }
    public static void log(Level level,String message){
        LOGGER.log(level,message);
    }
    /**
     * Registers the command {@code serversiderng_uploadRun} that will save the world and upload it's hash via the {@link ServerSideRNG#getAndUploadHash(File)} method.
     * @param dispatcher the {@link CommandDispatcher} to add the command to
     * @see ServerSideRNG#getAndUploadHash(File)  )
     * @author Void_X_Walker
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register( CommandManager.literal("serversiderng_uploadRun").executes(context -> {
            if(RNGSession.inSession()){
                uploadHash(false,true);
            }
            return 1;
        }));
    }
    /**
     * Packs the current world folder and the latest.log file into a {@code ZIP} file named "verification-[worldFileName].zip" in the {@link ServerSideRNG#verificationFolder} using  {@link IOUtils#packZipFile(String, String, String)}
     * It then converts the {@code ZIP-File} into a {@code Hash} using {@link IOUtils#zipToHash(File)}
     * and sends it to the {@code Verification-Server} using  {@link ServerSideRNG#uploadHashToken(long, String)}
     * This method should be called asynchronously via {@link ServerSideRNG#uploadHash(boolean, boolean)} if possible.
     * @param worldFile the file of the world to zip and upload the hash of
     * @see  IOUtils#zipToHash(File)
     * @see IOUtils#packZipFile(String, String, String)
     * @see ServerSideRNG#uploadHashToken(long, String) )
     * @author Void_X_Walker
     */
    public static void getAndUploadHash(File worldFile) {
        try {
            File logsFile = new File(MinecraftClient.getInstance().runDirectory,"logs/latest.log");
            File zipFile =  new File(
                ServerSideRNG.verificationFolder,FileNameUtil.getNextUniqueName(ServerSideRNG.verificationFolder.toPath(),"verification-" + worldFile.getName(),".zip")
            );
            IOUtils.packZipFile(zipFile.getPath(), worldFile.getPath(), logsFile.getPath());
            String hash = IOUtils.zipToHash(zipFile);
            ServerSideRNG.uploadHashToken(RNGSession.getInstance().runId, hash);
            ServerSideRNG.log(Level.INFO, "Successfully uploaded File Hash!");
        } catch (Exception e) {
            ServerSideRNG.log(Level.WARN, "Failed to uploaded File Hash: ");
            e.printStackTrace();
        }
    }
    public static void uploadHash(boolean lastSave,boolean playerFeedback){
        if( MinecraftClient.getInstance().getServer()!=null){
            MinecraftClient.getInstance().getServer().getPlayerManager().saveAllPlayerData();
            MinecraftClient.getInstance().getServer().save(true, false, false);
        }
        CompletableFuture.runAsync(()->{
            ServerSideRNG.getAndUploadHash(MinecraftClient
                    .getInstance()
                    .getServer()
                    .getSavePath(WorldSavePath.ROOT)
                    .toFile()
                    .getParentFile());
            if(MinecraftClient.getInstance().getServer()!=null&&playerFeedback){
                MinecraftClient
                        .getInstance()
                        .getServer()
                        .getCommandSource()
                        .sendFeedback(new LiteralText("Successfully uploaded the Run!")
                                .styled(style -> style.withColor(Formatting.GREEN)
                                ),false);
            }
            if(lastSave){
                ServerSideRNG.lastWorldFile=null;
            }
        });

    }
    /**
     * Sends a {@code startRunRequest} to the {@code Verification-Server} via the {@link ServerSideRNGConfig#START_RUN_URL},
     * and returns a {@link JsonObject} containing the {@code runId} of the session and the {@code random}.
     * Automatically grabs the {@code UUID} of the current {@link com.mojang.authlib.GameProfile} for the upload.
     * This method should be called asynchronous due to the delay associated with the request.
     * @return a {@link JsonObject} with the  {@code runId} as the {@code Long} value for the {@code "runId"} property
     * and the {@code random} as the {@code Long} value for the {@code "random"} property
     * @throws IOException: If an error occurred when making the request
     * @see  IOUtils#makeRequest(JsonObject, String)
     * @see ServerSideRNGConfig#START_RUN_URL
     * @author Void_X_Walker
     */
    static JsonObject getStartRunToken() throws IOException {
        JsonObject json = new JsonObject();
        json.add("auth", ClientAuth.getInstance().createMessageJson());
        json.addProperty("uuid", ClientAuth.getInstance().uuid.toString());
        return IOUtils.makeRequest(json, ServerSideRNGConfig.START_RUN_URL);
    }
    /**
     * Sends a {@code getRandomRequest} with the {@code runId} to the {@code Verification-Server} via the {@link ServerSideRNGConfig#GET_RANDOM_URL},
     * and returns a {@link JsonObject} containing the {@code random}.
     * Automatically grabs the {@code UUID} of the current {@link com.mojang.authlib.GameProfile} for the upload.
     * This method should be called asynchronous due to the delay associated with the request.
     * @param runId the {@code Long} {@code runId} for the {@link RNGSession} associated with the request.
     * @return a {@link JsonObject} with the {@code random} as the {@code Long} value for the {@code "random"} property
     * @throws IOException: If an error occurred when making the request
     * @see  IOUtils#makeRequest(JsonObject, String)
     * @see ServerSideRNGConfig#GET_RANDOM_URL
     * @author Void_X_Walker
     */
    static JsonObject getGetRandomToken(long runId) throws IOException {
        JsonObject json = new JsonObject();
        json.add("auth", ClientAuth.getInstance().createMessageJson());
        json.addProperty("uuid", ClientAuth.getInstance().uuid.toString());
        json.addProperty("runId", runId);
        return IOUtils.makeRequest(json, ServerSideRNGConfig.GET_RANDOM_URL);
    }
    /**
     * Uploads a {@code Hash} together with the {@code runId} to the {@code Verification-Server} via the {@link ServerSideRNGConfig#UPLOAD_HASH_URL}.
     * Automatically grabs the {@code UUID} of the current {@link com.mojang.authlib.GameProfile} for the upload.
     * This method should be called asynchronous due to the delay associated with the request.
     * @param runId the {@code Long} {@code runId} for the {@link RNGSession} associated with the {code Hash}.
     * @param hash the {@code Hash} that should be uploaded to the {@code Verification-Server}
     * @throws IOException: If an error occurred when making the request
     * @see  IOUtils#makeRequest(JsonObject, String)
     * @see ServerSideRNGConfig#UPLOAD_HASH_URL
     * @author Void_X_Walker
     */
    static void uploadHashToken(long runId, String hash) throws IOException {
        JsonObject json = new JsonObject();
        json.add("auth", ClientAuth.getInstance().createMessageJson());
        json.addProperty("uuid", ClientAuth.getInstance().uuid.toString());
        json.addProperty("hash", hash);
        json.addProperty("runId", runId);
        IOUtils.makeRequest(json, ServerSideRNGConfig.UPLOAD_HASH_URL);
    }
}
