package cz.tacr.elza.xmlimport;

/**
 * Nastavení generátoru dat pro testy xml imprtu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2015
 */
public class XmlDataGeneratorConfig {

    private int recordCount;

    private int variantRecordCount;

    private int partyCount;

    private int childrenCount;

    private int treeDepth;

    private int descItemsCount;

    private boolean valid;

    private int eventCount;

    private int partyGroupIdCount;

    private int partyTimeRangeCount;

    private int partyNameComplementsCount;

    public XmlDataGeneratorConfig(final int recordCount, final int variantRecordCount, final int partyCount,
            final int childrenCount, final int treeDepth, final int descItemsCount, final boolean valid,
            final int eventCount, final int partyGroupIdCount, final int partyTimeRangeCount,
            final int partyNameComplementsCount) {
        this.recordCount = recordCount;
        this.variantRecordCount = variantRecordCount;
        this.partyCount = partyCount;
        this.childrenCount = childrenCount;
        this.treeDepth = treeDepth;
        this.descItemsCount = descItemsCount;
        this.valid = valid;
        this.eventCount = eventCount;
        this.partyGroupIdCount = partyGroupIdCount;
        this.partyTimeRangeCount = partyTimeRangeCount;
        this.partyNameComplementsCount = partyNameComplementsCount;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public int getVariantRecordCount() {
        return variantRecordCount;
    }

    public void setVariantRecordCount(int variantRecordCount) {
        this.variantRecordCount = variantRecordCount;
    }

    public int getPartyCount() {
        return partyCount;
    }

    public void setPartyCount(int partyCount) {
        this.partyCount = partyCount;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public int getTreeDepth() {
        return treeDepth;
    }

    public void setTreeDepth(int treeDepth) {
        this.treeDepth = treeDepth;
    }

    public int getDescItemsCount() {
        return descItemsCount;
    }

    public void setDescItemsCount(int descItemsCount) {
        this.descItemsCount = descItemsCount;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }

    public int getPartyGroupIdCount() {
        return partyGroupIdCount;
    }

    public void setPartyGroupIdCount(int partyGroupIdCount) {
        this.partyGroupIdCount = partyGroupIdCount;
    }

    public int getPartyTimeRangeCount() {
        return partyTimeRangeCount;
    }

    public void setPartyTimeRangeCount(int partyTimeRangeCount) {
        this.partyTimeRangeCount = partyTimeRangeCount;
    }

    public int getPartyNameComplementsCount() {
        return partyNameComplementsCount;
    }

    public void setPartyNameComplementsCount(int partyNameComplementsCount) {
        this.partyNameComplementsCount = partyNameComplementsCount;
    }
}
