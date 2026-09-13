package net.backrooms.worldgen.block;

import net.backrooms.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A fluorescent light fixture. When LIT it emits light level 15 and owns a
 * {@link LightFlickerBlockEntity} that produces the flickering / dying-light
 * behaviour. When unlit it is a dead fixture (no block entity).
 */
public class FluorescentBlock extends Block implements EntityBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public FluorescentBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		if (state.getValue(LIT)) {
			return new LightFlickerBlockEntity(pos, state);
		}
		return null;
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
																BlockEntityType<T> type) {
		if (level.isClientSide() || type != ModBlockEntities.FLUORESCENT_LIGHT) {
			return null;
		}
		return (levelAccess, pos, st, blockEntity) -> ((LightFlickerBlockEntity) blockEntity).serverTick();
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}
}
