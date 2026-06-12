package mekanism.common.component.containers.heat;

import mekanism.api.SerializationConstants;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.common.component.containers.SimpleComponentBackedContainer;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.component.containers.type.HeatContainerType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class ComponentBackedHeatCapacitor extends SimpleComponentBackedContainer<HeatCapacitorData> implements IHeatCapacitor {

    private final double inverseConductionCoefficient;
    private final double inverseInsulationCoefficient;
    private final HeatCapacitorData defaultData;

    public ComponentBackedHeatCapacitor(ItemAccess attachedAccess, double inverseConductionCoefficient, double inverseInsulationCoefficient,
          double defaultHeatCapacity) {
        super(attachedAccess);
        this.inverseConductionCoefficient = inverseConductionCoefficient;
        this.inverseInsulationCoefficient = inverseInsulationCoefficient;
        this.defaultData = new HeatCapacitorData(defaultHeatCapacity);
    }

    protected boolean setContents(HeatCapacitorData old, HeatCapacitorData value) {
        return setContents(attached, value, null, true);
    }

    @Override
    protected boolean isEmpty(HeatCapacitorData value) {
        return value.equals(defaultData);
    }

    @Override
    protected HeatContainerType containerType() {
        return ContainerType.HEAT;
    }

    @Override
    public double getTemperature() {
        return getAttached().temperature();
    }

    @Override
    public double getInverseConduction() {
        return inverseConductionCoefficient;
    }

    @Override
    public double getInverseInsulation() {
        return inverseInsulationCoefficient;
    }

    @Override
    public double getHeatCapacity() {
        return getAttached().capacity();
    }

    @Override
    public double getHeat() {
        return getAttached().heatOrAmbient();
    }

    @Override
    public void setHeat(double heat) {
        HeatCapacitorData data = getAttached();
        setContents(data, data.withHeat(heat));
    }

    @Override
    public void handleHeat(double transfer) {
        if (transfer != 0 && Math.abs(transfer) > HeatAPI.EPSILON) {
            HeatCapacitorData stored = getAttached();
            setContents(stored, stored.withHeat(stored.heatOrAmbient() + transfer));
        }
    }

    @Override
    public boolean isAmbientTemperature() {
        return getAttached().heat().isEmpty();
    }

    @Override
    public void copyContents(IHeatCapacitor other, @Nullable TransactionContext transaction) {
        HeatCapacitorData attachedHeat = getAttached();
        setContents(attachedHeat, new HeatCapacitorData(other.getHeat(), other.getHeatCapacity()), transaction, true);
    }

    @Override
    public void serialize(ValueOutput output) {
        HeatCapacitorData data = getAttached();
        if (data.heat().isPresent()) {
            output.putDouble(SerializationConstants.STORED, data.heat().getAsDouble());
        }
        output.putDouble(SerializationConstants.HEAT_CAPACITY, data.capacity());
    }

    @Override
    public void deserialize(ValueInput input) {
        HeatCapacitorData data;
        double capacity = input.getDoubleOr(SerializationConstants.HEAT_CAPACITY, defaultData.capacity());
        double stored = input.getDoubleOr(SerializationConstants.STORED, -1);
        if (stored == -1) {
            data = new HeatCapacitorData(capacity);
        } else {
            data = new HeatCapacitorData(stored, capacity);
        }
        setContents(getAttached(), data);
    }
}