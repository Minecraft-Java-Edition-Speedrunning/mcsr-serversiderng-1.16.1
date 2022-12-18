package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.RNGSession;
import me.voidxwalker.serversiderng.ServerSideRNG;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.util.WorldSavePath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@Mixin(CreditsScreen.class)
public class CreditScreenMixin {
    /**
     * Packs the world folder of the current world and latest.log into a {@code ZIP-File} in the {@code Verification Folder } and sends a {@code Hash} of it to the {@code Verification-Server} when the player enters the end portal, which is assumed to be the end for most SpeedRuns
     * This is done async to avoid any lag when entering the end portal.
     * @param endCredits will not activate if called on the {@link net.minecraft.client.gui.screen.TitleScreen}.
     * @author Void_X_Walker
     */
     @Inject(method = "<init>", at = @At("TAIL"))
     public void initCreditsScreen(boolean endCredits, Runnable finishAction, CallbackInfo ci) {
         if (endCredits && RNGSession.inSession() && MinecraftClient.getInstance().getServer() != null) {
             File worldFile = MinecraftClient
                 .getInstance()
                 .getServer()
                 .getSavePath(WorldSavePath.ROOT)
                 .toFile()
                 .getParentFile();
             CompletableFuture.runAsync(()-> ServerSideRNG.getAndUploadHash(worldFile));
         }
     }
}
