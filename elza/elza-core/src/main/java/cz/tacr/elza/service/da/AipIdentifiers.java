package cz.tacr.elza.service.da;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.archivists.ead3.schema.Archdesc;
import org.archivists.ead3.schema.C;
import org.archivists.ead3.schema.Did;
import org.archivists.ead3.schema.Dsc;
import org.archivists.ead3.schema.Ead;
import org.archivists.ead3.schema.Unitid;

/**
 * Identifiers of the units an AIP describes, read from the EAD of its metadata package.
 *
 * The EAD of an AIP carries the context of the archived unit: the {@code archdesc}
 * describes the package and the nested {@code c} elements the hierarchy leading to the
 * archived units themselves. The archived units are the leaf {@code c} elements, so their
 * {@code unitid} values (any {@code localtype}) identify the AIP; an EAD without any
 * {@code c} identifies the AIP by the {@code unitid} values of the {@code archdesc}.
 */
public final class AipIdentifiers {

    private AipIdentifiers() {
    }

    /**
     * @return trimmed {@code unitid} values of the leaf units, in document order; empty when
     *         the EAD carries none
     */
    public static Set<String> fromEad(Ead ead) {
        if (ead == null || ead.getArchdesc() == null) {
            return Collections.emptySet();
        }
        Archdesc archdesc = ead.getArchdesc();

        List<C> leaves = new ArrayList<>();
        for (Object a : archdesc.getAccessrestrictOrAccrualsOrAcqinfo()) {
            if (a instanceof Dsc dsc) {
                for (C c : dsc.getC()) {
                    collectLeaves(c, leaves);
                }
            }
        }

        Set<String> result = new LinkedHashSet<>();
        if (leaves.isEmpty()) {
            result.addAll(unitids(archdesc.getDid()));
        } else {
            for (C leaf : leaves) {
                result.addAll(unitids(leaf.getDid()));
            }
        }
        return result;
    }

    private static void collectLeaves(C c, List<C> leaves) {
        boolean hasChild = false;
        for (Object t : c.getTheadAndC()) {
            if (t instanceof C child) {
                hasChild = true;
                collectLeaves(child, leaves);
            }
        }
        if (!hasChild) {
            leaves.add(c);
        }
    }

    private static Set<String> unitids(Did did) {
        Set<String> result = new LinkedHashSet<>();
        if (did == null) {
            return result;
        }
        for (Object o : did.getMDid()) {
            if (o instanceof Unitid unitid) {
                String value = text(unitid.getContent());
                if (StringUtils.isNotBlank(value)) {
                    result.add(value.trim());
                }
            }
        }
        return result;
    }

    private static String text(List<Serializable> content) {
        StringBuilder sb = new StringBuilder();
        for (Serializable s : content) {
            if (s instanceof String str) {
                sb.append(str);
            }
        }
        return sb.toString();
    }
}
