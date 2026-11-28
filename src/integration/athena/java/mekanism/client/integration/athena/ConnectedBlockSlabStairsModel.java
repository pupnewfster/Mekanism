package mekanism.client.integration.athena;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import earth.terrarium.athena.api.client.models.AthenaBlockModel;
import earth.terrarium.athena.api.client.models.AthenaModelAttributes;
import earth.terrarium.athena.api.client.models.AthenaModelType;
import earth.terrarium.athena.api.client.models.AthenaQuad;
import earth.terrarium.athena.api.client.utils.AppearanceAndTintGetter;
import earth.terrarium.athena.api.client.utils.AthenaUtils;
import earth.terrarium.athena.api.client.utils.AthenaUtils.UrMom;
import earth.terrarium.athena.api.client.utils.CtmState;
import earth.terrarium.athena.api.client.utils.CtmState.ConnectionCheck;
import earth.terrarium.athena.api.client.utils.ModelConnectionCondition;
import earth.terrarium.athena.impl.client.models.ctm.ConnectedTextureMap;
import earth.terrarium.athena.impl.client.models.materials.MaterialStorage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.Material.Baked;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ConnectedBlockSlabStairsModel implements AthenaBlockModel {

    private static final MapCodec<ConnectedBlockSlabStairsModel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          ConnectedTextureMap.DirectionalCtmProviders.CODEC.fieldOf("ctm_textures").forGetter(model -> model.textureProviders),
          ModelConnectionCondition.CONNECTS_TO_CODEC.forGetter(model -> model.connectTo),
          AthenaModelAttributes.TINT_CODEC.forGetter(model -> model.attributes)
    ).apply(instance, ConnectedBlockSlabStairsModel::new));

    public static final AthenaModelType TYPE = new AthenaModelType(CODEC);

    private final Map<BlockState, List<AthenaBlockModel.Part>> cachedParts = new Reference2ObjectOpenHashMap<>();
    private final MaterialStorage materials;
    private final ConnectedTextureMap textures;
    private final ConnectedTextureMap.DirectionalCtmProviders textureProviders;
    private final ModelConnectionCondition connectTo;
    private final AthenaModelAttributes attributes;

    public ConnectedBlockSlabStairsModel(
          ConnectedTextureMap.DirectionalCtmProviders textureProviders,
          ModelConnectionCondition connectTo,
          AthenaModelAttributes attributes
    ) {
        this.textureProviders = textureProviders;
        this.connectTo = connectTo;
        this.attributes = attributes;

        this.materials = new MaterialStorage();
        this.textures = textureProviders.resolve(this.materials);
    }

    @Override
    public AthenaModelType type() {
        return TYPE;
    }

    @Override
    public List<AthenaBlockModel.Part> selectParts(RandomSource random, BlockState state) {
        //Just look it up from the cache
        return getAllParts(state);
    }

    @Override
    public List<AthenaBlockModel.Part> getAllParts(BlockState state) {
        return cachedParts.computeIfAbsent(state, s -> Collections.singletonList(new ConnectedBlockSlabStairsPart(s)));
    }

    private class ConnectedBlockSlabStairsPart implements AthenaBlockModel.Part {

        private final Map<Direction, WeakReference<FaceShape>> cachedFaceShapes = new EnumMap<>(Direction.class);
        private final BlockState state;

        public ConnectedBlockSlabStairsPart(BlockState state) {
            this.state = state;
        }

        private FaceShape getFaceShape(Direction side) {
            //Weakly cache the shapes for how a state is on a given side
            FaceShape faceShape = cachedFaceShapes.computeIfAbsent(side, s -> new WeakReference<>(FaceShape.getFaceShape(state, s))).get();
            if (faceShape == null) {
                faceShape = FaceShape.getFaceShape(state, side);
                cachedFaceShapes.put(side, new WeakReference<>(faceShape));
            }
            return faceShape;
        }

        @Override
        public List<AthenaQuad> getQuads(AppearanceAndTintGetter level, BlockState state, BlockPos pos, Direction direction) {
            //Modified version of CtmState#from with more advanced connection logic
            BlockPos upPos = AthenaUtils.getFacingPos(pos, direction, AthenaUtils.UrMom.UP);
            BlockPos downPos = AthenaUtils.getFacingPos(pos, direction, AthenaUtils.UrMom.DOWN);
            ConnectionType up = test(level, state, pos, upPos, direction, ConnectionDirection.UP);
            ConnectionType down = test(level, state, pos, downPos, direction, ConnectionDirection.DOWN);
            ConnectionType left = test(level, state, pos, AthenaUtils.getFacingPos(pos, direction, UrMom.LEFT), direction, ConnectionDirection.LEFT);
            ConnectionType right = test(level, state, pos, AthenaUtils.getFacingPos(pos, direction, UrMom.RIGHT), direction, ConnectionDirection.RIGHT);

            ConnectionType upLeft = test(level, state, pos, AthenaUtils.getFacingPos(upPos, direction, UrMom.LEFT), direction, ConnectionDirection.UP_LEFT);
            ConnectionType upRight = test(level, state, pos, AthenaUtils.getFacingPos(upPos, direction, UrMom.RIGHT), direction, ConnectionDirection.UP_RIGHT);
            ConnectionType downLeft = test(level, state, pos, AthenaUtils.getFacingPos(downPos, direction, UrMom.LEFT), direction, ConnectionDirection.DOWN_LEFT);
            ConnectionType downRight = test(level, state, pos, AthenaUtils.getFacingPos(downPos, direction, UrMom.RIGHT), direction, ConnectionDirection.DOWN_RIGHT);
            return getQuads(direction, up, down, left, right, upLeft, upRight, downLeft, downRight);
        }

        /// Based on [ConnectionCheck#test(ConnectionCheck, AppearanceAndTintGetter, BlockState, BlockPos, BlockPos, Direction)]
        private ConnectionType test(AppearanceAndTintGetter level, BlockState modelStateRaw, BlockPos modelPos, BlockPos fromPos, Direction direction, ConnectionDirection connectionDirection) {
            AppearanceAndTintGetter.Query query = level.query(fromPos, direction, modelStateRaw, modelPos);
            BlockState fromAppearance = query.appearance();
            if (fromAppearance.isAir()) {
                return ConnectionType.NONE;
            }
            BlockState fromState = query.state();
            BlockState self = level.getAppearance(modelStateRaw, modelPos, direction, fromState, fromPos);
            if (connectTo.test(self, fromAppearance)) {
                return FaceShape.isCompatible(self, fromAppearance, direction, connectionDirection);
            }
            return ConnectionType.NONE;
        }

        @Override
        public Map<Direction, List<AthenaQuad>> getDefaultQuads(@Nullable Direction direction) {
            if (direction == null) {
                return Object2ObjectMaps.emptyMap();
            }
            return Object2ObjectMaps.<Direction, List<AthenaQuad>>singleton(direction, getQuads(direction,
                  ConnectionType.NONE, ConnectionType.NONE,
                  ConnectionType.NONE, ConnectionType.NONE,
                  ConnectionType.NONE, ConnectionType.NONE,
                  ConnectionType.NONE, ConnectionType.NONE
            ));
        }

        @Override
        public Int2ObjectMap<Baked> getTextures(Function<Material, Baked> getter) {
            return materials.resolve(getter);
        }

        @Override
        public AthenaModelAttributes getAttributes() {
            return attributes;
        }

        private static void addClippedQuad(List<AthenaQuad> quads, AthenaQuad quad, float left, float right, float top, float bottom) {
            //In general should always be true for the quads we have
            if (left < right && bottom < top) {
                quads.add(new AthenaQuad(quad.sprite(), left, right, top, bottom, quad.rotation(), quad.depth()));
            }
        }

        private static CtmState constructState(boolean recessed, ConnectionType up, ConnectionType down, ConnectionType left, ConnectionType right,
              ConnectionType upLeft, ConnectionType upRight, ConnectionType downLeft, ConnectionType downRight) {
            return new CtmState(up.check(recessed), down.check(recessed), left.check(recessed), right.check(recessed), upLeft.check(recessed), upRight.check(recessed),
                  downLeft.check(recessed), downRight.check(recessed));
        }

        private List<AthenaQuad> getQuads(Direction side, ConnectionType up, ConnectionType down, ConnectionType left, ConnectionType right,
              ConnectionType upLeft, ConnectionType upRight, ConnectionType downLeft, ConnectionType downRight) {
            FaceShape faceShape = getFaceShape(side);
            if (faceShape.topLeft().isFlush() && faceShape.topRight().isFlush() && faceShape.bottomLeft().isFlush() && faceShape.bottomRight().isFlush()) {
                if (!up.isSplit() && !down.isSplit() && !left.isSplit() && !right.isSplit()) {
                    //No quads are split so we can just use the overall combined quad
                    return textures.getQuads(side, constructState(false, up, down, left, right, upLeft, upRight, downLeft, downRight), 0);
                }
            }
            if (faceShape.topLeft().isRecessed() && faceShape.topRight().isRecessed() && faceShape.bottomLeft().isRecessed() && faceShape.bottomRight().isRecessed()) {
                if (!up.isSplit() && !down.isSplit() && !left.isSplit() && !right.isSplit()) {
                    //No quads are split so we can just use the overall combined quad
                    //All quads are recessed so handle adjust the depth to match
                    return textures.getQuads(side, constructState(true, up, down, left, right, upLeft, upRight, downLeft, downRight), 0.5F);
                }
            }
            List<AthenaQuad> clipped = new ArrayList<>();
            boolean topLeftHandled = false;
            boolean topRightHandled = false;
            boolean bottomLeftHandled = false;
            boolean bottomRightHandled = false;
            //Either both are flush or both are recessed
            boolean joinedBottom = faceShape.bottomLeft().isPresent() && faceShape.bottomLeft() == faceShape.bottomRight() && !down.isSplit();
            boolean joinedTop = faceShape.topLeft().isPresent() && faceShape.topLeft() == faceShape.topRight() && !up.isSplit();
            if (joinedBottom || joinedTop) {
                if (joinedBottom) {
                    addBottomQuads(clipped, side, faceShape.bottomLeft().isRecessed(), up, down, left, right, upLeft, upRight, downLeft, downRight);
                    bottomLeftHandled = true;
                    bottomRightHandled = true;
                }
                if (joinedTop) {
                    addTopQuads(clipped, side, faceShape.topLeft().isRecessed(), up, down, left, right, upLeft, upRight, downLeft, downRight);
                    topLeftHandled = true;
                    topRightHandled = true;
                }
            } else {
                //If our neither our top and bottom are joined, see if we have a vertical join
                if (faceShape.topLeft().isPresent() && faceShape.topLeft() == faceShape.bottomLeft() && !left.isSplit()) {
                    addLeftQuads(clipped, side, faceShape.topLeft().isRecessed(), up, down, left, right, upLeft, upRight, downLeft, downRight);
                    topLeftHandled = true;
                    bottomLeftHandled = true;
                }
                if (faceShape.topRight().isPresent() && faceShape.topRight() == faceShape.bottomRight() && !right.isSplit()) {
                    addRightQuads(clipped, side, faceShape.topRight().isRecessed(), up, down, left, right, upLeft, upRight, downLeft, downRight);
                    topRightHandled = true;
                    bottomRightHandled = true;
                }
            }
            //Add any remaining unhandled individual faces
            if (!topLeftHandled && faceShape.topLeft().isPresent()) {
                addTopLeftQuads(clipped, side, faceShape.topLeft().isRecessed(), up, down, left, right, upLeft, upRight, downLeft, downRight);
            }
            if (!topRightHandled && faceShape.topRight().isPresent()) {
                addTopRightQuads(clipped, side, faceShape.topRight().isRecessed(), up, down, left, right, upLeft, upRight, downLeft, downRight);
            }
            if (!bottomLeftHandled && faceShape.bottomLeft().isPresent()) {
                addBottomLeftQuads(clipped, side, faceShape.bottomLeft().isRecessed(), up, down, left, right, upLeft, upRight, downLeft, downRight);
            }
            if (!bottomRightHandled && faceShape.bottomRight().isPresent()) {
                addBottomRightQuads(clipped, side, faceShape.bottomRight().isRecessed(), up, down, left, right, upLeft, upRight, downLeft, downRight);
            }
            return clipped.isEmpty() ? Collections.emptyList() : clipped;
        }

        private void addTopQuads(List<AthenaQuad> clipped, Direction side, boolean recessed, ConnectionType up, ConnectionType down, ConnectionType left, ConnectionType right,
              ConnectionType upLeft, ConnectionType upRight, ConnectionType downLeft, ConnectionType downRight) {
            ConnectionType topLeft = recessed ? ConnectionType.TOP_LEFT_RECESSED : ConnectionType.TOP_LEFT_FLUSH;
            ConnectionType topRight = recessed ? ConnectionType.TOP_RIGHT_RECESSED : ConnectionType.TOP_RIGHT_FLUSH;
            for (AthenaQuad quad : textures.getQuads(side, new CtmState(
                  up.check(recessed) || up == topLeft || up == topRight,
                  down.check(recessed),
                  left.check(recessed) || left == topLeft,
                  right.check(recessed) || right == topRight,
                  upLeft.check(recessed), upRight.check(recessed), downLeft.check(recessed), downRight.check(recessed)), recessed ? 0.5F : 0)) {
                addClippedQuad(clipped, quad, quad.left(), quad.right(), quad.top(), Math.max(quad.bottom(), 0.5F));
            }
        }

        private void addBottomQuads(List<AthenaQuad> clipped, Direction side, boolean recessed, ConnectionType up, ConnectionType down, ConnectionType left, ConnectionType right,
              ConnectionType upLeft, ConnectionType upRight, ConnectionType downLeft, ConnectionType downRight) {
            ConnectionType bottomLeft = recessed ? ConnectionType.BOTTOM_LEFT_RECESSED : ConnectionType.BOTTOM_LEFT_FLUSH;
            ConnectionType bottomRight = recessed ? ConnectionType.BOTTOM_RIGHT_RECESSED : ConnectionType.BOTTOM_RIGHT_FLUSH;
            for (AthenaQuad quad : textures.getQuads(side, new CtmState(
                  up.check(recessed),
                  down.check(recessed) || down == bottomLeft || down == bottomRight,
                  left.check(recessed) || left == bottomLeft,
                  right.check(recessed) || right == bottomRight,
                  upLeft.check(recessed), upRight.check(recessed), downLeft.check(recessed), downRight.check(recessed)), recessed ? 0.5F : 0)) {
                addClippedQuad(clipped, quad, quad.left(), quad.right(), Math.min(quad.top(), 0.5F), quad.bottom());
            }
        }

        private void addLeftQuads(List<AthenaQuad> clipped, Direction side, boolean recessed, ConnectionType up, ConnectionType down, ConnectionType left, ConnectionType right,
              ConnectionType upLeft, ConnectionType upRight, ConnectionType downLeft, ConnectionType downRight) {
            ConnectionType topLeft = recessed ? ConnectionType.TOP_LEFT_RECESSED : ConnectionType.TOP_LEFT_FLUSH;
            ConnectionType bottomLeft = recessed ? ConnectionType.BOTTOM_LEFT_RECESSED : ConnectionType.BOTTOM_LEFT_FLUSH;
            for (AthenaQuad quad : textures.getQuads(side, new CtmState(
                  up.check(recessed) || up == topLeft,
                  down.check(recessed) || down == bottomLeft,
                  left.check(recessed) || left == topLeft || left == bottomLeft,
                  right.check(recessed),
                  upLeft.check(recessed), upRight.check(recessed), downLeft.check(recessed), downRight.check(recessed)), recessed ? 0.5F : 0)) {
                addClippedQuad(clipped, quad, quad.left(), Math.min(quad.right(), 0.5F), quad.top(), quad.bottom());
            }
        }

        private void addRightQuads(List<AthenaQuad> clipped, Direction side, boolean recessed, ConnectionType up, ConnectionType down, ConnectionType left, ConnectionType right,
              ConnectionType upLeft, ConnectionType upRight, ConnectionType downLeft, ConnectionType downRight) {
            ConnectionType topRight = recessed ? ConnectionType.TOP_RIGHT_RECESSED : ConnectionType.TOP_RIGHT_FLUSH;
            ConnectionType bottomRight = recessed ? ConnectionType.BOTTOM_RIGHT_RECESSED : ConnectionType.BOTTOM_RIGHT_FLUSH;
            for (AthenaQuad quad : textures.getQuads(side, new CtmState(
                  up.check(recessed) || up == topRight,
                  down.check(recessed) || down == bottomRight,
                  left.check(recessed),
                  right.check(recessed) || right == topRight || right == bottomRight,
                  upLeft.check(recessed), upRight.check(recessed), downLeft.check(recessed), downRight.check(recessed)), recessed ? 0.5F : 0)) {
                addClippedQuad(clipped, quad, Math.max(quad.left(), 0.5F), quad.right(), quad.top(), quad.bottom());
            }
        }

        private void addTopLeftQuads(List<AthenaQuad> clipped, Direction side, boolean recessed, ConnectionType up, ConnectionType down, ConnectionType left, ConnectionType right,
              ConnectionType upLeft, ConnectionType upRight, ConnectionType downLeft, ConnectionType downRight) {
            ConnectionType topLeft = recessed ? ConnectionType.TOP_LEFT_RECESSED : ConnectionType.TOP_LEFT_FLUSH;
            for (AthenaQuad quad : textures.getQuads(side, new CtmState(
                  up.check(recessed) || up == topLeft,
                  down.check(recessed),
                  left.check(recessed) || left == topLeft,
                  right.check(recessed),
                  upLeft.check(recessed), upRight.check(recessed), downLeft.check(recessed), downRight.check(recessed)), recessed ? 0.5F : 0)) {
                addClippedQuad(clipped, quad, quad.left(), Math.min(quad.right(), 0.5F), quad.top(), Math.max(quad.bottom(), 0.5F));
            }
        }

        private void addTopRightQuads(List<AthenaQuad> clipped, Direction side, boolean recessed, ConnectionType up, ConnectionType down, ConnectionType left, ConnectionType right,
              ConnectionType upLeft, ConnectionType upRight, ConnectionType downLeft, ConnectionType downRight) {
            ConnectionType topRight = recessed ? ConnectionType.TOP_RIGHT_RECESSED : ConnectionType.TOP_RIGHT_FLUSH;
            for (AthenaQuad quad : textures.getQuads(side, new CtmState(
                  up.check(recessed) || up == topRight,
                  down.check(recessed),
                  left.check(recessed),
                  right.check(recessed) || right == topRight,
                  upLeft.check(recessed), upRight.check(recessed), downLeft.check(recessed), downRight.check(recessed)), recessed ? 0.5F : 0)) {
                addClippedQuad(clipped, quad, Math.max(quad.left(), 0.5F), quad.right(), quad.top(), Math.max(quad.bottom(), 0.5F));
            }
        }

        private void addBottomLeftQuads(List<AthenaQuad> clipped, Direction side, boolean recessed, ConnectionType up, ConnectionType down, ConnectionType left, ConnectionType right,
              ConnectionType upLeft, ConnectionType upRight, ConnectionType downLeft, ConnectionType downRight) {
            ConnectionType bottomLeft = recessed ? ConnectionType.BOTTOM_LEFT_RECESSED : ConnectionType.BOTTOM_LEFT_FLUSH;
            for (AthenaQuad quad : textures.getQuads(side, new CtmState(
                  up.check(recessed),
                  down.check(recessed) || down == bottomLeft,
                  left.check(recessed) || left == bottomLeft,
                  right.check(recessed),
                  upLeft.check(recessed), upRight.check(recessed), downLeft.check(recessed), downRight.check(recessed)), recessed ? 0.5F : 0)) {
                addClippedQuad(clipped, quad, quad.left(), Math.min(quad.right(), 0.5F), Math.min(quad.top(), 0.5F), quad.bottom());
            }
        }

        private void addBottomRightQuads(List<AthenaQuad> clipped, Direction side, boolean recessed, ConnectionType up, ConnectionType down, ConnectionType left, ConnectionType right,
              ConnectionType upLeft, ConnectionType upRight, ConnectionType downLeft, ConnectionType downRight) {
            ConnectionType bottomRight = recessed ? ConnectionType.BOTTOM_RIGHT_RECESSED : ConnectionType.BOTTOM_RIGHT_FLUSH;
            for (AthenaQuad quad : textures.getQuads(side, new CtmState(
                  up.check(recessed),
                  down.check(recessed) || down == bottomRight,
                  left.check(recessed),
                  right.check(recessed) || right == bottomRight,
                  upLeft.check(recessed), upRight.check(recessed), downLeft.check(recessed), downRight.check(recessed)), recessed ? 0.5F : 0)) {
                addClippedQuad(clipped, quad, Math.max(quad.left(), 0.5F), quad.right(), Math.min(quad.top(), 0.5F), quad.bottom());
            }
        }
    }
}