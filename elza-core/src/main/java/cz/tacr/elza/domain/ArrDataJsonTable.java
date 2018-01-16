package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.table.ElzaTable;


/**
 * Hodnota atributu archivního popisu typu JsonTable.
 */
@Entity(name = "arr_data_json_table")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataJsonTable extends ArrData  {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Column(nullable = false)
    private String value;

	public ArrDataJsonTable() {

	}

	protected ArrDataJsonTable(ArrDataJsonTable src) {
		super(src);
		this.value = src.value;
	}

    public ElzaTable getValue() {
        return ElzaTable.fromJsonString(value);
    }

    public String getJsonValue() {
        return value;
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
        // elza table is not indexed
        return null;
    }

	@Override
	public ArrDataJsonTable makeCopy() {
		return new ArrDataJsonTable(this);
	}
}
