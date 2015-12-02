package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;


/**
 * Rozšířený repozitář pro {@link DescItemRepository}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
@Component
public class DescItemRepositoryImpl implements DescItemRepositoryCustom {

    @Autowired
    private DescItemRepository descItemRepository;

    @Override
    public List<ArrDescItem> findByNodes(final Collection<ArrNode> nodes, @Nullable final ArrChange lockChange) {
        Assert.notNull(nodes);

        if (nodes.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        if (lockChange == null) {
            return descItemRepository.findByNodesAndDeleteChangeIsNull(nodes);
        } else {
            return descItemRepository.findByNodesAndDeleteChange(nodes, lockChange);
        }
    }
}
