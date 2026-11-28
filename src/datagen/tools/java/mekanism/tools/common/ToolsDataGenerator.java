package mekanism.tools.common;

import java.util.ArrayList;
import java.util.List;
import mekanism.common.MekanismDataGenerator;
import mekanism.common.PersistingDisabledProvidersProvider;
import mekanism.tools.client.ToolsEquipmentAssetProvider;
import mekanism.tools.client.ToolsLangProvider;
import mekanism.tools.client.ToolsModelProvider;
import mekanism.tools.client.ToolsSplashProvider;
import mekanism.tools.client.ToolsSpriteSourceProvider;
import mekanism.tools.client.integration.emi.ToolsEmiDefaults;
import mekanism.tools.client.recipe_viewer.aliases.ToolsAliasMapping;
import mekanism.tools.common.recipe.ToolsRecipeProvider;
import net.minecraft.server.packs.OverlayMetadataSection.OverlayEntry;
import net.minecraft.server.packs.PackType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import net.neoforged.neoforge.data.event.PackGenerator;

@EventBusSubscriber(modid = MekanismTools.MODID)
public class ToolsDataGenerator {

    private ToolsDataGenerator() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        List<WithConditions<OverlayEntry>> overlays = new ArrayList<>();
        PackGenerator packGenerator = event.getDefaultPackGenerator();
        //Client side data generators
        packGenerator.createProvider(output -> MekanismDataGenerator.forModPack(output, ToolsLang.PACK_DESCRIPTION, overlays));
        packGenerator.createProvider(ToolsLangProvider::new);
        packGenerator.createProvider(ToolsSpriteSourceProvider::new);
        packGenerator.createProvider(output -> new ToolsModelProvider(output, event.getResourceManager(PackType.CLIENT_RESOURCES)));
        packGenerator.createProvider(ToolsEquipmentAssetProvider::new);
        packGenerator.createProvider(ToolsSplashProvider::new);
        //Server side data generators
        packGenerator.createProvider(ToolsTagProvider::new);
        packGenerator.createProvider(ToolsEmiDefaults::new);
        //Data generator to help with persisting data when porting across MC versions when optional deps aren't updated yet
        // DO NOT ADD OTHERS AFTER THIS ONE
        PersistingDisabledProvidersProvider.addDisabledEmiProvider(packGenerator, event.getModContainer(), ToolsAliasMapping::new);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onGatherRegistries(GatherDataRegistryEntriesEvent event) {
        MekanismDataGenerator.bootstrapConfigs(MekanismTools.MODID);
        event.advancement(ToolsAdvancementProvider::new)
              .recipe(ToolsRecipeProvider::new)
        ;
    }
}