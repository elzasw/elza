package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * One {@code <div>} of the logical structural map of the package, with the unit of description
 * of the EAD it stands for, as the scripts see it: the DA_IMPORT script gets one level at a time
 * (variable {@code LEVEL}), the DA_MATCH script the top one with the whole tree below it
 * (variable {@code ROOT}).
 *
 * The div and the unit are paired by id: the {@code ID} of the div is the {@code id} of the
 * {@code <c>} or of the {@code <archdesc>} (a package describing itself as one unit), or of the
 * {@code <fileplan>} for the file plan the package comes from.
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
    private final List<DaImportLevel> children = new ArrayList<>();

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

    /** The divs nested in this one, in the order of the map. */
    public List<DaImportLevel> getChildren() {
        return Collections.unmodifiableList(children);
    }

    void addChild(DaImportLevel child) {
        children.add(child);
    }

    /** 1 for a top div of the map. */
    public int getDepth() {
        return parent == null ? 1 : parent.getDepth() + 1;
    }

    /** Whether the div stands for the {@code <fileplan>} of the EAD, the file plan the package comes from. */
    public boolean isFileplan() {
        return fileplan;
    }

    /** {@code level} of the unit, e.g. series, file, item, otherlevel; null without a unit. */
    @Nullable
    public String getEadLevel() {
        return eadLevel;
    }

    /** {@code otherlevel} of the unit, e.g. vecnaskp, dokument, balicek; null when it has none. */
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
