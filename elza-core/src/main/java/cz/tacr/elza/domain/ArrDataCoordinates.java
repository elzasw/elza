package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


/**
 * Hodnota atributu archivního popisu typu Coordinates.
 *
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_coordinates")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataCoordinates extends ArrData {

    @Column(nullable = false, columnDefinition = "geometry")
    private Geometry value;

    public Geometry getValue() {
        return value;
    }

    public void setValue(final Geometry value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value == null ? null : new WKTWriter().writeFormatted(value);
    }

    @Override
    public String getFulltextValue() {
        return toString();
    }

    @Override
    public ArrData copy() {
        ArrDataCoordinates data = new ArrDataCoordinates();
        data.setDataType(this.getDataType());
        data.setValue(this.getValue());
        return data;
    }

    @Override
    public void merge(final ArrData data) {
        this.setValue(((ArrDataCoordinates) data).getValue());
    }
}
