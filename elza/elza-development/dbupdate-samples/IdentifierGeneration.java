package cz.tacr.elza.doc;

import java.time.LocalDateTime;

import cz.tacr.elza.domain.ArrFund;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cz.tacr.elza.repository.FundRepository;

/**
 * Popis vygenerování identifikátoru při uložení objektu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 17. 9. 2015
 */
public class IdentifierGeneration {

    @Autowired
    private FundRepository fundRepository;

    /**
     * Identifikátory se přidělují podle tabulky db_hibernate_sequences. V tabulce je pro každou entitu uložen
     * další volný identifikátor. Při ukládání nového objektu Hibernate načte volné id a zároveň zvýší hodnotu v tabulce o 1.
     */
    public void identifierGeneration() {
        ArrFund fund = new ArrFund();
        fund.setCreateDate(LocalDateTime.now());
        fund.setName("Archiv 1");

        ArrFund savedFund = fundRepository.save(fund);
        Assert.notNull(savedFund.getFundId());
    }
}
