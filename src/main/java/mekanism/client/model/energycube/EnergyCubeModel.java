package mekanism.client.model.energycube;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import mekanism.api.RelativeSide;
import mekanism.client.render.lib.QuadTransformation;
import mekanism.client.render.lib.QuadUtils;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.TileEntityEnergyCube.CubeSideState;
import mekanism.common.util.EnumUtils;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//TODO: Implement a custom loader??
// Currently this is a copy/hybrid of CompositeModel and ElementsModel
public class EnergyCubeModel implements IUnbakedGeometry<EnergyCubeModel> {

    private final List<BlockElement> frame;
    private final Map<RelativeSide, List<BlockElement>> leds;
    private final Map<RelativeSide, List<BlockElement>> ports;

    private EnergyCubeModel(List<BlockElement> frame, Map<RelativeSide, List<BlockElement>> leds, Map<RelativeSide, List<BlockElement>> ports) {
        this.frame = frame;
        this.leds = leds;
        this.ports = ports;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
          ItemOverrides overrides, ResourceLocation modelLocation) {
        TextureAtlasSprite particle = spriteGetter.apply(context.getMaterial("particle"));

        ResourceLocation renderTypeHint = context.getRenderTypeHint();
        RenderTypeGroup renderTypes = renderTypeHint == null ? RenderTypeGroup.EMPTY : context.getRenderType(renderTypeHint);

        Transformation rootTransform = context.getRootTransform();
        if (!rootTransform.isIdentity()) {
            modelState = new SimpleModelState(modelState.getRotation().compose(rootTransform), modelState.isUvLocked());
        }
        Function<String, TextureAtlasSprite> rawSpriteGetter = spriteGetter.compose(context::getMaterial);
        FaceData frame = bakeElement(rawSpriteGetter, modelState, modelLocation, this.frame);
        Map<RelativeSide, FaceData> leds = bakeElements(rawSpriteGetter, modelState, modelLocation, this.leds);
        Map<RelativeSide, FaceData> ports = bakeElements(rawSpriteGetter, modelState, modelLocation, this.ports);
        return new Baked(context.useAmbientOcclusion(), context.useBlockLight(), context.isGui3d(), context.getTransforms(), overrides, particle, frame, leds, ports,
              renderTypes);
    }

    private Map<RelativeSide, FaceData> bakeElements(Function<String, TextureAtlasSprite> spriteGetter, ModelState modelState,
          ResourceLocation modelLocation, Map<RelativeSide, List<BlockElement>> sideBasedElements) {
        Map<RelativeSide, FaceData> sideBasedFaceData = new EnumMap<>(RelativeSide.class);
        for (Map.Entry<RelativeSide, List<BlockElement>> entry : sideBasedElements.entrySet()) {
            FaceData faceData = bakeElement(spriteGetter, modelState, modelLocation, entry.getValue());
            sideBasedFaceData.put(entry.getKey(), faceData);
        }
        return sideBasedFaceData;
    }

    private FaceData bakeElement(Function<String, TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation modelLocation, List<BlockElement> elements) {
        FaceData data = new FaceData();
        for (BlockElement element : elements) {
            for (Entry<Direction, BlockElementFace> faceEntry : element.faces.entrySet()) {
                BlockElementFace face = faceEntry.getValue();
                TextureAtlasSprite sprite = spriteGetter.apply(face.texture);
                //TODO: Does this cull for wrong side given it is doing it based on actual side vs relative side
                Direction direction = face.cullForDirection == null ? null : modelState.getRotation().rotateTransform(face.cullForDirection);
                data.addFace(direction, BlockModel.bakeFace(element, face, sprite, faceEntry.getKey(), modelState, modelLocation));
            }
        }
        return data;
    }

    @Override
    public Collection<Material> getMaterials(IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter,
          Set<Pair<String, String>> missingTextureErrors) {
        Set<Material> textures = new HashSet<>();
        if (context.hasMaterial("particle")) {
            textures.add(context.getMaterial("particle"));
        }
        addMaterials(context, missingTextureErrors, frame, textures);
        for (List<BlockElement> elements : leds.values()) {
            addMaterials(context, missingTextureErrors, elements, textures);
        }
        for (List<BlockElement> elements : ports.values()) {
            addMaterials(context, missingTextureErrors, elements, textures);
        }
        return textures;
    }

