package mekanism.common.item.block.machine;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.lib.frequency.FrequencyTypes;
import mekanism.common.lib.frequency.IFrequencyItem;
import net.minecraft.world.item.Item;

public class ItemBlockTeleporter extends ItemBlockTooltip<BlockTile<?, ?>> implements IFrequencyItem {

    public ItemBlockTeleporter(BlockTile<?, ?> block, Item.Properties properties) {
        super(block, true, properties);
    }

    @Override
    public FrequencyType<?> getFrequencyType() {
        return FrequencyTypes.TELEPORTER;
    }
}