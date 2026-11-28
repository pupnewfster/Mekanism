package mekanism.additions.client.integration.athena;

import earth.terrarium.athena.api.client.models.TintProvider;
import earth.terrarium.athena.api.client.utils.ModelConnectionCondition.SameState;
import mekanism.additions.common.AdditionsTags;
import mekanism.additions.common.MekanismAdditions;
import mekanism.additions.common.registries.AdditionsBlocks;
import mekanism.api.text.EnumColor;
import mekanism.api.text.EnumColorCollection;
import mekanism.client.integration.athena.BaseAthenaModelProvider;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class AdditionsAthenaProvider extends BaseAthenaModelProvider {

    protected AdditionsAthenaProvider(PackOutput output) {
        super(output, MekanismAdditions.MODID);
    }

    @Override
    public void gather() {
        addSimplePlastic(AdditionsBlocks.PLASTIC_ROADS, "plastic_road");
        addSimplePlastic(AdditionsBlocks.REINFORCED_PLASTIC_BLOCKS, "reinforced_plastic_block");
        addSimplePlastic(AdditionsBlocks.SLICK_PLASTIC_BLOCKS, "slick_plastic_block");
        for (EnumColor color : EnumColor.VALUES) {
            addConnectedBlockSlabStairs("plastic_block", color, AdditionsBlocks.PLASTIC_BLOCKS, AdditionsBlocks.PLASTIC_SLABS, AdditionsBlocks.PLASTIC_STAIRS, AdditionsTags.Blocks.CT_PLASTIC_PLASTIC);
            addConnectedBlockSlabStairs("plastic_glow_block", color, AdditionsBlocks.PLASTIC_GLOW_BLOCKS, AdditionsBlocks.PLASTIC_GLOW_SLABS, AdditionsBlocks.PLASTIC_GLOW_STAIRS, AdditionsTags.Blocks.CT_PLASTIC_GLOW);
            addConnectedBlockSlabStairs("transparent_plastic_block", color, AdditionsBlocks.TRANSPARENT_PLASTIC_BLOCKS, AdditionsBlocks.TRANSPARENT_PLASTIC_SLABS, AdditionsBlocks.TRANSPARENT_PLASTIC_STAIRS, AdditionsTags.Blocks.CT_PLASTIC_TRANSPARENT);
        }
    }

    private void addSimplePlastic(EnumColorCollection<? extends Holder<Block>> plastics, String texture) {
        plastics.forEach(block -> add(block, simpleConnected(texture, true, new TintProvider.Index(0), SameState.INSTANCE)));
    }

    private void addConnectedBlockSlabStairs(String texture, EnumColor color, EnumColorCollection<? extends Holder<Block>> blocks,
          EnumColorCollection<? extends Holder<Block>> slabs, EnumColorCollection<? extends Holder<Block>> stairs, EnumColorCollection<TagKey<Block>> connections) {
        addConnectedBlockSlabStairs(texture, true, blocks.pick(color), slabs.pick(color), stairs.pick(color), connections.pick(color), new TintProvider.Index(0));
    }
}