    private void addMaterials(IGeometryBakingContext context, Set<Pair<String, String>> missingTextureErrors, List<BlockElement> elements, Set<Material> textures) {
        for (BlockElement part : elements) {
            for (BlockElementFace face : part.faces.values()) {
                Material texture = context.getMaterial(face.texture);
                if (texture.texture().equals(MissingTextureAtlasSprite.getLocation())) {
                    missingTextureErrors.add(Pair.of(face.texture, context.getModelName()));
                }
                textures.add(texture);
            }
        }
    }

    //TODO: Test all orientations
    public static class Baked implements IDynamicBakedModel {

        private static final CubeSideState[] INACTIVE = Util.make(new CubeSideState[EnumUtils.DIRECTIONS.length], sideStates -> Arrays.fill(sideStates, CubeSideState.INACTIVE));
        private static final QuadTransformation LED_TRANSFORMS = QuadTransformation.list(QuadTransformation.fullbright, QuadTransformation.uvShift(-2, 0));

        private final FaceData frame;
        private final Map<RelativeSide, FaceData> leds;
        private final Map<RelativeSide, FaceData> activeLEDs;
        private final Map<RelativeSide, FaceData> ports;
        private final Map<RelativeSide, FaceData> activePorts;
        private final ChunkRenderTypeSet blockRenderTypes;
        private final List<RenderType> itemRenderTypes;
        private final List<RenderType> fabulousItemRenderTypes;
        private final boolean isAmbientOcclusion;
        private final boolean usesBlockLight;
        private final boolean isGui3d;
        private final TextureAtlasSprite particle;
        private final ItemOverrides overrides;
        private final ItemTransforms transforms;

        public Baked(boolean useAmbientOcclusion, boolean usesBlockLight, boolean isGui3d, ItemTransforms transforms, ItemOverrides overrides, TextureAtlasSprite particle,
              FaceData frame, Map<RelativeSide, FaceData> leds, Map<RelativeSide, FaceData> ports, RenderTypeGroup renderTypes) {
            this.isAmbientOcclusion = useAmbientOcclusion;
            this.usesBlockLight = usesBlockLight;
            this.isGui3d = isGui3d;
            this.overrides = overrides;
            this.transforms = transforms;
            this.particle = particle;
            this.frame = frame;
            this.leds = leds;
            this.ports = ports;
            this.activeLEDs = new EnumMap<>(RelativeSide.class);
            this.activePorts = new EnumMap<>(RelativeSide.class);

            //TODO: Make this stuff lazy??
            for (Map.Entry<RelativeSide, FaceData> entry : this.leds.entrySet()) {
                activeLEDs.put(entry.getKey(), entry.getValue().transform(LED_TRANSFORMS));
            }
            for (Map.Entry<RelativeSide, FaceData> entry : this.ports.entrySet()) {
                activePorts.put(entry.getKey(), entry.getValue().transform(QuadTransformation.filtered_fullbright));
            }
            if (renderTypes.isEmpty()) {
                this.blockRenderTypes = null;
                this.itemRenderTypes = null;
                this.fabulousItemRenderTypes = null;
            } else {
                this.blockRenderTypes = ChunkRenderTypeSet.of(renderTypes.block());
                this.itemRenderTypes = Collections.singletonList(renderTypes.entity());
                this.fabulousItemRenderTypes = Collections.singletonList(renderTypes.entityFabulous());
            }
        }

        @NotNull
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data,
              @Nullable RenderType renderType) {
            //TODO: At some point mark some faces as culled (including for ports)
            CubeSideState[] sideStates = data.get(TileEntityEnergyCube.SIDE_STATE_PROPERTY);
            if (sideStates == null || sideStates.length != EnumUtils.SIDES.length) {
                sideStates = INACTIVE;
            }
            //TODO: Implement Caching
            List<BakedQuad> quads = new ArrayList<>(frame.getFaces(side));
            for (int i = 0; i < EnumUtils.SIDES.length; i++) {
                RelativeSide dir = EnumUtils.SIDES[i];
                CubeSideState sideState = sideStates[i];
                if (sideState == CubeSideState.ACTIVE_LIT) {
                    quads.addAll(activeLEDs.get(dir).getFaces(side));
                    quads.addAll(activePorts.get(dir).getFaces(side));
                } else {
                    quads.addAll(leds.get(dir).getFaces(side));
                    if (sideState == CubeSideState.ACTIVE_UNLIT) {
                        quads.addAll(ports.get(dir).getFaces(side));
                    }
                }
            }
            return quads;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return isAmbientOcclusion;
        }

