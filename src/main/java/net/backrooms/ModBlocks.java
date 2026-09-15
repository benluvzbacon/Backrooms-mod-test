package net.backrooms;

import net.backrooms.worldgen.block.FluorescentBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * All custom blocks. Kept deliberately small for v1 - more can be added
 * (different wallpaper variants, props, doors...) without touching the
 * chunk generator.
 */
public final class ModBlocks {
	public static final Block WALLPAPER = register(
			"wallpaper",
			new Block(Block.Properties.of()
					.strength(1.1F)
					.sound(SoundType.WOOL)));

	public static final Block WALLPAPER_DAMP = register(
			"wallpaper_damp",
			new Block(Block.Properties.of()
					.strength(1.0F)
					.sound(SoundType.WOOL)));

	public static final Block CARPET = register(
			"carpet",
			new Block(Block.Properties.of()
					.strength(0.6F)
					.sound(SoundType.WOOL)));

	public static final Block CEILING_TILE = register(
			"ceiling_tile",
			new Block(Block.Properties.of()
					.strength(0.5F)
					.sound(SoundType.WOOL)));

	/** Solid backing layer under floors / above ceilings. */
	public static final Block FOUNDATION = register(
			"foundation",
			new Block(Block.Properties.of()
					.strength(2.5F)
					.sound(SoundType.STONE)));

	/** Fluorescent ceiling fixture. Carries a {@link net.backrooms.worldgen.block.LightFlickerBlockEntity} while lit. */
	public static final Block FLUORESCENT = register(
			"fluorescent",
			new FluorescentBlock(Block.Properties.of()
					.strength(0.4F)
					.sound(SoundType.GLASS)
					.lightLevel(state -> state.getValue(FluorescentBlock.LIT) ? 15 : 0)));

	/** White ceramic pool tile used throughout the Poolrooms. */
	public static final Block POOL_TILE = register(
			"pool_tile",
			new Block(Block.Properties.of()
					.strength(1.4F)
					.sound(SoundType.STONE)));

	/** Shimmering, non-solid portal surface between Level 0 and the Poolrooms. */
	public static final Block POOL_PORTAL = register(
			"pool_portal",
			new net.backrooms.worldgen.block.PoolPortalBlock());

	private ModBlocks() {
	}

	public static Block register(String name, Block block) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, name);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
		return block;
	}

	public static void initialize() {
	}
}
