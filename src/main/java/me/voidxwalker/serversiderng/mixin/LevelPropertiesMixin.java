package me.voidxwalker.serversiderng.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import me.voidxwalker.serversiderng.ServerSideRng;
import me.voidxwalker.serversiderng.Speedrun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.SaveVersionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelProperties.class)
public class LevelPropertiesMixin {
    /**
     * Saves the {@link Speedrun#runId} to the level.dat file.
     * @author Void_X_Walker
     */
    @Inject(method = "updateProperties",at = @At("TAIL"))
    public void saveRunId(RegistryTracker registryTracker, CompoundTag compoundTag, CompoundTag compoundTag2, CallbackInfo ci){
        if(ServerSideRng.inSpeedrun()){
            compoundTag.putLong("server-side-rng-runId", ServerSideRng.currentSpeedrun.runId);
        }
    }
    /**
     * Creates a new {@link Speedrun} from the {@link Speedrun#runId} stored in the level.dat file.
     * @see Speedrun#Speedrun(long)
     * @author Void_X_Walker
     */
    @Inject(method = "method_29029",at = @At("HEAD"))
    private static void loadRunId(Dynamic<Tag> dynamic, DataFixer dataFixer, int i, CompoundTag compoundTag, LevelInfo levelInfo, SaveVersionInfo saveVersionInfo, GeneratorOptions generatorOptions, Lifecycle lifecycle, CallbackInfoReturnable<LevelProperties> cir){
        if(dynamic.get("server-side-rng-runId").result().isPresent()){
            dynamic.get("server-side-rng-runId").asNumber().result().ifPresentOrElse(result -> {
                ServerSideRng.currentSpeedrun= new Speedrun(result.longValue());
                ServerSideRng.LOGGER.info("Successfully loaded RunID from file!");
            },() -> ServerSideRng.LOGGER.warn("Failed to load RunID from file!"));
        }
    }
}
