package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.voidxwalker.serversiderng.auth.ClientAuth;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ServerSideRng implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    final static String BASE_URL="http://localhost:3000";
    final static String START_RUN_URL = BASE_URL+"/startRun";
    final static String GET_RANDOM_URL = BASE_URL+"/getRandom";

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
    static JsonObject getStartRunToken() throws IOException {
        ClientAuth auth= ServerSideRng.getClientAuth();
        JsonObject json = new JsonObject();
        UUID uuid = MinecraftClient.getInstance().getSession().getProfile().getId();
        json.add("auth", auth.createMessageJson(uuid,new SecureRandom().nextLong()));
        json.addProperty("uuid",uuid.toString());
        return makeRequest(json,START_RUN_URL);
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
        ServerSideRng.clientAuthCompletableFuture=CompletableFuture.supplyAsync(ServerSideRng::createClientAuth);
        ServerSideRng.speedrunCompletableFuture= CompletableFuture.supplyAsync(ServerSideRng::createSpeedrunOrNull);
    }
}
