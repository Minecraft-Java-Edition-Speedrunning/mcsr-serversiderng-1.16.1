package me.voidxwalker.serversiderng.mixin;

import com.mojang.brigadier.context.CommandContext;
import me.voidxwalker.serversiderng.IOUtils;
import me.voidxwalker.serversiderng.RNGSession;
import me.voidxwalker.serversiderng.ServerSideRNGConfig;
import net.minecraft.server.command.SeedCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SeedCommand.class)
public class SeedCommandMixin {
    @Inject(method = "method_13617",at = @At("TAIL"),remap = false)

    private static void serversiderng_uploadHashOnSeedCommand(CommandContext<ServerCommandSource> commandContext, CallbackInfoReturnable<Integer> cir){
        if(ServerSideRNGConfig.UPLOAD_ON_SEED){
            RNGSession.getInstance().ifPresent(rngSession -> IOUtils.uploadHash(rngSession.runId));

        }
    }
}
