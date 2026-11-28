package mekanism.generators.common;

import mekanism.api.MekanismRegistries;
import mekanism.api.chemical.Chemical;
import mekanism.common.Mekanism;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockItemTagId;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class GeneratorTags {

    private GeneratorTags() {
    }

    public static class Blocks {

        private Blocks() {
        }

        public static final TagKey<Block> CT_FISSION = connectionTag("fission");
        public static final TagKey<Block> CT_FUSION = connectionTag("fusion");
        public static final TagKey<Block> CT_REACTOR_GLASS = connectionTag("reactor_glass");
        public static final TagKey<Block> CT_TURBINE = connectionTag("turbine");

        private static TagKey<Block> connectionTag(String name) {
            return BlockTags.create(MekanismGenerators.rl("connected_texture_connection/" + name));
        }
    }

    public static class BlockItems {

        private BlockItems() {
        }

        public static final BlockItemTagId STRUCTURES_TURBINE = tag("structures/turbine");
        public static final BlockItemTagId STRUCTURES_REACTORS = tag("structures/reactors");
        public static final BlockItemTagId STRUCTURES_FISSION = tag("structures/reactors/fission");
        public static final BlockItemTagId STRUCTURES_FUSION = tag("structures/reactors/fusion");

        private static BlockItemTagId tag(String name) {
            return tag(MekanismGenerators.rl(name));
        }

        private static BlockItemTagId tag(Identifier tag) {
            return BlockItemTagId.create(tag, tag);
        }
    }

    public static class Fluids {

        private Fluids() {
        }

        public static final TagKey<Fluid> BIOETHANOL = commonTag("bioethanol");
        public static final TagKey<Fluid> DEUTERIUM = commonTag("deuterium");
        public static final TagKey<Fluid> FUSION_FUEL = commonTag("fusion_fuel");
        public static final TagKey<Fluid> TRITIUM = commonTag("tritium");

        private static TagKey<Fluid> commonTag(String name) {
            return FluidTags.create(Identifier.fromNamespaceAndPath("c", name));
        }
    }

    public static class Chemicals {

        private Chemicals() {
        }

        public static final TagKey<Chemical> DEUTERIUM = tag("deuterium");
        public static final TagKey<Chemical> TRITIUM = tag("tritium");
        public static final TagKey<Chemical> FUSION_FUEL = tag("fusion_fuel");

        private static TagKey<Chemical> tag(String name) {
            return TagKey.create(MekanismRegistries.Keys.CHEMICAL, Mekanism.rl(name));
        }
    }
}