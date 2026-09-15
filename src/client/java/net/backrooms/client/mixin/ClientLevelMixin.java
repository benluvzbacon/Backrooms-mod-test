package net.backrooms.client.mixin;

import net.backrooms.ModWorldgen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the Poolrooms client clock 4x fast so the sun and sky colours move
 * smoothly between the once-per-second server time synchronisations.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Shadow
	@Final
	private ClientLevel.ClientLevelData clientLevelData;

	@Inject(method = "tickTime()V", at = @At("HEAD"), cancellable = true)
	private void backrooms$fastPoolroomsClock(CallbackInfo ci) {
		ClientLevel self = (ClientLevel) (Object) this;
		if (!self.dimension().equals(ModWorldgen.POOLROOMS_LEVEL)) {
			return;
		}
		this.clientLevelData.setGameTime(this.clientLevelData.getGameTime() + 1L);
		if (this.clientLevelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
			this.clientLevelData.setDayTime(this.clientLevelData.getDayTime() + 4L);
		}
		ci.cancel();
	}
}
