package me.voidxwalker.serversiderng.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import me.voidxwalker.serversiderng.RNGSession;
import me.voidxwalker.serversiderng.ServerSideRNG;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.SaveVersionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LevelProperties.class)
public class LevelPropertiesMixin {
    @Unique private Long serverSideRNG_cachedRunID;
    /**
     * Saves the {@link RNGSession#runId} to the level.dat file.
     * @author Void_X_Walker
     */
    @Inject(method = "updateProperties",at = @At("TAIL"))
    public void saveRunId(RegistryTracker registryTracker, CompoundTag compoundTag, CompoundTag compoundTag2, CallbackInfo ci){
        if(RNGSession.inSession()){
            compoundTag.putLong("server-side-rng-runId", RNGSession.getInstance().runId);
            serverSideRNG_cachedRunID = RNGSession.getInstance().runId;
        }
        else if(serverSideRNG_cachedRunID !=null){
            compoundTag.putLong("server-side-rng-runId", serverSideRNG_cachedRunID);
        }
    }
    /**
     * Creates a new {@link RNGSession} from the {@link RNGSession#runId} stored in the level.dat file.
     * @see RNGSession#RNGSession(long)
     * @author Void_X_Walker
     */
    @Inject(method = "method_29029",at = @At("HEAD"))
    private static void loadRunId(Dynamic<Tag> dynamic, DataFixer dataFixer, int i, CompoundTag compoundTag, LevelInfo levelInfo, SaveVersionInfo saveVersionInfo, GeneratorOptions generatorOptions, Lifecycle lifecycle, CallbackInfoReturnable<LevelProperties> cir){
        if(dynamic.get("server-side-rng-runId").result().isPresent()){
            Optional<Number> optional= dynamic.get("server-side-rng-runId").asNumber().result();
            if(optional.isPresent()){
                RNGSession.instance = new RNGSession(optional.get().longValue());
                ServerSideRNG.LOGGER.info("Successfully loaded RunID from file!");
            }
            else {
                ServerSideRNG.LOGGER.warn("Failed to load RunID from file!");
            }
        }
    }
}
