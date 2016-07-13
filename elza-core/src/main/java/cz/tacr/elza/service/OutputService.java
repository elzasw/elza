package cz.tacr.elza.service;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.ArrOutputDefinition.OutputState;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.CopyActionResult;
import cz.tacr.elza.bulkaction.generator.result.DataceRangeActionResult;
import cz.tacr.elza.bulkaction.generator.result.NodeCountActionResult;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.bulkaction.generator.result.SerialNumberResult;
import cz.tacr.elza.bulkaction.generator.result.TableStatisticActionResult;
import cz.tacr.elza.bulkaction.generator.result.TextAggregationActionResult;
import cz.tacr.elza.bulkaction.generator.result.UnitCountActionResult;
import cz.tacr.elza.bulkaction.generator.result.UnitIdResult;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrItemJsonTable;
import cz.tacr.elza.domain.ArrItemSettings;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemText;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulActionRecommended;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.repository.ActionRecommendedRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeActionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.repository.OutputFileRepository;
import cz.tacr.elza.repository.OutputItemRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventChangeOutputItem;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.output.OutputGeneratorService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
    private NodeRegisterRepository nodeRegisterRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private EventNotificationService notificationService;

    @Autowired
    private OutputResultRepository outputResultRepository;

    @Autowired
    private OutputFileRepository outputFileRepository;

    @Autowired
    private OutputGeneratorService outputGeneratorService;

    @Autowired
    private BulkActionService bulkActionService;

    @Autowired
    private ActionRecommendedRepository actionRecommendedRepository;

    @Autowired
    private ItemSettingsRepository itemSettingsRepository;

    @Autowired
    private ItemTypeActionRepository itemTypeActionRepository;

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


        if (outputDefinition.getState() != OutputState.OPEN &&
                outputDefinition.getState() != OutputState.FINISHED &&
                outputDefinition.getState() != OutputState.OUTDATED) {
            throw new IllegalArgumentException("Nelze smazat výstup v tomto stavu.");
        }

        outputDefinition.setDeleted(true);

        Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(Integer[]::new);
        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, outputIds);
        eventNotificationService.publishEvent(event);

        return outputDefinitionRepository.save(outputDefinition);
    }

    /**
     * Vrátí stav do Otevřeno
     *
     * @param fundVersion      verze AS
     * @param outputDefinition pojmenovaný výstup
     * @return pojmenovaný výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputDefinition revertToOpenState(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                 final ArrOutputDefinition outputDefinition) {
        Assert.notNull(fundVersion);
        Assert.notNull(outputDefinition);

        if (fundVersion.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze vrátit do přípravy výstup v uzavřené verzi AS");
        }

        checkFund(fundVersion, outputDefinition);

        if (outputDefinition.getDeleted()) {
            throw new IllegalArgumentException("Nelze vrátit do přípravy smazaný výstup");
        }

        if (outputDefinition.getState() != OutputState.OUTDATED && outputDefinition.getState() != OutputState.FINISHED) {
            throw new IllegalArgumentException("Do stavu \"V přípravě\" lze vrátit pouze \"Neaktuální\" či \"Vygenerované\" výstupy");
        }

        outputDefinition.setState(OutputState.OPEN);

        ArrOutputResult outputResult = outputDefinition.getOutputResult();
        if (outputResult != null) {
            List<ArrOutputFile> outputFiles = outputResult.getOutputFiles();
            if (outputFiles != null && !outputFiles.isEmpty()) {
                outputFileRepository.delete(outputFiles);
            }
            outputResultRepository.delete(outputResult);
        }

        Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(Integer[]::new);
        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, outputIds);
        eventNotificationService.publishEvent(event);

        return outputDefinitionRepository.save(outputDefinition);
    }

    /**
     * Vytvoří kopii výstupu s nody bez resultů
     *
     * @param fundVersion       verze AS
     * @param originalOutputDef pojmenovaný výstup originál
     * @return pojmenovaný výstup kopie
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputDefinition cloneOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                           final ArrOutputDefinition originalOutputDef) {
        Assert.notNull(fundVersion);
        Assert.notNull(originalOutputDef);

        checkFund(fundVersion, originalOutputDef);

        if (originalOutputDef.getDeleted()) {
            throw new IllegalArgumentException("Nelze klonovat smazaný výstup");
        }

        final ArrOutputDefinition newOutputDef = createOutputDefinition(fundVersion,
                originalOutputDef.getName() + " (kopie)",
                originalOutputDef.getInternalCode() + " (kopie)",
                originalOutputDef.getTemporary(),
                originalOutputDef.getOutputType().getOutputTypeId(),
                originalOutputDef.getTemplate().getTemplateId()
        );

        final ArrChange change = newOutputDef.getOutputs().get(0).getCreateChange();
        final ArrayList<ArrNodeOutput> newNodes = new ArrayList<>();
        originalOutputDef.getOutputNodes().stream().forEach(node -> {
            ArrNodeOutput newNode = new ArrNodeOutput();
            newNode.setCreateChange(change);
            newNode.setNode(node.getNode());
            newNode.setOutputDefinition(newOutputDef);
            newNodes.add(newNode);
        });

        nodeOutputRepository.save(newNodes);

        return newOutputDef;
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
     * @param outputTypeId
     * @param templateId   id šablony
     * @return vytvořený výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputDefinition createOutputDefinition(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                      final String name,
                                                      final String internalCode,
                                                      final Boolean temporary,
                                                      final Integer outputTypeId,
                                                      final Integer templateId) {
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
        outputDefinition.setState(OutputState.OPEN);

        RulOutputType type = outputTypeRepository.findOne(outputTypeId);
        Assert.notNull(type);
        outputDefinition.setOutputType(type);

        if (templateId != null) {
            outputDefinition.setTemplate(templateRepository.findOne(templateId));
        } else {
            outputDefinition.setTemplate(null);
        }

        outputDefinitionRepository.save(outputDefinition);

        ArrChange change = arrangementService.createChange();
        ArrOutput output = createOutputWithChange(outputDefinition, change);
        List<ArrOutput> outputs = new ArrayList<>();
        outputs.add(output);
        outputDefinition.setOutputs(outputs);
        outputDefinitionRepository.save(outputDefinition);

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
    private ArrOutput createOutputWithChange(final ArrOutputDefinition outputDefinition,
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

        final ArrOutputDefinition outputDefinition = output.getOutputDefinition();

        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze odebrat uzly z výstupu, který není ve stavu otevřený");
        }

        checkFund(fundVersion, outputDefinition);

        List<ArrNodeOutput> outputNodes = outputDefinition.getOutputNodes().stream()
                .filter(nodeOutput -> nodeOutput.getDeleteChange() == null)
                .collect(Collectors.toList());

        Set<ArrNodeOutput> nodeOutputs = outputDefinition.getOutputNodes().stream()
                .filter(nodeOutput -> nodeOutput.getDeleteChange() == null && nodeIds.contains(nodeOutput.getNode().getNodeId()))
                .collect(Collectors.toSet());

        if (nodeOutputs.size() > 0) {
            nodeOutputs.stream().forEach(arrNodeOutput -> arrNodeOutput.setDeleteChange(change));
            nodeOutputRepository.save(nodeOutputs);

            outputNodes.removeAll(nodeOutputs);
            Set<ArrNode> allNodes = outputNodes.stream().map(ArrNodeOutput::getNode).collect(Collectors.toSet());

            if (allNodes.size() > 0) {
                storeResults(fundVersion, change, allNodes, outputDefinition, null);
            }

            Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(Integer[]::new);
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

        final ArrOutputDefinition outputDefinition = output.getOutputDefinition();


        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }

        outputDefinition.setName(name);
        outputDefinition.setInternalCode(internalCode);
        if (templateId != null) {
            outputDefinition.setTemplate(templateRepository.findOne(templateId));
        } else {
            outputDefinition.setTemplate(null);
        }

        outputDefinitionRepository.save(outputDefinition);

        Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(Integer[]::new);
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
            throw new IllegalArgumentException("Nelze přidat uzly u zamčeného výstupu");
        }

        final ArrOutputDefinition outputDefinition = output.getOutputDefinition();

        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze přidat uzly k výstupu, který není ve stavu otevřený");
        }

        checkFund(fundVersion, outputDefinition);

        Set<ArrNode> allNodes = outputDefinition.getOutputNodes().stream()
                .filter(arrNodeOutput -> arrNodeOutput.getDeleteChange() == null) // pouze nesmazané nody
                .map(ArrNodeOutput::getNode).collect(Collectors.toSet());

        Set<Integer> nodesIdsDb = allNodes.stream()
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

            allNodes.addAll(nodeOutputs.stream().map(ArrNodeOutput::getNode).collect(Collectors.toSet()));
            storeResults(fundVersion, change, allNodes, outputDefinition, null);

            Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(Integer[]::new);
            EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, outputIds);
            eventNotificationService.publishEvent(event);
        }
    }

    /**
     * Uložení výsledků z hromadných akcí podle nodů - pouze doporučené hromadné akce.
     *
     * @param fundVersion verze AS
     * @param change      změna překlopení
     * @param nodes       seznam uzlů
     */
    private void storeResults(final ArrFundVersion fundVersion,
                              final ArrChange change,
                              final Set<ArrNode> nodes,
                              final ArrOutputDefinition outputDefinition,
                              final RulItemType itemType) {
        List<ArrBulkActionRun> bulkActionRunList = bulkActionService.findBulkActionsByNodes(fundVersion, nodes);
        List<RulActionRecommended> actionRecommendeds = actionRecommendedRepository.findByOutputType(outputDefinition.getOutputType());

        for (ArrBulkActionRun bulkActionRun : bulkActionRunList) {
            RulAction action = bulkActionService.getBulkActionByCode(bulkActionRun.getBulkActionCode());
            for (RulActionRecommended actionRecommended : actionRecommendeds) {
                if (actionRecommended.getAction().equals(action)) {
                    storeResult(bulkActionRun.getResult(), fundVersion, nodes, change, itemType);
                }
            }
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
    public List<ArrOutput> getSortedOutputs(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        Assert.notNull(fundVersion);
        List<ArrOutput> outputs = outputRepository.findByFundVersionSorted(fundVersion);
        return outputs;
    }

    /**
     * Získání výstupů podle verze AS.
     *
     * @param fundVersion verze AS
     * @param state       stav outputu
     * @return seznam výstupů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrOutput> getSortedOutputsByState(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final OutputState state) {
        Assert.notNull(fundVersion);
        return outputRepository.findByFundVersionAndStateSorted(fundVersion, state);
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

    /**
     * Vrací seznam typů výstupu.
     *
     * @return seznam typů výstupu.
     */
    public List<RulOutputType> getOutputTypes() {
        return outputTypeRepository.findAll();
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param item                    data pro hodnotu atributu
     * @param outputDefinitionId      identifikátor výstupu
     * @param outputDefinitionVersion verze výstupu
     * @param fundVersionId           identifikátor verze fondu
     * @return vytvořená hodnota atributu
     */
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

        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }

        checkCalculatingAttribute(outputDefinition, item.getItemType());
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

    /**
     * Vytvoření hodnoty atributu pro výstup.
     *
     * @param outputItem hodnota atributu
     * @param version    verze AS
     * @param change     změna
     * @return vytvořená hodnota atributu
     */
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

    /**
     * Vytvoření hodnoty atributu pro výstup.
     *
     * @param descItemObjectId identfikátor hodnoty
     * @param outputVersion    verze výstupu
     * @param fundVersionId    identifikátor verze fondu
     * @return vytvořená hodnota atributu
     */
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
        ArrOutputDefinition outputDefinition = outputItem.getOutputDefinition();

        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }

        checkCalculatingAttribute(outputDefinition, outputItem.getItemType());

        outputDefinition.setVersion(outputVersion);

        saveOutputDefinition(outputDefinition);

        if (outputItem.getOutputDefinition().getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }

        ArrOutputItem outputItemDeleted = deleteOutputItem(outputItem, fundVersion, change, true);

        return outputItemDeleted;
    }

    /**
     * Smazání hodnoty atributu.
     *
     * @param outputItem hodnota atributu
     * @param version    verze AS
     * @param change     změna
     * @param moveAfter  posunout?
     * @return smazaná hodnota atributu
     */
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

        final ArrOutputDefinition outputDefinition = outputItemDB.getOutputDefinition();
        Assert.notNull(outputDefinition);

        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }

        checkCalculatingAttribute(outputDefinition, outputItemDB.getItemType());

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

    /**
     * @return dohledané Nody navázané na vstupní RegRecord
     */
    public List<ArrNode> getNodesByRegister(RegRecord regRecord) {
        final List<ArrNodeRegister> arrNodeRegisters = nodeRegisterRepository.findByRecordId(regRecord);
        return arrNodeRegisters.stream()
                .map(ArrNodeRegister::getNode)
                .collect(Collectors.toList());
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
        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }
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
                                                                     final Integer descItemTypeId) {
        ArrChange change = arrangementService.createChange();
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        RulItemType descItemType = itemTypeRepository.findOne(descItemTypeId);

        Assert.notNull(fundVersion, "Verze archivní pomůcky neexistuje");
        Assert.notNull(descItemType, "Typ hodnoty atributu neexistuje");

        final ArrOutputDefinition outputDefinition = findOutputDefinition(outputDefinitionId);
        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }

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

    /**
     * Smazání hodnot podle typu atributu.
     *
     * @param fundVersionId           identifikátor verze fondu
     * @param outputDefinitionId      identifikátor výstupu
     * @param outputDefinitionVersion verze výstupu
     * @param itemTypeId              identifikátor typu atributu
     * @return výstup
     */
    public ArrOutputDefinition deleteOutputItemsByType(final Integer fundVersionId,
                                                       final Integer outputDefinitionId,
                                                       final Integer outputDefinitionVersion,
                                                       final Integer itemTypeId) {

        ArrChange change = arrangementService.createChange();
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        RulItemType itemType = itemTypeRepository.findOne(itemTypeId);

        Assert.notNull(fundVersion, "Verze archivní pomůcky neexistuje");
        Assert.notNull(itemType, "Typ atributu neexistuje");

        ArrOutputDefinition outputDefinition = outputDefinitionRepository.findOne(outputDefinitionId);
        Assert.notNull(outputDefinition);

        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }

        checkCalculatingAttribute(outputDefinition, itemType);

        outputDefinition.setVersion(outputDefinitionVersion);
        saveOutputDefinition(outputDefinition);


        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(itemType, outputDefinition);

        if (outputItems.size() == 0) {
            throw new IllegalStateException("Nebyla nalezena žádná hodnota atributu ke smazání");
        }

        List<ArrOutputItem> outputItemsDeleted = new ArrayList<>(outputItems.size());
        outputItemsDeleted.addAll(outputItems.stream()
                .map(descItem -> deleteOutputItem(descItem, fundVersion, change, false))
                .collect(Collectors.toList()));

        return outputDefinition;
    }

    /**
     * Smazání hodnot podle typu atributu.
     *
     * @param fundVersion      verze fondu
     * @param outputDefinition výstup
     * @param itemType         typ atributu
     * @return výstup
     */
    private ArrOutputDefinition deleteOutputItemsByType(final ArrFundVersion fundVersion,
                                                        final ArrOutputDefinition outputDefinition,
                                                        final RulItemType itemType,
                                                        final ArrChange change) {
        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(itemType, outputDefinition);

        if (outputItems.size() == 0) {
            return outputDefinition;
        }

        List<ArrOutputItem> outputItemsDeleted = new ArrayList<>(outputItems.size());
        outputItemsDeleted.addAll(outputItems.stream()
                .map(descItem -> deleteOutputItem(descItem, fundVersion, change, false))
                .collect(Collectors.toList()));

        return outputDefinition;
    }

    /**
     * Změna stavů outputů podle nodů.
     *
     * @param fundVersion    verze AS
     * @param nodes          seznam uzlů
     * @param state          nastavovaný stav
     * @param changingStates filtrované stavy (null pro všechny)
     */
    @Transactional
    public void changeOutputsStateByNodes(final ArrFundVersion fundVersion,
                                          final Set<ArrNode> nodes,
                                          final OutputState state,
                                          final OutputState... changingStates) {
        List<ArrOutputDefinition> outputDefinitions = findOutputsByNodes(fundVersion, nodes, changingStates);
        for (ArrOutputDefinition outputDefinition : outputDefinitions) {
            changeOutputState(outputDefinition, state);
        }
    }

    /**
     * Uložení výsledku z hromadné akce.
     *
     * @param result      výsledek
     * @param fundVersion verze AS
     * @param nodes       seznam uzlů
     * @param change      změna překlopení
     * @param itemType    typ atributu
     */
    @Transactional
    public void storeResult(final Result result,
                            final ArrFundVersion fundVersion,
                            final Set<ArrNode> nodes,
                            final ArrChange change,
                            @Nullable final RulItemType itemType) {

        List<ArrOutputDefinition> outputDefinitions = findOutputsByNodes(fundVersion, nodes, OutputState.OPEN, OutputState.COMPUTING);

        for (ArrOutputDefinition outputDefinition : outputDefinitions) {

            List<ArrItemSettings> itemSettingsList = itemSettingsRepository.findByOutputDefinition(outputDefinition);
            Set<RulItemType> itemTypesIgnored = itemSettingsList.stream()
                    .filter(ArrItemSettings::getBlockActionResult)
                    .map(ArrItemSettings::getItemType)
                    .collect(Collectors.toSet());

            if (result != null) {
                for (ActionResult actionResult : result.getResults()) {
                    storeActionResult(outputDefinition, actionResult, fundVersion, change, itemType, itemTypesIgnored);
                }
            }
            changeOutputState(outputDefinition, OutputState.OPEN);
        }
    }

    /**
     * Změna stavu výstupu.
     *
     * @param outputDefinition výstup
     * @param state            nastavovaný stav
     */
    private void changeOutputState(final ArrOutputDefinition outputDefinition,
                                   final OutputState state) {
        outputDefinition.setState(state);
        outputDefinitionRepository.save(outputDefinition);
        outputGeneratorService.publishOutputStateEvent(outputDefinition, state.name());
    }

    /**
     * Uložení výsledků akcí k výstupu.
     *
     * @param outputDefinition výstup
     * @param actionResult     výsledek akce
     * @param fundVersion      verze AS
     * @param change           změna překlopení
     * @param itemType         typ atributu
     * @param itemTypesIgnored seznam typů atributů, které se nepřeklápí
     */
    private void storeActionResult(final ArrOutputDefinition outputDefinition,
                                   final ActionResult actionResult,
                                   final ArrFundVersion fundVersion,
                                   final ArrChange change,
                                   @Nullable final RulItemType itemType,
                                   @Nullable final Set<RulItemType> itemTypesIgnored) {
        RulItemType type;
        List<ArrItemData> dataItems;

        if (actionResult instanceof CopyActionResult) {
            CopyActionResult copyActionResult = (CopyActionResult) actionResult;
            String itemTypeCode = copyActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            dataItems = copyActionResult.getDataItems();
        } else if (actionResult instanceof DataceRangeActionResult) {
            DataceRangeActionResult dataceRangeActionResult = (DataceRangeActionResult) actionResult;
            String itemTypeCode = dataceRangeActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            ArrItemString itemString = new ArrItemString();
            itemString.setValue(dataceRangeActionResult.getText());
            dataItems = Arrays.asList(itemString);
        } else if (actionResult instanceof NodeCountActionResult) {
            NodeCountActionResult nodeCountActionResult = (NodeCountActionResult) actionResult;
            String itemTypeCode = nodeCountActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            ArrItemInt itemInt = new ArrItemInt();
            itemInt.setValue(nodeCountActionResult.getCount());
            dataItems = Arrays.asList(itemInt);
        } else if (actionResult instanceof SerialNumberResult) {
            return; // tohle se nikam nepřeklápí zatím
        } else if (actionResult instanceof TableStatisticActionResult) {
            TableStatisticActionResult tableStatisticActionResult = (TableStatisticActionResult) actionResult;
            String itemTypeCode = tableStatisticActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            ArrItemJsonTable itemJsonTable = new ArrItemJsonTable();
            itemJsonTable.setValue(tableStatisticActionResult.getTable());
            dataItems = Arrays.asList(itemJsonTable);
        } else if (actionResult instanceof TextAggregationActionResult) {
            TextAggregationActionResult textAggregationActionResult = (TextAggregationActionResult) actionResult;
            String itemTypeCode = textAggregationActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            ArrItemText itemText = new ArrItemText();
            itemText.setValue(textAggregationActionResult.getText());
            dataItems = Arrays.asList(itemText);
        } else if (actionResult instanceof UnitCountActionResult) {
            UnitCountActionResult unitCountActionResult = (UnitCountActionResult) actionResult;
            String itemTypeCode = unitCountActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            ArrItemInt itemInt = new ArrItemInt();
            itemInt.setValue(unitCountActionResult.getCount());
            dataItems = Arrays.asList(itemInt);
        } else if (actionResult instanceof UnitIdResult) {
            return; // tohle se nikam nepřeklápí zatím
        } else {
            throw new IllegalStateException("Nedefinovný typ výsledku: " + actionResult.getClass().getSimpleName());
        }

        if (itemTypesIgnored != null && itemTypesIgnored.contains(type)) {
            return;
        }

        if (itemType == null || itemType.equals(type)) {
            storeDataItems(type, dataItems, outputDefinition, fundVersion, change);
        }
    }

    /**
     * Uložení dat typu pro výstup.
     *
     * @param type             typ atributu
     * @param dataItems        seznam dat pro uložení
     * @param outputDefinition výstup
     * @param fundVersion      verze AS
     * @param change           změna překlopení
     */
    private void storeDataItems(final RulItemType type,
                                final List<ArrItemData> dataItems,
                                final ArrOutputDefinition outputDefinition,
                                final ArrFundVersion fundVersion,
                                final ArrChange change) {
        deleteOutputItemsByType(fundVersion, outputDefinition, type, change);
        for (ArrItemData dataItem : dataItems) {
            ArrOutputItem outputItem = new ArrOutputItem(dataItem);
            outputItem.setItemType(type);
            outputItem.setItemSpec(dataItem.getSpec());
            createOutputItem(outputItem, outputDefinition, fundVersion, change);
        }
    }

    /**
     * Vyhledání výstupů podle uzlů.
     *
     * @param fundVersion verze AS
     * @param nodes       seznam uzlů
     * @param states      stavy, podle kterých vyhledáváme
     * @return seznam výstupů
     */
    public List<ArrOutputDefinition> findOutputsByNodes(final ArrFundVersion fundVersion,
                                                        final Set<ArrNode> nodes,
                                                        final OutputState... states) {
        return outputDefinitionRepository.findOutputsByNodes(fundVersion, nodes, states);
    }

    /**
     * Změnit typ kalkulace typu atributu - uživatelsky/automaticky.
     *
     * @param outputDefinition pojmenovaný výstup
     * @param fundVersion      verze AS
     * @param itemType         typ atributu
     */
    public void switchOutputCalculating(final ArrOutputDefinition outputDefinition,
                                        final ArrFundVersion fundVersion,
                                        final RulItemType itemType) {
        Assert.notNull(outputDefinition, "Neplatný výstup");
        Assert.notNull(fundVersion, "Neplatná verze fondu");
        Assert.notNull(itemType, "Neplatný typ atributu");

        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }

        ArrItemSettings itemSettings = itemSettingsRepository.findOneByOutputDefinitionAndItemType(outputDefinition, itemType);

        ArrChange change = arrangementService.createChange();

        if (itemSettings == null) {
            itemSettings = new ArrItemSettings();
            itemSettings.setBlockActionResult(true);
            itemSettings.setItemType(itemType);
            itemSettings.setOutputDefinition(outputDefinition);
            itemSettingsRepository.save(itemSettings);

            List<ArrOutputItem> items = outputItemRepository.findOpenOutputItems(itemType, outputDefinition);
            for (ArrOutputItem item : items) {
                publishChangeOutputItem(fundVersion, item);
            }
        } else {
            itemSettingsRepository.delete(itemSettings);
            Set<ArrNode> nodes = outputDefinition.getOutputNodes().stream().map(ArrNodeOutput::getNode).collect(Collectors.toSet());
            deleteOutputItemsByType(fundVersion, outputDefinition, itemType, change);
            storeResults(fundVersion, change, nodes, outputDefinition, itemType);
        }
    }

    /**
     * Kontrola, že neměníme typ atributu, který je počítán automaticky.
     *
     * @param outputDefinition pojmenovaný výstup
     * @param itemType         typ atributu
     */
    private void checkCalculatingAttribute(final ArrOutputDefinition outputDefinition,
                                           final RulItemType itemType) {
        RulItemTypeAction itemTypeAction = itemTypeActionRepository.findOneByItemTypeCode(itemType.getCode());
        if (itemTypeAction != null) {
            ArrItemSettings itemSettings = itemSettingsRepository.findOneByOutputDefinitionAndItemType(outputDefinition, itemType);
            if (itemSettings == null || itemSettings.getBlockActionResult() == false) {
                throw new IllegalStateException("Tento atribut je počítán automaticky a nemůže být ručně editován");
            }
        }
    }
}
