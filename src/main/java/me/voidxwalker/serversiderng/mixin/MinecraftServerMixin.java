package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.IOUtils;
import me.voidxwalker.serversiderng.RNGInitializer;
import me.voidxwalker.serversiderng.RNGSession;
import me.voidxwalker.serversiderng.ServerSideRNG;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    /**
     * Listening to {@link ServerSideRNG#needsUpload()} this mixin provides a way for code on the client thread to save the world and upload a run on the server. Saving a world should not be done on the client thread
     * @see IOUtils#getAndUploadHash(File, long)
     * @author Void_X_Walker
     */
    @Inject(method = "tick",at = @At("HEAD"))
    public void serversiderng_upload(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        RNGSession.getInstance().filter(rngSession -> ServerSideRNG.needsUpload()).ifPresent(rngSession -> IOUtils.uploadHash(rngSession.runId));
    }
    @Inject(method = "<init>",at = @At("TAIL"))
    public void serversiderng_worldGenerationStart(CallbackInfo ci){
        RNGInitializer.setPaused(false);
    }
}
