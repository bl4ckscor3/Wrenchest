package bl4ckscor3.mod.wrenchest.datagen;

import bl4ckscor3.mod.wrenchest.Wrenchest;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeGenerator extends RecipeProvider {
	public static final TagKey<Item> INGOTS_IRON = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/iron"));
	public static final TagKey<Item> DUSTS_REDSTONE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dusts/redstone"));
	private final HolderGetter<Item> items;

	public RecipeGenerator(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
		super(recipeOutput, advancementOutput);
		items = recipeOutput.lookup(Registries.ITEM);
	}

	@Override
	public final void buildRecipes() {
		ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, Wrenchest.CHEST_WRENCH.get())
			.pattern(" I ")
			.pattern(" RI")
			.pattern("S  ")
			.define('I', INGOTS_IRON)
			.define('R', DUSTS_REDSTONE)
			.define('S', Items.STICK)
			.unlockedBy("has_redstone", has(DUSTS_REDSTONE))
			.save(output);
	}
}
