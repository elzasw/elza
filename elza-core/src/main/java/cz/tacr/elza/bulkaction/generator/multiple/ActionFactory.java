package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.utils.Yaml;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Továrna na vytváření akcí pro hromadné akce.
 *
 * @author Martin Šlapa
 * @since 30.06.2016
 */
@Component
@Configuration
public class ActionFactory {

    @Bean
    @Scope("prototype")
    public Action createNewAction(final ActionType type, final Yaml section) {

        switch (type) {

            case DATACE_RANGE: {
                return new DataceRangeAction(section);
            }

            case COPY: {
                return new CopyAction(section);
            }

            case NODE_COUNT: {
                return new NodeCountAction(section);
            }

            case TABLE_STATISTIC: {
                return new TableStatisticAction(section);
            }

            case TEXT_AGGREGATION: {
                return new TextAggregationAction(section);
            }

            case UNIT_COUNT: {
                throw new IllegalStateException("Neimplementováno");
            }

            default:
                throw new IllegalStateException("Neexistující akce");
        }
    }

}
