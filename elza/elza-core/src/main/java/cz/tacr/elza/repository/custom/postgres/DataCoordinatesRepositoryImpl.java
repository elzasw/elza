package cz.tacr.elza.repository.custom.postgres;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import cz.tacr.elza.common.db.OnPostgreSQLCondition;
import cz.tacr.elza.repository.DataCoordinatesRepositoryCustom;

@Component
@Conditional(OnPostgreSQLCondition.class)
public class DataCoordinatesRepositoryImpl implements DataCoordinatesRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

//    public String convertCoordinatesToEWKT(byte[] coordinates) { //TODO asi nadbytečné, nikde se nevolá
//        final String sql = "SELECT ST_AsEWKT(ST_GeomFromWKB(:coordinates))";
//
//        Object result = entityManager.createNativeQuery(sql)
//                .setParameter("coordinates", coordinates)
//                .getSingleResult();
//        return (String) result;
//    }

}
