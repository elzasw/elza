package cz.tacr.elza.service;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevIndex;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApRevIndexRepository;
import cz.tacr.elza.repository.ApRevPartRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;
import static cz.tacr.elza.repository.ExceptionThrow.revPart;

@Service
public class RevisionPartService {

    private static final Logger logger = LoggerFactory.getLogger(RevisionPartService.class);

    @Autowired
    private RevisionItemService revisionItemService;

    @Autowired
    private PartService partService;

    @Autowired
    private AccessPointItemService accessPointItemService;

    @Autowired
    private ApRevPartRepository revPartRepository;

    @Autowired
    private ApRevIndexRepository revIndexRepository;

    @Autowired
    private ApPartRepository partRepository;


    public List<ApRevPart> findByRevision(ApRevision revision) {
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

    @Transactional
    public void deleteRevisionPart(ApRevPart revPart, ApChange apChange) {
        revPart.setDeleteChange(apChange);
        revPartRepository.save(revPart);
    }

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

        return revPartRepository.save(revPart);
    }

    public ApRevPart createPart(final ApRevision revision,
                                final ApChange apChange,
                                final ApPart apPart) {
        ApRevPart revParentPart = apPart.getParentPart() != null ? findByOriginalPart(apPart.getParentPart()) : null;

        ApRevPart revPart = new ApRevPart();
        revPart.setRevision(revision);
        revPart.setPartType(apPart.getPartType());
        revPart.setOriginalPart(apPart);
        revPart.setCreateChange(apChange);
        revPart.setParentPart(apPart.getParentPart());
        revPart.setRevParentPart(revParentPart);

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

    public void createParts(List<ApRevPart> createdParts, Map<Integer, List<ApRevItem>> revItemMap, ApAccessPoint accessPoint) {
        if (CollectionUtils.isNotEmpty(createdParts)) {
            createdParts.sort((o1, o2) -> {
                if (o1.getParentPart() != null || o1.getRevParentPart() != null) {
                    return -1;
                } else if (o2.getParentPart() != null || o2.getRevParentPart() != null) {
                    return 1;
                }
                return 0;
            });

            List<ApPart> parts = new ArrayList<>();

            for (ApRevPart revPart : createdParts) {
                ApPart parentPart = revPart.getParentPart();
                if (parentPart == null && revPart.getRevParentPart() != null) {
                    parentPart = findParentPart(revPart.getRevParentPartId(), createdParts);
                }

                ApPart part = createPart(revPart, accessPoint, parentPart);
                revPart.setOriginalPart(part);
                parts.add(part);
            }

            partRepository.saveAll(parts);


            revisionItemService.createItems(createdParts, revItemMap);
        }
    }

    private ApPart createPart(ApRevPart revPart, ApAccessPoint accessPoint, ApPart parentPart) {
        ApPart part = new ApPart();
        part.setPartType(revPart.getPartType());
        part.setState(ApStateEnum.OK);
        part.setAccessPoint(accessPoint);
        part.setCreateChange(revPart.getCreateChange());
        part.setLastChange(revPart.getCreateChange());
        part.setParentPart(parentPart);

        return part;
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

    public void updateParts(List<ApRevPart> updatedParts,
                            Map<Integer, List<ApRevItem>> revItemMap,
                            List<ApItem> items,
                            ApChange change) {
        if (CollectionUtils.isNotEmpty(updatedParts)) {
            List<ApItem> deletedItems = new ArrayList<>();

            for (ApRevPart revPart : updatedParts) {
                List<ApItem> partItems = items.stream()
                        .filter(i -> i.getPartId().equals(revPart.getOriginalPartId()))
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(partItems)) {
                    deletedItems.addAll(partItems);
                }
            }

            revisionItemService.createItems(updatedParts, revItemMap);
            accessPointItemService.deleteItems(deletedItems, change);

        }
    }

    public void deleteParts(List<ApRevPart> deletedParts, List<ApItem> items, ApChange change) {
        if (CollectionUtils.isNotEmpty(deletedParts)) {
            List<ApPart> parts = new ArrayList<>();
            List<ApItem> deletedItems = new ArrayList<>();

            for (ApRevPart revPart : deletedParts) {
                parts.add(revPart.getOriginalPart());
                List<ApItem> itemList = items.stream()
                        .filter(i -> i.getPartId().equals(revPart.getOriginalPartId()))
                        .collect(Collectors.toList());

                deletedItems.addAll(itemList);
            }

            partService.deleteParts(parts, change);
            accessPointItemService.deleteItems(deletedItems, change);
        }
    }
}
