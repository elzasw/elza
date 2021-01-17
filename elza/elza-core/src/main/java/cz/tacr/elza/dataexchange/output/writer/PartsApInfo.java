package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.domain.ApPart;

import java.util.Collection;

public interface PartsApInfo {

    Collection<ApPart> getParts();

    void addPart(ApPart part);
}
