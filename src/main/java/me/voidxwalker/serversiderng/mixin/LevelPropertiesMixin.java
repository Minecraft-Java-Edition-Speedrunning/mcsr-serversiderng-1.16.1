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
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelProperties.class)
public class LevelPropertiesMixin {
    @Unique private Long serversiderng_cachedRunID;
    @Unique private Integer serversiderng_cachedSessionIndex;
    /**
     * Saves the {@link RNGSession#runId} and {@link RNGSession#sessionIndex} to the level.dat file.
     * @author Void_X_Walker
     */
    @Inject(method = "updateProperties", at = @At("TAIL"))
    public void serversiderng_saveRunId(RegistryTracker registryTracker, CompoundTag compoundTag, CompoundTag compoundTag2, CallbackInfo ci) {
        RNGSession.getInstance().ifPresentOrElse(rngSession -> {
            compoundTag.putLong("serversiderng-runid", rngSession.runId);
            compoundTag.putLong("serversiderng-cachedSessionIndex", rngSession.sessionIndex);
            serversiderng_cachedRunID = rngSession.runId;
            serversiderng_cachedSessionIndex=rngSession.sessionIndex;
        },() -> {
            if(serversiderng_cachedRunID!=null){
                compoundTag.putLong("serversiderng-runid", serversiderng_cachedRunID);
            }
            if(serversiderng_cachedSessionIndex!=null){
                compoundTag.putLong("serversiderng-cachedSessionIndex", serversiderng_cachedSessionIndex);
            }

        });
    }
    /**
     * Creates a new {@link RNGSession} from the {@link RNGSession#runId} stored in the level.dat file.
     * @see RNGSession#RNGSession(long,int)
     * @author Void_X_Walker
     */
    @Inject(method = "method_29029", at = @At("HEAD"))
    private static void serversiderng_loadRunId(
            Dynamic<Tag> dynamic,
            DataFixer dataFixer,
            int i,
            CompoundTag compoundTag,
            LevelInfo levelInfo,
            SaveVersionInfo saveVersionInfo,
            GeneratorOptions generatorOptions,
            Lifecycle lifecycle,
            CallbackInfoReturnable<LevelProperties> cir
    ) {
        dynamic.get("serversiderng-runid").asNumber().result().ifPresentOrElse(
                number ->  dynamic.get("serversiderng-runid").asNumber().result().ifPresentOrElse(
                        number1 ->ServerSideRNG.getRNGInitializer().ifPresent(
                                rngInitializer -> rngInitializer.setSession(new RNGSession(number.longValue(),number1.intValue()))),
                                ()-> ServerSideRNG.log(Level.INFO,"Failed to load RunID from file!")
                ),
                ()-> ServerSideRNG.log(Level.INFO,"Failed to load RunID from file!"));
    }
}
