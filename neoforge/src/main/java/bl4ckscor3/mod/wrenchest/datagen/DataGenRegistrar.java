package bl4ckscor3.mod.wrenchest.datagen;

import java.util.Set;

import bl4ckscor3.mod.wrenchest.Wrenchest;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Wrenchest.MODID)
public class DataGenRegistrar {
	private DataGenRegistrar() {}

	@SubscribeEvent
	public static void onGatherData(GatherDataEvent.Client event) {
		event.createReloadableRegistryObjects(
			new RegistrySetBuilder().add(RecipeProvider.asBootstrap(RecipeGenerator::new)),
			Set.of(Wrenchest.MODID)
		);
	}
}
