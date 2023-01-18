package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.RNGSession;
import me.voidxwalker.serversiderng.ServerSideRNG;
import me.voidxwalker.serversiderng.ServerSideRNGConfig;
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

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow @Nullable public ClientWorld world;
    @Shadow private @Nullable IntegratedServer server;
    @Shadow private boolean paused;
    @Shadow @Nullable public Screen currentScreen;
    public long serverSideRNG_lastInWorld;
    /**
     * If the client has been disconnected from a server for 5 seconds, the has of the last world gets uploaded
     * @see ServerSideRNG#getAndUploadHash(File)
     * @author Void_X_Walker
     */
    @Inject(method = "render",at = @At("HEAD"))
    public void trackLastInWorld(CallbackInfo ci){
        if(ServerSideRNGConfig.UPLOAD_ON_WORLD_LEAVE){
            serverSideRNG_lastInWorld=this.server!=null?System.nanoTime(): serverSideRNG_lastInWorld;
            if(ServerSideRNG.lastWorldFile!=null&& System.nanoTime()-serverSideRNG_lastInWorld> ServerSideRNGConfig.TIME_OUT_OF_WORLD_BEFORE_AUTOUPLOAD){
                ServerSideRNG.uploadHash(true,false);
            }
        }
    }
    /**
     * Updates the {@link RNGSession#currentRNGHandler} at the end of world generation, if the {@link RNGSession#rngHandlerCompletableFuture} has completed.
     * @see RNGSession#getRngHandlerFromFuture()
     * @author Void_X_Walker
     */
    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerNetworkIo;bindLocal()Ljava/net/SocketAddress;", shift = At.Shift.BEFORE)
    )
    public void updateHandlerAfterWorldGen(CallbackInfo ci) {
        if (RNGSession.inSession()) {
            if (RNGSession.getInstance().rngHandlerCompletableFuture.isDone()) {
                RNGSession.getInstance().getRngHandlerFromFuture();
            }
            ServerSideRNG.lastWorldFile= Objects.requireNonNull(MinecraftClient.getInstance().getServer())
                    .getSavePath(WorldSavePath.ROOT)
                    .toFile()
                    .getParentFile();
        }
    }
    /**
     * Tries to update the {@link RNGSession#currentRNGHandler} every game tick.
     * Pauses the {@link RNGSession#currentRNGHandler} from making requests if th game is Paused and the time the player has spent in the world is smaller than {@code 2 seconds}.
     * @see RNGSession#updateRNGHandler()
     * @author Void_X_Walker
     */
    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        if (RNGSession.inSession()) {
            if(!RNGSession.getInstance().isPaused()){
                RNGSession.getInstance().updateRNGHandler();
                if(this.paused&&currentScreen instanceof GameMenuScreen){
                    RNGSession.getInstance().tryToPause();
                }
            }
            else if(!this.paused){
                RNGSession.getInstance().setPaused(false);
            }
        }
    }
    /**
     * Tries to upload the Hash of the Run when Minecraft shuts
     * @see ServerSideRNG#getAndUploadHash(File)
     * @author Void_X_Walker
     */
    @Inject(method = "stop",at = @At("HEAD"))
    public void saveOnShutdown(CallbackInfo ci){
        if(ServerSideRNG.lastWorldFile!=null&&(this.server ==null||RNGSession.inSession())){
            ServerSideRNG.getAndUploadHash(ServerSideRNG.lastWorldFile);
        }
    }
    /**
     * Tracks the time the player joins the world
     * @author Void_X_Walker
     */
    @Inject(method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V", at = @At(value = "TAIL"))
    public void trackWorldRenderStart(CallbackInfo ci){
        if (RNGSession.inSession()&&RNGSession.getInstance().inStartup()) {
            RNGSession.getInstance().joinWorld();
        }
    }
}
