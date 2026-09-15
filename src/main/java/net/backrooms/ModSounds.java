package net.backrooms;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent HUM = register("ambient.hum");
	public static final SoundEvent STILL_LIFE_AMBIENT = register("still_life.ambient");
	public static final SoundEvent STILL_LIFE_HURT = register("still_life.hurt");
	public static final SoundEvent STILL_LIFE_DEATH = register("still_life.death");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void initialize() {
		// Class loading triggers registration.
	}
}
