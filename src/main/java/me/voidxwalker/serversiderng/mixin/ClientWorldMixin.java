package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.Speedrun;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.world.ClientWorld.class)
public class ClientWorldMixin {
    /**
     * Resets the {@link Speedrun#currentSpeedrun} to avoid it being updated while outside the world / speedrun.
     * @author Void_X_Walker
     */
    @Inject(method = "disconnect",at = @At("HEAD"))
    public void endSpeedrun(CallbackInfo ci){
        if(Speedrun.inSpeedrun()){
            Speedrun.currentSpeedrun=null;
        }
    }
}
