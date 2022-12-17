package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.RNGSession;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
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
        }
    }
    /**
     * Tries to update the {@link RNGSession#currentRNGHandler} every game tick.
     * @see RNGSession#updateRNGHandler()
     * @author Void_X_Walker
     */
    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        if (RNGSession.inSession()) {
            RNGSession.getInstance().updateRNGHandler();
        }
    }
}
