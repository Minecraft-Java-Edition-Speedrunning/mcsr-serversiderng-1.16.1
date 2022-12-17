package me.voidxwalker.serversiderng.mixin.rng_verification;

import me.voidxwalker.serversiderng.RNGHandler;
import me.voidxwalker.serversiderng.RNGSession;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.loot.context.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import java.util.function.Supplier;

@Mixin(FishingBobberEntity.class)
public class FishingBobberEntityMixin {
    @Shadow private int waitCountdown;

    @Shadow private int fishTravelCountdown;

    @Shadow private int hookCountdown;
    /**
     * Uses the from {@link RNGHandler#getRngValue(RNGHandler.RNGTypes)} obtained random {@code Long}, that has been generated by the {@code Verification-Server}, as a seed for the as a seed for the {@link RNGHandler.RNGTypes#FISHING_RESULT} RNG.
     * @author Void_X_Walker
     * @see RNGHandler#getRngValue(RNGHandler.RNGTypes)
     */
    @Redirect(
            method = "use",
            at = @At(value = "INVOKE",target = "Lnet/minecraft/loot/context/LootContext$Builder;random(Ljava/util/Random;)Lnet/minecraft/loot/context/LootContext$Builder;")
    )
    public LootContext.Builder modifyFishingResultRandom(LootContext.Builder instance, Random random) {
        Random targetRandom = RNGSession.getRngContext(RNGHandler.RNGTypes.FISHING_RESULT)
            .map(Supplier::get)
            .map(Random::new)
            .orElse(random);
        return instance.random(targetRandom);
    }
    /**
     * Uses the from {@link RNGHandler#getRngValue(RNGHandler.RNGTypes)} obtained random {@code Long}, that has been generated by the {@code Verification-Server}, as a seed for the {@link RNGHandler.RNGTypes#FISHING_TIME} RNG.
     * @author Void_X_Walker
     */
    @ModifyArg(
            method = "tickFishingLogic",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;nextInt(Ljava/util/Random;II)I"),
            index = 0
    )
    public Random modifyFishingTimeRandom(Random random) {
        return RNGSession.getRngContext(RNGHandler.RNGTypes.FISHING_TIME)
            .filter((it) -> waitCountdown <= 0 && fishTravelCountdown <= 0 && hookCountdown <= 0)
            .map(Supplier::get)
            .map(Random::new)
            .orElse(random);
    }
}
