package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulStructuredType;

public class StructObjectInfoImpl implements StructObjectInfo {

    private final List<ArrItem> items = new ArrayList<>();

    private final int id;

    private final String uuid;
    private final Boolean assignable;
    private final String value;
    private final String complement;

    private final RulStructuredType structType;

    public StructObjectInfoImpl(int id,
                                final String uuid,
                                final Boolean assignable,
                                final String value,
                                final String complement,
                                RulStructuredType structType) {
        this.id = id;
        this.uuid = uuid;
        this.assignable = assignable;
        this.value = value;
        this.complement = complement;
        this.structType = Validate.notNull(structType);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public Boolean getAssignable() {
        return assignable;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getComplement() {
        return complement;
    }

    @Override
    public RulStructuredType getStructType() {
        return structType;
    }

    @Override
    public List<ArrItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(ArrItem item) {
        items.add(item);
    }
}
