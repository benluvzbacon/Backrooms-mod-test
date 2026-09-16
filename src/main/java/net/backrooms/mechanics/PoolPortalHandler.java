package net.backrooms.mechanics;

import net.backrooms.ModBlocks;
import net.backrooms.ModWorldgen;
import net.backrooms.worldgen.Level0Layout;
import net.backrooms.worldgen.PoolroomsLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
 * Moves players between Level 0 and the Poolrooms when they dive into a sunken
 * portal basin and submerge. Basins are 3x3 tiled pools with a glowing lantern
 * floor and a shimmering drain at the centre (placed by the chunk generators);
 * in Level 0 a tiled deck rings the pool.
 *
 * <p>There is no walk-through trigger: the transfer fires only while the
 * player's head is underwater inside a basin. Duck under the dark water and
 * you are pulled through with a splash of bubbles. A short per-player cooldown
 * prevents immediate bounce-back.</p>
 */
public final class PoolPortalHandler {
	private static final long COOLDOWN_TICKS = 80L;
	private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

	private PoolPortalHandler() {
	}

	/** Scans both Backrooms levels for divers a few times a second. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 5L != 0L) {
			return;
		}
		ServerLevel level0 = server.getLevel(ModWorldgen.BACKROOMS_LEVEL);
		if (level0 != null) {
			Level0Layout layout = new Level0Layout(net.backrooms.worldgen.LevelSeeds.of(level0));
			for (ServerPlayer player : level0.players()) {
				if (isDiving(player, layout)) {
					onPortalTouch(player);
				}
			}
		}
		ServerLevel poolrooms = server.getLevel(ModWorldgen.POOLROOMS_LEVEL);
		if (poolrooms != null) {
			PoolroomsLayout layout = new PoolroomsLayout(net.backrooms.worldgen.LevelSeeds.of(poolrooms));
			for (ServerPlayer player : poolrooms.players()) {
				if (isDiving(player, layout)) {
					onPortalTouch(player);
				}
			}
		}
	}

	/** Head underwater with feet or eyes over a Level 0 basin column. */
	private static boolean isDiving(ServerPlayer player, Level0Layout layout) {
		if (player.isSpectator() || !player.isUnderWater()) {
			return false;
		}
		BlockPos feet = player.blockPosition();
		BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
		return layout.poolPadRoleAt(feet.getX(), feet.getZ()) != 0
				|| layout.poolPadRoleAt(eyes.getX(), eyes.getZ()) != 0;
	}

	/** Head underwater with feet or eyes over a Poolrooms basin column. */
	private static boolean isDiving(ServerPlayer player, PoolroomsLayout layout) {
		if (player.isSpectator() || !player.isUnderWater()) {
			return false;
		}
		BlockPos feet = player.blockPosition();
		BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
		return layout.exitPadRoleAt(feet.getX(), feet.getZ()) != 0
				|| layout.exitPadRoleAt(eyes.getX(), eyes.getZ()) != 0;
	}

	/** Called when a diver submerges in a basin (or touches a drain block). */
	public static void onPortalTouch(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		Long until = COOLDOWN_UNTIL.get(player.getUUID());
		if (until != null && server.getTickCount() < until) {
			return;
		}
		// The pull-through: a burst of bubbles, a splash, and a fresh breath.
		ServerLevel origin = (ServerLevel) player.level();
		BlockPos at = player.blockPosition();
		origin.sendParticles(ParticleTypes.BUBBLE, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5,
				40, 0.4, 0.7, 0.4, 0.08);
		origin.sendParticles(ParticleTypes.SPLASH, at.getX() + 0.5, at.getY() + 1.2, at.getZ() + 0.5,
				20, 0.4, 0.3, 0.4, 0.1);
		origin.playSound(null, at, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0F, 0.9F);
		player.setAirSupply(player.getMaxAirSupply());
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
		destination.playSound(null, standingOn, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.8F, 1.0F);
		destination.sendParticles(ParticleTypes.SPLASH, standingOn.getX() + 0.5, standingOn.getY() + 1.2,
				standingOn.getZ() + 0.5, 24, 0.4, 0.3, 0.4, 0.1);
	}

	/** Finds dry tiled floor near the origin of the Poolrooms. */
	public static BlockPos findPoolroomsSpawn(ServerLevel level) {
		PoolroomsLayout layout = new PoolroomsLayout(net.backrooms.worldgen.LevelSeeds.of(level));
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
