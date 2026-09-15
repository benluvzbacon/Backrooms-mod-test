package net.backrooms.mixin;

import net.backrooms.ModWorldgen;
import net.backrooms.mechanics.PoolroomsEnvironmentHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Surfaces the Poolrooms' independent fast clock ({@link PoolroomsEnvironmentHandler})
 * through the normal level time, so sky brightness and the per-dimension time
 * synchronisation packet use the 5-minute cycle. The client, whose clock runs
 * locally, is handled by {@code ClientLevelMixin}.
 */
@Mixin(Level.class)
public abstract class LevelMixin {
	@Inject(method = "getDayTime()J", at = @At("HEAD"), cancellable = true)
	private void backrooms$poolroomsDayTime(CallbackInfoReturnable<Long> cir) {
		Level self = (Level) (Object) this;
		if (!self.isClientSide()
				&& self.dimension().equals(ModWorldgen.POOLROOMS_LEVEL)
				&& self instanceof ServerLevel) {
			Long poolTime = PoolroomsEnvironmentHandler.currentPhase();
			if (poolTime != null) {
				cir.setReturnValue(poolTime);
			}
		}
	}
}
