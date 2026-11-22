package ro.abyssmc.abyssguard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ro.abyssmc.abyssguard.AbyssGuard;
import ro.abyssmc.abyssguard.Flag;

/** Catch bucket use on air and on blocks, including empty buckets. */
@Mixin(net.minecraft.world.item.BucketItem.class)
public abstract class BucketItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void abyssguard$bucketProtection(Level level, Player player, InteractionHand hand,
                                                CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer)) return;
        HitResult hit = player.pick(5.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos clicked = blockHit.getBlockPos();
        BlockPos adjacent = clicked.relative(blockHit.getDirection());
        boolean emptyBucket = player.getItemInHand(hand).is(Items.BUCKET);
        Flag flag = emptyBucket ? Flag.BLOCK_BREAK : Flag.BLOCK_PLACE;
        if (!AbyssGuard.canModify(player, clicked, flag) ||
            !AbyssGuard.canModify(player, adjacent, flag)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
