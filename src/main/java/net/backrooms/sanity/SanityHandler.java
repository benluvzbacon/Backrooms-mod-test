package net.backrooms.sanity;

import net.backrooms.ModWorldgen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Server-authoritative Sanity.
 *
 * <p>It slowly drains in Level 0 (faster in the dark or with a Still Life
 * nearby), regenerates in the Overworld and - more gently - in the calm
 * Poolrooms, and low sanity applies creepier status effects. Almond Water
 * restores it. The readout is sent to the action bar once per second so no
 * client mod code is required.</p>
 */
public final class SanityHandler {
	public static final float MAX = 1.0F;

	private static final float OVERWORLD_REGEN = 0.00020F;
	private static final float LEVEL0_DRAIN = 0.000060F;
	/** The Poolrooms are calm water and light - sanity slowly comes back. */
	private static final float POOLROOMS_REGEN = 0.00016F;
	private static final float DARK_MULTIPLIER = 2.0F;
	private static final float STILL_LIFE_MULTIPLIER = 3.5F;
	private static final float STILL_LIFE_RANGE = 18.0F;

	private SanityHandler() {
	}

	/** Per-tick sanity recovery inside the Poolrooms (test hook). */
	public static float poolroomsRegenPerTick() {
		return POOLROOMS_REGEN;
	}

	public static float get(ServerPlayer player) {
		return player.getAttachedOrElse(ModAttachments.SANITY, MAX);
	}

	public static void set(ServerPlayer player, float value) {
		player.setAttached(ModAttachments.SANITY, Math.max(0.0F, Math.min(MAX, value)));
	}

	/** Restores the given amount and returns the new sanity value. */
	public static float restore(ServerPlayer player, float amount) {
		float newValue = Math.min(MAX, get(player) + amount);
		set(player, newValue);
		return newValue;
	}

	public static void tick(MinecraftServer server) {
		boolean effectsTick = server.getTickCount() % 60L == 0L;
		boolean hudTick = server.getTickCount() % 20L == 0L;
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				if (player.isSpectator()) {
					continue;
				}
				float sanity = get(player);
				if (level.dimension().equals(ModWorldgen.POOLROOMS_LEVEL)) {
					// Safe, serene water rooms: sanity recovers and symptoms fade.
					if (sanity < MAX) {
						sanity = Math.min(MAX, sanity + POOLROOMS_REGEN);
						set(player, sanity);
					}
					if (effectsTick) {
						applyEffects(player, sanity);
					}
					if (hudTick) {
						sendHud(player, sanity, false);
					}
				} else if (level.dimension().equals(ModWorldgen.BACKROOMS_LEVEL)) {
					float drain = LEVEL0_DRAIN;
					int light = level.getMaxLocalRawBrightness(player.blockPosition());
					if (light <= 4) {
						drain *= DARK_MULTIPLIER;
					}
					boolean stillLifeNearby = !level.getEntitiesOfClass(
							net.backrooms.entity.StillLifeEntity.class,
							player.getBoundingBox().inflate(STILL_LIFE_RANGE)).isEmpty();
					if (stillLifeNearby) {
						drain *= STILL_LIFE_MULTIPLIER;
					}
					sanity -= drain;
					set(player, sanity);

					if (effectsTick) {
						applyEffects(player, sanity);
					}
					if (hudTick) {
						sendHud(player, sanity, stillLifeNearby);
					}
				} else {
					// Recover outside the Backrooms.
					if (sanity < MAX) {
						set(player, sanity + OVERWORLD_REGEN * 20.0F);
					}
					if (effectsTick) {
						clearEffects(player);
					}
				}
			}
		}
	}

	private static void applyEffects(ServerPlayer player, float sanity) {
		clearEffects(player);
		if (sanity < 0.33F) {
			// Darkness pulses around the edges of vision.
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false, false));
		}
		if (sanity < 0.18F) {
			player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, false, false));
		}
	}

	private static void clearEffects(ServerPlayer player) {
		player.removeEffect(MobEffects.DARKNESS);
		player.removeEffect(MobEffects.CONFUSION);
		player.removeEffect(MobEffects.WEAKNESS);
		player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
	}

	private static void sendHud(ServerPlayer player, float sanity, boolean stillLifeNearby) {
		int percent = Math.round(sanity * 100.0F);
		ChatFormatting color = sanity > 0.66F ? ChatFormatting.GREEN
				: sanity > 0.33F ? ChatFormatting.YELLOW : ChatFormatting.RED;
		Component bar = Component.literal("Sanity: " + percent + "%").withStyle(color);
		if (stillLifeNearby) {
			bar = bar.copy().append(Component.literal("   DON'T BLINK").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		}
		player.displayClientMessage(bar, true);
	}
}
