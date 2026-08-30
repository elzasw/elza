package cz.tacr.elza.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.codes.ArrangementCode;

/**
 * Whether an object of a digital repository may be attached to a unit of description.
 *
 * The rule belongs to the repository, not to the kind of object: "Vícenásobné napojení"
 * ({@link ArrDigitalRepository#getMultipleLinks()}) says whether one object may hang on more than
 * one unit of description at a time. It is kept here because the objects that obey it - the legacy
 * DAO, an item of a filesystem repository and the content of a digital archive - are stored
 * separately and would otherwise each carry their own copy of it.
 */
@Component
public class DaoLinkPolicy {

    /**
     * Decides what to do with a request to attach an object that may already be attached.
     *
     * @param liveLinks  the links of that one object which are not deleted
     * @param nodeId     the unit of description it is to be attached to
     * @param repository the repository the object belongs to
     * @return the link to return unchanged, when the object already hangs on this very unit of
     *         description; empty when a new link is to be created
     * @throws BusinessException when the object hangs on another unit of description and the
     *         repository does not allow more than one
     */
    public Optional<ArrDaoLink> checkCanLink(final List<? extends ArrDaoLink> liveLinks,
                                             final Integer nodeId,
                                             final ArrDigitalRepository repository) {
        // Attaching again where it already hangs changes nothing; the existing link is the answer.
        for (ArrDaoLink existing : liveLinks) {
            if (nodeId.equals(existing.getNodeId())) {
                return Optional.of(existing);
            }
        }
        if (!liveLinks.isEmpty() && !Boolean.TRUE.equals(repository.getMultipleLinks())) {
            throw new BusinessException(
                    "Digitální entita je již připojena k jiné jednotce popisu;"
                            + " opakované napojení není povoleno.",
                    ArrangementCode.DAO_ALREADY_LINKED).level(Level.WARNING);
        }
        return Optional.empty();
    }

    /**
     * Whether attaching would be refused, without refusing it. Used to tell the user what stands in
     * the way before they ask for the work to be done.
     */
    public boolean wouldBeRefused(final List<? extends ArrDaoLink> liveLinks,
                                  final Integer nodeId,
                                  final ArrDigitalRepository repository) {
        try {
            checkCanLink(liveLinks, nodeId, repository);
            return false;
        } catch (BusinessException e) {
            return true;
        }
    }
}
