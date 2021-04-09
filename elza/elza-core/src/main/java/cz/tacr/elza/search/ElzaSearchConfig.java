package cz.tacr.elza.search;

import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nullable;
import java.util.List;

public class ElzaSearchConfig {

    private List<FieldSearchConfig> fields;

    public List<FieldSearchConfig> getFields() {
        return fields;
    }

    public void setFields(List<FieldSearchConfig> fields) {
        this.fields = fields;
    }

    @Nullable
    public FieldSearchConfig getFieldSearchConfigByName(String name) {
        if (CollectionUtils.isNotEmpty(getFields())) {
            for (FieldSearchConfig fieldSearchConfig : getFields()) {
                if (fieldSearchConfig.getName() != null && fieldSearchConfig.getName().equals(name)) {
                    return fieldSearchConfig;
                }
            }
        }
        return null;
    }
}
