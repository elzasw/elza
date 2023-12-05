package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "data_value", nullable = false)
    private String value;

	public ArrDataJsonTable() {

	}

	protected ArrDataJsonTable(ArrDataJsonTable src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataJsonTable src) {
        this.value = src.value;
    }

    public ElzaTable getValue() {
        return ElzaTable.fromJsonString(value);
    }

    /**
     * Return raw Json value without any interpretation
     *
     * @return
     */
    @JsonIgnore
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

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataJsonTable src = (ArrDataJsonTable)srcData;
        return value.equals(src.value);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataJsonTable src = (ArrDataJsonTable) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        if (value == null) {
            throw new NullPointerException("Missing value in table ArrDataJsonTable");
        }
    }
}
