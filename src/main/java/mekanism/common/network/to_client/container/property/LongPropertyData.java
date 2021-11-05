package mekanism.common.network.to_client.container.property;

import mekanism.common.inventory.container.MekanismContainer;
import net.minecraft.network.PacketBuffer;

public class LongPropertyData extends PropertyData {

    private final long value;

    public LongPropertyData(short property, long value) {
        super(PropertyType.LONG, property);
        this.value = value;
    }

    @Override
    public void handleWindowProperty(MekanismContainer container) {
        container.handleWindowProperty(getProperty(), value);
    }

    @Override
    public void writeToPacket(PacketBuffer buffer, boolean skipType) {
        super.writeToPacket(buffer, skipType);
        buffer.writeVarLong(value);
    }
}