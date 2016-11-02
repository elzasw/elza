package cz.tacr.elza.bulkaction.factory;

import cz.tacr.elza.bulkaction.generator.MultipleBulkAction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.BulkAction;
import cz.tacr.elza.bulkaction.generator.FundValidationBulkAction;
import cz.tacr.elza.bulkaction.generator.SerialNumberBulkAction;
import cz.tacr.elza.bulkaction.generator.TestDataGenerator;
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

            case FundValidationBulkAction.TYPE:
                return new FundValidationBulkAction();

            case MultipleBulkAction.TYPE:
                return new MultipleBulkAction();

            case TestDataGenerator.TYPE:
            	return new TestDataGenerator();
            	
            default:
                throw new IllegalStateException("Hromadna akce " + code + " neexistuje");
        }
    }
}
