package cz.tacr.elza.asynchactions;

/**
 * Třída pro uchovávání informace o vytížení.
 */
public class ThreadLoadInfo {

    private final int[] slots;

    public ThreadLoadInfo(int count) {
        slots = new int[count];
    }

    public int[] getSlots() {
        return slots;
    }
}
