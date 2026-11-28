package mekanism.common;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.electronwill.nightconfig.core.concurrent.SynchronizedConfig;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import mekanism.api.text.ILangEntry;
import mekanism.client.integration.athena.IMekAthenaDatagen;
import mekanism.client.integration.emi.MekanismEmiDefaults;
import mekanism.client.lang.MekanismLangProvider;
import mekanism.client.model.MekanismEquipmentAssetProvider;
import mekanism.client.model.MekanismModelProvider;
import mekanism.client.sound.MekanismSoundProvider;
import mekanism.client.splash.MekanismSplashProvider;
import mekanism.client.texture.MekanismSpriteSourceProvider;
import mekanism.client.texture.PrideRobitTextureProvider;
import mekanism.common.advancements.MekanismAdvancementProvider;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.computer.ComputerHelpProvider;
import mekanism.common.lib.FieldReflectionHelper;
import mekanism.common.loot.MekanismBlockLootTables;
import mekanism.common.loot.MekanismEntityLootTables;
import mekanism.common.recipe.compat.ICompatRecipeDatagen;
import mekanism.common.recipe.impl.MekanismRecipeProvider;
import mekanism.common.tag.MekanismTagProvider;
import net.minecraft.DetectedVersion;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider.SubProviderEntry;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.conditions.NeoForgeConditions;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.common.data.GeneratingOverlayMetadataSection;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import net.neoforged.neoforge.data.event.PackGenerator;

@EventBusSubscriber(modid = Mekanism.MODID)
public class MekanismDataGenerator {

    @SuppressWarnings("UnstableApiUsage")
    private static final FieldReflectionHelper<ConfigTracker, EnumMap<ModConfig.Type, Set<ModConfig>>> CONFIG_SETS =
          new FieldReflectionHelper<>(ConfigTracker.class, "configSets", () -> new EnumMap<>(ModConfig.Type.class));
    private static final Constructor<?> LOADED_CONFIG;
    private static final Method SET_CONFIG;

