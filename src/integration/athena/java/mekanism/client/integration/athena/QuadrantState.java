package mekanism.client.integration.athena;

public enum QuadrantState {
    NONE,
    RECESSED,
    FLUSH;

    public boolean isPresent() {
        return this != NONE;
    }

    public boolean isRecessed() {
        return this == RECESSED;
    }

    public boolean isFlush() {
        return this == FLUSH;
    }
}