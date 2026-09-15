package net.backrooms.mechanics;

import net.backrooms.ModWorldgen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The entry mechanic. Once ordinary sand sits in the player's helmet slot the
 * player is moved into the Backrooms - exactly once, because the guard is the
 * dimension itself, not a ticking flag.
 *
 * <p>Vanilla inventories refuse to place a sand block in the helmet slot and
 * right-clicking sand normally places the block, so three reliable routes are
 * supported:</p>
 * <ul>
 *   <li>sand placed in the helmet slot by any means (creative menu, /item) -
 *       the tick watcher teleports immediately;</li>
 *   <li><b>right-click while looking at the sky / air</b> with sand in hand
 *       (vanilla air-use of sand does nothing, so this is free to hijack);</li>
 *   <li><b>sneak + right-click while looking at a block</b> (intercepted before
 *       the sand can be placed).</li>
 * </ul>
 */
public final class SandHelmetHandler {
	private SandHelmetHandler() {
	}

	public static boolean isPortalSand(ItemStack stack) {
		return stack.is(Items.SAND);
	}

	/**
	 * Sand-on-head entry only fires from OUTSIDE the Backrooms (e.g. the
	 * Overworld). It must never fire from Level 0 OR another Backrooms level
	 * such as the Poolrooms - otherwise stepping through a Pool Portal with
	 * sand still on the head (always the case in creative, where it isn't
	 * consumed) would yank the player straight back to Level 0.
	 */
	public static boolean isOutsideBackrooms(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
		return !dimension.equals(ModWorldgen.BACKROOMS_LEVEL)
				&& !dimension.equals(ModWorldgen.POOLROOMS_LEVEL);
	}

	/** Called every server tick; performs the teleport when sand is equipped. */
	public static void tick(MinecraftServer server) {
		ServerLevel target = server.getLevel(ModWorldgen.BACKROOMS_LEVEL);
		if (target == null) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isOutsideBackrooms(player.level().dimension())) {
				continue; // already inside a Backrooms level - sand does nothing
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

		// Consume one sand block on the way in (creative keeps it). This also
		// prevents a death/respawn loop: without sand on their head, players
		// who die stay in the Overworld after respawning. Done before the
		// dimension transfer so the inventory is copied across.
		if (!player.getAbilities().instabuild) {
			ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
			if (isPortalSand(head)) {
				head.shrink(1);
				if (head.isEmpty()) {
					player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
				}
			}
		}

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
	public static BlockPos findSpawn(ServerLevel level) {
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

	// ----------------------------------------------------------- equip routes

	/**
	 * Right-click in the air with sand: equips it. Sneaking is not required
	 * because vanilla air-use of a sand block does nothing.
	 */
	public static InteractionResultHolder<ItemStack> onUseItem(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!isPortalSand(stack) || isPortalSand(player.getItemBySlot(EquipmentSlot.HEAD))) {
			return InteractionResultHolder.pass(stack);
		}
		if (level.isClientSide()) {
			return InteractionResultHolder.success(stack);
		}
		return equipToHead(player, hand)
				? InteractionResultHolder.success(stack)
				: InteractionResultHolder.pass(stack);
	}

	/**
	 * Sneak + right-click on a block with sand: equips it instead of placing
	 * the block. Non-sneaking placement is left completely untouched.
	 */
	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		return useBlock(player, level, hand, player.isShiftKeyDown());
	}

	/** Test/override hook with an explicit sneak flag (the headless test has no input). */
	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, boolean sneaking) {
		ItemStack stack = player.getItemInHand(hand);
		if (!sneaking || !isPortalSand(stack) || isPortalSand(player.getItemBySlot(EquipmentSlot.HEAD))) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			// Cancel block placement on the client; it still informs the server.
			return InteractionResult.SUCCESS;
		}
		return equipToHead(player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	/** Moves one sand block from the given hand onto the player's head. */
	public static boolean equipToHead(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!isPortalSand(stack) || isPortalSand(player.getItemBySlot(EquipmentSlot.HEAD))) {
			return false;
		}
		ItemStack previousHelmet = player.getItemBySlot(EquipmentSlot.HEAD);
		ItemStack toEquip = player.getAbilities().instabuild ? stack.copyWithCount(1) : stack.split(1);
		if (toEquip.isEmpty()) {
			return false;
		}
		player.setItemSlot(EquipmentSlot.HEAD, toEquip);
		if (!previousHelmet.isEmpty()) {
			player.getInventory().placeItemBackInInventory(previousHelmet);
		}
		player.displayClientMessage(Component.translatable("message.backrooms.equipped"), true);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7F, 0.85F);
		return true;
	}
}
