package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.archivists.ead3.schema.Archdesc;
import org.archivists.ead3.schema.C;
import org.archivists.ead3.schema.Did;
import org.archivists.ead3.schema.Dsc;
import org.archivists.ead3.schema.Ead;
import org.archivists.ead3.schema.Fileplan;

import gov.loc.mets.v1_11.schema.DivType;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.mets.v1_11.schema.StructMapType;

/**
 * The divs of the logical structural map of a package as {@link DaImportLevel levels}, each
 * paired with the unit of description of the EAD of the same id.
 */
final class DaImportTree {

    private static final String LOGICAL = "LOGICAL";

    /** A unit of description of the EAD - a {@code <c>} or the {@code <archdesc>}. */
    private record Unit(@Nullable String level, @Nullable String otherLevel, @Nullable Did did) {
    }

    private final Map<String, Unit> units = new HashMap<>();
    private final Set<String> fileplanIds = new HashSet<>();
    private final Map<DivType, DaImportLevel> levels = new IdentityHashMap<>();
    private final List<DivType> rootDivs = new ArrayList<>();

    DaImportTree(MetsType mets, @Nullable Ead ead) {
        if (ead != null && ead.getArchdesc() != null) {
            index(ead.getArchdesc());
        }
        for (StructMapType structMap : mets.getStructMap()) {
            if (LOGICAL.equals(structMap.getTYPE()) && structMap.getDiv() != null) {
                rootDivs.add(structMap.getDiv());
                build(structMap.getDiv(), null);
            }
        }
    }

    /** Top divs of the logical structural maps - usually one. */
    List<DivType> getRootDivs() {
        return rootDivs;
    }

    /** Top levels of the logical structural maps - usually one. */
    List<DaImportLevel> getRoots() {
        return rootDivs.stream().map(levels::get).toList();
    }

    /** The level of a div of the tree. */
    DaImportLevel level(DivType div) {
        DaImportLevel level = levels.get(div);
        if (level == null) {
            throw new IllegalArgumentException("div " + div.getID() + " is not part of the tree");
        }
        return level;
    }

    private void index(Archdesc archdesc) {
        // a package describing itself as one unit pairs the archdesc with its top div
        if (archdesc.getId() != null) {
            units.put(archdesc.getId(), new Unit(archdesc.getLevel(), archdesc.getOtherlevel(), archdesc.getDid()));
        }
        for (Object o : archdesc.getAccessrestrictOrAccrualsOrAcqinfo()) {
            if (o instanceof Dsc dsc) {
                dsc.getC().forEach(this::index);
            } else if (o instanceof Fileplan fileplan && fileplan.getId() != null) {
                fileplanIds.add(fileplan.getId());
            }
        }
    }

    private void index(C c) {
        if (c.getId() != null) {
            units.put(c.getId(), new Unit(c.getLevel(), c.getOtherlevel(), c.getDid()));
        }
        for (Object o : c.getTheadAndC()) {
            if (o instanceof C child) {
                index(child);
            }
        }
    }

    private DaImportLevel build(DivType div, @Nullable DaImportLevel parent) {
        String id = div.getID();
        Unit unit = id == null ? null : units.get(id);
        List<DaImportElement> elements = new ArrayList<>();
        if (unit != null && unit.did() != null) {
            for (Object element : unit.did().getMDid()) {
                if (DidElementConverters.isSupported(element) && !DidElementConverters.isInherited(element)) {
                    elements.add(new DaImportElement(element, id));
                }
            }
        }
        DaImportLevel level = new DaImportLevel(id, StringUtils.trimToNull(div.getTYPE()),
                StringUtils.trimToNull(div.getLABEL()), parent, fileplanIds.contains(id),
                unit != null ? unit.level() : null, unit != null ? unit.otherLevel() : null, elements);
        levels.put(div, level);
        if (parent != null) {
            parent.addChild(level);
        }
        for (DivType child : div.getDiv()) {
            build(child, level);
        }
        return level;
    }
}
