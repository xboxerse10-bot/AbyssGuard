package ro.abyssmc.abyssguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.core.BlockPos;
import ro.abyssmc.abyssguard.AbyssGuard;
import ro.abyssmc.abyssguard.Flag;

/** Stops vanilla natural spawn attempts before mobs are created.
 * Does not remove existing mobs or affect commands/spawn eggs.
 * This hook targets Mojang mappings for Minecraft 26.3; runtime test required.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @Inject(method = "spawnCategoryForPosition", at = @At("HEAD"), cancellable = true, require = 0)
    private static void abyssguard$blockNaturalSpawns(MobCategory category, ServerLevel level,
            ChunkAccess chunk, BlockPos pos, CallbackInfo ci) {
        String dimension = level.dimension().identifier().toString();
        if (!AbyssGuard.regionAllows(dimension, pos, Flag.MOB_SPAWNING)) ci.cancel();
    }
}
