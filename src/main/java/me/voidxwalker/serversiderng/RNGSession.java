package me.voidxwalker.serversiderng;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RNGSession {
    private SessionState sessionState;
    public final long runId;
    public final int sessionIndex;

    private long worldJoinTime;
    private int handlerIndex;
    /**
     * A backup {@code RandomGenerator} that gets called if no new {@link RNGHandler} is available due to e.g. a connection issue
     */
    private final Random backupRandom;
    private RNGHandler currentRNGHandler;
    private CompletableFuture<Optional<RNGHandler>> rngHandlerCompletableFuture;

    public static boolean inSession(){
        return ServerSideRNG.getRNGInitializer().map(RNGInitializer::getInstance).isPresent();
    }
    public static Optional<RNGSession> getInstance(){
        return   ServerSideRNG.getRNGInitializer().flatMap(RNGInitializer::getInstance);
    }
    RNGSession(long runId, long seed,int sessionIndex) {
        this.runId = runId;
        this.sessionIndex=sessionIndex;
        sessionState=SessionState.STARTUP;
        Random random = new Random(seed);

        currentRNGHandler = new RNGHandler(random.nextLong());
        backupRandom = new Random(random.nextLong());
        currentRNGHandler.activate(handlerIndex++);
        rngHandlerCompletableFuture = CompletableFuture.supplyAsync(()-> RNGHandler.createRNGHandler(runId));
    }

    /**
     * Creates new {@link RNGSession}. Should only be used to reenter a previous {@code Session} with the {@code runId} that session saved.
     * Leaves the {@link RNGSession#currentRNGHandler} to be initialized async via the {@link RNGSession#rngHandlerCompletableFuture}
     * It is assumed that there will not be an immediate {@link RNGHandler#getRngValue(RNGHandler.RNGTypes,String)} call because the world is being loaded.
     * @param runId the {@code Long} that will be used as
     * @author Void_X_Walker
     */
    public RNGSession(long runId,int sessionIndex) {
        this.runId = runId;
        this.sessionIndex=sessionIndex;
        sessionState=SessionState.STARTUP;
        currentRNGHandler = null;
        rngHandlerCompletableFuture = CompletableFuture.supplyAsync(()-> RNGHandler.createRNGHandler(runId));
        backupRandom = null;
    }
    public void log(Level level,String message){
        ServerSideRNG.log(level,"["+this.runId+";"+this.sessionIndex+"]"+message);
    }
    private Optional<RNGHandler> getCurrentRNGHandler(){
        return Optional.ofNullable(currentRNGHandler);
    }
    private void setCurrentRNGHandler(RNGHandler handler){
        currentRNGHandler=handler;
    }
    public Optional< CompletableFuture<Optional<RNGHandler>>> getRngHandlerCompletableFuture(){
        return Optional.ofNullable(rngHandlerCompletableFuture);
    }
    private void setRngHandlerCompletableFuture(CompletableFuture<Optional<RNGHandler>> completableFuture){
        rngHandlerCompletableFuture=completableFuture;
    }
    private Optional<Random> getBackupRandom(){
        return Optional.ofNullable(backupRandom);
    }
    /**
     * Gets the current RNGHandler after trying updating it
     * @return the current RNGHandler after calling the {@link RNGSession#updateRNGHandler()} method on it.
     * @see RNGSession#updateRNGHandler()
     * @author Void_X_Walker
     */
    public Optional<RNGHandler> getAndUpdateCurrentRNGHandler() {
        updateRNGHandler();
        return getCurrentRNGHandler();
    }

    /**
     * Updates the {@link RNGSession#currentRNGHandler} if it's either out of normal time and the {@link RNGSession#rngHandlerCompletableFuture} has completed or it's out of extra time().
     * @see RNGHandler#outOfNormalTime()
     * @see RNGHandler#outOfExtraTime()
     * @author Void_X_Walker
     */
    public void updateRNGHandler() {
        getCurrentRNGHandler().ifPresentOrElse(
                rngHandler -> {
                    if(rngHandler.outOfNormalTime()){
                        getRngHandlerCompletableFuture().ifPresentOrElse(ignored -> {}, ()->{
                            System.out.println("updated future 1");
                            setRngHandlerCompletableFuture( CompletableFuture.supplyAsync(()-> RNGHandler.createRNGHandler(runId)));
                        });
                        if(getRngHandlerCompletableFuture().map(rngHandlerCompletableFuture1 -> rngHandlerCompletableFuture1.isDone()||rngHandler.outOfExtraTime()).orElse(false)){
                            rngHandler.log(Level.INFO,"Current RNGHandler ran out of time, updating it!");
                            getRngHandlerFromFuture();
                        }
                    }
                },
                ()-> {
                    System.out.println("updated future 2");
                    getRngHandlerCompletableFuture().ifPresentOrElse(ignored -> {},()->setRngHandlerCompletableFuture(CompletableFuture.supplyAsync(()-> RNGHandler.createRNGHandler(runId))));
                    getRngHandlerFromFuture();
                }
        );

    }
    public void setPaused(boolean paused){
        ServerSideRNG.log(Level.INFO,(paused?"Paused ":"Unpaused ")+"the RNGSession.");
        sessionState=paused?SessionState.PAUSED:SessionState.RUNNING;
    }
    public boolean isPaused(){
        return sessionState==SessionState.PAUSED;
    }
    public boolean inStartup(){
        return sessionState==SessionState.STARTUP;
    }
    public void joinWorld(){
        worldJoinTime=worldJoinTime==0? System.nanoTime():worldJoinTime;
    }
    boolean canBePaused(){
        return inStartup()&&updateSessionState();
    }
    public void tryToPause(){
        if(canBePaused()){
            setPaused(true);
        }
    }
    boolean updateSessionState(){
        if(System.nanoTime()-worldJoinTime > ServerSideRNGConfig.GRACE_PERIOD){
            sessionState=SessionState.RUNNING;
            return false;
        }
        return true;
    }
    /**
     * Tries to update the {@link RNGSession#currentRNGHandler} with the {@link RNGHandler} created by the {@link RNGSession#rngHandlerCompletableFuture} and then activates it.
     * <p>
     * If the {@code CompletableFuture} hasn't completed or doesn't complete after one second,
     * the {@link RNGSession#currentRNGHandler} will be updated using a {@link RNGHandler} created with the {@link RNGSession#backupRandom}.
     * @see RNGHandler#activate(int)
     * @author Void_X_Walker
     */
    public void getRngHandlerFromFuture() {
        Optional<RNGHandler> obtainedRNGHandler;
        try {
            obtainedRNGHandler=getRngHandlerCompletableFuture().orElseThrow(IllegalStateException::new).get(1L, TimeUnit.SECONDS);
        } catch (Throwable e) {
            log(Level.WARN, "Failed to update the current RNGHandler from future!");
            e.printStackTrace();
            obtainedRNGHandler=Optional.empty();
        }
        try {
            if(obtainedRNGHandler.isPresent()){
                setCurrentRNGHandler(obtainedRNGHandler.get());
            }
            else {
                setCurrentRNGHandler(
                        getBackupRandom()
                                .map(random -> {
                                    RNGHandler handler = new RNGHandler(random.nextLong());
                                    handler.log(Level.WARN, "Using RNGHandler created by the backup Random!");
                                    return handler;
                                }).orElseThrow(IllegalStateException::new));
            }
            getCurrentRNGHandler().ifPresent(rngHandler -> {
                rngHandler.activate(handlerIndex++);
                rngHandler.log(Level.INFO,"Successfully updated the current RNGHandler!");
            });
        } catch (IllegalStateException e) {
            log(Level.WARN, "Failed to update the current RNGHandler from backup Random!");
        }

        setRngHandlerCompletableFuture(CompletableFuture.supplyAsync(()-> RNGHandler.createRNGHandler(runId)));
    }
    /**
     * STARTUP = World creation
     * <p>
     * PAUSED = Paused the game in the first 2 seconds after joining
     * <p>
     * RUNNING = 2 seconds after join or after the first pause,whichever is first
     **/
    enum SessionState{
        STARTUP,
        PAUSED,
        RUNNING
    }
}
