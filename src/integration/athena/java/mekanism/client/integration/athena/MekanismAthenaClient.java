package mekanism.client.integration.athena;

import earth.terrarium.athena.api.client.models.FactoryManager;
import mekanism.common.Mekanism;
import mekanism.common.integration.MekanismHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = Mekanism.MODID, dist = Dist.CLIENT, depends = MekanismHooks.ATHENA_MOD_ID)
public class MekanismAthenaClient {

    public MekanismAthenaClient() {
        FactoryManager.register(Mekanism.rl("block_slab_stairs_ctm"), ConnectedBlockSlabStairsModel.TYPE);
    }
}