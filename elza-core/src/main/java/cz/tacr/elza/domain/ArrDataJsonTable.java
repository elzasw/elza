package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.domain.table.ElzaTable;
import org.hibernate.search.annotations.Indexed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


/**
 * Hodnota atributu archivního popisu typu JsonTable.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
@Entity(name = "arr_data_json_table")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataJsonTable extends ArrData  {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(RulItemType.class);

    @Column(nullable = false)
    private String value;

    public ElzaTable getValue() {
        return ElzaTable.fromJsonString(value);
    }

    public void setValue(final ElzaTable value) {
        try {
            this.value = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Problém při parsování JSON", e);
        }
    }

    @Override
    public String getFulltextValue() {
        ElzaTable value = getValue();
        return value == null ? null : value.toString();
    }

    @Override
    public ArrData copy() {
        ArrDataJsonTable data = new ArrDataJsonTable();
        data.setDataType(this.getDataType());
        data.setValue(this.getValue());
        return data;
    }

    @Override
    public void merge(final ArrData data) {
        this.setValue(((ArrDataJsonTable) data).getValue());
    }
}
