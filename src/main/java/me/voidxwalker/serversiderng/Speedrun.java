package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Speedrun {
    public long runId;
    public final Random backUpRandom;
    public RNGHandler currentRNGHandler;
    public CompletableFuture<RNGHandler> rngHandlerCompletableFuture;

    Speedrun( JsonObject startRunToken){
        this.runId=startRunToken.get("runId").getAsLong();
        Random random = new Random(startRunToken.get("random").getAsLong());
        this.backUpRandom= new Random(random.nextLong());
        this.currentRNGHandler =new RNGHandler(random.nextLong());
        this.currentRNGHandler.activate();
        this.rngHandlerCompletableFuture= CompletableFuture.supplyAsync(()-> ServerSideRng.createRngHandlerOrNull(this.runId));
    }
    public Speedrun(long runId){
        this.runId=runId;
        this.currentRNGHandler =null;
        this.rngHandlerCompletableFuture=CompletableFuture.supplyAsync(()-> ServerSideRng.createRngHandlerOrNull(this.runId));
        this.backUpRandom=null;
    }
    public RNGHandler getCurrentRNGHandler(){
        this.updateRNGHandler();
        return this.currentRNGHandler;
    }
    /**
     * Updates the {@link Speedrun#currentRNGHandler} if it's either out of normal time and the {@link Speedrun#rngHandlerCompletableFuture} has completed or it's out of extra time().
     * @see RNGHandler#outOfNormalTime()
     * @see RNGHandler#outOfExtraTime()
     * @author Void_X_Walker
     */
    public void updateRNGHandler(){
        if(this.currentRNGHandler ==null){
            if(this.rngHandlerCompletableFuture==null){
                this.rngHandlerCompletableFuture=CompletableFuture.supplyAsync(()-> ServerSideRng.createRngHandlerOrNull(this.runId));
            }
            this.getRngHandlerFromFuture();
        }
        else if(this.currentRNGHandler.outOfNormalTime()){
            if(this.rngHandlerCompletableFuture==null){
                this.rngHandlerCompletableFuture=CompletableFuture.supplyAsync(()-> ServerSideRng.createRngHandlerOrNull(this.runId));
            }
            if(this.rngHandlerCompletableFuture.isDone() ||!this.currentRNGHandler.outOfExtraTime()){
                ServerSideRng.LOGGER.info("Current RNGHandler ran out of time, updating it!");
                this.getRngHandlerFromFuture();
            }
        }
    }
    /**
     * Tries to update the {@link Speedrun#currentRNGHandler} with the {@link RNGHandler} created by the {@link Speedrun#rngHandlerCompletableFuture}.
     * <p>
     * If the {@code CompletableFuture} hasn't completed or doesn't complete after one second,
     * the {@link Speedrun#currentRNGHandler} will be updated using a {@link RNGHandler} created with the {@link Speedrun#backUpRandom}.
     * @author Void_X_Walker
     */
    public void getRngHandlerFromFuture() {
        try {
            this.currentRNGHandler = this.rngHandlerCompletableFuture.get(1L, TimeUnit.SECONDS);
            ServerSideRng.LOGGER.info("Successfully updated the current RNGHandler!");
            this.currentRNGHandler.activate();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        if(this.currentRNGHandler ==null){
            if(backUpRandom!=null){
                ServerSideRng.LOGGER.warn("Using RNGHandler created by the backup Random!");
                this.currentRNGHandler =new RNGHandler(backUpRandom.nextLong());
                this.currentRNGHandler.activate();
            }
            else {
                ServerSideRng.LOGGER.warn("Failed to update the current RNGHandler!");
            }
        }
        this.rngHandlerCompletableFuture=CompletableFuture.supplyAsync(()-> ServerSideRng.createRngHandlerOrNull(this.runId));
    }
}
