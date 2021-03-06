package mekanism.client.gui.robit;

import com.mojang.blaze3d.matrix.MatrixStack;
import javax.annotation.Nonnull;
import mekanism.client.SpecialColors;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiSideHolder;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.window.GuiRobitRename;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.entity.EntityRobit;
import mekanism.common.inventory.container.entity.robit.MainRobitContainer;
import mekanism.common.network.to_server.PacketGuiButtonPress;
import mekanism.common.network.to_server.PacketGuiButtonPress.ClickedEntityButton;
import mekanism.common.network.to_server.PacketRobit;
import mekanism.common.network.to_server.PacketRobit.RobitPacketType;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class GuiRobitMain extends GuiMekanism<MainRobitContainer> {

    private final EntityRobit robit;
    private MekanismImageButton renameButton;

    public GuiRobitMain(MainRobitContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        robit = container.getEntity();
        dynamicSlots = true;
        titleLabelX = 76;
    }

    @Override
    public void init() {
        super.init();
        addButton(new GuiSecurityTab(this, robit, 120));
        addButton(GuiSideHolder.create(this, 176, 6, 106, false, false, SpecialColors.TAB_ROBIT_MENU));
        addButton(new GuiInnerScreen(this, 27, 16, 122, 56));
        addButton(new GuiHorizontalPowerBar(this, robit.getEnergyContainer(), 27, 74, 120));
        addButton(new MekanismImageButton(this, leftPos + 6, topPos + 16, 18, getButtonLocation("home"), () -> {
            Mekanism.packetHandler.sendToServer(new PacketRobit(RobitPacketType.GO_HOME, robit.getId()));
            getMinecraft().setScreen(null);
        }, getOnHover(MekanismLang.ROBIT_TELEPORT)));
        addButton(new MekanismImageButton(this, leftPos + 6, topPos + 35, 18, getButtonLocation("drop"),
              () -> Mekanism.packetHandler.sendToServer(new PacketRobit(RobitPacketType.DROP_PICKUP, robit.getId())),
              getOnHover(MekanismLang.ROBIT_TOGGLE_PICKUP)));
        renameButton = addButton(new MekanismImageButton(this, leftPos + 6, topPos + 54, 18, getButtonLocation("rename"), () -> {
            GuiWindow window = new GuiRobitRename(this, 27, 16, robit);
            window.setListenerTab(() -> renameButton);
            renameButton.active = false;
            addWindow(window);
        }, getOnHover(MekanismLang.ROBIT_RENAME)));
        addButton(new MekanismImageButton(this, leftPos + 152, topPos + 54, 18, getButtonLocation("follow"),
              () -> Mekanism.packetHandler.sendToServer(new PacketRobit(RobitPacketType.FOLLOW, robit.getId())),
              getOnHover(MekanismLang.ROBIT_TOGGLE_FOLLOW)));
        addButton(new MekanismImageButton(this, leftPos + 179, topPos + 10, 18, getButtonLocation("main"), () -> {
            //Clicking main button doesn't do anything while already on the main GUI
        }, getOnHover(MekanismLang.ROBIT)));
        addButton(new MekanismImageButton(this, leftPos + 179, topPos + 30, 18, getButtonLocation("crafting"),
              () -> Mekanism.packetHandler.sendToServer(new PacketGuiButtonPress(ClickedEntityButton.ROBIT_CRAFTING, robit.getId())),
              getOnHover(MekanismLang.ROBIT_CRAFTING)));
        addButton(new MekanismImageButton(this, leftPos + 179, topPos + 50, 18, getButtonLocation("inventory"),
              () -> Mekanism.packetHandler.sendToServer(new PacketGuiButtonPress(ClickedEntityButton.ROBIT_INVENTORY, robit.getId())),
              getOnHover(MekanismLang.ROBIT_INVENTORY)));
        addButton(new MekanismImageButton(this, leftPos + 179, topPos + 70, 18, getButtonLocation("smelting"),
              () -> Mekanism.packetHandler.sendToServer(new PacketGuiButtonPress(ClickedEntityButton.ROBIT_SMELTING, robit.getId())),
              getOnHover(MekanismLang.ROBIT_SMELTING)));
        addButton(new MekanismImageButton(this, leftPos + 179, topPos + 90, 18, getButtonLocation("repair"),
              () -> Mekanism.packetHandler.sendToServer(new PacketGuiButtonPress(ClickedEntityButton.ROBIT_REPAIR, robit.getId())),
              getOnHover(MekanismLang.ROBIT_REPAIR)));
    }

    @Override
    protected void drawForegroundText(@Nonnull MatrixStack matrix, int mouseX, int mouseY) {
        drawString(matrix, MekanismLang.ROBIT.translate(), titleLabelX, titleLabelY, titleTextColor());
        drawTextScaledBound(matrix, MekanismLang.ROBIT_GREETING.translate(robit.getName()), 29, 18, screenTextColor(), 119);
        drawTextScaledBound(matrix, MekanismLang.ENERGY.translate(EnergyDisplay.of(robit.getEnergyContainer().getEnergy(), robit.getEnergyContainer().getMaxEnergy())), 29, 36 - 4, screenTextColor(), 119);
        drawTextScaledBound(matrix, MekanismLang.ROBIT_FOLLOWING.translate(robit.getFollowing()), 29, 45 - 4, screenTextColor(), 119);
        drawTextScaledBound(matrix, MekanismLang.ROBIT_DROP_PICKUP.translate(robit.getDropPickup()), 29, 54 - 4, screenTextColor(), 119);
        CharSequence owner = robit.getOwnerName().length() > 14 ? robit.getOwnerName().subSequence(0, 14) : robit.getOwnerName();
        drawTextScaledBound(matrix, MekanismLang.ROBIT_OWNER.translate(owner), 29, 63 - 4, screenTextColor(), 119);
        super.drawForegroundText(matrix, mouseX, mouseY);
    }
}