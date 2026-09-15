package net.backrooms.item;

import net.backrooms.sanity.SanityHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Almond Water - the only safe drink in the Backrooms. Restores Sanity and
 * grants a brief burst of regeneration. Bottles are returned like potions.
 * Found in supply chests scattered through Level 0.
 */
public class AlmondWaterItem extends Item {
	public static final int DRINK_DURATION = 32;
	public static final float SANITY_RESTORED = 0.6F;

	public AlmondWaterItem(Properties properties) {
		super(properties);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return DRINK_DURATION;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		SoundEvent sound = SoundEvents.GENERIC_DRINK;
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound,
				SoundSource.PLAYERS, 0.8F, 1.0F);

		if (!level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
			SanityHandler.restore(serverPlayer, SANITY_RESTORED);
			serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.REGENERATION, 160, 0, false, true, true));
		}

		if (entity instanceof Player player && player.getAbilities().instabuild) {
			return stack;
		}
		stack.shrink(1);
		return stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack;
	}
}
