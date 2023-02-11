package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.WorldSavePath;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow @Nullable public ClientWorld world;
    @Shadow private @Nullable IntegratedServer server;
    @Shadow private boolean paused;
    @Shadow @Nullable public Screen currentScreen;
    public long serverSideRNG_lastInWorld;
    /**
     * If the client has been disconnected from a server for 5 seconds, the has of the last world gets uploaded
     * @see IOUtils#getAndUploadHash(File, long)
     * @author Void_X_Walker
     */
    @Inject(method = "render",at = @At("HEAD"))
    public void serversiderng_trackLastInWorld(CallbackInfo ci){
        if(ServerSideRNGConfig.UPLOAD_ON_WORLD_LEAVE){
            serverSideRNG_lastInWorld=this.server!=null?System.nanoTime(): serverSideRNG_lastInWorld;
            if( System.nanoTime()-serverSideRNG_lastInWorld> ServerSideRNGConfig.TIME_OUT_OF_WORLD_BEFORE_AUTOUPLOAD){
                ServerSideRNG.getLastSession().ifPresent(lastSession -> {
                    serverSideRNG_lastInWorld=Long.MAX_VALUE;
                    IOUtils.getAndUploadHash(lastSession.lastWorldFile, lastSession.lastRunId);
                });
            }

        }
    }
    /**
     * Updates the {@code RNGSession.currentRNGHandler} at the end of world generation, if the {@code RNGSession.rngHandlerCompletableFuture} has completed.
     * @see RNGSession#getRngHandlerFromFuture()
     * @author Void_X_Walker
     */
    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerNetworkIo;bindLocal()Ljava/net/SocketAddress;", shift = At.Shift.BEFORE)
    )
    public void serversiderng_updateHandlerAfterWorldGen(CallbackInfo ci) {
        RNGSession.getInstance().ifPresent(rngSession -> rngSession.getRngHandlerCompletableFuture().ifPresent(completableFuture ->{
            if(completableFuture.isDone()){
                rngSession.getRngHandlerCompletableFuture();
            }
        } ));
        RNGSession.getInstance().ifPresent(rngSession -> ServerSideRNG.setLastSession(new ServerSideRNG.LastSession(
                Objects.requireNonNull(MinecraftClient.getInstance().getServer())
                        .getSavePath(WorldSavePath.ROOT)
                        .toFile()
                        .getParentFile(), rngSession.runId)
        ));
    }
    /**
     * Tries to update the {@code RNGSession.currentRNGHandler} every game tick.
     * Pauses the {@code RNGSession.currentRNGHandler} from making requests if th game is Paused and the time the player has spent in the world is smaller than {@code 2 seconds}.
     * @see RNGSession#updateRNGHandler()
     * @author Void_X_Walker
     */
    @Inject(method = "tick", at = @At("HEAD"))
    public void serversiderng_tick(CallbackInfo ci) {
        Optional<RNGInitializer> rngInitializerOptional= ServerSideRNG.getRNGInitializer();
        if(rngInitializerOptional.filter(rngInitializer -> !rngInitializer.outOfTime()).isEmpty()){
            RNGInitializer.update();
        }
        ServerSideRNG.getRNGInitializer().map(rngInitializer -> rngInitializer.outOfTime() ? rngInitializer : null).ifPresent(rngInitializer -> RNGInitializer.update());
        Optional<RNGSession> optional= RNGSession.getInstance();
        optional.ifPresent(rngSession -> {
            if(rngSession.isPaused()){
                rngSession.updateRNGHandler();
                if(this.paused && currentScreen instanceof GameMenuScreen){
                    rngSession.tryToPause();
                }
            }
            else {
                if(!this.paused){
                    rngSession.setPaused(false);
                }
            }
        });
    }
    /**
     * Tries to upload the Hash of the Run when Minecraft shuts
     * @see IOUtils#getAndUploadHash(File, long)
     * @author Void_X_Walker
     */
    @Inject(method = "stop",at = @At("HEAD"))
    public void serversiderng_saveOnShutdown(CallbackInfo ci){
        ServerSideRNG.getLastSession().ifPresent(lastSession -> IOUtils.getAndUploadHash(lastSession.lastWorldFile,ServerSideRNG.getRNGInitializer().flatMap(RNGInitializer::getInstance).map(rngSession -> rngSession.runId).orElse(lastSession.lastRunId)));

    }
    /**
     * Tracks the time the player joins the world
     * @author Void_X_Walker
     */
    @Inject(method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V", at = @At(value = "TAIL"))
    public void serversiderng_trackWorldRenderStart(CallbackInfo ci){
        ServerSideRNG.getRNGInitializer().flatMap(rngInitializer -> rngInitializer.getInstance().filter(RNGSession::inStartup)).ifPresent(RNGSession::joinWorld);
    }
}
