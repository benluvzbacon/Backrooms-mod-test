package net.backrooms;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent HUM = register("ambient.hum");
	public static final SoundEvent BACTERIA_AMBIENT = register("bacteria.ambient");
	public static final SoundEvent BACTERIA_HURT = register("bacteria.hurt");
	public static final SoundEvent BACTERIA_DEATH = register("bacteria.death");

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
