package net.backrooms.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Atmosphere for the Backrooms: no sky/clouds/weather (a no-op sky renderer is
 * registered separately), yellow-tinged fog light.
 */
public class BackroomsDimensionEffects extends DimensionSpecialEffects {
	public BackroomsDimensionEffects() {
		super(Float.NaN, false, DimensionSpecialEffects.SkyType.NONE, true, false);
	}

	@Override
	public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
		float b = clamp(brightness, 0.12F, 1.0F);
		return new Vec3(0.76D * b, 0.68D * b, 0.30D * b);
	}

	@Override
	public boolean isFoggyAt(int x, int z) {
		return false;
	}

	private static float clamp(float v, float lo, float hi) {
		return Math.max(lo, Math.min(hi, v));
	}
}
