package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.voidxwalker.serversiderng.auth.ClientAuth;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ServerSideRNG implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    final static String BASE_URL="https://serverside-rng-website-fputsekrmq-uc.a.run.app";
    final static String START_RUN_URL = BASE_URL+"/startRun";
    final static String GET_RANDOM_URL = BASE_URL+"/getRandom";
    final static String UPLOAD_HASH_URL = BASE_URL+"/uploadHash";
    final static String HASH_ALG = "MD5";
    final static String READ_ME_NAME = "readme.txt";
    final static String READ_ME= "Submit the Verification Zip File with the name the world yo played your run in alongside your speedrun.com submission.\nMake sure not to alter the ZIP in any way, as that may lead your run becoming unverifiable.\nFor more information read this: https://github.com/VoidXWalker/serverSideRNG/blob/master/README.md.\nIf you have any problems or unanswered questions feel free to open a help thread in the Minecraft Java Edition Speedrunning Discord: https://discord.com/invite/jmdFn3C.";

    final static File verificationFolder=new File("verification-zips");

    public static CompletableFuture<ClientAuth> clientAuthCompletableFuture;
    private static ClientAuth clientAuth;

    @Override
    public void onInitializeClient() {
        CompletableFuture.runAsync(ServerSideRNG::prepareVerificationFolder);
        ServerSideRNG.clientAuthCompletableFuture=CompletableFuture.supplyAsync(ServerSideRNG::createClientAuth);
        Speedrun.speedrunCompletableFuture= CompletableFuture.supplyAsync(ServerSideRNG::createSpeedrunOrNull);
    }

    static ClientAuth getClientAuth() {
        if(ServerSideRNG.clientAuth==null){
            if(ServerSideRNG.clientAuthCompletableFuture!=null){
                try {
                    ServerSideRNG.clientAuth = clientAuthCompletableFuture.get();
                    return ServerSideRNG.clientAuth;
                } catch (ExecutionException | InterruptedException ignored) {

                }
            }
            ServerSideRNG.clientAuth=createClientAuth();

        }
        return ServerSideRNG.clientAuth;
    }
    static ClientAuth createClientAuth() {
        try {
            return new ClientAuth();
        } catch (Exception e) {
            ServerSideRNG.LOGGER.warn("Failed to create Authentication: ");
            e.printStackTrace();
            return null;
        }
    }

    public static Speedrun createSpeedrunOrNull(){
        try {
            return new Speedrun(getStartRunToken());
        } catch (IOException e) {
            ServerSideRNG.LOGGER.warn("Failed to create new Speedrun: ");
            e.printStackTrace();
            return null;
        }
    }
    public static RNGHandler createRngHandlerOrNull(long runId){
        try {
            return new RNGHandler(getGetRandomToken(runId).get("random").getAsLong());
        } catch (IOException | NullPointerException e) {
            ServerSideRNG.LOGGER.warn("Failed to create new RNGHandler: ");
            e.printStackTrace();
            return null;
        }
    }
    public static void getAndUploadHash(){
        try {
            File logsFile = new File(MinecraftClient.getInstance().runDirectory,"logs/latest.log");
            File worldFile = MinecraftClient.getInstance().getServer().getSavePath(WorldSavePath.ROOT).toFile().getParentFile();
            File zipFile=  new File(ServerSideRNG.verificationFolder, "verification-" + worldFile.getName()+".zip");
            ServerSideRNG.packZipFile(zipFile.getPath(),worldFile.getPath(),logsFile.getPath());
            String hash= ServerSideRNG.zipToHash(zipFile);
            ServerSideRNG.uploadLogHash(Speedrun.currentSpeedrun.runId,hash);
            LOGGER.log(Level.INFO,"Successfully uploaded File Hash!");
        } catch (Exception e) {
            LOGGER.log(Level.WARN,"Failed to uploaded File Hash: ");
            e.printStackTrace();
        }
    }
    static JsonObject getStartRunToken() throws IOException {
        ClientAuth auth= ServerSideRNG.getClientAuth();
        JsonObject json = new JsonObject();
        UUID uuid = MinecraftClient.getInstance().getSession().getProfile().getId();
        json.add("auth", auth.createMessageJson(uuid,new SecureRandom().nextLong()));
        json.addProperty("uuid",uuid.toString());
        return makeRequest(json,START_RUN_URL);
    }
    static JsonObject getGetRandomToken(long runId) throws IOException {
        ClientAuth auth= ServerSideRNG.getClientAuth();
        JsonObject json = new JsonObject();
        UUID uuid = MinecraftClient.getInstance().getSession().getProfile().getId();
        json.add("auth", auth.createMessageJson(uuid,new SecureRandom().nextLong()));
        json.addProperty("uuid", uuid.toString());
        json.addProperty("runId",runId);
        return ServerSideRNG.makeRequest(json, ServerSideRNG.GET_RANDOM_URL);
    }
    static JsonObject uploadLogHash(long runId,String hash) throws IOException {
        ClientAuth auth= ServerSideRNG.getClientAuth();
        JsonObject json = new JsonObject();
        UUID uuid = MinecraftClient.getInstance().getSession().getProfile().getId();
        json.add("auth", auth.createMessageJson(uuid,new SecureRandom().nextLong()));
        json.addProperty("uuid",uuid.toString());
        json.addProperty("hash",hash);
        json.addProperty("runId",runId);
        return makeRequest(json,UPLOAD_HASH_URL);
    }
    static void packZipFile(String zipFilePath, String worldFilePath, String logsFilePath ) throws IOException {
        Path p = Files.createFile(Paths.get(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(worldFilePath);
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        if(!zipEntry.getName().equals("session.lock")){
                            try {
                                zs.putNextEntry(zipEntry);
                                Files.copy(path, zs);
                                zs.closeEntry();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    });
            Path path =Paths.get(logsFilePath);
            ZipEntry zipEntry = new ZipEntry(path.toFile().getName());
            zs.putNextEntry(zipEntry);
            Files.copy(path,zs);
            zs.closeEntry();
        }
    }
    static String zipToHash(File zipFile) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALG);
        try(InputStream is = new FileInputStream(zipFile)){
            byte[] buffer = new byte[8192];
            int read;
            while( (read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        }
    }
    static void prepareVerificationFolder() {
        verificationFolder.mkdir();
        File readMe = new File(ServerSideRNG.verificationFolder,READ_ME_NAME);
        try {
            if(readMe.createNewFile()){
                try (FileWriter writer= new FileWriter(readMe)){
                    writer.write(READ_ME);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARN,"Failed to create Verification Folder: ");
            e.printStackTrace();
        }
    }

    static JsonObject makeRequest(JsonObject input, String url) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        httpURLConnection.setDoOutput(true);
        try(OutputStream os = httpURLConnection.getOutputStream()){
            os.write(input.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            try(BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))){
                while ((inputLine = in .readLine()) != null) {
                    response.append(inputLine);
                }
            }
            return new JsonParser().parse(response.toString()).getAsJsonObject();
        }
        throw new IOException(""+httpURLConnection.getResponseCode());
    }
}
