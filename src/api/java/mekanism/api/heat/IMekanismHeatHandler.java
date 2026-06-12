package mekanism.api.heat;

import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface IMekanismHeatHandler extends ISidedHeatHandler {

    /// Returns the [IHeatCapacitor] that has the given index from the list of capacitors on the given side.
    ///
    /// @param side      The side we are interacting with the handler from (null for internal).
    ///
    /// @return The [IHeatCapacitor] that has the given index from the list of capacitors on the given side.
    @Nullable
    IHeatCapacitor getHeatCapacitor();

    //TODO - 26.1: Implement and docs?
    default boolean isAccessible(@Nullable Direction side) {
        return true;
    }

    @Override
    default double getTemperature(@Nullable Direction side) {
        IHeatCapacitor heatCapacitor = getHeatCapacitor();
        return heatCapacitor == null || !isAccessible(side) ? HeatAPI.AMBIENT_TEMP : heatCapacitor.getTemperature();
    }

    @Override
    default double getInverseConduction(@Nullable Direction side) {
        IHeatCapacitor heatCapacitor = getHeatCapacitor();
        return heatCapacitor == null || !isAccessible(side) ? HeatAPI.DEFAULT_INVERSE_CONDUCTION : heatCapacitor.getInverseConduction();
    }

    @Override
    default double getHeatCapacity(@Nullable Direction side) {
        IHeatCapacitor heatCapacitor = getHeatCapacitor();
        return heatCapacitor == null || !isAccessible(side) ? HeatAPI.DEFAULT_HEAT_CAPACITY : heatCapacitor.getHeatCapacity();
    }

    @Override
    default void handleHeat(double transfer, @Nullable Direction side) {
        IHeatCapacitor heatCapacitor = getHeatCapacitor();
        if (heatCapacitor != null && isAccessible(side)) {
            heatCapacitor.handleHeat(transfer);
        }
    }

    /// Returns the inverse insulation coefficient of a given capacitor. The larger the value the less heat dissipates into the environment.
    ///
    /// @param side      The side we are interacting with the handler from (null for internal).
    ///
    /// @return Inverse insulation coefficient of a given capacitor.
    default double getInverseInsulation(@Nullable Direction side) {
        //TODO - 26.1: Implement the override to this via a capacitor override instead of via the handler
        IHeatCapacitor heatCapacitor = getHeatCapacitor();
        return heatCapacitor == null || !isAccessible(side) ? HeatAPI.DEFAULT_INVERSE_INSULATION : heatCapacitor.getInverseInsulation();
    }
}