    static {
        try {
            Class<?> loadedConfig = Class.forName("net.neoforged.fml.config.LoadedConfig");
            LOADED_CONFIG = loadedConfig.getDeclaredConstructor(CommentedConfig.class, Path.class, ModConfig.class);
            LOADED_CONFIG.setAccessible(true);
            SET_CONFIG = ModConfig.class.getDeclaredMethod("setConfig", loadedConfig, Function.class);
            SET_CONFIG.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private MekanismDataGenerator() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        List<WithConditions<OverlayMetadataSection.OverlayEntry>> overlays = new ArrayList<>();
        //Add Athena resource pack overlays
        registerAthenaOverride(event, overlays);
        //Add compat recipe datapacks
        registerCompatRecipes(event, "ae2", overlays);
        registerCompatRecipes(event, "biomesoplenty", overlays);
        registerCompatRecipes(event, "biomeswevegone", overlays);
        registerCompatRecipes(event, "farmersdelight", overlays);
        //Main mekanism datagen
        PackGenerator packGenerator = event.getDefaultPackGenerator();
        ResourceManager clientResources = event.getResourceManager(PackType.CLIENT_RESOURCES);
        //Client side data generators
        packGenerator.createProvider(output -> forModPack(output, MekanismLang.PACK_DESCRIPTION, overlays));
        packGenerator.createProvider(MekanismLangProvider::new);
        packGenerator.createProvider(output -> new PrideRobitTextureProvider(output, clientResources));
        packGenerator.createProvider(MekanismSoundProvider::new);
        packGenerator.createProvider(MekanismSplashProvider::new);
        packGenerator.createProvider(MekanismSpriteSourceProvider::new);
        packGenerator.createProvider(output -> new MekanismModelProvider(output, clientResources));
        packGenerator.createProvider(MekanismEquipmentAssetProvider::new);
        //Server side data generators
        packGenerator.createProvider(MekanismTagProvider::new);
        packGenerator.createProvider(MekanismDataMapsProvider::new);
        packGenerator.createProvider((output, lookup) -> new ComputerHelpProvider(output, lookup, Mekanism.MODID));
        packGenerator.createProvider(MekanismEmiDefaults::new);
        //Data generator to help with persisting data when porting across MC versions when optional deps aren't updated yet
        // DO NOT ADD OTHERS AFTER THIS ONE
        PersistingDisabledProvidersProvider.addDisableableProviders(packGenerator, event.getModContainer());
    }

    public static void registerAthenaOverride(GatherDataEvent.Client event, List<WithConditions<OverlayMetadataSection.OverlayEntry>> overlays) {
        String modid = event.getModContainer().getModId();
        String directory = MekanismHooks.ATHENA_MOD_ID + "-compat";
        overlays.add(new WithConditions<>(new OverlayMetadataSection.OverlayEntry(
              //Calculate the version in the same way as PackMetadataGenerator#forFeaturePack does
              DetectedVersion.BUILT_IN.packVersion(PackType.CLIENT_RESOURCES).minorRange(), directory
        ), NeoForgeConditions.modLoaded(MekanismHooks.ATHENA_MOD_ID)));
        PackGenerator packGenerator = event.getPackGenerator(event.getGenerator().getPackOutput(directory));
        if (Mekanism.hooks.athena.isLoaded()) {
            IMekAthenaDatagen athenaDatagen = IMekAthenaDatagen.INSTANCES.get(modid);
            if (athenaDatagen != null) {
                packGenerator.createProvider(athenaDatagen::modelProvider);
                PersistingDisabledProvidersProvider.skipNothingInSubPack(packGenerator);
            }
        } else {
            Mekanism.logger.warn("Skipping and persisting existing {} data generated files for Athena connected texture models", modid);
            PersistingDisabledProvidersProvider.skipSubPack(packGenerator, Collections.singleton(MekanismHooks.ATHENA_MOD_ID), "Athena Models");
        }
    }

    private static void registerCompatRecipes(GatherDataEvent.Client event, String compatModId, List<WithConditions<OverlayMetadataSection.OverlayEntry>> overlays) {
        String directory = compatModId + "-compat";
        PackGenerator packGenerator = event.getPackGenerator(event.getGenerator().getPackOutput(directory));
        overlays.add(new WithConditions<>(new OverlayMetadataSection.OverlayEntry(
              //Calculate the version in the same way as PackMetadataGenerator#forFeaturePack does
              DetectedVersion.BUILT_IN.packVersion(PackType.SERVER_DATA).minorRange(), directory
        ), NeoForgeConditions.modLoaded(compatModId)));
        if (ModList.get().isLoaded(compatModId)) {
            ICompatRecipeDatagen compatRecipeDatagen = ICompatRecipeDatagen.INSTANCES.get(compatModId);
            if (compatRecipeDatagen == null) {
                throw new IllegalStateException("Mod: " + compatModId + " does not have a compat recipe service present");
            }
            packGenerator.createReloadableRegistryObjects(new RegistrySetBuilder().add(RecipeProvider.asBootstrap(compatRecipeDatagen)));
            PersistingDisabledProvidersProvider.skipNothingInSubPack(packGenerator);
        } else {
            Mekanism.logger.warn("Skipping and persisting existing data generated compat recipes for {}.", compatModId);
            //Data generator to help with persisting data when porting across MC versions when optional deps aren't updated yet
            PersistingDisabledProvidersProvider.skipSubPack(packGenerator, Collections.singleton(directory), "Registries for reloadable");
        }
    }

    public static PackMetadataGenerator forModPack(PackOutput output, ILangEntry langEntry, List<WithConditions<OverlayMetadataSection.OverlayEntry>> overlays) {
        PackMetadataGenerator generator = PackMetadataGenerator.forFeaturePack(output, langEntry.translate());
        if (!overlays.isEmpty()) {
            //Just add it as the server type as the client and server share the same name
            return generator.add(GeneratingOverlayMetadataSection.type(PackType.SERVER_DATA), new GeneratingOverlayMetadataSection(overlays));
        }
        return generator;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onGatherRegistries(GatherDataRegistryEntriesEvent event) {
        bootstrapConfigs(Mekanism.MODID);
        event.lootTable(
                    new SubProviderEntry(MekanismBlockLootTables::new, LootContextParamSets.BLOCK),
                    new SubProviderEntry(MekanismEntityLootTables::new, LootContextParamSets.ENTITY)
              )
              .advancement(MekanismAdvancementProvider::new)
              .recipe(MekanismRecipeProvider::new)
        ;
    }

    /// Used to bootstrap configs to their default values so that if we are querying if things exist we don't have issues with it happening to early or in cases we have
    /// fake tiles.
    @SuppressWarnings("UnstableApiUsage")
    public static void bootstrapConfigs(String modid) {
        for (Set<ModConfig> configs : CONFIG_SETS.getValue(ConfigTracker.INSTANCE).values()) {
            for (ModConfig config : configs) {
                if (config.getModId().equals(modid)) {
                    //Similar to how ConfigTracker#loadDefaultServerConfigs works for loading default server configs on the client
                    // except we don't bother firing an event as it is private, and we are already at defaults if we had called earlier,
                    // and we also don't fully initialize the mod config as the spec is what we care about, and we can do so without having
                    // to reflect into package private methods
                    CommentedConfig commentedConfig = new SynchronizedConfig(InMemoryCommentedFormat.defaultInstance(), LinkedHashMap::new);
                    config.getSpec().correct(commentedConfig);
                    try {
                        SET_CONFIG.invoke(config, LOADED_CONFIG.newInstance(commentedConfig, null, config),
                              (Function<ModConfig, ModConfigEvent>) ModConfigEvent.Loading::new);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /// Basically a copy of [DataProvider#saveStable(CachedOutput, JsonElement, Path)] but it takes a consumer of the output stream instead of serializes json using GSON.
    /// Use it to write arbitrary files.
    @SuppressWarnings({"UnstableApiUsage", "deprecation"})
    public static CompletableFuture<?> save(CachedOutput cache, IOConsumer<OutputStream> osConsumer, Path path) {
        return CompletableFuture.runAsync(() -> {
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                 HashingOutputStream hashedBytes = new HashingOutputStream(Hashing.sha1(), bytes)) {
                osConsumer.accept(hashedBytes);
                cache.writeIfNeeded(path, bytes.toByteArray(), hashedBytes.hash());
            } catch (IOException ioexception) {
                DataProvider.LOGGER.error("Failed to save file to {}", path, ioexception);
            }
        }, Util.backgroundExecutor());
    }

    @FunctionalInterface
    public interface IOConsumer<T> {

        void accept(T value) throws IOException;
    }
}