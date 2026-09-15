package net.backrooms.worldgen.block;

import net.minecraft.core.BlockPos;
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
 * The shimmering surface of a Poolrooms portal pad. It has no collision and
 * glows faintly; walking into it transfers the player between Level 0 and the
 * Poolrooms (handled server-side by the portal tick handler).
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
	public void entityInside(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide() || !(entity instanceof net.minecraft.server.level.ServerPlayer player)) {
			return;
		}
		net.backrooms.mechanics.PoolPortalHandler.onPortalTouch(player);
	}
}
