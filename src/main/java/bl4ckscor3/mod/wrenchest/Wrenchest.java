package bl4ckscor3.mod.wrenchest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.minecraft.util.Mth.frac;

@Mod(Wrenchest.MODID)
@EventBusSubscriber
public class Wrenchest {
	public static final String MODID = "wrenchest";
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
	public static final DeferredItem<Item> CHEST_WRENCH = ITEMS.registerItem("chest_wrench", p -> new Item(p) {
		@Override
		public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
			InteractionResult result = checkConnections(ctx);

			if (result == InteractionResult.SUCCESS && !ctx.getPlayer().isCreative())
				stack.hurtAndBreak(1, ctx.getPlayer(), LivingEntity.getSlotForHand(ctx.getHand()));

			return result;
		}

		/**
		 * Checks which way two chests might be facing each other and then connects them
		 *
		 * @see Item#onItemUseFirst
		 */
		private InteractionResult checkConnections(UseOnContext ctx) {
			if (!(ctx.getLevel().getBlockState(ctx.getClickedPos()).getBlock() instanceof ChestBlock))
				return InteractionResult.PASS;

			Level level = ctx.getLevel();
			BlockPos pos = ctx.getClickedPos();
			BlockState chestState = level.getBlockState(pos);

			//disconnect double chests
			if (chestState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
				Direction facingTowardsOther = ChestBlock.getConnectedDirection(chestState);

				level.setBlockAndUpdate(pos, chestState.setValue(ChestBlock.TYPE, ChestType.SINGLE));
				level.setBlockAndUpdate(pos.relative(facingTowardsOther), level.getBlockState(pos.relative(facingTowardsOther)).setValue(ChestBlock.TYPE, ChestType.SINGLE));
				return InteractionResult.SUCCESS;
			}
			//connect single chests, UP/DOWN check is here so double chests can be disconnected by clicking on all faces
			else if (ctx.getClickedFace() != Direction.UP && ctx.getClickedFace() != Direction.DOWN) {
				BlockPos otherPos = pos.relative(ctx.getClickedFace());
				BlockState otherState = level.getBlockState(otherPos);

				if (otherState.getBlock() instanceof ChestBlock && otherState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
					Direction facing = chestState.getValue(ChestBlock.FACING);
					Direction otherFacing = otherState.getValue(ChestBlock.FACING);

					//the chests are facing (away from) each other
					if ((ctx.getClickedFace() == facing || ctx.getClickedFace() == otherFacing) && facing.getOpposite() == otherFacing) {
						if (connectChests(level, pos, otherPos, chestState, otherState, ctx.getClickedFace(), facing, frac(ctx.getClickLocation().x), frac(ctx.getClickLocation().z), true))
							return InteractionResult.SUCCESS;
						else
							return InteractionResult.PASS;
					}
					//the clicked chest has the other chest to its left/right
					else if (ctx.getClickedFace().getClockWise() == facing || ctx.getClickedFace().getCounterClockWise() == facing) {
						if (connectChests(level, pos, otherPos, chestState, otherState, ctx.getClickedFace(), facing, frac(ctx.getClickLocation().x), frac(ctx.getClickLocation().z), false))
							return InteractionResult.SUCCESS;
						else
							return InteractionResult.PASS;
					}
					//the clicked chest has its neighbor to the front/back
					else if (ctx.getClickedFace().getClockWise() == otherFacing || ctx.getClickedFace().getCounterClockWise() == otherFacing) {
						if (connectChests(level, pos, otherPos, chestState, otherState, ctx.getClickedFace(), otherFacing, frac(ctx.getClickLocation().x), frac(ctx.getClickLocation().z), false))
							return InteractionResult.SUCCESS;
						else
							return InteractionResult.PASS;
					}
					//the chests are facing in the same direction, but are placed behind each other. the case where they are standing next to each other facing the same direction is covered before
					else if (facing == otherFacing) {
						if (connectChests(level, pos, otherPos, chestState, otherState, ctx.getClickedFace(), facing.getClockWise(), frac(ctx.getClickLocation().x), frac(ctx.getClickLocation().z), false))
							return InteractionResult.SUCCESS;
						else
							return InteractionResult.PASS;
					}
				}
			}

			return InteractionResult.PASS;
		}

		/**
		 * Connects two chests with each other based on hit data
		 *
		 * @param level The world the two chests are in
		 * @param clickedPos The position of the clicked chest
		 * @param otherPos The position of the chest to connect the clicked chest to
		 * @param clickedState The state of the clicked chest
		 * @param otherState The state of the chest to connect the clicked chest to
		 * @param clickedFace The face that got clicked
		 * @param chestFacing The facing of the chest that should be used to determine the new direction to face, can be arbitrary
		 *            under certain circumstances.
		 * @param hitX The x coordinate that was hit on the face
		 * @param hitZ The z coordinate that was hit on the face
		 * @param swapDirections Whether to swap the directions to check the hit data on
		 * @return true if the chests were connected, false otherwise
		 */
		private boolean connectChests(Level level, BlockPos clickedPos, BlockPos otherPos, BlockState clickedState, BlockState otherState, Direction clickedFace, Direction chestFacing, double hitX, double hitZ, boolean swapDirections) {
			Direction newFacing = Direction.UP;

			if (!swapDirections) {
				if (chestFacing == Direction.NORTH || chestFacing == Direction.SOUTH)
					newFacing = hitZ < 0.5 ? Direction.NORTH : Direction.SOUTH;
				else if (chestFacing == Direction.WEST || chestFacing == Direction.EAST)
					newFacing = hitX < 0.5 ? Direction.WEST : Direction.EAST;
			}
			else {
				if (chestFacing == Direction.WEST || chestFacing == Direction.EAST)
					newFacing = hitZ < 0.5 ? Direction.NORTH : Direction.SOUTH;
				else if (chestFacing == Direction.NORTH || chestFacing == Direction.SOUTH)
					newFacing = hitX < 0.5 ? Direction.WEST : Direction.EAST;
			}

			if (newFacing != Direction.UP) {
				ChestType newType = getNewChestType(clickedFace, newFacing);

				level.setBlockAndUpdate(clickedPos, clickedState.setValue(ChestBlock.FACING, newFacing).setValue(ChestBlock.TYPE, newType));
				level.setBlockAndUpdate(otherPos, otherState.setValue(ChestBlock.FACING, newFacing).setValue(ChestBlock.TYPE, newType.getOpposite()));
				return true;
			}

			return false;
		}

		/**
		 * Figures out which side the new connection should go on
		 *
		 * @param clickedFace The face that was clicked with the chest connector
		 * @param chestFacing The facing of the chest that was clicked
		 * @return The new chest type for the clicked chest
		 */
		private ChestType getNewChestType(Direction clickedFace, Direction chestFacing) {
			return switch (chestFacing) {
				case NORTH -> clickedFace == Direction.WEST ? ChestType.RIGHT : ChestType.LEFT;
				case SOUTH -> clickedFace == Direction.WEST ? ChestType.LEFT : ChestType.RIGHT;
				case EAST -> clickedFace == Direction.NORTH ? ChestType.RIGHT : ChestType.LEFT;
				case WEST -> clickedFace == Direction.NORTH ? ChestType.LEFT : ChestType.RIGHT;
				default -> ChestType.SINGLE;
			};
		}
	}, new Item.Properties().durability(256).repairable(Items.IRON_INGOT));

	public Wrenchest(IEventBus modEventBus) {
		ITEMS.register(modEventBus);
	}

	@SubscribeEvent
	public static void onCreativeModeTabBuildContents(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
			event.insertBefore(new ItemStack(Items.FISHING_ROD), new ItemStack(CHEST_WRENCH.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
	}
}
