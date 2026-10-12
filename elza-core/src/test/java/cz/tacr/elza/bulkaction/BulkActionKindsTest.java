package cz.tacr.elza.bulkaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.bulkaction.generator.FundValidationConfig;
import cz.tacr.elza.bulkaction.generator.GenerateUnitIdConfig;
import cz.tacr.elza.bulkaction.generator.PersistentSortConfig;
import cz.tacr.elza.controller.vo.BulkActionType;

/**
 * The kinds of bulk actions are the YAML tags of their configurations and the enum BulkActionType of
 * the REST API; a new kind needs both.
 */
class BulkActionKindsTest {

    @Test
    void kindsAreTheEnumOfTheRestApi() {
        assertThat(BulkActionConfigManager.getKinds())
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(BulkActionType.values())
                        .map(BulkActionType::getValue)
                        .toList());
    }

    @Test
    void kindIsTheYamlTagNotTheClassName() {
        assertThat(new PersistentSortConfig().getType()).isEqualTo(PersistentSortConfig.TYPE);
        assertThat(new FundValidationConfig().getType()).isEqualTo("FundValidation");
        // the class GenerateUnitIdConfig is tagged !UnitIdGenerator
        assertThat(new GenerateUnitIdConfig().getType()).isEqualTo("UnitIdGenerator");
        assertThat(new BrokenActionConfig(new RuntimeException()).getType()).isNull();
    }
}
