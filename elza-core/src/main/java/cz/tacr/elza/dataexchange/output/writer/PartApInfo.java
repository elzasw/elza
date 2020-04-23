package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.domain.ApPart;

import java.util.Collection;

public interface PartApInfo {

    Collection<ApPart> getParts();

    void setParts(Collection<ApPart> parts);
}
