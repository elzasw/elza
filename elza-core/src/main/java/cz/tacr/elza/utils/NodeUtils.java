package cz.tacr.elza.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;


/**
 * Pomocné metody pro práci s nody.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.11.2015
 */
public class NodeUtils {

    /**
     * Ze seznamu uzlů vytvoří seznam nodů.
     *
     * @param levels seznam uzlů
     * @return seznam nodů
     */
    public static List<ArrNode> createNodeList(final Collection<ArrLevel> levels) {
        Assert.notNull(levels);

        List<ArrNode> result = new ArrayList<ArrNode>(levels.size());
        for (ArrLevel level : levels) {
            result.add(level.getNode());
        }
        return result;
    }

}
