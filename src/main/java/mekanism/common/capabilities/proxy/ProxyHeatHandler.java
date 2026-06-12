package mekanism.common.capabilities.proxy;

import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IHeatHandler;
import mekanism.api.heat.ISidedHeatHandler;
import mekanism.common.capabilities.holder.single.ISingleContainerHolder;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public class ProxyHeatHandler extends ProxyHandler<ISingleContainerHolder<IHeatCapacitor>> implements IHeatHandler {

    private final ISidedHeatHandler heatHandler;

    public ProxyHeatHandler(ISidedHeatHandler heatHandler, @Nullable Direction side, ISingleContainerHolder<IHeatCapacitor> holder) {
        super(side, holder);
        this.heatHandler = heatHandler;
    }

    @Override
    public double getTemperature() {
        return heatHandler.getTemperature(side);
    }

    @Override
    public double getInverseConduction() {
        return heatHandler.getInverseConduction(side);
    }

    @Override
    public double getHeatCapacity() {
        return heatHandler.getHeatCapacity(side);
    }

    @Override
    public void handleHeat(double transfer) {
        if (!readOnly) {
            heatHandler.handleHeat(transfer, side);
        }
    }
}
