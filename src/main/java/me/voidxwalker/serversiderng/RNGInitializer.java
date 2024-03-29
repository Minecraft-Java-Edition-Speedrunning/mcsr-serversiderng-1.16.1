package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;
import me.voidxwalker.serversiderng.auth.ClientAuth;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class RNGInitializer {
    private RNGSession instance;
    private long startTime;
    private final long runId;
    private final Random initializer;
    private static boolean pauseUpdates;
    private int sessionIndex;
    public RNGInitializer(JsonObject jsonObject) {
        this(
            jsonObject.getAsJsonPrimitive("random").getAsLong(),
            jsonObject.getAsJsonPrimitive("runId").getAsLong()
        );
    }
    public RNGInitializer(long seed,long runId){
        initializer=new Random(seed);
        this.runId=runId;
    }
    public static void setPaused(boolean paused){
        ServerSideRNG.log(Level.INFO,paused?"Paused RNGInitializer updates":"Unpaused RNGInitializer updates");
        pauseUpdates=paused;
    }
    public static boolean getPaused(){
        return pauseUpdates;
    }
    public void setSession(RNGSession session){
        this.instance=session;
    }
    /**
     * Stops and leaves the {@link RNGInitializer#instance}
     * @author Void_X_Walker
     */
    public void stopRNGSession() {
        instance = null;
    }


    /**
     * Returns the current {@link RNGSession}
     * @return {@link RNGInitializer#instance}
     */
    public Optional<RNGSession> getInstance() {
        return Optional.ofNullable(instance);
    }
    public void startRNGSession() {
        RNGSession session=createRNGSession();
        instance=session;
        ServerSideRNG.log(Level.INFO, "Started RNGSession [" + session.runId+";"+session.sessionIndex+"]");
    }
    /**
     * Creates a new {@link RNGSession} using the {@code StartRun} token obtained from the {@code Verification-Server}.
     * This method should be called asynchronous due to the delay associated with the request.
     * @return a new {@link RNGSession} or {@code null} if an {@link IOException} occurred when making the request
     * @see  RNGSession#RNGSession(long,int)
     * @see IOUtils#getStartRunToken(me.voidxwalker.serversiderng.auth.ClientAuth)
     * @author Void_X_Walker
     */
    public RNGSession createRNGSession() {
        if(initializer==null){
            return null;
        }
        return new RNGSession(runId,initializer.nextLong(),sessionIndex++);
    }
    public static Optional<RNGInitializer> createRNGInitializer(){
        try {
            return Optional.of(
                    new RNGInitializer(
                            IOUtils.getStartRunToken(
                                    ClientAuth.getInstance().orElseThrow(
                                            (Supplier<Throwable>) () -> new IllegalStateException("Failed to retrieve ClientAuth")
                                    )
                            )
                    )
            );
        }  catch (ConnectException e){
            ServerSideRNG.log(Level.WARN,"Failed to create new RNGInitializer: Could not connect to the Server.");
            return Optional.empty();
        } catch (Throwable e) {
            ServerSideRNG.log(Level.WARN,"Failed to create new RNGInitializer: ");
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private void activate() {
        startTime = System.nanoTime();
    }
    /**
     * Returns whether the {@link RNGInitializer} has passed its standard {@link ServerSideRNGConfig#HANDLER_USE_TIME}
     * @return {@code true} if the current {@link System#nanoTime()} minus the {@link RNGInitializer#startTime} is bigger than the {@link ServerSideRNGConfig#HANDLER_USE_TIME}
     * @author Void_X_Walker
     */
    public boolean outOfTime() {
        return System.nanoTime() - startTime > ServerSideRNGConfig.INITIALIZER_USE_TIME;
    }

    static long lastUpdateTime;
    public static void update(){
        lastUpdateTime=System.nanoTime();
        ServerSideRNG.getRngInitializerCompletableFuture().ifPresentOrElse(completableFuture -> completableFuture.getNow(Optional.empty()).ifPresent(rngInitializer -> {
            rngInitializer.activate();
            ServerSideRNG.log(Level.WARN,"Updating RNGInitializer!");
            ServerSideRNG.setCurrentInitializer(rngInitializer);
        }),()->ServerSideRNG.log(Level.WARN,"Failed to update RNGInitializer!") );
        ServerSideRNG.setRngInitializerCompletableFuture( CompletableFuture.supplyAsync(RNGInitializer::createRNGInitializer));
        ServerSideRNG.getRNGInitializer().ifPresent(RNGInitializer::activate);
    }
    public static void tryUpdate(){
        if(!RNGSession.inSession()&&!getPaused()&&System.nanoTime()-lastUpdateTime> ServerSideRNGConfig.INITIALIZR_UPDATE_COOLDOWN_TIME){
            update();
        }
    }
}
