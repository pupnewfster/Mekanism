package mekanism.api.heat;

import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/// A sided variant of [IHeatHandler]
//TODO - 26.1: Re-evaluate this class
public interface ISidedHeatHandler extends IHeatHandler {

    /// The side this [ISidedHeatHandler] is for. This defaults to null, which is for internal use.
    ///
    /// @return The default side to use for the normal [IHeatHandler] methods when wrapping them into [ISidedHeatHandler] methods.
    @Nullable
    default Direction getHeatSideFor() {
        return null;
    }

    default int getHeatCapacitorCount(@Nullable Direction side) {
        return getHeatCapacitorCount();
    }

    /// A sided variant of [IHeatHandler#getTemperature()], docs copied for convenience.
    ///
    /// Returns the temperature of a given capacitor.
    ///
    /// @param side      The side we are interacting with the handler from (null for internal).
    ///
    /// @return Temperature of a given capacitor.
    double getTemperature(@Nullable Direction side);

    @Override
    default double getTemperature() {
        return getTemperature(getHeatSideFor());
    }

    /// A sided variant of [IHeatHandler#getInverseConduction()], docs copied for convenience.
    ///
    /// Returns the inverse conduction coefficient of a given capacitor. This value defines how much heat is allowed to be dissipated. The larger the number the less heat
    /// can dissipate. The trade-off is that it also allows for lower amounts of heat to be inserted.
    ///
    /// @param side      The side we are interacting with the handler from (null for internal).
    ///
    /// @return Inverse conduction coefficient of a given capacitor.
    ///
    /// @apiNote Must be greater than 0
    double getInverseConduction(@Nullable Direction side);

    @Override
    default double getInverseConduction() {
        return getInverseConduction(getHeatSideFor());
    }

    /// A sided variant of [IHeatHandler#getHeatCapacity()], docs copied for convenience.
    ///
    /// Returns the heat capacity of a given capacitor.
    ///
    /// @param side      The side we are interacting with the handler from (null for internal).
    ///
    /// @return Heat capacity of a given capacitor.
    ///
    /// @apiNote Must be at least 1
    double getHeatCapacity(@Nullable Direction side);

    @Override
    default double getHeatCapacity() {
        return getHeatCapacity(getHeatSideFor());
    }

    /// A sided variant of [IHeatHandler#handleHeat(double)], docs copied for convenience.
    ///
    /// Handles a change of heat in this block. Can be positive or negative.
    ///
    /// @param transfer The amount being transferred.
    /// @param side     The side we are interacting with the handler from (null for internal).
    void handleHeat(double transfer, @Nullable Direction side);

    @Override
    default void handleHeat(double transfer) {
        handleHeat(transfer, getHeatSideFor());
    }
}
