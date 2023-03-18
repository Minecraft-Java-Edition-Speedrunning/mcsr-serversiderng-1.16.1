package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.voidxwalker.serversiderng.auth.ClientAuth;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.FileNameUtil;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class IOUtils {

    /**
     * Packs the provided {@code latest.log File} and {@code World File} into the provided {@code ZIP-File}.
     * Hashes a {@code ZIP-File}.
     * @param name the file name that should be converted into a {@code Hash} value.
     * @param worldsFile the full {@code Filepath} of the {@code World File}
     * @param logsFile the full {@code Filepath} of the {@code latest.log File}
     * @return the hash of the {@code ZIP-File} in {@code String} form.
     * @throws IOException if opening and reading the {@code ZIP-File} failed
     * @author Void_X_Walker
     */
    static String zipFileAndGetHash(String name, String worldsFile, String logsFile) throws IOException {
        File zipFile =  new File(ServerSideRNG.verificationFolder,name);
        Path p = Files.createFile(Paths.get(zipFile.getPath()));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(worldsFile);
            Files.walk(pp)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                        if (!zipEntry.getName().equals("session.lock")) {
                            try {
                                zs.putNextEntry(zipEntry);
                                Files.copy(path, zs);
                                zs.closeEntry();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    });
            Path path = Paths.get(logsFile);
            ZipEntry zipEntry = new ZipEntry(path.toFile().getName());
            zs.putNextEntry(zipEntry);
            Files.copy(path,zs);
            zs.closeEntry();
        }

        try(InputStream is = Files.newInputStream(zipFile.toPath())) {
            MessageDigest digest = MessageDigest.getInstance(ServerSideRNGConfig.VERIFICATION_FOLDER_HASH_ALG);
            byte[] buffer = new byte[8192];
            int read;
            while( (read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException ignored) {
            return "";
        }
    }
    /**
     * Prepare the {@link ServerSideRNG#verificationFolder} with the {@code readme.txt}.
     * @author Void_X_Walker
     */
    static void prepareVerificationFolder() {
        if (ServerSideRNG.verificationFolder.mkdir()) {
            File readMe = new File(ServerSideRNG.verificationFolder, ServerSideRNGConfig.READ_ME_NAME);
            try {
                if (readMe.createNewFile()) {
                    try (FileWriter writer = new FileWriter(readMe)) {
                        writer.write(ServerSideRNGConfig.READ_ME);
                    }
                }
            } catch (IOException e) {
                ServerSideRNG.log(Level.WARN,"Failed to create Verification Folder: ");
                e.printStackTrace();
            }
        }

    }
    /**
     * Makes a post request with the provided {@code JSON} to the provided {@code URL}
     * @param url the {@code URL} to make the post request to
     * @param input the {@link JsonObject} to be posted
     * @return the response of the server in form of a {@link JsonObject}
     * @throws IOException if the request response code is not {@code 200} or any other I/O exception occurred when making the request
     * @author Void_X_Walker
     */
    static JsonObject makeRequest(JsonObject input, String url) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        httpURLConnection.setDoOutput(true);
        try (OutputStream os = httpURLConnection.getOutputStream()) {
            os.write(input.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
            return new JsonParser().parse(response.toString()).getAsJsonObject();
        }
        throw new IOException(""+httpURLConnection.getResponseCode());
    }

    /**
     * Packs the current world folder and the latest.log file into a {@code ZIP} file named "verification-[worldFileName].zip" in the {@link ServerSideRNG#verificationFolder} }
     * It then converts the {@code ZIP-File} into a {@code Hash}
     * and sends it to the {@code Verification-Server} using  {@link IOUtils#uploadHashToken(long, String,ClientAuth)}
     * This method should be called asynchronously via {@link IOUtils#uploadHash( long)} if possible.
     * @param worldFile the file of the world to zip and upload the hash of
     * @see IOUtils#uploadHashToken(long, String,ClientAuth)
     * @author Void_X_Walker
     */
    public static void getAndUploadHash(File worldFile,long runId) {
        try {
            ServerSideRNG.log(Level.INFO, "Creating Verification ZIP File...");
            File logsFile = new File(MinecraftClient.getInstance().runDirectory,"logs/latest.log");
            String hash= zipFileAndGetHash(FileNameUtil.getNextUniqueName(ServerSideRNG.verificationFolder.toPath(),"verification-" + worldFile.getName(),".zip"),worldFile.getPath(),logsFile.getPath());

            uploadHashToken(runId, hash,ClientAuth.getInstance().orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("Failed to retrieve ClientAuth")));
            ServerSideRNG.log(Level.INFO, "Successfully uploaded File Hash!");
        } catch (Throwable e) {
            ServerSideRNG.log(Level.WARN, "Failed to uploaded File Hash: ");
            e.printStackTrace();
        }
    }

    public static void uploadHash(long runId){
        MinecraftServer server = MinecraftClient.getInstance().getServer();
        if( server!=null) {
            server.getPlayerManager().saveAllPlayerData();
            server.save(true, false, false);
            server.getCommandSource()
                    .sendFeedback(new LiteralText("Successfully uploaded the Run!")
                            .styled(style -> style.withColor(Formatting.GREEN)
                            ), false);
            CompletableFuture.runAsync(() -> {
                getAndUploadHash(server
                        .getSavePath(WorldSavePath.ROOT)
                        .toFile()
                        .getParentFile(), runId);
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.getCommandSource().sendFeedback(new LiteralText("Successfully uploaded the Run!")
                                    .styled(style -> style.withColor(Formatting.GREEN)
                                    ), false);
                }
            });
        }
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
    static JsonObject getStartRunToken(ClientAuth auth) throws IOException {
        JsonObject json = new JsonObject();
        json.add("auth", auth.createMessageJson());
        json.addProperty("uuid", auth.uuid.toString());
        return makeRequest(json, ServerSideRNGConfig.START_RUN_URL);
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
    static JsonObject getGetRandomToken(long runId,ClientAuth auth) throws IOException {
        JsonObject json = new JsonObject();
        json.add("auth", auth.createMessageJson());
        json.addProperty("uuid", auth.uuid.toString());
        json.addProperty("runId", runId);
        return makeRequest(json, ServerSideRNGConfig.GET_RANDOM_URL);
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
    static void uploadHashToken(long runId, String hash,ClientAuth auth) throws IOException {
        JsonObject json = new JsonObject();
        json.add("auth", auth.createMessageJson());
        json.addProperty("uuid", auth.uuid.toString());
        json.addProperty("hash", hash);
        json.addProperty("runId", runId);
        makeRequest(json, ServerSideRNGConfig.UPLOAD_HASH_URL);
    }
}
