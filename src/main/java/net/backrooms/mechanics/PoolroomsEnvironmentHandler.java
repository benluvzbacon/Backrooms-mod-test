package net.backrooms.mechanics;

import net.backrooms.ModWorldgen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Poolrooms run their own fast day/night cycle: a full cycle every 6000
 * ticks (5 minutes), i.e. the clock advances 4 time units per tick. Day lasts
 * the first half of the cycle, night the second half.
 *
 * <p>Vanilla gives every non-overworld dimension a {@code DerivedLevelData}
 * whose {@code setDayTime} is a no-op and whose {@code getDayTime} mirrors the
 * Overworld, so the cycle phase is owned here and surfaced to the game clock
 * by {@code LevelMixin} (server) and {@code ClientLevelMixin} (smooth sun
 * movement on the client). During the day the pool water heats up and scalds
 * anyone swimming in it; at night the water is calm and safe.</p>
 */
public final class PoolroomsEnvironmentHandler {
	/** Clock time units advanced per tick. */
	public static final long TIME_PER_TICK = 4L;
	/** 24000 vanilla time units take this many ticks: 24000/4 = 6000 = 5 min. */
	public static final long FULL_CYCLE_TICKS = 24000L / TIME_PER_TICK;

	private static Long phase;

	private PoolroomsEnvironmentHandler() {
	}

	public static void tick(MinecraftServer server) {
		ServerLevel pool = server.getLevel(ModWorldgen.POOLROOMS_LEVEL);
		if (pool == null) {
			return;
		}
		if (phase == null) {
			// Join the cycle wherever the shared clock currently is.
			phase = Math.floorMod(pool.getDayTime(), 24000L);
		}
		phase = (phase + TIME_PER_TICK) % 24000L;

		if (server.getTickCount() % 20L != 0L) {
			return;
		}
		boolean hot = isHotDaytime(phase);
		for (ServerPlayer player : pool.players()) {
			if (player.isSpectator()) {
				continue;
			}
			// isInWater follows the fluid at the player's feet/eyes.
			if (hot && player.isInWater()) {
				player.hurt(pool.damageSources().inFire(), 2.0F);
				player.displayClientMessage(
						Component.translatable("message.backrooms.hot_water")
								.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
						true);
			}
		}
	}

	/** Current Poolrooms time-of-day (0..23999), or null before first tick. */
	public static Long currentPhase() {
		return phase;
	}

	/** Test hook: pins the cycle at a given time-of-day. */
	public static void resetPhaseForTest(long timeOfDay) {
		phase = Math.floorMod(timeOfDay, 24000L);
	}

	/** Daytime occupies the first half of the fast cycle (time-of-day 0..11999). */
	public static boolean isHotDaytime(long dayTime) {
		long t = Math.floorMod(dayTime, 24000L);
		return t < 12000L;
	}
}
