package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Geometry;

import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.common.GeometryConvertor.GeometryJsonDeserializer;
import cz.tacr.elza.common.GeometryConvertor.GeometryJsonSerializer;


/**
 * Hodnota atributu archivního popisu typu Coordinates.
 */
@Entity(name = "arr_data_coordinates")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataCoordinates extends ArrData {

    @Column(nullable = false, columnDefinition = "geometry")
    @JsonDeserialize(using = GeometryJsonDeserializer.class)
    @JsonSerialize(using = GeometryJsonSerializer.class)
    private Geometry value;

	public ArrDataCoordinates() {

	}

	protected ArrDataCoordinates(final ArrDataCoordinates src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataCoordinates src) {
        this.value = src.value;
    }

    public Geometry getValue() {
        return value;
    }

    public void setValue(final Geometry value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        String str = GeometryConvertor.convert(value);
        return str;
    }

	@Override
	public ArrDataCoordinates makeCopy() {
		ArrDataCoordinates copy = new ArrDataCoordinates(this);
		return copy;
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataCoordinates src = (ArrDataCoordinates)srcData;
        return value.equals(src.value);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataCoordinates src = (ArrDataCoordinates) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(value);
    }
}
