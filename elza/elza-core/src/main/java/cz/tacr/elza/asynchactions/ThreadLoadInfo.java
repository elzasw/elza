package cz.tacr.elza.asynchactions;

/**
 * Třída pro uchovávání informace o vytížení jednotlivých vláken
 */
public class ThreadLoadInfo {
    private int[] nodeSlots = new int[3600];
    private int[] bulkSlots = new int[3600];
    private int[] outputSlots = new int[3600];

    public int[] getNodeSlots() {
        return nodeSlots;
    }

    public void setNodeSlots(int[] nodeSlots) {
        this.nodeSlots = nodeSlots;
    }

    public int[] getBulkSlots() {
        return bulkSlots;
    }

    public void setBulkSlots(int[] bulkSlots) {
        this.bulkSlots = bulkSlots;
    }

    public int[] getOutputSlots() {
        return outputSlots;
    }

    public void setOutputSlots(int[] outputSlots) {
        this.outputSlots = outputSlots;
    }
}
