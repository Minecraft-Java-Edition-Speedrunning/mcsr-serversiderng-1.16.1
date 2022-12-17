package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RNGSession {
    public static CompletableFuture<RNGSession> rngSessionCompletableFuture;
    public static RNGSession instance;
    public long runId;
    /**
     * A backup {@code RandomGenerator} that gets called if no new {@link RNGHandler} is available due to e.g. a connection issue
     */
    public final Random backupRandom;
    public RNGHandler currentRNGHandler;
    public CompletableFuture<RNGHandler> rngHandlerCompletableFuture;
    /**
     * Creates new {@link RNGSession} from the provided {@link JsonObject}, which should have been retrieved from the {@code Verification-Server}.
     * The {@code random} {@code Long} property of the {@link JsonObject} will be used to initialize the {@link RNGSession#currentRNGHandler} and {@link RNGSession#backupRandom}.
     * The order of {@link Random#nextLong()} calls should not be tampered with as to not disrupt any Verification Tools.
     * @param startRunToken A {@link JsonObject} containing a {@code runId} {@code Long} property and a {@code random} {@code Long} property.
     * @see RNGHandler#RNGHandler(long)
     * @author Void_X_Walker
     */
    RNGSession(JsonObject startRunToken) {
        runId = startRunToken.get("runId").getAsLong();
        Random random = new Random(startRunToken.get("random").getAsLong());

        currentRNGHandler = new RNGHandler(random.nextLong());
        backupRandom = new Random(random.nextLong());
        currentRNGHandler.activate();
        rngHandlerCompletableFuture = CompletableFuture.supplyAsync(()-> RNGHandler.createRNGHandlerOrNull(runId));
    }
    /**
     * Creates new {@link RNGSession}. Should only be used to reenter a previous {@code Session} with the {@code runId} that session saved.
     * Leaves the {@link RNGSession#currentRNGHandler} to be initialized async via the {@link RNGSession#rngHandlerCompletableFuture}
     * It is assumed that there will not be an immediate {@link RNGHandler#getRngValue(RNGHandler.RNGTypes)} call because the world is being loaded.
     * @param runId the {@code Long} that will be used as
     * @author Void_X_Walker
     */
    public RNGSession(long runId) {
        this.runId = runId;
        currentRNGHandler = null;
        rngHandlerCompletableFuture = CompletableFuture.supplyAsync(()-> RNGHandler.createRNGHandlerOrNull(runId));
        backupRandom = null;
    }
    /**
     * Returns the current {@link RNGSession}
     * @return {@link RNGSession#instance}
     */
    public static RNGSession getInstance() {
        return instance;
    }
    /**
     * Starts a new {@link RNGSession}.
     * If a new {@link RNGSession} has already been created async via the {@link RNGSession#rngSessionCompletableFuture} it will be retrieved via the {@link CompletableFuture#get()} method.
     * In case of a failure, a new RNGSession will be created synchronously which could lead to noticeable lag.
     * @author Void_X_Walker
     */
    public static void startRNGSession() {
        if (rngSessionCompletableFuture != null) {
            try {
                instance = rngSessionCompletableFuture.get();


            } catch (InterruptedException | ExecutionException e) {
                ServerSideRNG.LOGGER.warn("Failed to start RNGSession async!");
                instance = null;
            }
        }
        else {
            instance = createRNGSessionOrNull();
            ServerSideRNG.LOGGER.warn("Started RNGSession sync!");
        }
        if (instance != null) {
            ServerSideRNG.LOGGER.log(Level.INFO, "Started RNGSession for runID = " + instance.runId);
        }
        rngSessionCompletableFuture = CompletableFuture.supplyAsync(RNGSession::createRNGSessionOrNull);
    }
    /**
     * Stops and leaves the {@link RNGSession#instance}
     * @author Void_X_Walker
     */
    public static void stopRNGSession() {
        instance = null;
    }
    /**
     * Returns whether {@code ServerSideRNG} is in a session
     * @return true if the {@link RNGSession#instance} and the {@link RNGSession#currentRNGHandler} aren't {@code null}
     * @see RNGSession#updateRNGHandler()
     * @author Void_X_Walker
     */
    public static boolean inSession() {
        return instance != null && instance.getCurrentRNGHandler() != null;
    }
    /**
     * Creates a new {@link RNGSession} using the {@code StartRun} token obtained from the {@code Verification-Server}.
     * This method should be called asynchronous due to the delay associated with the request.
     * @return a new {@link RNGSession} or {@code null} if an {@link IOException} occurred when making the request
     * @see  RNGSession#RNGSession(JsonObject)
     * @see ServerSideRNG#getStartRunToken()
     * @author Void_X_Walker
     */
    public static RNGSession createRNGSessionOrNull() {
        try {
            return new RNGSession(ServerSideRNG.getStartRunToken());
        } catch (IOException e) {
            ServerSideRNG.LOGGER.warn("Failed to create new RNGSession: ");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the current RNGHandler after trying updating it
     * @return the current RNGHandler after calling the {@link RNGSession#updateRNGHandler()} method on it.
     * @see RNGSession#updateRNGHandler()
     * @author Void_X_Walker
     */
    public RNGHandler getCurrentRNGHandler() {
        updateRNGHandler();
        return currentRNGHandler;
    }
    /**
     * Updates the {@link RNGSession#currentRNGHandler} if it's either out of normal time and the {@link RNGSession#rngHandlerCompletableFuture} has completed or it's out of extra time().
     * @see RNGHandler#outOfNormalTime()
     * @see RNGHandler#outOfExtraTime()
     * @author Void_X_Walker
     */
    public void updateRNGHandler() {
        if (currentRNGHandler == null) {
            if (rngHandlerCompletableFuture == null) {
                rngHandlerCompletableFuture = CompletableFuture.supplyAsync(()-> RNGHandler.createRNGHandlerOrNull(runId));
            }
            getRngHandlerFromFuture();
        }
        else if (currentRNGHandler.outOfNormalTime()) {
            if (rngHandlerCompletableFuture == null) {
                rngHandlerCompletableFuture = CompletableFuture.supplyAsync(()-> RNGHandler.createRNGHandlerOrNull(runId));
            }
            if (rngHandlerCompletableFuture.isDone() || !currentRNGHandler.outOfExtraTime()) {
                ServerSideRNG.LOGGER.info("Current RNGHandler ran out of time, updating it!");
                getRngHandlerFromFuture();
            }
        }
    }
    /**
     * Tries to update the {@link RNGSession#currentRNGHandler} with the {@link RNGHandler} created by the {@link RNGSession#rngHandlerCompletableFuture} and then activates it.
     * <p>
     * If the {@code CompletableFuture} hasn't completed or doesn't complete after one second,
     * the {@link RNGSession#currentRNGHandler} will be updated using a {@link RNGHandler} created with the {@link RNGSession#backupRandom}.
     * @see RNGHandler#activate()
     * @author Void_X_Walker
     */
    public void getRngHandlerFromFuture() {
        try {
            currentRNGHandler = rngHandlerCompletableFuture.get(1L, TimeUnit.SECONDS);
            ServerSideRNG.LOGGER.info("Successfully updated the current RNGHandler!");
            currentRNGHandler.activate();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        if (currentRNGHandler == null) {
            if (backupRandom != null) {
                currentRNGHandler = new RNGHandler(backupRandom.nextLong());
                ServerSideRNG.LOGGER.warn("Using RNGHandler created by the backup Random!");
                currentRNGHandler.activate();
            }
            else {
                ServerSideRNG.LOGGER.warn("Failed to update the current RNGHandler!");
            }
        }
        rngHandlerCompletableFuture = CompletableFuture.supplyAsync(()-> RNGHandler.createRNGHandlerOrNull(runId));
    }
}
