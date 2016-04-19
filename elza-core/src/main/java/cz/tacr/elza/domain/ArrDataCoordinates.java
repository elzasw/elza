package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class ArrDataCoordinates extends ArrData implements cz.tacr.elza.api.ArrDataCoordinates {

    @Column(nullable = false)
    @Type(type="org.hibernate.spatial.GeometryType")
    private Geometry value;

    @Override
    public Geometry getValue() {
        return value;
    }

    @Override
    public void setValue(final Geometry value) {
        this.value = value;
    }
}
