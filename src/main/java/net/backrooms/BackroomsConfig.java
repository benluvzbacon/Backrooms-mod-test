package net.backrooms;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Lightweight file-backed configuration. Values are intentionally simple so a
 * future Forge/NeoForge port or a Cloth Config screen can replace the storage
 * without touching game logic.
 */
public final class BackroomsConfig {
	public static final BackroomsConfig INSTANCE = new BackroomsConfig();

	// --- Still Life spawning (the values most likely to need tuning) ---
	public boolean stillLifeEnabled = true;
	/** One spawn attempt per player happens every N ticks (200 = 10s). */
	public int stillLifeSpawnIntervalTicks = 200;
	/** Probability an attempt actually produces a Still Life. */
	public double stillLifeSpawnChance = 0.10D;
	/** At most this many Still Lives near a player. */
	public int stillLifeMaxNearPlayer = 2;
	/** Players won't see one appear closer than this. */
	public int stillLifeMinSpawnDistance = 22;
	/** Spawn attempts search up to this far away. */
	public int stillLifeMaxSpawnDistance = 46;

	// --- Sand helmet entry ---
	/** Set to false to also accept red sand as a portal trigger. */
	public boolean normalSandOnly = true;

	private BackroomsConfig() {
	}

	private Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("backrooms.properties");
	}

	public void load() {
		Path path = file();
		Properties props = new Properties();
		if (Files.exists(path)) {
			try (InputStream in = Files.newInputStream(path)) {
				props.load(in);
			} catch (IOException e) {
				Backrooms.LOGGER.warn("[Backrooms] failed to read config, using defaults", e);
			}
		}
		// Accept the old bacteria* keys from pre-1.0.5 configs as fallbacks.
		stillLifeEnabled = Boolean.parseBoolean(props.getProperty("stillLifeEnabled",
				props.getProperty("bacteriaEnabled", String.valueOf(stillLifeEnabled)))));
		stillLifeSpawnIntervalTicks = Integer.parseInt(props.getProperty("stillLifeSpawnIntervalTicks",
				props.getProperty("bacteriaSpawnIntervalTicks", String.valueOf(stillLifeSpawnIntervalTicks))));
		stillLifeSpawnChance = Double.parseDouble(props.getProperty("stillLifeSpawnChance",
				props.getProperty("bacteriaSpawnChance", String.valueOf(stillLifeSpawnChance))));
		stillLifeMaxNearPlayer = Integer.parseInt(props.getProperty("stillLifeMaxNearPlayer",
				props.getProperty("bacteriaMaxNearPlayer", String.valueOf(stillLifeMaxNearPlayer))));
		stillLifeMinSpawnDistance = Integer.parseInt(props.getProperty("stillLifeMinSpawnDistance",
				props.getProperty("bacteriaMinSpawnDistance", String.valueOf(stillLifeMinSpawnDistance))));
		stillLifeMaxSpawnDistance = Integer.parseInt(props.getProperty("stillLifeMaxSpawnDistance",
				props.getProperty("bacteriaMaxSpawnDistance", String.valueOf(stillLifeMaxSpawnDistance))));
		normalSandOnly = Boolean.parseBoolean(props.getProperty("normalSandOnly", String.valueOf(normalSandOnly)));
		save();
	}

	public void save() {
		Properties props = new Properties();
		props.setProperty("stillLifeEnabled", String.valueOf(stillLifeEnabled));
		props.setProperty("stillLifeSpawnIntervalTicks", String.valueOf(stillLifeSpawnIntervalTicks));
		props.setProperty("stillLifeSpawnChance", String.valueOf(stillLifeSpawnChance));
		props.setProperty("stillLifeMaxNearPlayer", String.valueOf(stillLifeMaxNearPlayer));
		props.setProperty("stillLifeMinSpawnDistance", String.valueOf(stillLifeMinSpawnDistance));
		props.setProperty("stillLifeMaxSpawnDistance", String.valueOf(stillLifeMaxSpawnDistance));
		props.setProperty("normalSandOnly", String.valueOf(normalSandOnly));
		try {
			Files.createDirectories(file().getParent());
			try (OutputStream out = Files.newOutputStream(file())) {
				props.store(out, "The Backrooms mod configuration");
			}
		} catch (IOException e) {
			Backrooms.LOGGER.warn("[Backrooms] failed to write config", e);
		}
	}
}
