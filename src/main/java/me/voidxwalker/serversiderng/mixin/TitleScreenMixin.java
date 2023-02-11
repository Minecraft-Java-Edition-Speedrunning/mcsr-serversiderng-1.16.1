package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.RNGInitializer;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    /**
     * Resets the {@link RNGInitializer#instance} to avoid it being updated while outside the world / speedrun.
     * @see RNGInitializer#stopRNGSession()
     * @author Void_X_Walker
     */
    @Inject(method = "init", at = @At("HEAD"))
    public void endRNGSession(CallbackInfo ci) {
        if (RNGInitializer.inSession()) {
            RNGInitializer.stopRNGSession();
        }
    }
}
