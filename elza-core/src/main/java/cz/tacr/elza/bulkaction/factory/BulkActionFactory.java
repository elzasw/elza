package cz.tacr.elza.bulkaction.factory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.BulkAction;
import cz.tacr.elza.bulkaction.generator.CleanDescriptionItemBulkAction;
import cz.tacr.elza.bulkaction.generator.FindingAidValidationBulkAction;
import cz.tacr.elza.bulkaction.generator.SerialNumberBulkAction;
import cz.tacr.elza.bulkaction.generator.UnitIdBulkAction;


/**
 * Factory na vytváření instancí hromadných akcí.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
@Component
@Configuration
public class BulkActionFactory {

    /**
     * Vytvoření instance hromadné akce podle kódu.
     *
     * @param code kód hromadné akce
     * @return instance hromadné akce
     */
    @Bean
    @Scope("prototype")
    public BulkAction getByCode(String code) {

        switch (code) {
            case UnitIdBulkAction.TYPE:
                return new UnitIdBulkAction();

            case SerialNumberBulkAction.TYPE:
                return new SerialNumberBulkAction();

            case CleanDescriptionItemBulkAction.TYPE:
                return new CleanDescriptionItemBulkAction();

            case FindingAidValidationBulkAction.TYPE:
                return new FindingAidValidationBulkAction();

            default:
                throw new IllegalStateException("Hromadna akce " + code + " neexistuje");
        }
    }


}
