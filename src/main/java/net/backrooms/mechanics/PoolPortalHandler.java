package net.backrooms.mechanics;

import net.backrooms.ModBlocks;
import net.backrooms.ModWorldgen;
import net.backrooms.worldgen.PoolroomsLayout;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Moves players between Level 0 and the Poolrooms when they step into a pool
 * portal block. Portals exist as rare 3x3 lamp-ringed pads in both dimensions
 * (placed by the chunk generators). A short per-player cooldown prevents
 * immediate bounce-back.
 */
public final class PoolPortalHandler {
	private static final long COOLDOWN_TICKS = 80L;
	private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

	private PoolPortalHandler() {
	}

	/** Polled fallback (the block's entityInside hook is the primary trigger). */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10L != 0L) {
			return;
		}
		ServerLevel level0 = server.getLevel(ModWorldgen.BACKROOMS_LEVEL);
		ServerLevel poolrooms = server.getLevel(ModWorldgen.POOLROOMS_LEVEL);
		if (level0 != null) {
			for (ServerPlayer player : level0.players()) {
				if (player.level().getBlockState(player.blockPosition()).is(ModBlocks.POOL_PORTAL)) {
					onPortalTouch(player);
				}
			}
		}
		if (poolrooms != null) {
			for (ServerPlayer player : poolrooms.players()) {
				if (player.level().getBlockState(player.blockPosition()).is(ModBlocks.POOL_PORTAL)) {
					onPortalTouch(player);
				}
			}
		}
	}

	/** Called when a player intersects a portal block (or the poll above finds one). */
	public static void onPortalTouch(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		Long until = COOLDOWN_UNTIL.get(player.getUUID());
		if (until != null && server.getTickCount() < until) {
			return;
		}
		if (player.level().dimension().equals(ModWorldgen.BACKROOMS_LEVEL)) {
			ServerLevel pool = server.getLevel(ModWorldgen.POOLROOMS_LEVEL);
			if (pool != null) {
				transfer(player, pool, findPoolroomsSpawn(pool),
						"message.backrooms.enter.poolrooms",
						"message.backrooms.enter.poolrooms.subtitle", ChatFormatting.AQUA);
			}
		} else if (player.level().dimension().equals(ModWorldgen.POOLROOMS_LEVEL)) {
			ServerLevel level0 = server.getLevel(ModWorldgen.BACKROOMS_LEVEL);
			if (level0 != null) {
				transfer(player, level0, SandHelmetHandler.findSpawn(level0),
						"message.backrooms.enter",
						"message.backrooms.enter.return.subtitle", ChatFormatting.YELLOW);
			}
		}
	}

	private static void transfer(ServerPlayer player, ServerLevel destination, BlockPos standingOn,
			String titleKey, String subtitleKey, ChatFormatting titleColor) {
		destination.getChunk(standingOn.getX() >> 4, standingOn.getZ() >> 4);
		COOLDOWN_UNTIL.put(player.getUUID(), player.getServer().getTickCount() + COOLDOWN_TICKS);

		player.teleportTo(
				destination,
				standingOn.getX() + 0.5,
				standingOn.getY() + 1.05,
				standingOn.getZ() + 0.5,
				player.getYRot(),
				player.getXRot());
		player.setDeltaMovement(0, 0, 0);
		player.fallDistance = 0.0F;

		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
		player.connection.send(new ClientboundSetSubtitleTextPacket(
				Component.translatable(subtitleKey).withStyle(ChatFormatting.GRAY)));
		player.connection.send(new ClientboundSetTitleTextPacket(
				Component.translatable(titleKey).withStyle(titleColor, ChatFormatting.BOLD)));
		destination.playSound(null, standingOn, SoundEvents.CONDUIT_ACTIVATE, SoundSource.HOSTILE, 1.0F, 0.7F);
	}

	/** Finds dry tiled floor near the origin of the Poolrooms. */
	public static BlockPos findPoolroomsSpawn(ServerLevel level) {
		PoolroomsLayout layout = new PoolroomsLayout(level.getSeed());
		for (int radius = 0; radius <= 48; radius++) {
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dx = -radius; dx <= radius; dx++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue; // ring only
					}
					int x = dx * 3;
					int z = dz * 3;
					if (!layout.isDryFloor(x, z)) {
						continue;
					}
					BlockPos floor = BlockPos.containing(x, PoolroomsLayout.FLOOR_Y, z);
					level.getChunk(floor.getX() >> 4, floor.getZ() >> 4);
					if (level.getBlockState(floor).is(ModBlocks.POOL_TILE)
							&& level.getBlockState(floor.above()).isAir()
							&& level.getBlockState(floor.above(2)).isAir()) {
						return floor;
					}
				}
			}
		}
		return BlockPos.containing(0, PoolroomsLayout.FLOOR_Y, 0);
	}

	/** Test/utility hook: clears the portal cooldown map (used by the self-test). */
	public static void clearCooldowns() {
		COOLDOWN_UNTIL.clear();
	}
}
