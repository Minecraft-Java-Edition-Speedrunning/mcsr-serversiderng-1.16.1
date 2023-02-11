package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class RNGInitializer {
    private RNGSession instance;
    private long startTime;
    private final long runId;
    private final Random initializer;
    public RNGInitializer(JsonObject jsonObject){
        this(jsonObject.get("seed").getAsLong(),jsonObject.get("runId").getAsLong());
    }
    public RNGInitializer(long seed,long runId){
        initializer=new Random(seed);
        this.runId=runId;
    }
    public RNGInitializer(){
        initializer=null;
        this.runId=-1;
    }
    public void setSession(RNGSession session){
        this.instance=session;
    }



    /**
     * Stops and leaves the {@link ServerSideRNG#instance}
     * @author Void_X_Walker
     */
    public void stopRNGSession() {
        instance = null;
    }

    /**
     * Returns whether {@code ServerSideRNG} is in a session
     * @return true if the {@link ServerSideRNG#instance} and the {@link RNGSession#currentRNGHandler} aren't {@code null}
     * @see RNGSession#updateRNGHandler()
     * @author Void_X_Walker
     */
    public boolean inSession() {
        return getInstance().filter(rngSession -> rngSession.currentRNGHandler !=null).isPresent();
    }

    /**
     * Returns the current {@link RNGSession}
     * @return {@link ServerSideRNG#instance}
     */
    public Optional<RNGSession> getInstance() {
        return Optional.ofNullable(instance);
    }
    public void startRNGSession() {
        instance=createRNGSession();
        ServerSideRNG.log(Level.INFO, "Started RNGSession for runID = " + instance.runId);
    }
    /**
     * Creates a new {@link RNGSession} using the {@code StartRun} token obtained from the {@code Verification-Server}.
     * This method should be called asynchronous due to the delay associated with the request.
     * @return a new {@link RNGSession} or {@code null} if an {@link IOException} occurred when making the request
     * @see  RNGSession#RNGSession(JsonObject)
     * @see IOUtils#getStartRunToken()
     * @author Void_X_Walker
     */
    public RNGSession createRNGSession() {
        if(initializer==null){
            return null;
        }
        return new RNGSession(runId,initializer.nextLong());
    }
    public RNGInitializer getRNGInitializerOrNull(){
        try {
            return new RNGInitializer(IOUtils.getStartRunToken());
        }  catch (ConnectException e){
            ServerSideRNG.log(Level.WARN,"Failed to create new RNGInitializer: Could not connect to the Server.");
            return null;
        }
        catch (IOException e) {
            ServerSideRNG.log(Level.WARN,"Failed to create new RNGInitializer: ");
            e.printStackTrace();
            return null;
        }
    }

    private void activate() {
        startTime = System.nanoTime();
    }
    /**
     * Returns whether the {@link RNGHandler} has passed its standard {@link ServerSideRNGConfig#USE_TIME}
     * @return {@code true} if the current {@link System#nanoTime()} minus the {@link RNGHandler#startTime} is bigger than the {@link ServerSideRNGConfig#USE_TIME}
     * @author Void_X_Walker
     */
    public boolean outOfTime() {
        return System.nanoTime() - startTime > ServerSideRNGConfig.USE_TIME;
    }

    public void update(){
        if(!inSession()){
            if(ServerSideRNG.rngInitializerCompletableFuture!=null){
                ServerSideRNG.currentInitializer = ServerSideRNG.rngInitializerCompletableFuture.getNow(null);
                if(ServerSideRNG.currentInitializer!=null){
                    ServerSideRNG.currentInitializer.activate();
                }
            }
            else {
                ServerSideRNG.currentInitializer =null;
            }
            ServerSideRNG.rngInitializerCompletableFuture= CompletableFuture.supplyAsync(this::getRNGInitializerOrNull);
        }
    }
}
