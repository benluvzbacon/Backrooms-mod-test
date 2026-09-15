package net.backrooms.worldgen.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drives the fluorescent fixture: long stable periods, occasional rapid
 * flicker bursts and rare "light dies for a while" dark periods.
 *
 * <p>Everything is timed locally per fixture - no global per-tick scanning -
 * and only the block state's {@code LIT} flag changes, letting vanilla's
 * incremental light engine do the (cheap) relighting.</p>
 */
public class LightFlickerBlockEntity extends BlockEntity {
	private enum Mode { ON, BLIP_OFF, BLIP_ON, LONG_OFF }

	private Mode mode = Mode.ON;
	private int timer;
	private int blips;

	public LightFlickerBlockEntity(BlockPos pos, BlockState state) {
		super(net.backrooms.ModBlockEntities.FLUORESCENT_LIGHT, pos, state);
		// Stagger fixtures so the whole level doesn't flicker in unison.
		this.timer = 40 + Math.floorMod(Long.hashCode(pos.asLong()), 360);
	}

	public void serverTick() {
		Level level = this.level;
		if (level == null || level.isClientSide()) {
			return;
		}
		if (this.timer > 0) {
			this.timer--;
			return;
		}
		var random = level.getRandom();
		switch (this.mode) {
			case ON -> {
				this.timer = 120 + random.nextInt(480);
				float roll = random.nextFloat();
				if (roll < 0.5F) {
					// Rapid flicker burst.
					this.mode = Mode.BLIP_OFF;
					this.blips = 1 + random.nextInt(3);
					setLit(level, false);
					this.timer = 1 + random.nextInt(3);
				} else if (roll < 0.72F) {
					// Fixture dies for a few seconds (dark section).
					this.mode = Mode.LONG_OFF;
					setLit(level, false);
					this.timer = 60 + random.nextInt(260);
				}
				// Otherwise just enjoy the calm hum until the next event.
			}
			case BLIP_OFF -> {
				setLit(level, true);
				this.mode = Mode.BLIP_ON;
				this.timer = 2 + random.nextInt(4);
			}
			case BLIP_ON -> {
				if (this.blips-- > 0) {
					setLit(level, false);
					this.mode = Mode.BLIP_OFF;
					this.timer = 1 + random.nextInt(3);
				} else {
					setLit(level, true);
					this.mode = Mode.ON;
					this.timer = 120 + random.nextInt(600);
				}
			}
			case LONG_OFF -> {
				setLit(level, true);
				this.mode = Mode.ON;
				this.timer = 240 + random.nextInt(800);
			}
		}
	}

	private void setLit(Level level, boolean lit) {
		BlockState state = getBlockState();
		if (state.getValue(FluorescentBlock.LIT) != lit) {
			level.setBlockAndUpdate(this.worldPosition, state.setValue(FluorescentBlock.LIT, lit));
		}
	}
}
