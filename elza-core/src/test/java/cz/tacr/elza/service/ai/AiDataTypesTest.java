package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.aiprovider.client.vo.DataType;

class AiDataTypesTest {

    /**
     * Every Elza data type exists in the AI provider contract; a value added to the
     * domain enum without the contract fails here, not at run time.
     */
    @Test
    void everyDomainDataTypeIsInTheContract() {
        for (cz.tacr.elza.core.data.DataType domain : cz.tacr.elza.core.data.DataType.values()) {
            DataType contract = AiDataTypes.toContract(domain);
            assertThat(contract.getValue()).isEqualTo(domain.name());
        }
    }
}
