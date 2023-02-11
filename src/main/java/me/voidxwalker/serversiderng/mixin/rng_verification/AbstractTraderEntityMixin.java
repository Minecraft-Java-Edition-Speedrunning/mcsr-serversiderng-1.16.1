package me.voidxwalker.serversiderng.mixin.rng_verification;

import me.voidxwalker.serversiderng.RNGHandler;
import me.voidxwalker.serversiderng.ServerSideRNG;
import net.minecraft.entity.passive.AbstractTraderEntity;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import java.util.function.Supplier;

@Mixin(AbstractTraderEntity.class)
public class AbstractTraderEntityMixin {
    @SuppressWarnings("all")
    @Redirect(method = "fillRecipesFromPool",at = @At(value = "INVOKE",target = "Ljava/util/Random;nextInt(I)I"))
    public int serversiderng_modifySelectOfferRandom(Random random, int i){
        String profession=null;
        if((AbstractTraderEntity)(Object)this instanceof VillagerEntity) {
            profession=((VillagerEntity) (Object) this).getVillagerData().getProfession().toString();
        }
        return ServerSideRNG.getRngContext(RNGHandler.RNGTypes.VILLAGER_SELECT_OFFER,profession)
                .map(Supplier::get)
                .map((it)-> new Random(it))
                .orElse(random).nextInt(i);
    }
    @SuppressWarnings("all")
    @ModifyArg(
            method = "fillRecipesFromPool",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/village/TradeOffers$Factory;create(Lnet/minecraft/entity/Entity;Ljava/util/Random;)Lnet/minecraft/village/TradeOffer;"),
            index = 1
    )
    public Random serversiderng_modifyOfferRandom(Random random){
        String profession=null;
        if((AbstractTraderEntity)(Object)this instanceof VillagerEntity) {
            profession=((VillagerEntity) (Object) this).getVillagerData().getProfession().toString();
        }
        return ServerSideRNG.getRngContext(RNGHandler.RNGTypes.VILLAGER_OFFER,profession)
                .map(Supplier::get)
                .map(Random::new)
                .orElse(random);
    }

}
