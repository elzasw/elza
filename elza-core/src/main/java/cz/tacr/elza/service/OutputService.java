package cz.tacr.elza.service;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputItemRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventChangeOutputItem;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serviska pro práci s výstupy.
 *
 * @author Martin Šlapa
 * @since 03.05.2016
 */
@Service
public class OutputService {

    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;

    @Autowired
    private OutputRepository outputRepository;

    @Autowired
    private OutputItemRepository outputItemRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private OutputTypeRepository outputTypeRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private NodeOutputRepository nodeOutputRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private EventNotificationService notificationService;

    /**
     * Vyhledá platné nody k výstupu.
     *
     * @param output výstup
     * @return seznam nodů k výstupu
     */
    public List<ArrNode> getNodesForOutput(final ArrOutput output) {
        Assert.notNull(output);
        if (output.getLockChange() == null) {
            return nodeRepository.findNodesForOutput(output, output.getCreateChange());
        } else {
            return nodeRepository.findNodesForOutput(output, output.getCreateChange(), output.getLockChange());
        }
    }

    /**
     * Smazat pojmenovaný výstup.
     *
     * @param fundVersion      verze AS
     * @param outputDefinition pojmenovaný výstup
     * @return smazaný pojmenovaný výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputDefinition deleteNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                 final ArrOutputDefinition outputDefinition) {
        Assert.notNull(fundVersion);
        Assert.notNull(outputDefinition);

        if (fundVersion.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze smazat výstup v uzavřené verzi AS");
        }

        checkFund(fundVersion, outputDefinition);

        if (outputDefinition.getDeleted()) {
            throw new IllegalArgumentException("Nelze smazat již smazaný výstup");
        }

        outputDefinition.setDeleted(true);

        Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(size -> new Integer[size]);
        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, outputIds);
        eventNotificationService.publishEvent(event);

        return outputDefinitionRepository.save(outputDefinition);
    }

    /**
     * Získání výstupu podle identifikátoru.
     *
     * @param outputId identifikátor výstupu
     * @return výstup
     */
    public ArrOutput getOutput(final Integer outputId) {
        return outputRepository.getOneCheckExist(outputId);
    }

    /**
     * Zamknutí výstupu.
     *
     * @param fundVersion verze AS
     * @param output      výstup
     * @return zamknutý výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutput outputLock(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                final ArrOutput output) {
        ArrChange change = arrangementService.createChange();
        return outputLock(fundVersion, output, change);
    }

    /**
     * Zamknutí výstupu.
     *
     * @param fundVersion verze AS
     * @param output      výstup
     * @param change      změna
     * @return výstup
     */
    private ArrOutput outputLock(final ArrFundVersion fundVersion,
                                 final ArrOutput output,
                                 final ArrChange change) {
        Assert.notNull(fundVersion);
        Assert.notNull(output);
        Assert.notNull(change);

        if (fundVersion.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze zamknout výstup v uzavřené verzi AS");
        }

        checkFund(fundVersion, output.getOutputDefinition());

        if (output.getLockChange() != null) {
            throw new IllegalArgumentException("Výstup je již zamknutý");
        }

        output.setLockChange(change);

        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, output.getOutputId());
        eventNotificationService.publishEvent(event);

