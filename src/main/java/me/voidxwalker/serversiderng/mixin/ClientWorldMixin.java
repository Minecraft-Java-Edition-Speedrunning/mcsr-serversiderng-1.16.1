package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.ServerSideRng;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.world.ClientWorld.class)
public class ClientWorldMixin {
    /**
     * Resets the {@link ServerSideRng#currentSpeedrun} to avoid it being updated while outside the world / speedrun.
     * @author Void_X_Walker
     */
    @Inject(method = "disconnect",at = @At("HEAD"))
    public void endSpeedrun(CallbackInfo ci){
        if(ServerSideRng.inSpeedrun()){
            ServerSideRng.currentSpeedrun=null;
        }
    }
}
