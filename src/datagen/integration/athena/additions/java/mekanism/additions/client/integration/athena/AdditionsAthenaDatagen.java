package mekanism.additions.client.integration.athena;

import mekanism.additions.common.MekanismAdditions;
import mekanism.client.integration.athena.IMekAthenaDatagen;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public class AdditionsAthenaDatagen implements IMekAthenaDatagen {

    @Override
    public DataProvider modelProvider(PackOutput output) {
        return new AdditionsAthenaProvider(output);
    }

    @Override
    public String modid() {
        return MekanismAdditions.MODID;
    }
}