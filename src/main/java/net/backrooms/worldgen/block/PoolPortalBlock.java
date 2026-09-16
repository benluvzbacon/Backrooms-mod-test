package net.backrooms.worldgen.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shimmering drain at the bottom of a portal basin. It has no collision
 * and glows faintly; diving into the basin and submerging transfers the player
 * between Level 0 and the Poolrooms (handled server-side by the portal tick
 * handler, with this block's touch hook as a backup).
 */
public class PoolPortalBlock extends Block {
	public PoolPortalBlock() {
		super(Properties.of()
				.strength(-1.0F, 3600000.0F)
				.sound(SoundType.GLASS)
				.lightLevel(state -> 12)
				.noLootTable()
				.noOcclusion()
				.isValidSpawn((state, level, pos, entityType) -> false)
				.isSuffocating((state, level, pos) -> false)
				.isViewBlocking((state, level, pos) -> false)
				.pushReaction(PushReaction.BLOCK));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
			net.minecraft.util.RandomSource random) {
		// Bubble columns rise from the drain through the basin water.
		if (random.nextFloat() < 0.35F
				&& level.getFluidState(pos.above()).is(FluidTags.WATER)) {
			double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
			double y = pos.getY() + 1.2;
			double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
			level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 0.0, 0.08, 0.0);
		}
	}

	@Override
	public void entityInside(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide() || !(entity instanceof net.minecraft.server.level.ServerPlayer player)) {
			return;
		}
		net.backrooms.mechanics.PoolPortalHandler.onPortalTouch(player);
	}
}
