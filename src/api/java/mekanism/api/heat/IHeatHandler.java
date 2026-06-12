package mekanism.api.heat;

public interface IHeatHandler {

    /// Returns the number of heat storage units ("capacitors") available
    ///
    /// @return The number of capacitors available
    default int getHeatCapacitorCount() {
        return 1;
    }

    /// Returns the temperature of a given capacitor.
    ///
    /// @return Temperature of a given capacitor.
    double getTemperature();

    /// Returns the inverse conduction coefficient of a given capacitor. This value defines how much heat is allowed to be dissipated. The larger the number the less heat
    /// can dissipate. The trade-off is that it also allows for lower amounts of heat to be inserted.
    ///
    /// @return Inverse conduction coefficient of a given capacitor.
    ///
    /// @apiNote Must be at least 1.
    double getInverseConduction();

    /// Returns the heat capacity of a given capacitor. This number can be thought of as the specific heat of the capacitor.
    ///
    /// @return Heat capacity of a given capacitor.
    ///
    /// @apiNote Must be at least 1.
    double getHeatCapacity();

    /// Handles a change of heat in this block. Can be positive or negative.
    ///
    /// @param transfer The amount being transferred.
    void handleHeat(double transfer);
}