        return outputRepository.save(output);
    }

    /**
     * Vytvoření pojmenovaného výstupu.
     *
     * @param fundVersion  verze AS
     * @param name         název výstupu
     * @param internalCode kód výstupu
     * @param temporary    dočasný výstup?
     * @return vytvořený výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputDefinition createOutputDefinition(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                      final String name,
                                                      final String internalCode,
                                                      final Boolean temporary,
                                                      final Integer outputTypeId) {
        Assert.notNull(fundVersion);
        Assert.notNull(name);
        Assert.notNull(temporary);
        Assert.notNull(outputTypeId);

        if (fundVersion.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze vytvořit výstup v uzavřené verzi AS");
        }

        ArrOutputDefinition outputDefinition = new ArrOutputDefinition();
        outputDefinition.setFund(fundVersion.getFund());
        outputDefinition.setName(name);
        outputDefinition.setInternalCode(internalCode);
        outputDefinition.setDeleted(false);
        outputDefinition.setTemporary(temporary);

        RulOutputType type = outputTypeRepository.findOne(outputTypeId);
        Assert.notNull(type);

        outputDefinition.setOutputType(type);

        outputDefinitionRepository.save(outputDefinition);

        ArrChange change = arrangementService.createChange();
        ArrOutput output = createOutput(outputDefinition, change);

        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, output.getOutputId());
        eventNotificationService.publishEvent(event);

        return outputDefinition;
    }

    /**
     * Vytvoření výstupu.
     *
     * @param outputDefinition pojmenovaný výstup
     * @param change           změna
     * @return vytvořený výstup
     */
    private ArrOutput createOutput(final ArrOutputDefinition outputDefinition,
                                   final ArrChange change) {
        Assert.notNull(outputDefinition);
        Assert.notNull(change);

        ArrOutput output = new ArrOutput();
        output.setCreateChange(change);
        output.getOutputDefinition(outputDefinition);

        return outputRepository.save(output);
    }

    /**
     * Odstranění uzlů z výstupu.
     *
     * @param fundVersion verze AS
     * @param output      výstup
     * @param nodeIds     seznam identifikátorů uzlů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public void removeNodesNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                       final ArrOutput output,
                                       final List<Integer> nodeIds) {
        ArrChange change = arrangementService.createChange();
        removeNodesNamedOutput(fundVersion, output, nodeIds, change);
    }

    /**
     * Odstranění uzlů z výstupu.
     *
     * @param fundVersion verze AS
     * @param output      pojmenovaný výstup
     * @param nodeIds     seznam identifikátorů uzlů
     * @param change      změna
     */
    private void removeNodesNamedOutput(final ArrFundVersion fundVersion,
                                        final ArrOutput output,
                                        final List<Integer> nodeIds,
                                        final ArrChange change) {
        Assert.notNull(fundVersion);
        Assert.notNull(output);
        Assert.notEmpty(nodeIds);
        Assert.notNull(change);

        if (fundVersion.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze odebrat uzly u výstupu v uzavřené verzi AS");
        }

        if (output.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze odebrat uzly u zamčeného výstupu");
        }

        ArrOutputDefinition outputDefinition = output.getOutputDefinition();

        checkFund(fundVersion, outputDefinition);

        Set<ArrNodeOutput> nodeOutputs = outputDefinition.getOutputNodes().stream()
                .filter(nodeOutput -> nodeOutput.getDeleteChange() == null && nodeIds.contains(nodeOutput.getNode().getNodeId()))
                .collect(Collectors.toSet());

        if (nodeOutputs.size() > 0) {
            nodeOutputs.stream().forEach(arrNodeOutput -> arrNodeOutput.setDeleteChange(change));
            nodeOutputRepository.save(nodeOutputs);

            Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(size -> new Integer[size]);
            EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, outputIds);
            eventNotificationService.publishEvent(event);
        }
    }

    /**
     * Upravení výstupu.
     *
     * @param fundVersion  verze AS
     * @param output       pojmenovaný výstup
     * @param name         název výstupu
     * @param internalCode kód výstupu
     * @param templateId   id šablony
     * @return upravený výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputDefinition updateNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                 final ArrOutput output,
                                                 final String name,
                                                 final String internalCode,
                                                 final Integer templateId) {
        Assert.notNull(fundVersion);
        Assert.notNull(output);
        Assert.notNull(name);

        if (fundVersion.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze upravovat výstup v uzavřené verzi AS");
        }

        if (output.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze upravit uzavřený výstup");
        }

        ArrOutputDefinition outputDefinition = output.getOutputDefinition();

        outputDefinition.setName(name);
        outputDefinition.setInternalCode(internalCode);
        if (templateId != null) {
            outputDefinition.setTemplate(templateRepository.findOne(templateId));
        } else {
            outputDefinition.setTemplate(null);
        }

        outputDefinitionRepository.save(outputDefinition);

        Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(size -> new Integer[size]);
        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, outputIds);
        eventNotificationService.publishEvent(event);

        return outputDefinition;
    }

    /**
     * Kontrola AS u verze a výstupu.
     *
     * @param fundVersion      verze AS
     * @param outputDefinition pojmenovaný výstup
     */
    private void checkFund(final ArrFundVersion fundVersion, final ArrOutputDefinition outputDefinition) {
        if (!outputDefinition.getFund().equals(fundVersion.getFund())) {
            throw new IllegalArgumentException("Output a verze AS nemají společný AS");
        }
    }

    /**
     * Přidání uzlů do výstupu.
     *
     * @param fundVersion verze AS
     * @param output      výstup
     * @param nodeIds     seznam identifikátorů uzlů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public void addNodesNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                    final ArrOutput output,
                                    final List<Integer> nodeIds) {
        ArrChange change = arrangementService.createChange();
        addNodesNamedOutput(fundVersion, output, nodeIds, change);
    }

    /**
     * Přidání uzlů do výstupu.
     *
     * @param fundVersion verze AS
     * @param output      pojmenovaný výstup
     * @param nodeIds     seznam identifikátorů uzlů
     * @param change      změna
     */
    private void addNodesNamedOutput(final ArrFundVersion fundVersion,
                                     final ArrOutput output,
                                     final List<Integer> nodeIds,
                                     final ArrChange change) {
        Assert.notNull(fundVersion);
        Assert.notNull(output);
        Assert.notEmpty(nodeIds);
        Assert.notNull(change);

        if (fundVersion.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze přidat uzly k výstupu v uzavřené verzi AS");
        }

        if (output.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze odebrat uzly u zamčeného výstupu");
        }

        ArrOutputDefinition outputDefinition = output.getOutputDefinition();

        checkFund(fundVersion, outputDefinition);

        Set<Integer> nodesIdsDb = outputDefinition.getOutputNodes().stream()
                .filter(arrNodeOutput -> arrNodeOutput.getDeleteChange() == null) // pouze nesmazané nody
                .map(ArrNodeOutput::getNode)
                .map(ArrNode::getNodeId)
                .collect(Collectors.toSet());

        nodeIds.removeAll(nodesIdsDb);

        if (nodeIds.size() > 0) {

            List<ArrNodeOutput> nodeOutputs = nodeIds.stream()
                    .map(nodeId -> entityManager.find(ArrNode.class, nodeId))
                    .map(node -> {
                        ArrNodeOutput nodeOutput = new ArrNodeOutput();
                        nodeOutput.setNode(node);
                        nodeOutput.setOutputDefinition(outputDefinition);
                        nodeOutput.setCreateChange(change);
                        return nodeOutput;
                    }).collect(Collectors.toList());

            nodeOutputRepository.save(nodeOutputs);

            Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(size -> new Integer[size]);
            EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, outputIds);
            eventNotificationService.publishEvent(event);
        }
    }

    /**
     * Získání výstupů podle verze AS.
     *
     * @param fundVersion verze AS
     * @return seznam výstupů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrOutput> getOutputs(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        Assert.notNull(fundVersion);
        List<ArrOutput> outputs = outputRepository.findByFundVersion(fundVersion);
        return outputs;
    }

    /**
     * Získání pojmenovaného výstupu.
     *
     * @param fundVersion verze AS
     * @param output      výstup
     * @return pojmenovaný výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrOutputDefinition getNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                              final ArrOutput output) {
        Assert.notNull(fundVersion);
        Assert.notNull(output);
        checkFund(fundVersion, output.getOutputDefinition());
        return output.getOutputDefinition();
    }

    public List<RulOutputType> getOutputTypes() {
        return outputTypeRepository.findAll();
    }

    // pro controler
    public ArrOutputItem createOutputItem(final ArrOutputItem item,
                                          final Integer outputDefinitionId,
                                          final Integer outputDefinitionVersion,
                                          final Integer fundVersionId) {
        Assert.notNull(item);
        Assert.notNull(outputDefinitionId);
        Assert.notNull(outputDefinitionVersion);

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);

        ArrOutputDefinition outputDefinition = outputDefinitionRepository.findOne(outputDefinitionId);
        Assert.notNull(outputDefinition);
        outputDefinition.setVersion(outputDefinitionVersion);
        saveOutputDefinition(outputDefinition);

        return createOutputItem(item, outputDefinition, fundVersion, null);
    }

    private ArrOutputDefinition saveOutputDefinition(final ArrOutputDefinition outputDefinition) {
        outputDefinition.setLastUpdate(LocalDateTime.now());
        outputDefinitionRepository.save(outputDefinition);
        outputDefinitionRepository.flush();
        return outputDefinition;
    }

    public ArrOutputItem createOutputItem(final ArrOutputItem outputItem,
                                          final ArrOutputDefinition outputDefinition,
                                          final ArrFundVersion version,
                                          @Nullable final ArrChange createChange) {
        ArrChange change = createChange == null ? arrangementService.createChange() : createChange;

        outputItem.setOutputDefinition(outputDefinition);

        outputItem.setOutputDefinition(outputDefinition);
        outputItem.setCreateChange(change);
        outputItem.setDeleteChange(null);
        outputItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

        ArrOutputItem outputItemCreated = createOutputItem(outputItem, version, change);

        // sockety
        publishChangeOutputItem(version, outputItemCreated);

        return outputItemCreated;
    }

    private ArrOutputItem createOutputItem(final ArrOutputItem outputItem,
                                           final ArrFundVersion version,
                                           final ArrChange change) {
        Assert.notNull(outputItem);
        Assert.notNull(version);
        Assert.notNull(change);

        // pro vytvoření musí být verze otevřená
        itemService.checkFundVersionLock(version);

        // kontrola validity typu a specifikace
        itemService.checkValidTypeAndSpec(outputItem);

        int maxPosition = getMaxPosition(outputItem);

        if (outputItem.getPosition() == null || (outputItem.getPosition() > maxPosition)) {
            outputItem.setPosition(maxPosition + 1);
        }

        // načtení hodnot, které je potřeba přesunout níž
        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItemsAfterPosition(
                outputItem.getItemType(),
                outputItem.getOutputDefinition(),
                outputItem.getPosition() - 1);

        // posun prvků
        itemService.moveDown(outputItems, change);

        outputItem.setCreateChange(change);
        return itemService.save(outputItem, true);
    }

    /**
     * Vyhledá maximální pozici v hodnotách atributu podle typu.
     *
     * @param outputItem hodnota atributu
     * @return maximální pozice (počet položek)
     */
    private int getMaxPosition(final ArrOutputItem outputItem) {
        int maxPosition = 0;
        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItemsAfterPosition(
                outputItem.getItemType(),
                outputItem.getOutputDefinition(),
                0);
        for (ArrOutputItem item : outputItems) {
            if (item.getPosition() > maxPosition) {
                maxPosition = item.getPosition();
            }
        }
        return maxPosition;
    }

    // pro controller
    public ArrOutputItem deleteOutputItem(final Integer descItemObjectId,
                                          final Integer outputVersion,
                                          final Integer fundVersionId) {
        Assert.notNull(descItemObjectId);
        Assert.notNull(outputVersion);
        Assert.notNull(fundVersionId);

        ArrChange change = arrangementService.createChange();
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(descItemObjectId);

        if (outputItems.size() > 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        } else if (outputItems.size() == 0) {
            throw new IllegalStateException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }

        ArrOutputItem outputItem = outputItems.get(0);
        outputItem.getOutputDefinition().setVersion(outputVersion);
        saveOutputDefinition(outputItem.getOutputDefinition());

        ArrOutputItem outputItemDeleted = deleteOutputItem(outputItem, fundVersion, change, true);

        return outputItemDeleted;
    }

    public ArrOutputItem deleteOutputItem(final ArrOutputItem outputItem,
                                          final ArrFundVersion version,
                                          final ArrChange change,
                                          final boolean moveAfter) {
        Assert.notNull(outputItem);
        Assert.notNull(version);
        Assert.notNull(change);

        // pro mazání musí být verze otevřená
        itemService.checkFundVersionLock(version);

        if (moveAfter) {
            // načtení hodnot, které je potřeba přesunout výš
            List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItemsAfterPosition(
                    outputItem.getItemType(),
                    outputItem.getOutputDefinition(),
                    outputItem.getPosition());

            itemService.copyItems(change, outputItems, -1, version);
        }

        outputItem.setDeleteChange(change);

        // sockety
        publishChangeOutputItem(version, outputItem);

        return outputItemRepository.save(outputItem);
    }

    public ArrOutputItem updateOutputItem(final ArrOutputItem outputItemItem,
                                          final Integer outputDefinitionVersion,
                                          final Integer fundVersionId,
                                          final Boolean createNewVersion) {
        Assert.notNull(outputItemItem);
        Assert.notNull(outputItemItem.getPosition());
        Assert.notNull(outputItemItem.getDescItemObjectId());
        Assert.notNull(outputDefinitionVersion);
        Assert.notNull(fundVersionId);
        Assert.notNull(createNewVersion);

        ArrChange change = null;
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);

        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(outputItemItem.getDescItemObjectId());

        if (outputItems.size() > 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        } else if (outputItems.size() == 0) {
            throw new IllegalStateException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }
        ArrOutputItem outputItemDB = outputItems.get(0);

        ArrOutputDefinition outputDefinition = outputItemDB.getOutputDefinition();
        Assert.notNull(outputDefinition);

        if (createNewVersion) {
            outputDefinition.setVersion(outputDefinitionVersion);

            // uložení uzlu (kontrola optimistických zámků)
            saveOutputDefinition(outputDefinition);

            // vytvoření změny
            change = arrangementService.createChange();
        }

        ArrOutputItem outputItemUpdated = updateOutputItem(outputItemItem, outputItemDB, fundVersion, change, createNewVersion);

        return outputItemUpdated;
    }

    public ArrOutputItem updateOutputItem(final ArrOutputItem outputItem,
                                          final ArrOutputItem outputItemDB,
                                          final ArrFundVersion version,
                                          final ArrChange change,
                                          final Boolean createNewVersion) {

        if (createNewVersion ^ change != null) {
            throw new IllegalArgumentException("Pokud vytvářím novou verzi, musí být předaná reference změny. Pokud verzi nevytvářím, musí být reference změny null.");
        }

        if (createNewVersion && version.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze provést verzovanou změnu v uzavřené verzi.");
        }

        ArrOutputItem outputItemOrig;
        if (outputItemDB == null) {
            List<ArrOutputItem> descItems = outputItemRepository.findOpenOutputItems(outputItem.getDescItemObjectId());

            if (descItems.size() > 1) {
                throw new IllegalStateException("Hodnota musí být právě jedna");
            } else if (descItems.size() == 0) {
                throw new IllegalStateException("Hodnota neexistuje, pravděpodobně byla již smazána");
            }

            outputItemOrig = descItems.get(0);
        } else {
            outputItemOrig = outputItemDB;
        }

        itemService.loadData(outputItemOrig);
        ArrOutputItem outputItemUpdated;

        if (createNewVersion) {

            Integer positionOrig = outputItemOrig.getPosition();
            Integer positionNew = outputItem.getPosition();

            // změnila pozice, budou se provádět posuny
            if (positionOrig != positionNew) {

                int maxPosition = getMaxPosition(outputItemOrig);

                if (outputItem.getPosition() == null || (outputItem.getPosition() > maxPosition)) {
                    outputItem.setPosition(maxPosition + 1);
                }

                List<ArrOutputItem> outputItemsMove;
                Integer diff;

                if (positionNew < positionOrig) {
                    diff = 1;
                    outputItemsMove = findOutputItemsBetweenPosition(outputItemOrig, positionNew, positionOrig - 1);
                } else {
                    diff = -1;
                    outputItemsMove = findOutputItemsBetweenPosition(outputItemOrig, positionOrig + 1, positionNew);
                }

                itemService.copyItems(change, outputItemsMove, diff, version);
            }

            try {
                ArrOutputItem descItemNew = new ArrOutputItem();
                BeanUtils.copyProperties(outputItemOrig, descItemNew);
                itemService.copyPropertiesSubclass(outputItem, descItemNew, ArrOutputItem.class);
                descItemNew.setItemSpec(outputItem.getItemSpec());

                outputItemOrig.setDeleteChange(change);
                descItemNew.setItemId(null);
                descItemNew.setCreateChange(change);
                descItemNew.setPosition(positionNew);
                descItemNew.setItem(outputItem.getItem());

                itemService.save(outputItemOrig, true);
                outputItemUpdated = itemService.save(descItemNew, true);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            itemService.copyPropertiesSubclass(outputItem, outputItemOrig, ArrOutputItem.class);
            outputItemOrig.setItemSpec(outputItem.getItemSpec());
            outputItemUpdated = itemService.save(outputItemOrig, false);
        }

        // sockety
        publishChangeOutputItem(version, outputItemUpdated);

        return outputItemUpdated;
    }

    /**
     * Vyhledá všechny hodnoty atributu mezi pozicemi.
     *
     * @param outputItem   hodnota atributu
     * @param positionFrom od pozice (včetně)
     * @param positionTo   do pozice (včetně)
     * @return seznam nalezených hodnot atributů
     */
    private List<ArrOutputItem> findOutputItemsBetweenPosition(final ArrOutputItem outputItem,
                                                               final Integer positionFrom,
                                                               final Integer positionTo) {

        List<ArrOutputItem> descItems = outputItemRepository.findOpenOutputItemsBetweenPositions(outputItem.getItemType(),
                outputItem.getOutputDefinition(), positionFrom, positionTo);
        return descItems;
    }

    public ArrOutputDefinition findOutputDefinition(final Integer outputDefinitionId) {
        return outputDefinitionRepository.findOne(outputDefinitionId);
    }

    public List<ArrOutputItem> getOutputItems(final ArrFundVersion version,
                                              final ArrOutputDefinition outputDefinition) {
        List<ArrOutputItem> itemList;

        if (version.getLockChange() == null) {
            itemList = outputItemRepository.findByOutputAndDeleteChangeIsNull(outputDefinition);
        } else {
            itemList = outputItemRepository.findByOutputAndChange(outputDefinition, version.getLockChange());
        }

        return itemService.loadData(itemList);
    }

    private void publishChangeOutputItem(final ArrFundVersion version, final ArrOutputItem outputItem) {
        notificationService.publishEvent(
                new EventChangeOutputItem(EventType.OUTPUT_ITEM_CHANGE, version.getFundVersionId(),
                        outputItem.getDescItemObjectId(), outputItem.getOutputDefinition().getOutputDefinitionId(), outputItem.getOutputDefinition().getVersion()));
    }

    public ArrOutputDefinition getOutputDefinition(final Integer outputDefinitionId) {
        return outputDefinitionRepository.findOne(outputDefinitionId);
    }

    public List<ArrOutputItem> createOutputItems(final List<ArrOutputItem> outputItems,
                                                 final Integer outputDefinitionId,
                                                 final Integer outputDefinitionVersion,
                                                 final Integer fundVersionId) {
        Assert.notNull(outputItems);
        Assert.notEmpty(outputItems);
        Assert.notNull(outputDefinitionId);
        Assert.notNull(outputDefinitionVersion);
        Assert.notNull(fundVersionId);

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        ArrOutputDefinition outputDefinition = findOutputDefinition(outputDefinitionId);
        Assert.notNull(outputDefinition);

        // uložení uzlu (kontrola optimistických zámků)
        outputDefinition.setVersion(outputDefinitionVersion);
        saveOutputDefinition(outputDefinition);

        return createOutputItems(outputItems, outputDefinition, version, null);
    }

    public List<ArrOutputItem> createOutputItems(final List<ArrOutputItem> outputItems,
                                                    final ArrOutputDefinition outputDefinition,
                                                    final ArrFundVersion version,
                                                    @Nullable final ArrChange createChange) {

        ArrChange change = createChange == null ? arrangementService.createChange() : createChange;
        List<ArrOutputItem> createdItems = new ArrayList<>();
        for (ArrOutputItem outputItem :
                outputItems) {
            outputItem.setOutputDefinition(outputDefinition);
            outputItem.setCreateChange(change);
            outputItem.setDeleteChange(null);
            outputItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

            ArrOutputItem created = createOutputItem(outputItem, version, change);
            createdItems.add(created);

            // sockety
            publishChangeOutputItem(version, created);
        }

        return createdItems;
    }

    public ArrOutputDefinition deleteOutputItemsByTypeWithoutVersion(final Integer fundVersionId,
                                                                     final Integer outputDefinitionId,
                                                                     final Integer outputDefinitionVersion,
                                                                     final Integer descItemTypeId) {
        ArrChange change = arrangementService.createChange();
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        RulItemType descItemType = itemTypeRepository.findOne(descItemTypeId);

        Assert.notNull(fundVersion, "Verze archivní pomůcky neexistuje");
        Assert.notNull(descItemType, "Typ hodnoty atributu neexistuje");

        ArrOutputDefinition outputDefinition = findOutputDefinition(outputDefinitionId);

        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(descItemType, outputDefinition);

        if (outputItems.size() == 0) {
            throw new IllegalStateException("Nebyla nalezena žádná hodnota atributu ke smazání");
        }

        List<ArrOutputItem> outputItemsDeleted = new ArrayList<>(outputItems.size());
        for (ArrOutputItem outputItem : outputItems) {
            outputItemsDeleted.add(deleteOutputItem(outputItem, fundVersion, change, false));
        }

        return outputDefinition;
    }
}