        @Override
        public boolean isGui3d() {
            return isGui3d;
        }

        @Override
        public boolean usesBlockLight() {
            return usesBlockLight;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @NotNull
        @Override
        @Deprecated
        public TextureAtlasSprite getParticleIcon() {
            return particle;
        }

        @NotNull
        @Override
        @Deprecated
        public ItemOverrides getOverrides() {
            return overrides;
        }

        @NotNull
        @Override
        @Deprecated
        public ItemTransforms getTransforms() {
            return transforms;
        }

        @NotNull
        @Override
        public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
            return blockRenderTypes == null ? IDynamicBakedModel.super.getRenderTypes(state, rand, data) : blockRenderTypes;
        }

        @NotNull
        @Override
        public List<RenderType> getRenderTypes(@NotNull ItemStack stack, boolean fabulous) {
            if (fabulous) {
                if (fabulousItemRenderTypes != null) {
                    return fabulousItemRenderTypes;
                }
            } else if (itemRenderTypes != null) {
                return itemRenderTypes;
            }
            return IDynamicBakedModel.super.getRenderTypes(stack, fabulous);
        }
    }

    private static class FaceData {

        private List<BakedQuad> unculledFaces;
        private Map<Direction, List<BakedQuad>> culledFaces;

        public List<BakedQuad> getFaces(@Nullable Direction side) {
            if (side == null) {
                return unculledFaces == null ? Collections.emptyList() : unculledFaces;
            }
            return culledFaces == null ? Collections.emptyList() : culledFaces.getOrDefault(side, Collections.emptyList());
        }

        public void addFace(@Nullable Direction direction, BakedQuad quad) {
            List<BakedQuad> quads;
            if (direction == null) {
                if (unculledFaces == null) {
                    unculledFaces = new ArrayList<>();
                }
                quads = unculledFaces;
            } else {
                if (culledFaces == null) {
                    culledFaces = new EnumMap<>(Direction.class);
                }
                quads = culledFaces.computeIfAbsent(direction, dir -> new ArrayList<>());
            }
            quads.add(quad);
        }

        public FaceData transform(QuadTransformation transformation) {
            if (unculledFaces == null && culledFaces == null) {
                return this;
            }
            FaceData transformed = new FaceData();
            if (unculledFaces != null) {
                transformed.unculledFaces = QuadUtils.transformBakedQuads(unculledFaces, transformation);
            }
            if (culledFaces != null) {
                transformed.culledFaces = new EnumMap<>(Direction.class);
                for (Map.Entry<Direction, List<BakedQuad>> entry : culledFaces.entrySet()) {
                    transformed.culledFaces.put(entry.getKey(), QuadUtils.transformBakedQuads(entry.getValue(), transformation));
                }
            }
            return transformed;
        }
    }

    /**
     * Mekanism model loader that gets automatically wrapped into a robit baked model
     */
    public static class Loader implements IGeometryLoader<EnergyCubeModel> {//TODO: Fix javadocs

        public static final Loader INSTANCE = new Loader();

        private Loader() {
        }

        @NotNull
        @Override
        public EnergyCubeModel read(@NotNull JsonObject jsonObject, @NotNull JsonDeserializationContext ctx) {
            List<BlockElement> frame = readElements(jsonObject, ctx, "frame");
            Map<RelativeSide, List<BlockElement>> leds = new EnumMap<>(RelativeSide.class);
            Map<RelativeSide, List<BlockElement>> ports = new EnumMap<>(RelativeSide.class);
            for (RelativeSide side : EnumUtils.SIDES) {
                String name = side.name().toLowerCase(Locale.ROOT);
                leds.put(side, readElements(jsonObject, ctx, name + "LEDs"));
                ports.put(side, readElements(jsonObject, ctx, name + "Port"));
            }
            return new EnergyCubeModel(frame, leds, ports);
        }

        private List<BlockElement> readElements(JsonObject jsonObject, JsonDeserializationContext ctx, String key) {
            List<BlockElement> elements = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(jsonObject, key)) {
                elements.add(ctx.deserialize(element, BlockElement.class));
            }
            if (elements.isEmpty()) {
                throw new JsonParseException("Energy cube models requires a \"" + key + "\" element with at least one element.");
            }
            return elements;
        }
    }
}
