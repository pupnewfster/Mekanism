package mekanism.client.gui.qio;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.common.Mekanism;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.inventory.container.item.QIOFrequencySelectItemContainer;
import mekanism.common.lib.frequency.Frequency.FrequencyIdentity;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.network.to_server.PacketGuiButtonPress;
import mekanism.common.network.to_server.PacketGuiButtonPress.ClickedItemButton;
import mekanism.common.network.to_server.PacketGuiSetFrequency;
import mekanism.common.network.to_server.PacketGuiSetFrequency.FrequencyUpdate;
import mekanism.common.network.to_server.PacketQIOSetColor;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class GuiQIOItemFrequencySelect extends GuiQIOFrequencySelect<QIOFrequencySelectItemContainer> {

    public GuiQIOItemFrequencySelect(QIOFrequencySelectItemContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
    }

    @Override
    public void init() {
        super.init();
        addButton(new MekanismImageButton(this, leftPos + 6, topPos + 6, 14, getButtonLocation("back"),
              () -> Mekanism.packetHandler.sendToServer(new PacketGuiButtonPress(ClickedItemButton.BACK_BUTTON, menu.getHand()))));
    }

    @Override
    public void sendSetFrequency(FrequencyIdentity identity) {
        Mekanism.packetHandler.sendToServer(PacketGuiSetFrequency.create(FrequencyUpdate.SET_ITEM, FrequencyType.QIO, identity, menu.getHand()));
    }

    @Override
    public void sendRemoveFrequency(FrequencyIdentity identity) {
        Mekanism.packetHandler.sendToServer(PacketGuiSetFrequency.create(FrequencyUpdate.REMOVE_ITEM, FrequencyType.QIO, identity, menu.getHand()));
    }

    @Override
    public void sendColorUpdate(int extra) {
        QIOFrequency freq = getFrequency();
        if (freq != null) {
            Mekanism.packetHandler.sendToServer(PacketQIOSetColor.create(menu.getHand(), freq, extra));
        }
    }

    @Nullable
    @Override
    public QIOFrequency getFrequency() {
        return menu.getFrequency();
    }

    @Override
    public String getOwnerUsername() {
        return menu.getOwnerUsername();
    }

    @Override
    public UUID getOwnerUUID() {
        return menu.getOwnerUUID();
    }

    @Override
    public List<QIOFrequency> getPublicFrequencies() {
        return menu.getPublicCache();
    }

    @Override
    public List<QIOFrequency> getPrivateFrequencies() {
        return menu.getPrivateCache();
    }
}
