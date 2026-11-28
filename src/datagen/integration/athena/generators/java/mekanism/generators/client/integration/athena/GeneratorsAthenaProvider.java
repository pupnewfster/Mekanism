package mekanism.generators.client.integration.athena;

import com.google.common.collect.ImmutableMap;
import earth.terrarium.athena.api.client.models.AthenaModelAttributes;
import earth.terrarium.athena.api.client.utils.ModelConnectionCondition;
import earth.terrarium.athena.impl.client.models.ConnectedBlockModel;
import earth.terrarium.athena.impl.client.models.MultiVariantBlockModel;
import earth.terrarium.athena.impl.client.models.ctm.ConnectedTextureMap.DirectionalCtmProviders;
import earth.terrarium.athena.impl.client.models.ctm.CtmProvider;
import java.util.Optional;
import mekanism.client.integration.athena.BaseAthenaModelProvider;
import mekanism.common.block.attribute.AttributeStateActive;
import mekanism.generators.common.GeneratorTags;
import mekanism.generators.common.MekanismGenerators;
import mekanism.generators.common.block.attribute.AttributeStateFissionPortMode;
import mekanism.generators.common.block.attribute.AttributeStateFissionPortMode.FissionPortMode;
import mekanism.generators.common.registries.GeneratorsBlocks;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.util.random.WeightedList;

public class GeneratorsAthenaProvider extends BaseAthenaModelProvider {

    protected GeneratorsAthenaProvider(PackOutput output) {
        super(output, MekanismGenerators.MODID);
    }

    @Override
    public void gather() {//TODO - 26.3: We want the equivalent of CTM's connect_inside = false for all of these
        addSimpleConnected(GeneratorsBlocks.ELECTROMAGNETIC_COIL);
        addSimpleConnected(GeneratorsBlocks.SATURATING_CONDENSER);
        addSimpleConnected(GeneratorsBlocks.TURBINE_CASING, GeneratorTags.Blocks.CT_TURBINE);
        addSimpleConnected(GeneratorsBlocks.TURBINE_VALVE, GeneratorTags.Blocks.CT_TURBINE);
        addSimpleConnected(GeneratorsBlocks.TURBINE_VENT, GeneratorTags.Blocks.CT_TURBINE);

        addSimpleConnected(GeneratorsBlocks.REACTOR_GLASS, GeneratorTags.Blocks.CT_REACTOR_GLASS);
        addSimpleConnected(GeneratorsBlocks.LASER_FOCUS_MATRIX, GeneratorTags.Blocks.CT_REACTOR_GLASS);
        addFusionController();
        addSimpleConnected(GeneratorsBlocks.FUSION_REACTOR_FRAME, GeneratorTags.Blocks.CT_FUSION);
        addSimpleConnected(GeneratorsBlocks.FUSION_REACTOR_LOGIC_ADAPTER, GeneratorTags.Blocks.CT_FUSION);
        addMultiLayerMultivariant(GeneratorsBlocks.FUSION_REACTOR_PORT, AttributeStateActive.activeProperty, ImmutableMap.of(
              false, MultiLayerTexture.simpleLed("fusion_reactor_port"),
              true, MultiLayerTexture.simpleLed("fusion_reactor_port_output")
        ), GeneratorTags.Blocks.CT_FUSION);

        addSimpleConnected(GeneratorsBlocks.FISSION_REACTOR_CASING, GeneratorTags.Blocks.CT_FISSION);
        addSimpleConnected(GeneratorsBlocks.FISSION_REACTOR_LOGIC_ADAPTER, GeneratorTags.Blocks.CT_FISSION);
        //TODO: LEDs
        addBasicMultivariant(GeneratorsBlocks.FISSION_REACTOR_PORT, AttributeStateFissionPortMode.modeProperty, ImmutableMap.of(
              FissionPortMode.INPUT, "fission_reactor_port_input",
              FissionPortMode.OUTPUT_COOLANT, "fission_reactor_port_output_coolant",
              FissionPortMode.OUTPUT_WASTE, "fission_reactor_port_output_waste"
        ), GeneratorTags.Blocks.CT_FISSION);
    }

    private void addFusionController() {
        CtmProvider.Type frameProviderType = fourSliceType(texture(GeneratorsBlocks.FUSION_REACTOR_FRAME));
        ModelConnectionCondition controllerConnectsTo = new ModelConnectionCondition.Tag(GeneratorTags.Blocks.CT_FUSION);
        add(GeneratorsBlocks.FUSION_REACTOR_CONTROLLER, new MultiVariantBlockModel(ImmutableMap.of(
              PropertyValueList.of(AttributeStateActive.activeProperty.value(false)).getKey(), WeightedList.of(new ConnectedBlockModel(new DirectionalCtmProviders(ImmutableMap.of(
                    Optional.empty(), frameProviderType,
                    Optional.of(Direction.UP), fourSliceType("fusion_reactor_controller")
              )), controllerConnectsTo, AthenaModelAttributes.EMPTY)),
              PropertyValueList.of(AttributeStateActive.activeProperty.value(true)).getKey(), WeightedList.of(new ConnectedBlockModel(new DirectionalCtmProviders(ImmutableMap.of(
                    Optional.empty(), frameProviderType,
                    Optional.of(Direction.UP), fourSliceType("fusion_reactor_controller_on")
              )), controllerConnectsTo, AthenaModelAttributes.EMPTY))
        )));
    }
}