package mekanism.common.network.to_client.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.to_client.container.property.PropertyData;
import mekanism.common.network.to_client.container.property.PropertyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class PacketUpdateContainer implements IMekanismPacket {

    //We set our compression threshold at needing more than two elements of the same type,
    // as if we only have two, then we would end up with the same total size as uncompressed
    // due to having to write the size of how many types we have of the data so we may as well
    // just skip compressing it in that situation
    private static final int COMPRESS_THRESHOLD = 2;

    public static PacketUpdateContainer create(short windowId, List<PropertyData> data) {
        if (data.size() > COMPRESS_THRESHOLD) {
            //Only try compressing if we have enough elements that if they are the same type,
            // we would have enough elements to compress them
            boolean shouldCompress = false;
            Map<PropertyType, List<PropertyData>> dataByType = new EnumMap<>(PropertyType.class);
            //TODO: Should we allow the data to mark whether or not it can be compressed???
            // The bigger issue (and why this is put on hold for now), is due to some things
            // might care about the order things are read in
            //Convert the data into a map of type to data of that type so that we can easier filter it down
            for (PropertyData propertyData : data) {
                List<PropertyData> dataForType = dataByType.computeIfAbsent(propertyData.getType(), type -> new ArrayList<>());
                dataForType.add(propertyData);
                if (!shouldCompress && dataForType.size() > COMPRESS_THRESHOLD) {
                    shouldCompress = true;
                }
            }
            if (shouldCompress) {
                //If we have at least one type of data we are going to be compressing
                List<PropertyData> uncompressedData = new ArrayList<>();
                for (Iterator<Map.Entry<PropertyType, List<PropertyData>>> iter = dataByType.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry<PropertyType, List<PropertyData>> entry = iter.next();
                    List<PropertyData> dataForType = entry.getValue();
                    if (dataForType.size() <= COMPRESS_THRESHOLD) {
                        //Add all the PropertyData we aren't compressing to our uncompressed data
                        // and remove it from what data we have "compressed"
                        uncompressedData.addAll(dataForType);
                        iter.remove();
                    }
                }
                return new PacketUpdateContainer(windowId, uncompressedData, dataByType);
            }
        }
        return new PacketUpdateContainer(windowId, data, Collections.emptyMap());
    }

    private final Map<PropertyType, List<PropertyData>> compressedData;
    private final List<PropertyData> data;
    //Note: windowId gets transferred over the network as an unsigned byte
    private final short windowId;

    private PacketUpdateContainer(short windowId, List<PropertyData> data, Map<PropertyType, List<PropertyData>> compressedData) {
        this.windowId = windowId;
        this.data = data;
        this.compressedData = compressedData;
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        //Ensure that the container is one of ours and that the window id is the same as we expect it to be
        if (player != null && player.containerMenu instanceof MekanismContainer && player.containerMenu.containerId == windowId) {
            //If so then handle the packet
            data.forEach(data -> data.handleWindowProperty((MekanismContainer) player.containerMenu));
        }
    }

    @Override
    public void encode(PacketBuffer buffer) {
        buffer.writeByte(windowId);
        buffer.writeVarInt(data.size());
        for (PropertyData data : data) {
            data.writeToPacket(buffer, false);
        }
        //Write compressed data in the form of: Property type - data of that type
        buffer.writeVarInt(compressedData.size());
        for (Map.Entry<PropertyType, List<PropertyData>> entry : compressedData.entrySet()) {
            buffer.writeEnum(entry.getKey());
            List<PropertyData> dataForType = entry.getValue();
            buffer.writeVarInt(dataForType.size());
            for (PropertyData data : dataForType) {
                data.writeToPacket(buffer, true);
            }
        }
    }

    public static PacketUpdateContainer decode(PacketBuffer buffer) {
        short windowId = buffer.readUnsignedByte();
        int size = buffer.readVarInt();
        List<PropertyData> data = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            PropertyData propertyData = PropertyData.fromBuffer(buffer);
            if (propertyData != null) {
                data.add(propertyData);
            }
        }
        //Read compressed data in the form of: Property type - data of that type
        // Note: We read this into the normal property data so that it is easier
        // to loop it when applying
        int compressedSize = buffer.readVarInt();
        for (int i = 0; i < compressedSize; i++) {
            PropertyType type = buffer.readEnum(PropertyType.class);
            int numberOfType = buffer.readVarInt();
            for (int j = 0; j < numberOfType; j++) {
                short property = buffer.readShort();
                PropertyData propertyData = type.createData(property, buffer);
                if (propertyData != null) {
                    data.add(propertyData);
                }
            }
        }
        return new PacketUpdateContainer(windowId, data, Collections.emptyMap());
    }
}