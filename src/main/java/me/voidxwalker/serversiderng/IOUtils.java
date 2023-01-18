package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class IOUtils {

    /**
     * Packs the provided {@code latest.log File} and {@code World File} into the provided {@code ZIP-File}.
     * @param zipFilePath the full {@code Filepath} of the {@code ZIP-File}
     * @param worldFilePath the full {@code Filepath} of the {@code World File}
     * @param logsFilePath the full {@code Filepath} of the {@code latest.log File}
     * @author Void_X_Walker
     */
    static void packZipFile(String zipFilePath, String worldFilePath, String logsFilePath ) throws IOException {
        Path p = Files.createFile(Paths.get(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(worldFilePath);
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
            Path path = Paths.get(logsFilePath);
            ZipEntry zipEntry = new ZipEntry(path.toFile().getName());
            zs.putNextEntry(zipEntry);
            Files.copy(path,zs);
            zs.closeEntry();
        }
    }
    /**
     * Hashes a {@code ZIP-File}.
     * @param zipFile the file that should be converted into a {@code Hash} value.
     * @return the hash of the {@code ZIP-File} in {@code String} form.
     * @throws IOException if opening and reading the {@code ZIP-File} failed
     * @throws NoSuchAlgorithmException if the algorithm doesn't exist, can be ignored
     * @author Void_X_Walker
     */
    static String zipToHash(File zipFile) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(ServerSideRNG.HASH_ALG);
        try(InputStream is = Files.newInputStream(zipFile.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while( (read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return Base64.getEncoder().encodeToString(digest.digest());
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
}
