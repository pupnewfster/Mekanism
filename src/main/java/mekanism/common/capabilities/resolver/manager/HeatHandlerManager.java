package mekanism.common.capabilities.resolver.manager;

import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IHeatHandler;
import mekanism.api.heat.IMekanismHeatHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.single.ISingleContainerHolder;
import mekanism.common.capabilities.proxy.ProxyHeatHandler;
import mekanism.common.capabilities.resolver.BasicSingleContainerHandlerManager;

/// Helper class to make reading instead of having as messy generics
public class HeatHandlerManager extends BasicSingleContainerHandlerManager<IHeatCapacitor, IHeatHandler> {

    public HeatHandlerManager(ISingleContainerHolder<IHeatCapacitor> holder) {
        //TODO - 26.1: Evaluate if we want to change this to be more like the other things where the handler isn't implemented by the tile itself
        super(holder, Capabilities.HEAT, (side, capacitorHolder) -> new ProxyHeatHandler((IMekanismHeatHandler) () -> capacitorHolder.getContainer(side), side, capacitorHolder));
    }
}