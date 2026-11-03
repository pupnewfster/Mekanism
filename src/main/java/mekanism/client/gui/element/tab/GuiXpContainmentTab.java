package mekanism.client.gui.element.tab;

import mekanism.client.SpecialColors;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInsetElement;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.network.PacketUtils;
import mekanism.common.network.to_server.PacketEntityGuiInteract;
import mekanism.common.network.to_server.PacketEntityGuiInteract.GuiInteractionEntity;
import mekanism.common.network.to_server.PacketGuiInteract;
import mekanism.common.network.to_server.PacketGuiInteract.GuiInteraction;
import mekanism.common.recipe.lookup.IRecipeLookupHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GuiXpContainmentTab extends GuiInsetElement<IRecipeLookupHandler<?>> {

    private static final Identifier ICON = Mekanism.rl("xp_containment");

    public GuiXpContainmentTab(IGuiWrapper gui, IRecipeLookupHandler<?> lookupHandler, boolean left) {
        super(ICON, gui, lookupHandler, left ? -26 : gui.getImageWidth(), 90, 26, 18, left);
        setTooltip(MekanismLang.XP_CONTAINMENT_TAB);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
        if (dataSource instanceof BlockEntity tile) {
            PacketUtils.sendToServer(new PacketGuiInteract(GuiInteraction.COLLECT_XP, tile));
        } else if (dataSource instanceof Entity entity) {
            PacketUtils.sendToServer(new PacketEntityGuiInteract(GuiInteractionEntity.COLLECT_XP, entity));
        }
    }

    @Override
    protected int getTabColor() {
        return SpecialColors.TAB_XP_CONTAINMENT.argb();
    }

    @Override
    public void tick() {
        //TODO - 26.3: Sync this to the client
        visible = dataSource.hasXpContainment();
        super.tick();
    }
}