package ro.abyssmc.abyssguard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ro.abyssmc.abyssguard.AbyssGuard;
import ro.abyssmc.abyssguard.Flag;

import java.lang.reflect.Field;

/** Conservative region explosion guard. Avoids changing world data or entities when denied.
 * This mixin requires runtime testing on the target Minecraft version.
 */
@Mixin(targets = "net.minecraft.world.level.ServerExplosion")
public abstract class ServerExplosionMixin {
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true, require = 0)
    private void abyssguard$protectExplosions(CallbackInfo ci) {
        try {
            ServerLevel level = null;
            Vec3 center = null;
            String sourceName = "";
            for (Class<?> type = this.getClass(); type != null; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    if (value instanceof ServerLevel l) level = l;
                    if (value instanceof Vec3 v) center = v;
                    if (value instanceof net.minecraft.world.entity.Entity entity) {
                        sourceName = entity.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
                    }
                }
            }
            if (level == null || center == null) return;
            Flag flag = sourceName.contains("tnt") ? Flag.TNT
                    : sourceName.contains("creeper") ? Flag.CREEPER_EXPLOSION
                    : Flag.OTHER_EXPLOSION;
            String dim = level.dimension().identifier().toString();
            BlockPos origin = BlockPos.containing(center);
            // An explosion near a protected region must not destroy blocks inside it.
            for (int x = -8; x <= 8; x += 2) {
                for (int y = -8; y <= 8; y += 2) {
                    for (int z = -8; z <= 8; z += 2) {
                        if (!AbyssGuard.regionAllows(dim, origin.offset(x,y,z), flag)) {
                            ci.cancel();
                            return;
                        }
                    }
                }
            }
            if (!AbyssGuard.regionAllows(dim, origin, flag)) ci.cancel();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Do not crash the server if a Minecraft update changes explosion internals.
        }
    }
}
