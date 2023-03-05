package me.voidxwalker.serversiderng;

import com.mojang.brigadier.CommandDispatcher;
import me.voidxwalker.serversiderng.auth.ClientAuth;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ServerSideRNG implements ClientModInitializer {
    final static File verificationFolder = new File("verification-zips");

    private static final Logger LOGGER = LogManager.getLogger("ServerSideRNG");

    private static LastSession lastSession;

    static AtomicBoolean impendingUpload= new AtomicBoolean(false);
    private static CompletableFuture<Optional<RNGInitializer>> rngInitializerCompletableFuture;
    private static RNGInitializer currentInitializer;

    public static boolean needsUpload(){
        return impendingUpload.getAndSet(false);
    }
    public static Optional<LastSession> getLastSession(){
        return Optional.ofNullable(lastSession);
    }
    public static void setLastSession(LastSession session){
        lastSession=session;
    }
    protected static void setCurrentInitializer(RNGInitializer initializer){
        currentInitializer=initializer;
    }
    protected static Optional<CompletableFuture<Optional<RNGInitializer>>> getRngInitializerCompletableFuture(){
        return Optional.ofNullable(rngInitializerCompletableFuture);
    }
    protected static void setRngInitializerCompletableFuture(CompletableFuture<Optional<RNGInitializer>> future){
        rngInitializerCompletableFuture=future;
    }

    public static Optional<Supplier<Long>> getRngContext(RNGHandler.RNGTypes type, @Nullable String subType) {
        return getRNGInitializer().flatMap(RNGInitializer::getInstance)
            .flatMap(RNGSession::getAndUpdateCurrentRNGHandler)
            .map((it) ->()-> it.getRngValue(type,subType));
    }
    public static Optional<RNGInitializer> getRNGInitializer(){
        return Optional.ofNullable(currentInitializer);
    }

    public static Optional<Supplier<Long>> getRngContext(RNGHandler.RNGTypes type) {
        return getRngContext(type,null);
    }
    /**
     * Adds an event to the {@code onComplete} listener of <a href="https://github.com/RedLime/SpeedRunIGT">SpeedRunIGT</a> that fires at run completion.
     * It will save the world and upload its Hash after a delay of one second.
     * @see IOUtils#getAndUploadHash(File, long)  )
     * @author Void_X_Walker
     */
    @Override
    public void onInitializeClient() {
        ServerSideRNGConfig.init();
        try {
            if(FabricLoader.getInstance().isModLoaded("speedrunigt")){
                Class.forName("com.redlimerl.speedrunigt.timer.InGameTimer")
                        .getMethod("onComplete", Consumer.class)
                        .invoke(null,(Consumer<Object>) o -> new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if( RNGSession.inSession()){
                                   impendingUpload.set(true);
                                }
                            }
                        },1000));
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            ServerSideRNG.log(Level.WARN,"Failed to connect to TimerMod interface: ");
            e.printStackTrace();
        }
        CompletableFuture.runAsync(IOUtils::prepareVerificationFolder);
        ClientAuth.setClientAuthCompletableFuture( CompletableFuture.supplyAsync(()->{
            Optional<ClientAuth> auth = ClientAuth.createClientAuth();
            ServerSideRNG.setRngInitializerCompletableFuture( CompletableFuture.supplyAsync(RNGInitializer::createRNGInitializer));
            return auth;
        } ));

    }
    public static void log(Level level,String message){
        LOGGER.log(level,message);
    }
    /**
     * Registers the command {@code serversiderng_uploadRun} that will save the world and upload it's hash via the {@link IOUtils#getAndUploadHash(File, long)} method.
     * @param dispatcher the {@link CommandDispatcher} to add the command to
     * @see IOUtils#getAndUploadHash(File, long)  )
     * @author Void_X_Walker
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register( CommandManager.literal("serversiderng_uploadRun").executes(context -> {
            RNGSession.getInstance().ifPresent(rngInitializer -> IOUtils.uploadHash(rngInitializer.runId));
            return 1;
        }));
    }

    public static class LastSession{
        public  File lastWorldFile;
        public long lastRunId;
        public LastSession(File lastWorldFile, long lastRunId){
            this.lastRunId=lastRunId;
            this.lastWorldFile=lastWorldFile;
        }
    }
}
