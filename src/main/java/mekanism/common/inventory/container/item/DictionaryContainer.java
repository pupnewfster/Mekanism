package mekanism.common.inventory.container.item;

import mekanism.common.item.ItemDictionary;
import mekanism.common.registries.MekanismContainerTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;

public class DictionaryContainer extends MekanismItemContainer {

    public DictionaryContainer(int id, PlayerInventory inv, Hand hand, ItemStack stack) {
        super(MekanismContainerTypes.DICTIONARY, id, inv, hand, stack);
    }

    public DictionaryContainer(int id, PlayerInventory inv, PacketBuffer buf) {
        this(id, inv, buf.readEnum(Hand.class), getStackFromBuffer(buf, ItemDictionary.class));
    }

    @Override
    protected int getInventoryYOffset() {
        return super.getInventoryYOffset() + 5;
    }
}