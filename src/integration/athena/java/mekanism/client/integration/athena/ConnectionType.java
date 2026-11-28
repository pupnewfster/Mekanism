package mekanism.client.integration.athena;

public enum ConnectionType {
    NONE,
    RECESSED,
    FLUSH,
    BOTH,
    TOP_LEFT_FLUSH,
    TOP_RIGHT_FLUSH,
    BOTTOM_LEFT_FLUSH,
    BOTTOM_RIGHT_FLUSH,
    TOP_LEFT_RECESSED,
    TOP_RIGHT_RECESSED,
    BOTTOM_LEFT_RECESSED,
    BOTTOM_RIGHT_RECESSED;

    public boolean isSplit() {
        return this == TOP_LEFT_FLUSH || this == TOP_RIGHT_FLUSH || this == BOTTOM_LEFT_FLUSH || this == BOTTOM_RIGHT_FLUSH
               || this == TOP_LEFT_RECESSED || this == TOP_RIGHT_RECESSED || this == BOTTOM_LEFT_RECESSED || this == BOTTOM_RIGHT_RECESSED;
    }

    public boolean check(boolean recessed) {
        if (this == BOTH) {
            return true;
        }
        return recessed ? this == RECESSED : this == FLUSH;
    }
}