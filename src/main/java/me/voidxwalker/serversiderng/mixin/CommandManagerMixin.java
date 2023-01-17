package me.voidxwalker.serversiderng.mixin;

import com.mojang.brigadier.CommandDispatcher;
import me.voidxwalker.serversiderng.ServerSideRNG;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Shadow @Final private CommandDispatcher<ServerCommandSource> dispatcher;
    /**
     * Registers UploadHash Command.
     * @author Void_X_Walker
     */
    @Inject(method = "<init>",at = @At("TAIL"))
    private void registerUploadRunCommand(CommandManager.RegistrationEnvironment environment, CallbackInfo ci){
        ServerSideRNG.register(this.dispatcher);
    }
}
