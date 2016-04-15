package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNamedOutput;


/**
 * Respozitory pro výstup z archivního souboru.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Repository
public interface NamedOutputRepository extends JpaRepository<ArrNamedOutput, Integer> {

    /**
     * Najde platné AP (není deleted), pro které existuje otevřená verze.
     *
     * @param fund archivní fond
     * @return seznam platných AP pro fond
     */
    @Query("SELECT DISTINCT no FROM arr_output o JOIN o.namedOutput no WHERE no.fund=?1 AND no.deleted = false AND o.lockChange IS NULL")
    List<ArrNamedOutput> findValidNamedOutputByFund(ArrFund fund);


    /**
     * Najde platné AP (není deleted), pro které existuje alespoň jedna uzavřená verze
     *
     * @param fund archivní fond
     * @return seznam platných historických AP pro fond
     */
    @Query("SELECT DISTINCT no FROM arr_output o JOIN o.namedOutput no WHERE no.fund=?1 AND no.deleted = false AND o.lockChange IS NOT NULL")
    List<ArrNamedOutput> findHistoricalNamedOutputByFund(ArrFund fund);

}
