package cz.tacr.elza.repository.custom.h2;

import org.geotools.geometry.jts.WKBReader;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import cz.tacr.elza.common.db.OnH2Condition;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataCoordinatesRepositoryCustom;

@Component
@Conditional(OnH2Condition.class)
public class DataCoordinatesRepositoryImpl implements DataCoordinatesRepositoryCustom {

    @Override
    public String convertCoordinatesToEWKT(byte[] coordinates) {
        try {
            Geometry geom = new WKBReader().read(coordinates);
            return geom.toText();
        } catch (ParseException e) {
            throw new BusinessException("Failed to parse coordinates",
                    e, BaseCode.INVALID_STATE);
        }
    }

}
