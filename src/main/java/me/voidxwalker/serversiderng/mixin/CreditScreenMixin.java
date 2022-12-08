package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.ServerSideRNG;
import me.voidxwalker.serversiderng.Speedrun;
import net.minecraft.client.gui.screen.CreditsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CreditsScreen.class)
public class CreditScreenMixin {
    /**
     * Uploads a zip of the world folder and latest.log to the server when the player enters the end portal, which is assumed to be the end for most speedruns
     * @author Void_X_Walker
     */
     @Inject(method = "<init>",at = @At("TAIL"))
     public void initCreditsScreen(boolean endCredits, Runnable finishAction, CallbackInfo ci){
         if(endCredits&& Speedrun.inSpeedrun()){
             CompletableFuture.runAsync(ServerSideRNG::getAndUploadHash);
         }
     }
}
