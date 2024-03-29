package me.voidxwalker.serversiderng.mixin.rng_verification.dragon;

import me.voidxwalker.serversiderng.RNGHandler;
import me.voidxwalker.serversiderng.ServerSideRNG;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import java.util.function.Supplier;

@Mixin(EnderDragonFight.class)
public class EnderDragonFightMixin {
    /**
     * Uses the from {@link ServerSideRNG#getRngContext(RNGHandler.RNGTypes)} obtained random {@code Long}, that has been generated by the {@code Verification-Server}, for the {@link RNGHandler.RNGTypes#ENDER_DRAGON_ROTATION}.
     * @return A {@code Random} generated with a seed obtained by the verification or the vanilla {@code Random} if {@code ServerSideRNG} is not in a session
     * @see  ServerSideRNG#getRngContext(RNGHandler.RNGTypes)
     *
     * @author Void_X_Walker
     */
    @Redirect(
            method = "createDragon",
            at = @At(value = "INVOKE",
            target = "Ljava/util/Random;nextFloat()F")
    )
    private float serversiderng_modifyDragonAngle(Random instance) {
        return ServerSideRNG.getRngContext(RNGHandler.RNGTypes.ENDER_DRAGON_ROTATION)
            .map(Supplier::get)
            .map(Random::new)
            .orElse(instance).nextFloat();
    }
}
