package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;

/**
 * popis {@link cz.tacr.elza.api.ArrDataCoordinates}.
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
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
    public String getFulltextValue() {
        return new WKTWriter().writeFormatted(value);
    }
}
