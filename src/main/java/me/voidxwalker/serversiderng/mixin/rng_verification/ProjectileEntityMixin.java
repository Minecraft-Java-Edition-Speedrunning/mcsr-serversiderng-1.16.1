package me.voidxwalker.serversiderng.mixin.rng_verification;

import me.voidxwalker.serversiderng.RNGHandler;
import me.voidxwalker.serversiderng.RNGSession;
import me.voidxwalker.serversiderng.ServerSideRNG;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Random;
import java.util.function.Supplier;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
    @Shadow @Nullable public abstract Entity getOwner();
    @Unique
    private float serversiderng_divergence;
    /**
     * Obtains the {@link ProjectileEntityMixin#serversiderng_divergence} {@code Float}, so it can later be used in {@link ProjectileEntityMixin#serversiderng_modifyArrowRandom( Args)} method.
     * @author Void_X_Walker
     */
    @Inject(method = "setVelocity", at = @At("HEAD"))
    public void serversiderng_getDivergence(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
        if (RNGSession.inSession()) {
            serversiderng_divergence = divergence;
        }
    }
    /**
     * Uses the from {@link  ServerSideRNG#getRngContext(RNGHandler.RNGTypes)} obtained random {@code Long}, that has been generated by the {@code Verification-Server}, as a seed for the {@link RNGHandler.RNGTypes#PROJECTILE} RNG.
     * @see  ServerSideRNG#getRngContext(RNGHandler.RNGTypes)
     * @author Void_X_Walker
     */
    @ModifyArgs(method = "setVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;"))
    public void serversiderng_modifyArrowRandom(Args args) {
        ServerSideRNG.getRngContext(RNGHandler.RNGTypes.PROJECTILE)
            .filter((it) -> this.getOwner() instanceof PlayerEntity)
            .map(Supplier::get)
            .map(Random::new)
            .ifPresent((random) -> {
                args.set(0, random.nextGaussian() * 0.007499999832361937D * (double) serversiderng_divergence);
                args.set(1, random.nextGaussian() * 0.007499999832361937D * (double) serversiderng_divergence);
                args.set(2, random.nextGaussian() * 0.007499999832361937D * (double) serversiderng_divergence);
            });
    }
}
