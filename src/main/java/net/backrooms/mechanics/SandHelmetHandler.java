package net.backrooms.mechanics;

import net.backrooms.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The entry mechanic. Once ordinary sand sits in the player's helmet slot the
 * player is moved into the Backrooms - exactly once, because the guard is the
 * dimension itself, not a ticking flag.
 *
 * <p>Sand cannot normally be equipped in survival, so sneaking + right
 * clicking with sand in hand also places one block on the head.</p>
 */
public final class SandHelmetHandler {
	private SandHelmetHandler() {
	}

	public static boolean isPortalSand(ItemStack stack) {
		return stack.is(Items.SAND);
	}

	/** Called every server tick; performs the teleport when sand is equipped. */
	public static void tick(MinecraftServer server) {
		ServerLevel target = server.getLevel(ModWorldgen.BACKROOMS_LEVEL);
		if (target == null) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.level().dimension() == ModWorldgen.BACKROOMS_LEVEL) {
				continue; // already inside - sand on head does nothing here
			}
			ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
			if (isPortalSand(head)) {
				enter(player, target);
			}
		}
	}

	public static void enter(ServerPlayer player, ServerLevel target) {
		BlockPos spawn = findSpawn(target);
		target.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
		player.teleportTo(
				target,
				spawn.getX() + 0.5,
				spawn.getY() + 1.05,
				spawn.getZ() + 0.5,
				player.getYRot(),
				player.getXRot());
		player.setDeltaMovement(0, 0, 0);
		player.fallDistance = 0.0F;

		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
		player.connection.send(new ClientboundSetSubtitleTextPacket(
				Component.translatable("message.backrooms.enter.subtitle")
						.withStyle(ChatFormatting.DARK_GRAY)));
		player.connection.send(new ClientboundSetTitleTextPacket(
				Component.translatable("message.backrooms.enter")
						.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
		target.playSound(null, spawn, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 0.55F);
	}

	/**
	 * Finds a safe arrival point near the global spawn (centre of an
	 * intersection). Deterministic per player so arrivals spread out slightly.
	 */
	private static BlockPos findSpawn(ServerLevel level) {
		int[][] offsets = {{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}, {8, 0}, {-8, 0}, {0, 8}, {0, -8}};
		for (int[] o : offsets) {
			BlockPos carpet = BlockPos.containing(o[0], net.backrooms.worldgen.Level0Layout.FLOOR_Y, o[1]);
			BlockPos feet = carpet.above();
			BlockPos head = feet.above();
			if (level.getBlockState(carpet).is(net.backrooms.ModBlocks.CARPET)
					&& level.getBlockState(feet).isAir()
					&& level.getBlockState(head).isAir()) {
				return carpet;
			}
		}
		return BlockPos.containing(0, net.backrooms.worldgen.Level0Layout.FLOOR_Y, 0);
	}

	/** Sneak + right-click with sand in hand puts the sand on the player's head. */
	public static InteractionResultHolder<ItemStack> onUseItem(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!player.isShiftKeyDown() || !isPortalSand(stack) || isPortalSand(player.getItemBySlot(EquipmentSlot.HEAD))) {
			return InteractionResultHolder.pass(stack);
		}
		if (level.isClientSide()) {
			return InteractionResultHolder.sidedSuccess(stack, false);
		}
		ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
		ItemStack toEquip = player.getAbilities().instabuild ? stack.copyWithCount(1) : stack.split(1);
		player.setItemSlot(EquipmentSlot.HEAD, toEquip);
		if (!head.isEmpty()) {
			player.getInventory().placeItemBackInInventory(head);
		}
		player.displayClientMessage(Component.translatable("message.backrooms.equipped"), true);
		return InteractionResultHolder.sidedSuccess(stack, false);
	}
}
