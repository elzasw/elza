package cz.tacr.elza.deimport.aps.context;

import java.io.Serializable;
import java.util.Objects;

import cz.tacr.elza.deimport.context.StatefulIdHolder;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;

/**
 * Access point import info which primarily stores id and result of record pairing.
 */
public class RecordImportInfo extends StatefulIdHolder {

    private final String apEntryId;

    private final RegRegisterType registerType;

    private String fulltext;

    RecordImportInfo(String apEntryId, RegRegisterType registerType) {
        this.apEntryId = Objects.requireNonNull(apEntryId);
        this.registerType = Objects.requireNonNull(registerType);
    }

    public String getApEntryId() {
        return apEntryId;
    }

    public RegRegisterType getRegisterType() {
        return registerType;
    }

    public String getFulltext() {
        return Objects.requireNonNull(fulltext);
    }

    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }

    @Override
    public Integer getId() {
        return (Integer) super.getId();
    }

    @Override
    public void checkReferenceClass(Class<?> entityClass) {
        if (RegRecord.class != entityClass) {
            throw new IllegalStateException("Class " + entityClass + " is not suitable as entity reference");
        }
    }

    @Override
    protected void init(Serializable id, State state) {
        super.init(id, state);
    }
}
