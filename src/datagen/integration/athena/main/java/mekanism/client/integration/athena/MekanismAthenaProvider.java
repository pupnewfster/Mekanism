package mekanism.client.integration.athena;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import earth.terrarium.athena.api.client.models.AthenaBlockModel;
import earth.terrarium.athena.api.client.models.AthenaModelAttributes;
import earth.terrarium.athena.api.client.utils.ModelConnectionCondition;
import earth.terrarium.athena.api.client.utils.ModelConnectionCondition.SameState;
import earth.terrarium.athena.impl.client.models.AthenaCompositeModel;
import earth.terrarium.athena.impl.client.models.ConnectedBlockModel;
import earth.terrarium.athena.impl.client.models.MultiVariantBlockModel;
import earth.terrarium.athena.impl.client.models.ctm.ConnectedTextureMap.DirectionalCtmProviders;
import earth.terrarium.athena.impl.client.models.ctm.CtmProvider;
import earth.terrarium.athena.impl.client.models.ctm.FourtySevenSliceCtmProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mekanism.common.Mekanism;
import mekanism.common.block.attribute.AttributeStateActive;
import mekanism.common.block.attribute.AttributeStateBoilerValveMode;
import mekanism.common.block.attribute.AttributeStateBoilerValveMode.BoilerValveMode;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tags.MekanismTags;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class MekanismAthenaProvider extends BaseAthenaModelProvider {

    protected MekanismAthenaProvider(PackOutput output) {
        super(output, Mekanism.MODID);
    }

    @Override
    public void gather() {//TODO - 26.3: We want the equivalent of CTM's connect_inside = false for everything but induction cells/providers, and the teleporter frame
        addSimpleConnected(MekanismBlocks.PRESSURE_DISPERSER);
        addSimpleConnected(MekanismBlocks.STEEL_CASING);
        addSimpleConnected(MekanismBlocks.STRUCTURAL_GLASS, MekanismTags.Blocks.CT_STRUCTURAL_GLASS);

        ModelConnectionCondition teleporterConnectsTo = new ModelConnectionCondition.Tag(MekanismTags.Blocks.CT_TELEPORTER);
        add(MekanismBlocks.TELEPORTER, MultiLayerTexture.simpleLed(texture(MekanismBlocks.TELEPORTER), true)
              .createModel(this, teleporterConnectsTo));
        add(MekanismBlocks.TELEPORTER_FRAME, MultiLayerTexture.simpleLed(texture(MekanismBlocks.TELEPORTER_FRAME), true)
              .createModel(this, teleporterConnectsTo));

        addSimpleConnected(MekanismBlocks.BOILER_CASING, false, MekanismTags.Blocks.CT_BOILER);
        addMultiLayerMultivariant(MekanismBlocks.BOILER_VALVE, AttributeStateBoilerValveMode.modeProperty, ImmutableMap.of(
              BoilerValveMode.INPUT, MultiLayerTexture.simpleLed("boiler_valve_input"),
              BoilerValveMode.OUTPUT_STEAM, MultiLayerTexture.simpleLed("boiler_valve_output_steam"),
              BoilerValveMode.OUTPUT_COOLANT, MultiLayerTexture.simpleLed("boiler_valve_output_coolant")
        ), MekanismTags.Blocks.CT_BOILER);
        addMultiLayerMultivariant(MekanismBlocks.SUPERHEATING_ELEMENT, AttributeStateActive.activeProperty, ImmutableMap.of(
              false, new MultiLayerTexture(texture(MekanismBlocks.SUPERHEATING_ELEMENT)),
              true, MultiLayerTexture.simpleLed("superheating_element_on", true)
        ), SameState.INSTANCE);

        addSimpleConnected(MekanismBlocks.DYNAMIC_TANK, MekanismTags.Blocks.CT_TANK);
        addSimpleConnected(MekanismBlocks.DYNAMIC_VALVE, MekanismTags.Blocks.CT_TANK);

        addEvaporation();

        addSimpleConnected(MekanismBlocks.SPS_CASING, MekanismTags.Blocks.CT_SPS);
        addMultiLayerMultivariant(MekanismBlocks.SPS_PORT, AttributeStateActive.activeProperty, ImmutableMap.of(
              false, MultiLayerTexture.simpleLed("sps_port"),
              true, MultiLayerTexture.simpleLed("sps_port_output")
        ), MekanismTags.Blocks.CT_SPS);

        addSimpleConnected(MekanismBlocks.INDUCTION_CASING, MekanismTags.Blocks.CT_MATRIX_EXTERIOR);
        addMultiLayerMultivariant(MekanismBlocks.INDUCTION_PORT, AttributeStateActive.activeProperty, ImmutableMap.of(
              false, MultiLayerTexture.simpleLed("induction_port"),
              true, MultiLayerTexture.simpleLed("induction_port_output")
        ), MekanismTags.Blocks.CT_MATRIX_EXTERIOR);

        addInductionCell(MekanismBlocks.BASIC_INDUCTION_CELL);
        addInductionCell(MekanismBlocks.ADVANCED_INDUCTION_CELL);
        addInductionCell(MekanismBlocks.ELITE_INDUCTION_CELL);
        addInductionCell(MekanismBlocks.ULTIMATE_INDUCTION_CELL);
        addInductionProvider(MekanismBlocks.BASIC_INDUCTION_PROVIDER);
        addInductionProvider(MekanismBlocks.ADVANCED_INDUCTION_PROVIDER);
        addInductionProvider(MekanismBlocks.ELITE_INDUCTION_PROVIDER);
        addInductionProvider(MekanismBlocks.ULTIMATE_INDUCTION_PROVIDER);
    }

    private void addEvaporation() {
        String frame = texture(MekanismBlocks.THERMAL_EVAPORATION_BLOCK);
        CtmProvider.Type frameProviderType = new FourtySevenSliceCtmProvider.Type(Either.right(fortySevenSlice(frame)));
        CtmProvider.Type controllerType = fourSliceType("thermal_evaporation_controller");
        CtmProvider.Type controllerLedType = fourSliceType("thermal_evaporation_controller_on_led");
        ModelConnectionCondition evaporationConnectsTo = new ModelConnectionCondition.Tag(MekanismTags.Blocks.CT_TEP);
        //Ensure consistent ordering
        Map<String, WeightedList<AthenaBlockModel>> variants = new LinkedHashMap<>();
        for (Direction direction : Plane.HORIZONTAL) {
            AthenaBlockModel controllerModel = new ConnectedBlockModel(new DirectionalCtmProviders(ImmutableMap.of(
                  Optional.empty(), frameProviderType,
                  Optional.of(direction), controllerType
            )), evaporationConnectsTo, AthenaModelAttributes.EMPTY);

            variants.put(variantKey(false, direction), WeightedList.of(controllerModel));
            //TODO - 26.3: Active controller led
            variants.put(variantKey(true, direction), WeightedList.of(controllerModel));
            /*variants.put(variantKey(true, direction), WeightedList.of(new AthenaCompositeModel(List.of(
                  controllerModel,
                  new ConnectedBlockModel(new DirectionalCtmProviders(Map.of(Optional.of(direction), controllerLedType)), evaporationConnectsTo, new AthenaModelAttributes(Optional.empty(), 15))
            ))));*/
        }
        add(MekanismBlocks.THERMAL_EVAPORATION_CONTROLLER, new MultiVariantBlockModel(variants));
        addSimpleConnected(MekanismBlocks.THERMAL_EVAPORATION_VALVE, MekanismTags.Blocks.CT_TEP);
        add(MekanismBlocks.THERMAL_EVAPORATION_BLOCK, weightedVariants(WeightedList.<AthenaBlockModel>builder()
              .add(thermalWithSimpleTop("thermal_evaporation_block", evaporationConnectsTo), 10)
              .add(thermalWithSimpleTop("thermal_evaporation_block_1", evaporationConnectsTo), 8)
              .add(simpleConnected("thermal_evaporation_block_2", false, MekanismTags.Blocks.CT_TEP), 3)
              .add(simpleConnected("thermal_evaporation_block_3", false, MekanismTags.Blocks.CT_TEP), 1)
              .build()
        ));
    }

    private String variantKey(boolean active, Direction facing) {
        return PropertyValueList.of(
              AttributeStateActive.activeProperty.value(active),
              BlockStateProperties.HORIZONTAL_FACING.value(facing)
        ).getKey();
    }

    private AthenaBlockModel thermalWithSimpleTop(String texture, ModelConnectionCondition connectTo) {
        CtmProvider.Type vertical = fourSliceType("thermal_evaporation_block_top");
        return new ConnectedBlockModel(new DirectionalCtmProviders(ImmutableMap.of(
              Optional.empty(), new FourtySevenSliceCtmProvider.Type(Either.right(fortySevenSlice(texture))),
              Optional.of(Direction.DOWN), vertical,
              Optional.of(Direction.UP), vertical
        )), connectTo, AthenaModelAttributes.EMPTY);
    }

    private void addInductionCell(Holder<Block> inductionCell) {
        add(inductionCell, new AthenaCompositeModel(List.of(
              simpleConnected(texture(inductionCell), true, MekanismTags.Blocks.CT_MATRIX_INTERIOR),
              unconnected("induction_cell_glow", 9),
              unconnected("induction_cell_led", 15)
        )));
    }

    private void addInductionProvider(Holder<Block> inductionProvider) {
        add(inductionProvider, new AthenaCompositeModel(List.of(
              simpleConnected(texture(inductionProvider), true, MekanismTags.Blocks.CT_MATRIX_INTERIOR),
              unconnected("induction_cell_glow", 9),
              unconnected(inductionProvider.key().identifier().getPath() + "_led", 15)
        )));
    }
}