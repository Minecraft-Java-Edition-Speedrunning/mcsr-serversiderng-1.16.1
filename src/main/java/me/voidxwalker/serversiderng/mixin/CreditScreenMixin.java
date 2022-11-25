package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.ServerSideRng;
import net.minecraft.client.gui.screen.CreditsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CreditsScreen.class)
public class CreditScreenMixin {
     @Inject(method = "<init>",at = @At("TAIL"))
     public void initCreditsScreen(boolean endCredits, Runnable finishAction, CallbackInfo ci){
         if(endCredits&& ServerSideRng.inSpeedrun()){
             CompletableFuture.runAsync(ServerSideRng::getAndUploadHash);
         }
     }
}
