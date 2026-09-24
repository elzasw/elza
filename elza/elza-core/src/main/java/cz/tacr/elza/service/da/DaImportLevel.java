package cz.tacr.elza.service.da;

import java.util.List;

import javax.annotation.Nullable;

/**
 * One {@code <div>} of the logical structural map of the package, with the unit of description
 * of the EAD it stands for, as the DA_IMPORT script sees it (variable {@code LEVEL}).
 *
 * The div and the unit are paired by id: the {@code ID} of the div is the {@code id} of the
 * {@code <c>}, or of the {@code <fileplan>} for the file plan the package comes from.
 */
public class DaImportLevel {

    private final String id;
    private final String divType;
    private final String label;
    private final DaImportLevel parent;
    private final boolean fileplan;
    private final String eadLevel;
    private final String eadOtherLevel;
    private final List<DaImportElement> elements;

    DaImportLevel(String id, @Nullable String divType, @Nullable String label, @Nullable DaImportLevel parent,
                  boolean fileplan, @Nullable String eadLevel, @Nullable String eadOtherLevel,
                  List<DaImportElement> elements) {
        this.id = id;
        this.divType = divType;
        this.label = label;
        this.parent = parent;
        this.fileplan = fileplan;
        this.eadLevel = eadLevel;
        this.eadOtherLevel = eadOtherLevel;
        this.elements = List.copyOf(elements);
    }

    /** ID of the div, e.g. uuid-ac73f202-a2a0-4309-9ce7-6c1e426be654. */
    public String getId() {
        return id;
    }

    /** TYPE of the div, e.g. spisplan, vecnaskp, dokument, komponenta; null when it has none. */
    @Nullable
    public String getDivType() {
        return divType;
    }

    /** LABEL of the div - the name of the unit; null when it has none. */
    @Nullable
    public String getLabel() {
        return label;
    }

    /** The div this one is nested in; null for a top div of the map. */
    @Nullable
    public DaImportLevel getParent() {
        return parent;
    }

    /** 1 for a top div of the map. */
    public int getDepth() {
        return parent == null ? 1 : parent.getDepth() + 1;
    }

    /** Whether the div stands for the {@code <fileplan>} of the EAD, the file plan the package comes from. */
    public boolean isFileplan() {
        return fileplan;
    }

    /** {@code level} of the {@code <c>}, e.g. series, file, item, otherlevel; null without a unit. */
    @Nullable
    public String getEadLevel() {
        return eadLevel;
    }

    /** {@code otherlevel} of the {@code <c>}, e.g. vecnaskp, dokument; null when it has none. */
    @Nullable
    public String getEadOtherLevel() {
        return eadOtherLevel;
    }

    /**
     * Elements of {@code <did>} of the unit that can be taken over, in document order.
     * Inherited values are left out - the unit they are inherited from carries them.
     */
    public List<DaImportElement> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return "div " + id + (divType != null ? " (" + divType + ")" : "");
    }
}
