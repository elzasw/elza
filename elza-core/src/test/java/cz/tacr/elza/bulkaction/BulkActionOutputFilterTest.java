package cz.tacr.elza.bulkaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulOutputFilter;
import cz.tacr.elza.repository.ActionRepository;

/**
 * Which runs of bulk actions belong to an output: only an action marked with
 * {@code applyOutputFilter} is computed with the output filter, the other actions always process
 * all levels. Uses the real ZP2015 configurations.
 */
class BulkActionOutputFilterTest {

    private static final Path ZP2015_BULK_ACTIONS = Paths.get("..", "rules-cz-zp2015", "src", "rul_rule_set", "ZP2015",
                                                              "bulk_actions");

    private static final String INTRO = "ZP2015_INTRO";

    private static final String SERIAL_NUMBER = "ZP2015_GENERATOR_SERIAL_NUMBER";

    private static final String UNIT_ID = "ZP2015_GENERATOR_UNIT_ID";

    private static final String FUND_VALIDATION = "ZP2015_FUND_VALIDATION";

    private static final String ACCESS_RESTRICT = "ZP_ACCESS_RESTRICT";

    private BulkActionConfigManager configManager;

    @BeforeEach
    void loadZp2015Actions() {
        List<RulAction> actions = new ArrayList<>();
        for (String code : List.of(INTRO, SERIAL_NUMBER, UNIT_ID, FUND_VALIDATION)) {
            RulAction action = new RulAction();
            action.setFilename(RulAction.getFileNameFromCode(code));
            actions.add(action);
        }
        ActionRepository actionRepository = mock(ActionRepository.class);
        when(actionRepository.findAll()).thenReturn(actions);
        ResourcePathResolver resourcePathResolver = mock(ResourcePathResolver.class);
        when(resourcePathResolver.getFunctionFile(any(RulAction.class)))
                .thenAnswer(invocation -> ZP2015_BULK_ACTIONS.resolve(invocation.<RulAction>getArgument(0).getFilename()));

        configManager = new BulkActionConfigManager(actionRepository, resourcePathResolver);
        configManager.load();
    }

    @Test
    void onlyIntroAggregationAppliesOutputFilter() {
        assertThat(configManager.get(INTRO).isApplyOutputFilter()).isTrue();
        assertThat(configManager.get(SERIAL_NUMBER).isApplyOutputFilter()).isFalse();
        assertThat(configManager.get(UNIT_ID).isApplyOutputFilter()).isFalse();
        assertThat(configManager.get(FUND_VALIDATION).isApplyOutputFilter()).isFalse();
    }

    @Test
    void requiredFilterFollowsTheOutput() {
        ArrOutput filteredOutput = output(ACCESS_RESTRICT);
        assertThat(configManager.getRequiredOutputFilterCode(INTRO, filteredOutput)).isEqualTo(ACCESS_RESTRICT);
        assertThat(configManager.getRequiredOutputFilterCode(INTRO, output(null))).isNull();
        // the generators number all units regardless of the filter
        assertThat(configManager.getRequiredOutputFilterCode(SERIAL_NUMBER, filteredOutput)).isNull();
        assertThat(configManager.getRequiredOutputFilterCode("UNKNOWN_ACTION", filteredOutput)).isNull();
    }

    @Test
    void introRunIsValidOnlyForOutputWithTheSameFilter() {
        ArrBulkActionRun filteredRun = run(INTRO, ACCESS_RESTRICT);
        ArrBulkActionRun unfilteredRun = run(INTRO, null);

        assertThat(configManager.isRunValidForOutput(filteredRun, output(ACCESS_RESTRICT))).isTrue();
        assertThat(configManager.isRunValidForOutput(unfilteredRun, output(ACCESS_RESTRICT))).isFalse();
        assertThat(configManager.isRunValidForOutput(filteredRun, output(null))).isFalse();
        assertThat(configManager.isRunValidForOutput(unfilteredRun, output(null))).isTrue();
        assertThat(configManager.isRunValidForOutput(filteredRun, output("OTHER_FILTER"))).isFalse();
    }

    @Test
    void generatorRunIsValidForEveryOutput() {
        ArrBulkActionRun generatorRun = run(SERIAL_NUMBER, null);

        assertThat(configManager.isRunValidForOutput(generatorRun, output(ACCESS_RESTRICT))).isTrue();
        assertThat(configManager.isRunValidForOutput(generatorRun, output(null))).isTrue();
    }

    private static ArrOutput output(String outputFilterCode) {
        ArrOutput output = new ArrOutput();
        if (outputFilterCode != null) {
            RulOutputFilter outputFilter = new RulOutputFilter();
            outputFilter.setCode(outputFilterCode);
            output.setOutputFilter(outputFilter);
        }
        return output;
    }

    private static ArrBulkActionRun run(String actionCode, String outputFilterCode) {
        ArrBulkActionRun run = new ArrBulkActionRun();
        run.setBulkActionCode(actionCode);
        run.setOutputFilterCode(outputFilterCode);
        return run;
    }
}
