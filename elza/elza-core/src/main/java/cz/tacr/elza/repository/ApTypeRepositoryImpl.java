package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 02.02.2016
 */
public class ApTypeRepositoryImpl implements ApTypeRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public Set<Integer> findSubtreeIds(final Collection<Integer> apTypeIds) {

        if (apTypeIds.isEmpty()) {
            return Collections.emptySet();
        }

        String hql = "SELECT DISTINCT " +
                "t1.ap_type_id as t1, " +
                "t2.ap_type_id as t2, " +
                "t3.ap_type_id as t3, " +
                "t4.ap_type_id as t4, " +
                "t5.ap_type_id as t5 " +
                "FROM ap_type t1 " +
                "LEFT JOIN ap_type t2 ON t2.parent_ap_type_id = t1.ap_type_id " +
                "LEFT JOIN ap_type t3 ON t3.parent_ap_type_id = t2.ap_type_id " +
                "LEFT JOIN ap_type t4 ON t4.parent_ap_type_id = t3.ap_type_id " +
                "LEFT JOIN ap_type t5 ON t5.parent_ap_type_id = t4.ap_type_id " +
                "WHERE t1.ap_type_id IN (:ids)";

        final Query query = entityManager.createNativeQuery(hql);

        final Set<Integer> leaves = new HashSet<>(apTypeIds);

        final Set<Integer> result = new HashSet<>();

        Consumer<Set<Integer>> function = (ids) -> {
            query.setParameter("ids", ids);

            leaves.clear();
            for (Object[] row : (List<Object[]>) query.getResultList()) {
                result.add((Integer) row[0]);
                result.add((Integer) row[1]);
                result.add((Integer) row[2]);
                result.add((Integer) row[3]);
                result.add((Integer) row[4]);

                leaves.add((Integer) row[4]);
            }
            leaves.remove(null);
        };

        while (!leaves.isEmpty()) {
            function.accept(new HashSet<>(leaves));
        }
        result.remove(null);
        return result;
    }
}
