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

	// --- Bacteria spawning (the values most likely to need tuning) ---
	public boolean bacteriaEnabled = true;
	/** One spawn attempt per player happens every N ticks (200 = 10s). */
	public int bacteriaSpawnIntervalTicks = 200;
	/** Probability an attempt actually produces a Bacteria. */
	public double bacteriaSpawnChance = 0.10D;
	/** At most this many Bacteria near a player. */
	public int bacteriaMaxNearPlayer = 2;
	/** Players won't see one appear closer than this. */
	public int bacteriaMinSpawnDistance = 22;
	/** Spawn attempts search up to this far away. */
	public int bacteriaMaxSpawnDistance = 46;
	/** Ignore peaceful / gamerule-disabled spawning? (always respected anyway) */

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
		bacteriaEnabled = Boolean.parseBoolean(props.getProperty("bacteriaEnabled", String.valueOf(bacteriaEnabled)));
		bacteriaSpawnIntervalTicks = Integer.parseInt(
				props.getProperty("bacteriaSpawnIntervalTicks", String.valueOf(bacteriaSpawnIntervalTicks)));
		bacteriaSpawnChance = Double.parseDouble(
				props.getProperty("bacteriaSpawnChance", String.valueOf(bacteriaSpawnChance)));
		bacteriaMaxNearPlayer = Integer.parseInt(
				props.getProperty("bacteriaMaxNearPlayer", String.valueOf(bacteriaMaxNearPlayer)));
		bacteriaMinSpawnDistance = Integer.parseInt(
				props.getProperty("bacteriaMinSpawnDistance", String.valueOf(bacteriaMinSpawnDistance)));
		bacteriaMaxSpawnDistance = Integer.parseInt(
				props.getProperty("bacteriaMaxSpawnDistance", String.valueOf(bacteriaMaxSpawnDistance)));
		normalSandOnly = Boolean.parseBoolean(props.getProperty("normalSandOnly", String.valueOf(normalSandOnly)));
		save();
	}

	public void save() {
		Properties props = new Properties();
		props.setProperty("bacteriaEnabled", String.valueOf(bacteriaEnabled));
		props.setProperty("bacteriaSpawnIntervalTicks", String.valueOf(bacteriaSpawnIntervalTicks));
		props.setProperty("bacteriaSpawnChance", String.valueOf(bacteriaSpawnChance));
		props.setProperty("bacteriaMaxNearPlayer", String.valueOf(bacteriaMaxNearPlayer));
		props.setProperty("bacteriaMinSpawnDistance", String.valueOf(bacteriaMinSpawnDistance));
		props.setProperty("bacteriaMaxSpawnDistance", String.valueOf(bacteriaMaxSpawnDistance));
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
