package me.voidxwalker.serversiderng.mixin;

import com.mojang.brigadier.context.CommandContext;
import me.voidxwalker.serversiderng.RNGSession;
import me.voidxwalker.serversiderng.ServerSideRNG;
import me.voidxwalker.serversiderng.ServerSideRNGConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.SeedCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SeedCommand.class)
public class SeedCommandMixin {
    @Inject(method = "method_13617",at = @At("TAIL"))
    private static void uploadHashOnSeedCommand(CommandContext commandContext, CallbackInfoReturnable<Integer> cir){
        if(RNGSession.inSession()&& ServerSideRNGConfig.UPLOAD_ON_SEED){
            assert MinecraftClient.getInstance().getServer()!=null;
            MinecraftClient.getInstance().getServer().getPlayerManager().saveAllPlayerData();
            MinecraftClient.getInstance().getServer().save(true, false, false);
            ServerSideRNG.getAndUploadHash(MinecraftClient
                    .getInstance()
                    .getServer()
                    .getSavePath(WorldSavePath.ROOT)
                    .toFile()
                    .getParentFile());
            ((ServerCommandSource)commandContext.getSource()).sendFeedback(new LiteralText("Successfully uploaded the Run!").styled(style -> style.withColor(Formatting.GREEN)),false);

        }
    }
}
