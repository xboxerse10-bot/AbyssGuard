package ro.abyssmc.abyssguard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ro.abyssmc.abyssguard.AbyssGuard;
import ro.abyssmc.abyssguard.Flag;

/** Stop any fire placement inside a protected region, including fire lit by lava. */
@Mixin(Level.class)
public abstract class LevelFireMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void abyssguard$preventFire(BlockPos pos, BlockState newState, int flags, int recursionLeft,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof ServerLevel serverLevel)) return;
        if (!(newState.getBlock() instanceof BaseFireBlock)) return;
        String dimension = serverLevel.dimension().identifier().toString();
        if (!AbyssGuard.regionAllows(dimension, pos, Flag.BLOCK_PLACE) ||
            !AbyssGuard.regionAllows(dimension, pos, Flag.FIRE_SPREAD)) {
            cir.setReturnValue(false);
        }
    }
}
