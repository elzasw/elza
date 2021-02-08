package cz.tacr.elza.dataexchange.output.writer;

import java.util.Collection;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulStructuredType;

public interface StructObjectInfo {

    int getId();

    public String getUuid();

    public Boolean getAssignable();

    public String getValue();

    public String getComplement();

    RulStructuredType getStructType();

    Collection<ArrItem> getItems();
}