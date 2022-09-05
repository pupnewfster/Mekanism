package mekanism.client.render.item.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import mekanism.api.NBTConstants;
import mekanism.api.RelativeSide;
import mekanism.client.model.ModelEnergyCore;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.MekanismISTER;
import mekanism.client.render.tileentity.RenderEnergyCube;
import mekanism.common.item.block.ItemBlockEnergyCube;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.TileEntityEnergyCube.CubeSideState;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.StorageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

public class RenderEnergyCubeItem extends MekanismISTER {

    public static final RenderEnergyCubeItem RENDERER = new RenderEnergyCubeItem();
    private ModelEnergyCore core;

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        core = new ModelEnergyCore(getEntityModels());
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull TransformType transformType, @NotNull PoseStack matrix, @NotNull MultiBufferSource renderer, int light,
          int overlayLight) {
        EnergyCubeTier tier = ((ItemBlockEnergyCube) stack.getItem()).getTier();

        CubeSideState[] sideStates = new CubeSideState[EnumUtils.SIDES.length];
        CompoundTag configData = ItemDataUtils.getDataMapIfPresent(stack);
        if (configData != null && configData.contains(NBTConstants.COMPONENT_CONFIG, Tag.TAG_COMPOUND)) {
            CompoundTag sideConfig = configData.getCompound(NBTConstants.COMPONENT_CONFIG).getCompound(NBTConstants.CONFIG + TransmissionType.ENERGY.ordinal());
            //TODO: Maybe improve on this, but for now this is a decent way of making it not have disabled sides show
            for (RelativeSide side : EnumUtils.SIDES) {
                DataType dataType = DataType.byIndexStatic(sideConfig.getInt(NBTConstants.SIDE + side.ordinal()));
                CubeSideState state = CubeSideState.INACTIVE;
                if (dataType != DataType.NONE) {
                    state = dataType.canOutput() ? CubeSideState.ACTIVE_LIT : CubeSideState.ACTIVE_UNLIT;
                }
                sideStates[side.ordinal()] = state;
            }
        } else {
            for (RelativeSide side : EnumUtils.SIDES) {
                sideStates[side.ordinal()] = tier == EnergyCubeTier.CREATIVE || side == RelativeSide.FRONT ? CubeSideState.ACTIVE_LIT : CubeSideState.ACTIVE_UNLIT;
            }
        }
        ModelData modelData = ModelData.builder().with(TileEntityEnergyCube.SIDE_STATE_PROPERTY, sideStates).build();
        renderItem(stack, transformType, matrix, renderer, light, overlayLight, modelData);
        double energyPercentage = StorageUtils.getStoredEnergyFromNBT(stack).divideToLevel(tier.getMaxEnergy());
        if (energyPercentage > 0) {
            float ticks = Minecraft.getInstance().levelRenderer.ticks + MekanismRenderer.getPartialTick();
            float scaledTicks = 4 * ticks;
            matrix.pushPose();
            matrix.translate(0.5, 0.5, 0.5);
            matrix.scale(0.4F, 0.4F, 0.4F);
            matrix.translate(0, Math.sin(Math.toRadians(3 * ticks)) / 7, 0);
            matrix.mulPose(Vector3f.YP.rotationDegrees(scaledTicks));
            matrix.mulPose(RenderEnergyCube.coreVec.rotationDegrees(36F + scaledTicks));
            core.render(matrix, renderer, LightTexture.FULL_BRIGHT, overlayLight, tier.getBaseTier().getColor(), (float) energyPercentage);
            matrix.popPose();
        }
    }

    //TODO: Do this for the fluid tank and maybe other models as well
    private void renderItem(@NotNull ItemStack stack, @NotNull TransformType transformType, @NotNull PoseStack matrix, @NotNull MultiBufferSource renderer, int light,
          int overlayLight, ModelData modelData) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        boolean fabulous;
        if (transformType != ItemTransforms.TransformType.GUI && !transformType.firstPerson() && stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            fabulous = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
        } else {
            fabulous = true;
        }
        long seed = 42;
        BakedModel pModel = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(((BlockItem) stack.getItem()).getBlock().defaultBlockState());
        for (BakedModel model : pModel.getRenderPasses(stack, fabulous)) {
            for (RenderType renderType : model.getRenderTypes(stack, fabulous)) {
                VertexConsumer buffer;
                if (fabulous) {
                    buffer = ItemRenderer.getFoilBufferDirect(renderer, renderType, true, stack.hasFoil());
                } else {
                    buffer = ItemRenderer.getFoilBuffer(renderer, renderType, true, stack.hasFoil());
                }
                itemRenderer.renderModelLists(model, stack, light, overlayLight, matrix, buffer);

                RandomSource random = RandomSource.create();

                for (Direction direction : EnumUtils.DIRECTIONS) {
                    random.setSeed(seed);
                    itemRenderer.renderQuadList(matrix, buffer, pModel.getQuads(null, direction, random, modelData, renderType), stack, light, overlayLight);
                }

                random.setSeed(seed);
                itemRenderer.renderQuadList(matrix, buffer, pModel.getQuads(null, null, random, modelData, renderType), stack, light, overlayLight);
            }
        }
    }
}