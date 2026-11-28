package mekanism

enum PerModule {
    NONE,
    MODULE_ONLY,
    DATA_ONLY,
    ALL;

    boolean enabled() {
        return this != NONE
    }

    boolean hasModule() {
        return this == MODULE_ONLY || this == ALL
    }

    boolean datagen() {
        return this == DATA_ONLY || this == ALL
    }
}