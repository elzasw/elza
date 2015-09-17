package cz.tacr.elza.doc;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.repository.FindingAidRepository;

/**
 * Popis vygenerování identifikátoru při uložení objektu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 17. 9. 2015
 */
public class IdentifierGeneration {

    @Autowired
    private FindingAidRepository findingAidRepository;

    /**
     * Identifikátory se přidělují podle tabulky db_hibernate_sequences. V tabulce je pro každou entitu uložen
     * další volný identifikátor. Při ukládání nového objektu Hibernate načte volné id a zároveň zvýší hodnotu v tabulce o 1.
     */
    public void identifierGeneration() {
        ArrFindingAid findingAid = new ArrFindingAid();
        findingAid.setCreateDate(LocalDateTime.now());
        findingAid.setName("Archiv 1");

        ArrFindingAid savedFindingAid = findingAidRepository.save(findingAid);
        Assert.notNull(savedFindingAid.getFindingAidId());
    }
}
