package me.voidxwalker.serversiderng.mixin;

import me.voidxwalker.serversiderng.RNGInitializer;
import me.voidxwalker.serversiderng.RNGSession;
import me.voidxwalker.serversiderng.ServerSideRNG;
import net.minecraft.world.gen.GeneratorOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalLong;
@Mixin(GeneratorOptions.class)
public class GeneratorOptionsMixin {
    /**
     * Starts a new {@link RNGSession}, right before world generation starts.
     * @author Void_X_Walker
     * @see RNGInitializer#startRNGSession()
     */
    @Inject(method = "withHardcore",at = @At("HEAD"))
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public void serversiderng_startRun(boolean hardcore, OptionalLong seed, CallbackInfoReturnable<GeneratorOptions> cir) {
        ServerSideRNG.getRNGInitializer().ifPresent(RNGInitializer::startRNGSession);
    }
}
