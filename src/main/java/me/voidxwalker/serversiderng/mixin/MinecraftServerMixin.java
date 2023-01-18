package me.voidxwalker.serversiderng.mixin;

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
     * @see ServerSideRNG#getAndUploadHash(File)
     * @author Void_X_Walker
     */
    @Inject(method = "tick",at = @At("HEAD"))
    public void upload(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        if(ServerSideRNG.needsUpload()){
            ServerSideRNG.uploadHash(true,true);
        }
    }
}
