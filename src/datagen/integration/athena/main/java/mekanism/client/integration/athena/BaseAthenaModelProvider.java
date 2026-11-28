package mekanism.client.integration.athena;

import com.mojang.datafixers.util.Either;
import earth.terrarium.athena.api.client.data.AthenaModelProvider;
import earth.terrarium.athena.api.client.models.AthenaBlockModel;
import earth.terrarium.athena.api.client.models.AthenaModelAttributes;
import earth.terrarium.athena.api.client.models.TintProvider;
import earth.terrarium.athena.api.client.utils.CtmMaterials;
import earth.terrarium.athena.api.client.utils.ModelConnectionCondition;
import earth.terrarium.athena.api.client.utils.ModelConnectionCondition.False;
import earth.terrarium.athena.impl.client.models.AthenaCompositeModel;
import earth.terrarium.athena.impl.client.models.ConnectedBlockModel;
import earth.terrarium.athena.impl.client.models.MultiVariantBlockModel;
import earth.terrarium.athena.impl.client.models.ctm.ConnectedTextureMap.DirectionalCtmProviders;
import earth.terrarium.athena.impl.client.models.ctm.CtmProvider;
import earth.terrarium.athena.impl.client.models.ctm.FourSliceCtmProvider;
import earth.terrarium.athena.impl.client.models.ctm.FourtySevenSliceCtmProvider;
import earth.terrarium.athena.impl.client.models.ctm.SingleSpriteCtmProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import mekanism.common.integration.MekanismHooks;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class BaseAthenaModelProvider extends AthenaModelProvider {

    private final String modid;

    protected BaseAthenaModelProvider(PackOutput output, String modid) {
        this.modid = modid;
        super(output);
    }

    protected Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(modid, path);
    }

    protected void add(Holder<Block> holder, AthenaBlockModel model) {
        add(holder.value(), model);
    }

    protected String texture(Holder<Block> holder) {
        return holder.key().identifier().getPath();
    }

    public record MultiLayerTexture(String texture, Optional<String> ledTexture, boolean ledIsConnected) {

        public MultiLayerTexture(String texture) {
            this(texture, Optional.empty(), false);
        }

        public static MultiLayerTexture simpleLed(String texture) {
            return simpleLed(texture, false);
        }

        public static MultiLayerTexture simpleLed(String texture, boolean ledIsConnected) {
            return new MultiLayerTexture(texture, Optional.of(texture + "_led"), ledIsConnected);
        }

        public AthenaBlockModel createModel(BaseAthenaModelProvider provider, ModelConnectionCondition connectsTo) {
            AthenaBlockModel baseModel = provider.simpleConnected(texture, true, connectsTo);
            if (ledTexture.isPresent()) {
                AthenaBlockModel ledModel;
                if (ledIsConnected) {
                    ledModel = provider.simpleConnected(ledTexture.get(), true, new AthenaModelAttributes(Optional.empty(), 15), connectsTo);
                } else {
                    ledModel = provider.unconnected(ledTexture.get(), 15);
                }
                return new AthenaCompositeModel(List.of(baseModel, ledModel));
            }
            return baseModel;
        }

    }

    protected <T extends Comparable<T>> void addBasicMultivariant(Holder<Block> holder, Property<T> property, Map<T, String> propertyTextures, TagKey<Block> connectsTo) {
        Map<T, MultiLayerTexture> mapped = new LinkedHashMap<>(propertyTextures.size());
        for (Entry<T, String> entry : propertyTextures.entrySet()) {
            mapped.put(entry.getKey(), new MultiLayerTexture(entry.getValue()));
        }
        addMultiLayerMultivariant(holder, property, mapped, connectsTo);
    }

    protected <T extends Comparable<T>> void addMultiLayerMultivariant(Holder<Block> holder, Property<T> property, Map<T, MultiLayerTexture> propertyTextures, TagKey<Block> connectsTo) {
        addMultiLayerMultivariant(holder, property, propertyTextures, new ModelConnectionCondition.Tag(connectsTo));
    }

    protected <T extends Comparable<T>> void addMultiLayerMultivariant(Holder<Block> holder, Property<T> property, Map<T, MultiLayerTexture> propertyTextures, ModelConnectionCondition connectsToCondition) {
        Map<PropertyValueList, MultiLayerTexture> mapped = new LinkedHashMap<>(propertyTextures.size());
        for (Entry<T, MultiLayerTexture> entry : propertyTextures.entrySet()) {
            mapped.put(PropertyValueList.of(property.value(entry.getKey())), entry.getValue());
        }
        addMultivariant(holder, mapped, connectsToCondition);
    }

    protected void addMultivariant(Holder<Block> holder, Map<PropertyValueList, MultiLayerTexture> propertyTextures, ModelConnectionCondition connectsToCondition) {
        Map<String, WeightedList<AthenaBlockModel>> mapped = new LinkedHashMap<>(propertyTextures.size());
        for (Entry<PropertyValueList, MultiLayerTexture> entry : propertyTextures.entrySet()) {
            mapped.put(entry.getKey().getKey(), WeightedList.of(entry.getValue().createModel(this, connectsToCondition)));
        }
        add(holder, new MultiVariantBlockModel(mapped));
    }

    protected final void addSimpleConnected(Holder<Block> holder) {
        add(holder.value(), simpleConnected(texture(holder), true, ModelConnectionCondition.SameState.INSTANCE));
    }

    protected void addSimpleConnected(Holder<Block> holder, TagKey<Block> connection) {
        addSimpleConnected(holder, true, connection);
    }

    protected void addSimpleConnected(Holder<Block> holder, boolean fourSlice, TagKey<Block> connection) {
        add(holder.value(), simpleConnected(texture(holder), fourSlice, connection));
    }

    protected Material modTexture(String path) {
        return new Material(id(path).withPrefix("block/"));
    }

    protected Identifier fourSlice(String path) {
        return id(path).withPrefix(MekanismHooks.ATHENA_MOD_ID + "/ctm/");
    }

    protected Identifier fortySevenSlice(String path) {
        return id(path).withPrefix(MekanismHooks.ATHENA_MOD_ID + "/ctm_full/");
    }

    protected AthenaBlockModel simpleConnected(String texture, boolean fourSlice, TagKey<Block> connection) {
        return simpleConnected(texture, fourSlice, new ModelConnectionCondition.Tag(connection));
    }

    protected AthenaBlockModel simpleConnected(String texture, boolean fourSlice, ModelConnectionCondition connectTo) {
        return simpleConnected(texture, fourSlice, AthenaModelAttributes.EMPTY, connectTo);
    }

    protected AthenaBlockModel simpleConnected(String texture, boolean fourSlice, TintProvider tintProvider, ModelConnectionCondition connectTo) {
        return simpleConnected(texture, fourSlice, new AthenaModelAttributes(Optional.of(tintProvider)), connectTo);
    }

    protected CtmProvider.Type fourSliceType(String texture) {
        return new FourSliceCtmProvider.Type(new CtmMaterials(
              modTexture(texture),
              new Material(fourSlice(texture))
        ));
    }

    protected AthenaBlockModel simpleConnected(String texture, boolean fourSlice, AthenaModelAttributes attributes, ModelConnectionCondition connectTo) {
        CtmProvider.Type ctmProviderType;
        if (fourSlice) {
            ctmProviderType = fourSliceType(texture);
        } else {
            ctmProviderType = new FourtySevenSliceCtmProvider.Type(Either.right(fortySevenSlice(texture)));
        }
        return new ConnectedBlockModel(new DirectionalCtmProviders(Map.of(Optional.empty(), ctmProviderType)), connectTo, attributes);
    }

    protected AthenaBlockModel unconnected(String texture, int lightLevel) {
        return new ConnectedBlockModel(new DirectionalCtmProviders(Map.of(Optional.empty(), new SingleSpriteCtmProvider.Type(modTexture(texture)))), False.INSTANCE,
              new AthenaModelAttributes(Optional.empty(), lightLevel));
    }

    protected void addConnectedBlockSlabStairs(String texture, boolean fourSlice, Holder<Block> block, Holder<Block> slab, Holder<Block> stairs, TagKey<Block> connectsTo, TintProvider tintProvider) {
        addConnectedBlockSlabStairs(texture, fourSlice, block, slab, stairs, connectsTo, new AthenaModelAttributes(Optional.of(tintProvider)));
    }

    protected AthenaBlockModel weightedVariants(WeightedList<AthenaBlockModel> variants) {
        return new MultiVariantBlockModel(Map.of("", variants));
    }

    protected void addConnectedBlockSlabStairs(String texture, boolean fourSlice, Holder<Block> block, Holder<Block> slab, Holder<Block> stairs, TagKey<Block> connectsTo, AthenaModelAttributes attributes) {
        CtmProvider.Type ctmProviderType;
        if (fourSlice) {
            ctmProviderType = fourSliceType(texture);
        } else {
            ctmProviderType = new FourtySevenSliceCtmProvider.Type(Either.right(fortySevenSlice(texture)));
        }
        ModelConnectionCondition connectTo = new ModelConnectionCondition.Tag(connectsTo);
        DirectionalCtmProviders ctmProviders = new DirectionalCtmProviders(Map.of(Optional.empty(), ctmProviderType));
        add(block, new ConnectedBlockSlabStairsModel(ctmProviders, connectTo, attributes));
        add(slab, new ConnectedBlockSlabStairsModel(ctmProviders, connectTo, attributes));
        add(stairs, new ConnectedBlockSlabStairsModel(ctmProviders, connectTo, attributes));
    }
}