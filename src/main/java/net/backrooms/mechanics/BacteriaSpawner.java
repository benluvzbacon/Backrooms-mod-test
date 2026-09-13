package net.backrooms.mechanics;

import net.backrooms.BackroomsConfig;
import net.backrooms.ModEntities;
import net.backrooms.ModWorldgen;
import net.backrooms.entity.BacteriaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Deliberately lightweight natural spawner for the Bacteria.
 *
 * <p>Vanilla monster spawning only happens in darkness, but Level 0 is mostly
 * brightly lit - so we run sparse, fully configurable per-player spawn
 * attempts. At most {@code bacteriaMaxNearPlayer} Bacteria may be near a
 * player, which caps entity counts for performance and keeps encounters rare.
 * Entities remain despawnable (vanilla hard-despawn rules apply).</p>
 */
public final class BacteriaSpawner {
	private BacteriaSpawner() {
	}

	public static void tick(MinecraftServer server) {
		BackroomsConfig cfg = BackroomsConfig.INSTANCE;
		if (!cfg.bacteriaEnabled) {
			return;
		}
		if (server.getTickCount() % Math.max(20, cfg.bacteriaSpawnIntervalTicks) != 0) {
			return;
		}
		ServerLevel level = server.getLevel(ModWorldgen.BACKROOMS_LEVEL);
		if (level == null || level.getDifficulty() == Difficulty.PEACEFUL) {
			return;
		}
		if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			if (level.random.nextDouble() >= cfg.bacteriaSpawnChance) {
				continue;
			}
			int count = level.getEntitiesOfClass(BacteriaEntity.class,
					player.getBoundingBox().inflate(64.0)).size();
			if (count >= cfg.bacteriaMaxNearPlayer) {
				continue;
			}
			trySpawn(level, player, cfg);
		}
	}

	private static void trySpawn(ServerLevel level, ServerPlayer player, BackroomsConfig cfg) {
		double min = cfg.bacteriaMinSpawnDistance;
		double max = cfg.bacteriaMaxSpawnDistance;
		for (int attempt = 0; attempt < 10; attempt++) {
			double angle = level.random.nextDouble() * Math.PI * 2.0;
			double dist = min + level.random.nextDouble() * (max - min);
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
			int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
			BlockPos ground = findGround(level, x, z);
			if (ground == null) {
				continue;
			}
			// Prefer gloom; occasionally spawn in a bright hall to keep you paranoid.
			int light = level.getMaxLocalRawBrightness(ground.above());
			if (light > 4 && level.random.nextFloat() < 0.7F) {
				continue;
			}
			BacteriaEntity bacteria = ModEntities.BACTERIA.create(level);
			if (bacteria == null) {
				return;
			}
			bacteria.moveTo(ground.getX() + 0.5, ground.getY() + 1.02, ground.getZ() + 0.5,
					level.random.nextFloat() * 360.0F, 0.0F);
			bacteria.finalizeSpawn(level, level.getCurrentDifficultyAt(ground.above()),
					MobSpawnType.NATURAL, null);
			if (level.addFreshEntity(bacteria)) {
				return;
			}
		}
	}

	/** Searches a short vertical range for a solid floor with two air blocks above. */
	private static BlockPos findGround(Level level, int x, int z) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int y = 72; y >= 58; y--) {
			cursor.set(x, y, z);
			boolean feetAir = level.getBlockState(cursor).isAir()
					&& level.getBlockState(cursor.above()).isAir();
			boolean ground = !level.getBlockState(cursor.below())
					.getCollisionShape(level, cursor.below()).isEmpty();
			if (feetAir && ground) {
				return cursor.below().immutable();
			}
		}
		return null;
	}
}
