package cz.tacr.elza.filter.condition;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * Podmínka přes Hibernate. Počítá s tím že se budou hledat jen uzly bez hodnot, takže se pak nekontroluje
 * platnost hodnot těchto uzlů.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 2. 5. 2016
 */
public interface HibernateDescItemCondition extends DescItemCondition {

    /**
     * Vytvoří dotaz.
     *
     * @param entityManager entityManager
     * @param fundId id archivního souboru
     * @param descItemTypeId id typu atributu
     * @param lockChangeId id uzamčené verze,může být null
     *
     * @return dotaz
     */
    public Query createHibernateQuery(EntityManager entityManager, Integer fundId, Integer descItemTypeId, Integer lockChangeId);
}
