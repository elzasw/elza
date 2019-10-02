package cz.tacr.elza.dataexchange.output.writer;

import java.util.Collection;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulStructuredType;

public interface StructObjectInfo {

    int getId();

    RulStructuredType getStructType();

    Collection<ArrItem> getItems();
}