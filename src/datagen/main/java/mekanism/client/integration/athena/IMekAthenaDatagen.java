package mekanism.client.integration.athena;

import java.util.Map;
import mekanism.api.MekanismAPI;
import mekanism.api.ModBasedService;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public interface IMekAthenaDatagen extends ModBasedService {

    Map<String, IMekAthenaDatagen> INSTANCES = MekanismAPI.getModBasedServices(IMekAthenaDatagen.class);

    DataProvider modelProvider(PackOutput output);
}