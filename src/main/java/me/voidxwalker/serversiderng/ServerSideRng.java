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

public class ServerSideRng implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    final static String BASE_URL="https://serverside-rng-website-fputsekrmq-uc.a.run.app";
    final static String START_RUN_URL = BASE_URL+"/startRun";
    final static String GET_RANDOM_URL = BASE_URL+"/getRandom";
    final static String UPLOAD_HASH_URL = BASE_URL+"/uploadHash";
    final static String HASH_ALG = "MD5";
    public final static File verificationFolder=new File("verification-zips");

    public static CompletableFuture<Speedrun> speedrunCompletableFuture;
    public static Speedrun currentSpeedrun;

    public static CompletableFuture<ClientAuth> clientAuthCompletableFuture;
    private static ClientAuth clientAuth;

    public static ClientAuth getClientAuth() {
        if(ServerSideRng.clientAuth==null){
            if(ServerSideRng.clientAuthCompletableFuture!=null){
                try {
                    ServerSideRng.clientAuth = clientAuthCompletableFuture.get();
                    return ServerSideRng.clientAuth;
                } catch (ExecutionException | InterruptedException ignored) {

                }
            }
            ServerSideRng.clientAuth=createClientAuth();

        }
        return ServerSideRng.clientAuth;
    }
    public static ClientAuth createClientAuth() {
        try {
            return new ClientAuth();
        } catch (Exception e) {
            ServerSideRng.LOGGER.warn("Failed to create Authentication: ");
            e.printStackTrace();
            return null;
        }
    }
    public static boolean inSpeedrun(){
        return ServerSideRng.currentSpeedrun!=null &&ServerSideRng.currentSpeedrun.getCurrentRNGHandler()!=null;
    }
    public static void startSpeedrun(){
        if(ServerSideRng.speedrunCompletableFuture !=null){
            try {
                ServerSideRng.currentSpeedrun = ServerSideRng.speedrunCompletableFuture.get();

            } catch (InterruptedException | ExecutionException e) {
                ServerSideRng.LOGGER.warn("Failed to start Speedrun!");
                ServerSideRng.currentSpeedrun =null;
            }
        }
        else {
            ServerSideRng.currentSpeedrun = ServerSideRng.createSpeedrunOrNull();
        }
        ServerSideRng.speedrunCompletableFuture= CompletableFuture.supplyAsync(ServerSideRng::createSpeedrunOrNull);
    }
    public static Speedrun createSpeedrunOrNull(){
        try {
            return new Speedrun(getStartRunToken());
        } catch (IOException e) {
            ServerSideRng.LOGGER.warn("Failed to create new Speedrun: ");
            e.printStackTrace();
            return null;
        }
    }
    static RNGHandler createRngHandlerOrNull(long runId){
        try {
            return new RNGHandler(getGetRandomToken(runId).get("random").getAsLong());
        } catch (IOException e) {
            ServerSideRng.LOGGER.warn("Failed to create new RNGHandler: ");
            e.printStackTrace();
            return null;
        }
    }

    static JsonObject getGetRandomToken(long runId) throws IOException {
        ClientAuth auth= ServerSideRng.getClientAuth();
        JsonObject json = new JsonObject();
        UUID uuid = MinecraftClient.getInstance().getSession().getProfile().getId();
        json.add("auth", auth.createMessageJson(uuid,new SecureRandom().nextLong()));
        json.addProperty("uuid", uuid.toString());
        json.addProperty("runId",runId);
        return ServerSideRng.makeRequest(json,ServerSideRng.GET_RANDOM_URL);
    }
    public static JsonObject uploadLogHash(long runId,String hash) throws IOException {
        ClientAuth auth= ServerSideRng.getClientAuth();
        JsonObject json = new JsonObject();
        UUID uuid = MinecraftClient.getInstance().getSession().getProfile().getId();
        json.add("auth", auth.createMessageJson(uuid,new SecureRandom().nextLong()));
        json.addProperty("uuid",uuid.toString());
        json.addProperty("hash",hash);
        json.addProperty("runId",runId);
        return makeRequest(json,UPLOAD_HASH_URL);
    }
    static JsonObject getStartRunToken() throws IOException {
        ClientAuth auth= ServerSideRng.getClientAuth();
        JsonObject json = new JsonObject();
        UUID uuid = MinecraftClient.getInstance().getSession().getProfile().getId();
        json.add("auth", auth.createMessageJson(uuid,new SecureRandom().nextLong()));
        json.addProperty("uuid",uuid.toString());
        return makeRequest(json,START_RUN_URL);
    }
    public static void pack(String zipFilePath,String worldFilePath,String logsFilePath ) throws IOException {
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
                                System.err.println(e);
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
    public static void getAndUploadHash(){
        try {
            File logsFile = new File(MinecraftClient.getInstance().runDirectory,"logs/latest.log");

            File worldFile = MinecraftClient.getInstance().getServer().getSavePath(WorldSavePath.ROOT).toFile().getParentFile();
            File zipFile=  new File(ServerSideRng.verificationFolder, "verification-" + worldFile.getName()+".zip");
            ServerSideRng.pack(zipFile.getPath(),worldFile.getPath(),logsFile.getPath());
            String hash=ServerSideRng.zipToHash(zipFile);
            ServerSideRng.uploadLogHash(ServerSideRng.currentSpeedrun.runId,hash);
            LOGGER.log(Level.INFO,"Successfully uploaded file Hash!");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    public static String zipToHash(File zipFile) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALG);
        try(InputStream is = new FileInputStream(zipFile)){
            byte[] buffer = new byte[8192];
            int read = 0;
            while( (read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            return Base64.getEncoder().encodeToString(md5sum);

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
    @Override
    public void onInitializeClient() {
        verificationFolder.mkdir();
        ServerSideRng.clientAuthCompletableFuture=CompletableFuture.supplyAsync(ServerSideRng::createClientAuth);
        ServerSideRng.speedrunCompletableFuture= CompletableFuture.supplyAsync(ServerSideRng::createSpeedrunOrNull);
    }
}
