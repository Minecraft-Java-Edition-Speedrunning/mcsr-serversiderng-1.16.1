package me.voidxwalker.serversiderng.mixin;

import com.mojang.brigadier.context.CommandContext;
import me.voidxwalker.serversiderng.RNGSession;
import me.voidxwalker.serversiderng.ServerSideRNG;
import me.voidxwalker.serversiderng.ServerSideRNGConfig;
import net.minecraft.server.command.SeedCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SeedCommand.class)
public class SeedCommandMixin {
    @Inject(method = "method_13617",at = @At("TAIL"))
    @SuppressWarnings("rawtypes")
    private static void uploadHashOnSeedCommand(CommandContext commandContext, CallbackInfoReturnable<Integer> cir){
        if(RNGSession.inSession()&& ServerSideRNGConfig.UPLOAD_ON_SEED){
            ServerSideRNG.uploadHash(false,true);
        }
    }
}
