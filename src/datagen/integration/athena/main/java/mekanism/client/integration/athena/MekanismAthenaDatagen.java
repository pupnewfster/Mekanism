package mekanism.client.integration.athena;

import mekanism.common.Mekanism;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public class MekanismAthenaDatagen implements IMekAthenaDatagen {

    @Override
    public DataProvider modelProvider(PackOutput output) {
        return new MekanismAthenaProvider(output);
    }

    @Override
    public String modid() {
        return Mekanism.MODID;
    }
}