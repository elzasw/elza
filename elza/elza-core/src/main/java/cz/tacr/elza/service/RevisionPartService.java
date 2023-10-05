package cz.tacr.elza.service;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static cz.tacr.elza.repository.ExceptionThrow.revPart;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevIndex;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApRevIndexRepository;
import cz.tacr.elza.repository.ApRevPartRepository;

@Service
public class RevisionPartService {

    private static final Logger logger = LoggerFactory.getLogger(RevisionPartService.class);

    @Autowired
    private ApRevPartRepository revPartRepository;

    @Autowired
    private ApRevIndexRepository revIndexRepository;

    public List<ApRevPart> findPartsByRevision(ApRevision revision) {
        return revPartRepository.findByRevision(revision);
    }

    @Transactional
    public void deleteRevisionParts(List<ApRevPart> parts, ApChange change) {
        if (CollectionUtils.isNotEmpty(parts)) {
            for (ApRevPart part : parts) {
                part.setDeleteChange(change);
            }
            revPartRepository.saveAll(parts);
        }
    }

    /**
     * Delete revision part
     * 
     * Method is used to mark RevPart as non existent
     * 
     * @param revPart
     * @param apChange
     */
    @Transactional
    public void deleteRevisionPart(ApRevPart revPart, ApChange apChange) {
        revPart.setDeleteChange(apChange);
        revPartRepository.save(revPart);
    }

    /**
     * Create new RevPart
     * 
     * @param revision
     * @param apChange
     * @param origPart
     * @return
     */
    public ApRevPart createPart(final RulPartType partType,
                                final ApRevision revision,
                                final ApChange apChange,
                                final ApRevPart revParentPart,
                                final ApPart parentPart) {
        Validate.notNull(partType, "Typ partu musí být vyplněn");

        ApRevPart revPart = new ApRevPart();
        revPart.setCreateChange(apChange);
        revPart.setPartType(partType);
        revPart.setRevision(revision);
        revPart.setRevParentPart(revParentPart);
        revPart.setParentPart(parentPart);
        revPart.setDeleted(false);

        return revPartRepository.save(revPart);
    }

    /**
     * Create RevPart based on ApPart
     * 
     * @param revision
     * @param apChange
     * @param origPart
     * @param deleted
     *            Flag if part is deleted
     * @return
     */
    public ApRevPart createPart(final ApRevision revision,
                                final ApChange apChange,
                                final ApPart origPart,
                                boolean deleted) {

        ApRevPart revPart = new ApRevPart();
        revPart.setRevision(revision);
        revPart.setPartType(origPart.getPartType());
        revPart.setOriginalPart(origPart);
        revPart.setCreateChange(apChange);
        revPart.setParentPart(origPart.getParentPart());
        revPart.setDeleted(deleted);

        return revPartRepository.save(revPart);
    }

    /**
     * Create new RevPart with ApPart Type
     * 
     * @param revision
     * @param apChange
     * @param origPart
     * @param revParentPart
     * 
     * @return
     */
    public ApRevPart createPart(final ApRevision revision,
                                final ApChange apChange,
                                final ApPart origPart,
                                final ApRevPart revParentPart) {

        ApRevPart revPart = new ApRevPart();
        revPart.setRevision(revision);
        revPart.setPartType(origPart.getPartType());
        revPart.setCreateChange(apChange);
        revPart.setRevParentPart(revParentPart);
        revPart.setDeleted(false);

        return revPartRepository.save(revPart);
    }

    public void updatePartValue(ApRevPart part,
                                GroovyResult result) {

        Map<String, String> indexMap = result.getIndexes();

        String displayName = indexMap != null ? indexMap.get(DISPLAY_NAME) : null;
        if (displayName == null) {
            throw new SystemException("Povinný index typu [" + DISPLAY_NAME + "] není vyplněn");
        }

        Map<String, ApRevIndex> indexMapByType = revIndexRepository.findByPart(part).stream()
                .collect(Collectors.toMap(ApRevIndex::getIndexType, Function.identity()));

        for (Map.Entry<String, String> entry : indexMap.entrySet()) {

            String indexType = StringUtils.stripToNull(entry.getKey());
            if (indexType == null) {
                throw new SystemException("Neplatný typ indexu ApRevIndex").set("indexType", indexType);
            }

            String value = entry.getValue();
            if (value == null) {
                throw new SystemException("Neplatná hodnota indexu ApRevIndex").set("indexType", indexType).set("value", value);
            }

            if (value.length() > StringLength.LENGTH_4000) {
                value = value.substring(0, StringLength.LENGTH_4000 - 1);
                logger.warn("Hodnota indexu byla příliš dlouhá, byla oříznuta: partId={}, indexType={}, value={}", part.getPartId(), indexType, value);
            }

            ApRevIndex index = indexMapByType.remove(indexType);

            if (index == null) {
                index = new ApRevIndex();
                index.setPart(part);
                index.setIndexType(indexType);
                index.setValue(value);
                revIndexRepository.save(index);
            } else {
                if (!value.equals(index.getValue())) {
                    index.setValue(value);
                    revIndexRepository.save(index);
                }
            }
        }

        // smazat to, co zbylo
        if (!indexMapByType.isEmpty()) {
            revIndexRepository.deleteAll(indexMapByType.values());
        }
    }

    public List<ApRevPart> findPartsByParentPart(ApPart part) {
        return revPartRepository.findByParentPart(part);
    }

    public List<ApRevPart> findPartsByRevParentPart(ApRevPart part) {
        return revPartRepository.findByRevParentPart(part);
    }

    public ApRevPart findByOriginalPart(ApPart part) {
        return revPartRepository.findByOriginalPart(part);
    }

    public ApRevPart findById(Integer partId) {
        Validate.notNull(partId);
        return revPartRepository.findById(partId)
                .orElseThrow(revPart(partId));
    }

    @Nullable
    private ApPart findParentPart(Integer revParentPartId, List<ApRevPart> createdParts) {
        for (ApRevPart revPart : createdParts) {
            if (revPart.getPartId().equals(revParentPartId)) {
                return revPart.getOriginalPart();
            }
        }
        return null;
    }

    /**
     * Mark part as deleted
     * 
     * Method can be used only to RevPart based on real part
     * 
     * @param apPart
     * @param revPart
     * @return
     */
    public ApRevPart deletePart(ApPart apPart, ApRevPart revPart) {
        Validate.notNull(apPart);
        Validate.notNull(revPart);
        Validate.isTrue(revPart.getOriginalPartId() != null);
        Validate.isTrue(revPart.getOriginalPartId().equals(apPart.getPartId()));

        revPart.setDeleted(true);
        return revPartRepository.save(revPart);
    }

    public List<ApRevIndex> findIndexesByRevision(ApRevision revision) {
        return revIndexRepository.findByRevision(revision);
    }
}
