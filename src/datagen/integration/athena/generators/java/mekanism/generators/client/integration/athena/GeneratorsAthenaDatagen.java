package mekanism.generators.client.integration.athena;

import mekanism.client.integration.athena.IMekAthenaDatagen;
import mekanism.generators.common.MekanismGenerators;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public class GeneratorsAthenaDatagen implements IMekAthenaDatagen {

    @Override
    public DataProvider modelProvider(PackOutput output) {
        return new GeneratorsAthenaProvider(output);
    }

    @Override
    public String modid() {
        return MekanismGenerators.MODID;
    }
}