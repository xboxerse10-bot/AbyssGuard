package ro.abyssmc.abyssguard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ro.abyssmc.abyssguard.AbyssGuard;
import ro.abyssmc.abyssguard.Flag;

/** Stops fire's scheduled spread tick when its origin is inside a protected region.
 * Fire entering from outside requires a separate boundary check.
 */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void abyssguard$stopFireSpread(BlockState state, ServerLevel level, BlockPos pos,
                                           RandomSource random, CallbackInfo ci) {
        String dim = level.dimension().identifier().toString();
        if (!AbyssGuard.regionAllows(dim, pos, Flag.FIRE_SPREAD)
                || !AbyssGuard.regionAllows(dim, pos, Flag.BLOCK_PLACE)) {
            ci.cancel();
        }
    }
}
