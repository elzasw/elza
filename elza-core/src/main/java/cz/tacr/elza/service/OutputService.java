package cz.tacr.elza.service;

import static cz.tacr.elza.domain.RulItemType.Type.RECOMMENDED;
import static cz.tacr.elza.domain.RulItemType.Type.REQUIRED;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;


import com.fasterxml.jackson.databind.deser.Deserializers;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.exception.ObjectNotFoundException;

import cz.tacr.elza.repository.ItemSpecRepository;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.CopyActionResult;
import cz.tacr.elza.bulkaction.generator.result.DateRangeActionResult;
import cz.tacr.elza.bulkaction.generator.result.NodeCountActionResult;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.bulkaction.generator.result.SerialNumberResult;
import cz.tacr.elza.bulkaction.generator.result.TableStatisticActionResult;
import cz.tacr.elza.bulkaction.generator.result.TestDataGeneratorResult;
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
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulActionRecommended;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.interfaces.IArrItemStringValue;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.OutputCode;
import cz.tacr.elza.repository.ActionRecommendedRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
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

/**
 * Serviska pro práci s výstupy.
 *
 * @author Martin Šlapa
 * @author Petr Pytelka
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

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    private static final Logger logger = LoggerFactory.getLogger(OutputService.class);

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
            throw new BusinessException("Nelze smazat výstup v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        checkFund(fundVersion, outputDefinition);

        if (outputDefinition.getDeleted()) {
            throw new BusinessException("Nelze smazat již smazaný výstup", OutputCode.ALREADY_DELETED);
        }

        List<OutputState> allowedState = Arrays.asList(OutputState.OPEN, OutputState.FINISHED, OutputState.OUTDATED);

        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze smazat výstup v tomto stavu: " + outputDefinition.getState(),
                    OutputCode.CANNOT_DELETED_IN_STATE).set("state", outputDefinition.getState());
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
            throw new BusinessException("Nelze vrátit do přípravy výstup v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        checkFund(fundVersion, outputDefinition);

        if (outputDefinition.getDeleted()) {
            throw new BusinessException("Nelze vrátit do přípravy smazaný výstup", OutputCode.CANNOT_CHANGE_STATE);
        }

        List<OutputState> allowedState = Arrays.asList(OutputState.FINISHED, OutputState.OUTDATED);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Do stavu \"V přípravě\" lze vrátit pouze \"Neaktuální\" či \"Vygenerované\" výstupy", OutputCode.CANNOT_CHANGE_STATE);
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
            throw new BusinessException("Nelze klonovat smazaný výstup", OutputCode.CANNOT_CLONE_DELETED);
        }

        String copy = " - kopie";
        String newName = originalOutputDef.getName() + copy;
        if (outputDefinitionRepository.existsByName(newName)) {
            int num = 1;
            String newNameWithNum;
            do {
                newNameWithNum = newName + " " + num++;
            } while(outputDefinitionRepository.existsByName(newNameWithNum));
            newName = newNameWithNum;
        }

        final ArrOutputDefinition newOutputDef = createOutputDefinition(fundVersion,
                newName,
                originalOutputDef.getInternalCode(),
                originalOutputDef.getTemporary(),
                originalOutputDef.getOutputType().getOutputTypeId(),
                originalOutputDef.getTemplate() != null ? originalOutputDef.getTemplate().getTemplateId() : null
                );

        final ArrChange change = newOutputDef.getOutputs().get(0).getCreateChange();
        final ArrayList<ArrNodeOutput> newNodes = new ArrayList<>();
        originalOutputDef.getOutputNodes().forEach(node -> {
            if (node.getDeleteChange() == null) {
                ArrNodeOutput newNode = new ArrNodeOutput();
                newNode.setCreateChange(change);
                newNode.setNode(node.getNode());
                newNode.setOutputDefinition(newOutputDef);
                newNodes.add(newNode);
            }
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
        ArrChange change = arrangementService.createChange(null);
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
            throw new BusinessException("Nelze zamknout výstup v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        checkFund(fundVersion, output.getOutputDefinition());

        if (output.getLockChange() != null) {
            throw new BusinessException("Výstup je již zamknutý", ArrangementCode.VERSION_ALREADY_CLOSED);
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
            throw new BusinessException("Nelze vytvořit výstup v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
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

        ArrChange change = arrangementService.createChange(null);
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
        ArrChange change = arrangementService.createChange(ArrChange.Type.REMOVE_NODES_OUTPUT);
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
            throw new BusinessException("Nelze odebrat uzly u výstupu v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        if (output.getLockChange() != null) {
            throw new BusinessException("Nelze odebrat uzly u zamčeného výstupu", OutputCode.LOCKED);
        }

        final ArrOutputDefinition outputDefinition = output.getOutputDefinition();

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze odebrat uzly z výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkFund(fundVersion, outputDefinition);

        List<ArrNodeOutput> outputNodes = outputDefinition.getOutputNodes().stream()
                .filter(nodeOutput -> nodeOutput.getDeleteChange() == null)
                .collect(Collectors.toList());

        Set<ArrNodeOutput> nodeOutputs = new HashSet<>();
        for (ArrNodeOutput nodeOutput : outputDefinition.getOutputNodes()) {
            if (nodeOutput.getDeleteChange() == null) {
                ArrNode node = nodeOutput.getNode();
                if (nodeIds.contains(node.getNodeId())) {
                    nodeOutput.setDeleteChange(change);
                    nodeOutputs.add(nodeOutput);
                }
            }
        }

        if (nodeIds.size() != nodeOutputs.size()) {
            throw new BusinessException("Byl předán seznam s neplatným identifikátorem uzlu: " + nodeIds, ArrangementCode.NODE_NOT_FOUND).set("id", nodeIds);
        }

        nodeOutputRepository.save(nodeOutputs);

        Set<ArrNode> oldNodes = outputNodes.stream().map(ArrNodeOutput::getNode).collect(Collectors.toSet());
        outputNodes.removeAll(nodeOutputs);
        Set<ArrNode> newNodes = outputNodes.stream().map(ArrNodeOutput::getNode).collect(Collectors.toSet());

        storeResults(fundVersion, change, oldNodes, newNodes, outputDefinition, null);

        Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(Integer[]::new);
        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, outputIds);
        eventNotificationService.publishEvent(event);
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
            throw new BusinessException("Nelze upravovat výstup v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        if (output.getLockChange() != null) {
            throw new BusinessException("Nelze upravit uzavřený výstup", OutputCode.LOCKED);
        }

        final ArrOutputDefinition outputDefinition = output.getOutputDefinition();

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
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
            throw new SystemException("Output a verze AS nemají společný AS", BaseCode.DB_INTEGRITY_PROBLEM);
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
        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_NODES_OUTPUT);
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
            throw new BusinessException("Nelze přidat uzly k výstupu v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        if (output.getLockChange() != null) {
            throw new BusinessException("Nelze přidat uzly u zamčeného výstupu", OutputCode.LOCKED);
        }

        final ArrOutputDefinition outputDefinition = output.getOutputDefinition();

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze přidat uzly k výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkFund(fundVersion, outputDefinition);

        Set<ArrNode> newNodes = outputDefinition.getOutputNodes().stream()
                .filter(arrNodeOutput -> arrNodeOutput.getDeleteChange() == null) // pouze nesmazané nody
                .map(ArrNodeOutput::getNode).collect(Collectors.toSet());

        Set<Integer> nodesIdsDb = newNodes.stream()
                .map(ArrNode::getNodeId)
                .collect(Collectors.toSet());

        for (Integer nodeId : nodesIdsDb) {
            for (Integer nodeIdAdd : nodeIds) {
                if (nodeId.equals(nodeIdAdd)) {
                    throw new BusinessException("Nelze přidat již přidaný uzel. (ID=" + nodeIdAdd + ")", ArrangementCode.ALREADY_ADDED);
                }
            }
        }

        List<ArrNode> nodes = nodeRepository.findAll(nodeIds);

        if (nodes.size() != nodeIds.size()) {
            throw new BusinessException("Byl předán seznam s neplatným identifikátorem uzlu: " + nodeIds, ArrangementCode.NODE_NOT_FOUND).set("id", nodeIds);
        }

        List<ArrNodeOutput> nodeOutputs = new ArrayList<>(nodes.size());
        for (ArrNode node : nodes) {
            ArrNodeOutput nodeOutput = new ArrNodeOutput();
            nodeOutput.setNode(node);
            nodeOutput.setOutputDefinition(outputDefinition);
            nodeOutput.setCreateChange(change);
            nodeOutputs.add(nodeOutput);
        }

        nodeOutputRepository.save(nodeOutputs);

        Set<ArrNode> oldNodes = new HashSet<>(newNodes);
        newNodes.addAll(nodes);
        storeResults(fundVersion, change, oldNodes, newNodes, outputDefinition, null);

        Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(Integer[]::new);
        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, outputIds);
        eventNotificationService.publishEvent(event);
    }

    /**
     * Uložení výsledků z hromadných akcí podle nodů - pouze doporučené hromadné akce.
     *
     * - pokud jsou seznamy {oldNodes} a {newNodes} rozdílené, ještě se provede smazání odlišných počítaných atributů
     *
     * @param fundVersion verze AS
     * @param change      změna překlopení
     * @param oldNodes    seznam původních uzlů
     * @param newNodes    seznam nových uzlů
     */
    private boolean storeResults(final ArrFundVersion fundVersion,
                                 final ArrChange change,
                                 final Set<ArrNode> oldNodes,
                                 final Set<ArrNode> newNodes,
                                 final ArrOutputDefinition outputDefinition,
                                 final RulItemType itemType) {
        List<RulItemType> itemTypesDelete = null;
        // pokud se jedná o rozdílné seznamy, je potřeba odstranit odlišné počítané atributy; cenu mazat má jen v případě, že předchozí stav má alespoň jeden uzel
        if ((!oldNodes.containsAll(newNodes) || !newNodes.containsAll(oldNodes)) && oldNodes.size() > 0) {
            List<RulItemType> itemTypesOld = findCountItemTypes(fundVersion, oldNodes);
            if (newNodes.size() > 0) {
                itemTypesDelete = new ArrayList<>(itemTypesOld);
                List<RulItemType> itemTypesNew = findCountItemTypes(fundVersion, newNodes);

                // odeberu všechny, které jsou v budoucím stavu (ty nemusím mazat)
                itemTypesDelete.removeAll(itemTypesNew);
            } else {
                // pokud nejsou žádné uzly u budoucího stavu, smažu všechny počítané typy
                itemTypesDelete = itemTypesOld;
            }
        }

        // pokud je co ke smazání, provede se výmaz typů u výstupů
        if (itemTypesDelete != null && itemTypesDelete.size() > 0) {
            for (RulItemType rulItemType : itemTypesDelete) {
                deleteOutputItemsByType(fundVersion, outputDefinition, rulItemType, change);
            }
        }

        if (newNodes.size() == 0) {
            return false;
        }

        List<ArrBulkActionRun> bulkActionRunList = bulkActionService.findBulkActionsByNodes(fundVersion, newNodes);
        List<RulActionRecommended> actionRecommendeds = actionRecommendedRepository.findByOutputType(outputDefinition.getOutputType());

        ArrChangeLazy changeLazy = () -> change;

        boolean result = false;
        for (ArrBulkActionRun bulkActionRun : bulkActionRunList) {
            RulAction action = bulkActionService.getBulkActionByCode(bulkActionRun.getBulkActionCode());
            for (RulActionRecommended actionRecommended : actionRecommendeds) {
                if (actionRecommended.getAction().equals(action)) {
                    Boolean changed = storeResultInternal(bulkActionRun.getResult(), fundVersion, newNodes, changeLazy, itemType).getSecond();
                    if (changed) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Vyhledá podle uzlů typy atributů, které se automaticky počítají.
     *
     * @param fundVersion verze fondu
     * @param nodes       seznam uzlů
     * @return seznam typů atributů, které se pro seznam uzlů automaticky počítají
     */
    private List<RulItemType> findCountItemTypes(final ArrFundVersion fundVersion, final Set<ArrNode> nodes) {
        List<ArrBulkActionRun> bulkActionRunListOld = bulkActionService.findBulkActionsByNodes(fundVersion, nodes);

        // získám kódy hromadných akcí
        List<String> actionCodes = new ArrayList<>(bulkActionRunListOld.size());
        for (ArrBulkActionRun bulkActionRun : bulkActionRunListOld) {
            actionCodes.add(bulkActionRun.getBulkActionCode());
        }
        // získám hromadné akce, které souvisí s výstupem
        List<RulAction> actions = bulkActionService.getBulkActionByCodes(actionCodes);

        List<RulItemType> itemTypes = new ArrayList<>();
        if (actions.size() > 0) {
            // vyhledám typy atributů, které jsou počítané a souvisí s výstupem
            itemTypes = itemTypeActionRepository.findByAction(actions);
        }
        return itemTypes;
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
    public List<RulOutputType> getOutputTypes(final Integer versionId) {
        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
        return outputTypeRepository.findByRulPackage(version.getRuleSet().getPackage());
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
    @AuthMethod(permission = {UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputItem createOutputItem(final ArrOutputItem item,
                                          final Integer outputDefinitionId,
                                          final Integer outputDefinitionVersion,
                                          @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        Assert.notNull(item);
        Assert.notNull(outputDefinitionId);
        Assert.notNull(outputDefinitionVersion);

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);

        ArrOutputDefinition outputDefinition = outputDefinitionRepository.findOne(outputDefinitionId);
        Assert.notNull(outputDefinition);
        outputDefinition.setVersion(outputDefinitionVersion);
        saveOutputDefinition(outputDefinition);

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkCalculatingAttribute(outputDefinition, item.getItemType());
        item.setUndefined(false);
        return createOutputItem(item, outputDefinition, fundVersion, null);
    }

    /**
     * Uložení definice výstupu - zámky.
     *
     * @param outputDefinition pojmenovaný výstup
     * @return pojmenovaný výstup
     */
    private ArrOutputDefinition saveOutputDefinition(final ArrOutputDefinition outputDefinition) {
        outputDefinition.setLastUpdate(LocalDateTime.now());
        outputDefinitionRepository.save(outputDefinition);
        outputDefinitionRepository.flush();
        return outputDefinition;
    }

    /**
     * Vytvoření výstupu.
     *
     * @param outputItem       hodnota atributu
     * @param outputDefinition pojmenovaný výstup
     * @param fundVersion      verze AS
     * @param createChange     použitá změna
     * @return
     */
    public ArrOutputItem createOutputItem(final ArrOutputItem outputItem,
                                          final ArrOutputDefinition outputDefinition,
                                          final ArrFundVersion fundVersion,
                                          @Nullable final ArrChange createChange) {
        ArrChange change = createChange == null ? arrangementService.createChange(null) : createChange;

        outputItem.setOutputDefinition(outputDefinition);

        outputItem.setOutputDefinition(outputDefinition);
        outputItem.setCreateChange(change);
        outputItem.setDeleteChange(null);
        outputItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

        ArrOutputItem outputItemCreated = createOutputItem(outputItem, fundVersion, change);

        // sockety
        publishChangeOutputItem(fundVersion, outputItemCreated);

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
    @AuthMethod(permission = {UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputItem deleteOutputItem(final Integer descItemObjectId,
                                          final Integer outputVersion,
                                          @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        Assert.notNull(descItemObjectId);
        Assert.notNull(outputVersion);
        Assert.notNull(fundVersionId);

        ArrChange change = arrangementService.createChange(null);
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(descItemObjectId);

        if (outputItems.size() > 1) {
            throw new SystemException("Hodnota musí být právě jedna", BaseCode.DB_INTEGRITY_PROBLEM);
        } else if (outputItems.size() == 0) {
            throw new SystemException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }

        ArrOutputItem outputItem = outputItems.get(0);
        ArrOutputDefinition outputDefinition = outputItem.getOutputDefinition();

        if (outputDefinition.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }

        checkCalculatingAttribute(outputDefinition, outputItem.getItemType());

        outputDefinition.setVersion(outputVersion);

        saveOutputDefinition(outputDefinition);

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
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

    /**
     * Úprava hodnoty atributu.
     *
     * @param outputItem              hodnota atributu
     * @param outputDefinitionVersion verze outputu
     * @param fundVersionId           identifikátor verze fondu
     * @param createNewVersion        vytvořit novou?
     * @return upravená hodnota atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputItem updateOutputItem(final ArrOutputItem outputItem,
                                          final Integer outputDefinitionVersion,
                                          @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                          final Boolean createNewVersion) {
        Assert.notNull(outputItem);
        Assert.notNull(outputItem.getPosition());
        Assert.notNull(outputItem.getDescItemObjectId());
        Assert.notNull(outputDefinitionVersion);
        Assert.notNull(fundVersionId);
        Assert.notNull(createNewVersion);

        ArrChange change = null;
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);

        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(outputItem.getDescItemObjectId());

        if (outputItems.size() > 1) {
            throw new SystemException("Hodnota musí být právě jedna", BaseCode.DB_INTEGRITY_PROBLEM);
        } else if (outputItems.size() == 0) {
            throw new SystemException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }
        ArrOutputItem outputItemDB = outputItems.get(0);

        final ArrOutputDefinition outputDefinition = outputItemDB.getOutputDefinition();
        Assert.notNull(outputDefinition);

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkCalculatingAttribute(outputDefinition, outputItemDB.getItemType());

        if (createNewVersion) {
            outputDefinition.setVersion(outputDefinitionVersion);

            // uložení uzlu (kontrola optimistických zámků)
            saveOutputDefinition(outputDefinition);

            // vytvoření změny
            change = arrangementService.createChange(null);
        }

        ArrOutputItem outputItemUpdated = updateOutputItem(outputItem, outputItemDB, fundVersion, change, createNewVersion);

        return outputItemUpdated;
    }

    /**
     * Úprava hodnoty atributu.
     *
     * @param outputItem       hodnota atributu
     * @param outputItemDB     hodnota atributu z DB
     * @param version          verze AS
     * @param change           změna pro úpravu
     * @param createNewVersion odverzovat?
     * @return upravená hodnota
     */
    public ArrOutputItem updateOutputItem(final ArrOutputItem outputItem,
                                          final ArrOutputItem outputItemDB,
                                          final ArrFundVersion version,
                                          final ArrChange change,
                                          final Boolean createNewVersion) {

        if (createNewVersion ^ change != null) {
            throw new SystemException("Pokud vytvářím novou verzi, musí být předaná reference změny. Pokud verzi nevytvářím, musí být reference změny null.");
        }

        if (createNewVersion && version.getLockChange() != null) {
            throw new SystemException("Nelze provést verzovanou změnu v uzavřené verzi.");
        }

        ArrOutputItem outputItemOrig;
        if (outputItemDB == null) {
            List<ArrOutputItem> descItems = outputItemRepository.findOpenOutputItems(outputItem.getDescItemObjectId());

            if (descItems.size() > 1) {
                throw new SystemException("Hodnota musí být právě jedna", BaseCode.DB_INTEGRITY_PROBLEM);
            } else if (descItems.size() == 0) {
                throw new SystemException("Hodnota neexistuje, pravděpodobně byla již smazána");
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
                throw new SystemException(e);
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

    /**
     * Vyhledání definice podle identifikátoru výstupu.
     *
     * @param outputDefinitionId identifikátor výstupu
     * @return výstup
     */
    public ArrOutputDefinition findOutputDefinition(final Integer outputDefinitionId) {
        return outputDefinitionRepository.findOne(outputDefinitionId);
    }

    /**
     * Vyhledání hodnot atributu výstupu.
     *
     * @param fundVersion      verze AS
     * @param outputDefinition pojmenovaný výstup
     * @return seznam hodnot atrubutů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrOutputItem> getOutputItems(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                              final ArrOutputDefinition outputDefinition) {
        return getOutputItemsInner(fundVersion, outputDefinition);
    }

    /**
     * Vyhledání hodnot atributu výstupu.
     *
     * @param fundVersion      verze AS
     * @param outputDefinition pojmenovaný výstup
     * @return seznam hodnot atrubutů
     */
    public List<ArrOutputItem> getOutputItemsInner(final ArrFundVersion fundVersion,
                                                   final ArrOutputDefinition outputDefinition) {
        List<ArrOutputItem> itemList;

        if (fundVersion.getLockChange() == null) {
            itemList = outputItemRepository.findByOutputAndDeleteChangeIsNull(outputDefinition);
        } else {
            itemList = outputItemRepository.findByOutputAndChange(outputDefinition, fundVersion.getLockChange());
        }

        Map<Integer, RulItemTypeExt> itemTypeExtMap = ruleService.getOutputItemTypes(outputDefinition).stream().collect(Collectors.toMap(RulItemType::getItemTypeId, Function.identity()));
        itemService.loadData(itemList);
        for (ArrOutputItem outputItem : itemList) {
            if (outputItem.getItem() == null) {
                RulItemTypeExt itemTypeExt = itemTypeExtMap.get(outputItem.getItemTypeId());
                if (itemTypeExt.getIndefinable()) {
                    outputItem.setItem(descItemFactory.createItemByType(itemTypeExt.getDataType()));
                }
            }
        }

        return itemList;
    }

    /**
     * Publikovat změnu - sockety.
     *
     * @param fundVersion verze AS
     * @param outputItem  hodnota atributu
     */
    private void publishChangeOutputItem(final ArrFundVersion fundVersion,
                                         final ArrOutputItem outputItem) {
        notificationService.publishEvent(
                new EventChangeOutputItem(EventType.OUTPUT_ITEM_CHANGE, fundVersion.getFundVersionId(),
                        outputItem.getDescItemObjectId(), outputItem.getOutputDefinition().getOutputDefinitionId(), outputItem.getOutputDefinition().getVersion()));
    }

    /**
     * @return dohledané Nody navázané na vstupní RegRecord
     */
    public List<ArrNode> getNodesByRegister(final RegRecord regRecord) {
        final List<ArrNodeRegister> arrNodeRegisters = nodeRegisterRepository.findByRecordId(regRecord);
        return arrNodeRegisters.stream()
                .map(ArrNodeRegister::getNode)
                .collect(Collectors.toList());
    }

    /**
     * Vytvoření hodnoty atributu výstupu.
     *
     * @param outputItems             seznam hodnot
     * @param outputDefinitionId      identifikátor výstupu
     * @param outputDefinitionVersion verze výstupu
     * @param fundVersionId           identifikátor verze fondu
     * @return seznam hodnot
     */
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

    /**
     * Vytvoření hodnoty atributu výstupu.
     *
     * @param outputItems      seznam hodnot
     * @param outputDefinition pojmenovaný výstup
     * @param version          verze AS
     * @param createChange     použitá změna
     * @return seznam hodnot
     */
    public List<ArrOutputItem> createOutputItems(final List<ArrOutputItem> outputItems,
                                                 final ArrOutputDefinition outputDefinition,
                                                 final ArrFundVersion version,
                                                 @Nullable final ArrChange createChange) {
        List<OutputState> allowStates = Collections.singletonList(OutputState.OPEN);
        if (!allowStates.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }
        ArrChange change = createChange == null ? arrangementService.createChange(null) : createChange;
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

    /**
     * Smazání hodnot výstupů podle typu atributu.
     *
     * @param fundVersionId      identifikátor verze fondu
     * @param outputDefinitionId identifikátor výstupu
     * @param descItemTypeId     identifikátor typu atributu
     * @return výstup
     */
    public ArrOutputDefinition deleteOutputItemsByTypeWithoutVersion(final Integer fundVersionId,
                                                                     final Integer outputDefinitionId,
                                                                     final Integer descItemTypeId) {
        ArrChange change = arrangementService.createChange(null);
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        RulItemType descItemType = itemTypeRepository.findOne(descItemTypeId);

        Assert.notNull(fundVersion, "Verze archivní pomůcky neexistuje");
        Assert.notNull(descItemType, "Typ hodnoty atributu neexistuje");

        final ArrOutputDefinition outputDefinition = findOutputDefinition(outputDefinitionId);
        List<OutputState> allowStates = Collections.singletonList(OutputState.OPEN);
        if (!allowStates.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
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

        ArrChange change = arrangementService.createChange(null);
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        RulItemType itemType = itemTypeRepository.findOne(itemTypeId);

        Assert.notNull(fundVersion, "Verze archivní pomůcky neexistuje");
        Assert.notNull(itemType, "Typ atributu neexistuje");

        ArrOutputDefinition outputDefinition = outputDefinitionRepository.findOne(outputDefinitionId);
        Assert.notNull(outputDefinition);

        List<OutputState> allowStates = Collections.singletonList(OutputState.OPEN);
        if (!allowStates.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstup, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
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
     * - kontroluje, jestli ukládané typy odpovídají přípustný a naopak
     *
     * @param bulkActionRun hromadná akce
     * @param nodes         seznam uzlů
     * @param itemType      typ atributu
     */
    @Transactional
    public void storeResultBulkAction(final ArrBulkActionRun bulkActionRun,
                                      final Set<ArrNode> nodes,
                                      @Nullable final RulItemType itemType) {
        ArrChangeLazy change = new ArrChangeLazy() {
            private ArrChange change = null;

            @Override
            public ArrChange getOrCreateChange() {
                if (change == null) {
                    change = arrangementService.createChange(ArrChange.Type.UPDATE_OUTPUT);
                }
                return change;
            }
        };

        List<RulItemType> itemTypes = storeResultInternal(bulkActionRun.getResult(), bulkActionRun.getFundVersion(), nodes, change, itemType).getFirst();

        RulAction action = bulkActionService.getBulkActionByCode(bulkActionRun.getBulkActionCode());
        List<RulItemType> recommendedItemTypes = itemTypeActionRepository.findByAction(Collections.singletonList(action));

        List<RulItemType> itemTypesMissing = new ArrayList<>(recommendedItemTypes);
        itemTypesMissing.removeAll(itemTypes);

        if (itemTypesMissing.size() > 0) {
            logger.warn("Při ukládání výsledků z hromadné akce '" + bulkActionRun.getBulkActionCode()
            + "' nebyly nalezeny přípustné typy atributů: "
            + itemTypesMissing.stream().map(RulItemType::getCode).collect(Collectors.joining(", ")));
        }

        List<RulItemType> itemTypesMoreover = new ArrayList<>(itemTypes);
        itemTypesMoreover.removeAll(recommendedItemTypes);

        if (itemTypesMoreover.size() > 0) {
            logger.warn("Při ukládání výsledků z hromadné akce '" + bulkActionRun.getBulkActionCode()
            + "' byly nalezeny typy atributů, které nejsou v seznamu přípustných: "
            + itemTypesMoreover.stream().map(RulItemType::getCode).collect(Collectors.joining(", ")));
        }
    }

    /**
     * Uložení výsledku do výstupů.
     *
     * @param result      výsledek, může být null
     * @param fundVersion verze AS
     * @param nodes       seznam uzlů
     * @param change      změna překlopení
     * @param itemType    typ atributu
     */
    public Pair<List<RulItemType>, Boolean> storeResultInternal(final Result result,
                                                                final ArrFundVersion fundVersion,
                                                                final Set<ArrNode> nodes,
                                                                final ArrChangeLazy change,
                                                                @Nullable final RulItemType itemType) {
        if (nodes.size() == 0) {
            return Pair.of(Collections.emptyList(), false);
        }

        boolean changed = false;
        List<ArrOutputDefinition> outputDefinitions = findOutputsByNodes(fundVersion, nodes, OutputState.OPEN, OutputState.COMPUTING);

        List<RulItemType> itemTypesResult = new ArrayList<>();
        for (ArrOutputDefinition outputDefinition : outputDefinitions) {

            if (result != null) {
                // Prepare set of ignored item types
                List<ArrItemSettings> itemSettingsList = itemSettingsRepository.findByOutputDefinition(outputDefinition);
                Set<RulItemType> itemTypesIgnored = itemSettingsList.stream()
                        .filter(ArrItemSettings::getBlockActionResult)
                        .map(ArrItemSettings::getItemType)
                        .collect(Collectors.toSet());

                // načtení typy atributů z pravidel výstupů,
                // přidá do seznamu ignorovaných ty, které jsou nemožné
                List<RulItemTypeExt> outputItemTypes = ruleService.getOutputItemTypes(outputDefinition);
                for (RulItemTypeExt outputItemType : outputItemTypes) {
                    if (outputItemType.getType().equals(RulItemType.Type.IMPOSSIBLE)) {
                        itemTypesIgnored.add(outputItemType);
                    }
                }

                for (ActionResult actionResult : result.getResults()) {
                    Pair<RulItemType, Boolean> resultPair = storeActionResult(outputDefinition, actionResult, fundVersion, change, itemType, itemTypesIgnored);
                    if (resultPair != null) {
                        RulItemType itemTypeStore = resultPair.getFirst();
                        itemTypesResult.add(itemTypeStore);
                        if (!changed) {
                            changed = resultPair.getSecond();
                        }
                    }
                }
            }
            changeOutputState(outputDefinition, OutputState.OPEN);
        }

        return Pair.of(itemTypesResult, changed);
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
    private Pair<RulItemType, Boolean> storeActionResult(final ArrOutputDefinition outputDefinition,
                                                         final ActionResult actionResult,
                                                         final ArrFundVersion fundVersion,
                                                         final ArrChangeLazy change,
                                                         @Nullable final RulItemType itemType,
                                                         @Nullable final Set<RulItemType> itemTypesIgnored) {
        RulItemType type;
        List<ArrItemData> dataItems;

        if (actionResult instanceof CopyActionResult) {
            CopyActionResult copyActionResult = (CopyActionResult) actionResult;
            String itemTypeCode = copyActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            dataItems = copyActionResult.getDataItems();
        } else if (actionResult instanceof DateRangeActionResult) {
            DateRangeActionResult dateRangeActionResult = (DateRangeActionResult) actionResult;
            String itemTypeCode = dateRangeActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            ArrItemString itemString = new ArrItemString();
            itemString.setValue(dateRangeActionResult.getText());
            dataItems = Collections.singletonList(itemString);
        } else if (actionResult instanceof NodeCountActionResult) {
            NodeCountActionResult nodeCountActionResult = (NodeCountActionResult) actionResult;
            String itemTypeCode = nodeCountActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            ArrItemInt itemInt = new ArrItemInt();
            itemInt.setValue(nodeCountActionResult.getCount());
            dataItems = Collections.singletonList(itemInt);
        } else if (actionResult instanceof SerialNumberResult) {
            return null; // tohle se nikam nepřeklápí zatím
        } else if (actionResult instanceof TableStatisticActionResult) {
            TableStatisticActionResult tableStatisticActionResult = (TableStatisticActionResult) actionResult;
            String itemTypeCode = tableStatisticActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            ArrItemJsonTable itemJsonTable = new ArrItemJsonTable();
            itemJsonTable.setValue(tableStatisticActionResult.getTable());
            dataItems = Collections.singletonList(itemJsonTable);
        } else if (actionResult instanceof TextAggregationActionResult) {
            TextAggregationActionResult textAggregationActionResult = (TextAggregationActionResult) actionResult;
            String itemTypeCode = textAggregationActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            // Check if item should be created
            if(textAggregationActionResult.isCreateInOutput()) {
                ArrItemText itemText = new ArrItemText();
                itemText.setValue(textAggregationActionResult.getText());
                dataItems = Collections.singletonList(itemText);
            } else {
                // no items will be created
                dataItems = Collections.emptyList();
            }
        } else if (actionResult instanceof UnitCountActionResult) {
            UnitCountActionResult unitCountActionResult = (UnitCountActionResult) actionResult;
            String itemTypeCode = unitCountActionResult.getItemType();
            type = itemTypeRepository.findOneByCode(itemTypeCode);
            ArrItemJsonTable itemJsonTable = new ArrItemJsonTable();
            itemJsonTable.setValue(unitCountActionResult.getTable());
            dataItems = Collections.singletonList(itemJsonTable);
        } else if (actionResult instanceof UnitIdResult) {
            return null; // tohle se nikam nepřeklápí zatím
        } else if (actionResult instanceof TestDataGeneratorResult) {
            return null; // tohle se nikam nepřeklápí zatím
        } else {
            throw new IllegalStateException("Nedefinovný typ výsledku: " + actionResult.getClass().getSimpleName());
        }

        if (itemTypesIgnored != null && itemTypesIgnored.contains(type)) {
            logger.warn("Při ukládání výsledků hromadné akce do výstupu " + outputDefinition.getName()
            + " [ID=" + outputDefinition.getOutputDefinitionId() + "] byl přeskočen atribut " + type.getName()
            + " [CODE=" + type.getCode() + "], protože je v seznamu ignorovaných");
            return null;
        }

        boolean store = false;
        if (itemType == null || itemType.equals(type)) {
            store = storeDataItems(type, dataItems, outputDefinition, fundVersion, change);
        }

        return Pair.of(type, store);
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
    private boolean storeDataItems(final RulItemType type,
                                   final List<ArrItemData> dataItems,
                                   final ArrOutputDefinition outputDefinition,
                                   final ArrFundVersion fundVersion,
                                   final ArrChangeLazy change) {
        if (isDataChanged(type, dataItems, outputDefinition)) {
            deleteOutputItemsByType(fundVersion, outputDefinition, type, change.getOrCreateChange());

            // donačte entity, které jsou reprezentované v JSON pouze s ID (odkazové)
            itemService.refItemsLoader(dataItems);

            for (ArrItemData dataItem : dataItems) {
                ArrOutputItem outputItem = new ArrOutputItem(dataItem);
                outputItem.setItemType(type);
                outputItem.setItemSpec(dataItem.getSpec());
                outputItem.setUndefined(BooleanUtils.isTrue(dataItem.getUndefined()));
                createOutputItem(outputItem, outputDefinition, fundVersion, change.getOrCreateChange());
            }
            return true;
        }
        return false;
    }

    /**
     * Detekuje, jestli jsou data změněná a je potřeba je přeuložit.
     *
     * @return {true} pokud se data změnily
     */
    private boolean isDataChanged(final RulItemType itemType,
                                  final List<ArrItemData> dataItems,
                                  final ArrOutputDefinition outputDefinition) {
        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(itemType, outputDefinition);

        // pokud se liší počet, musela nastat změna
        if (outputItems.size() != dataItems.size()) {
            return true;
        }

        // pomocné pole pro porovnávání
        ArrayList<ArrItemData> dataItemsToCompare = new ArrayList<>(dataItems);

        itemService.loadData(outputItems);

        // procházím všechny kombinace
        // pokud naleznu shodu, odeberu položku z pomocného pole
        for (ArrOutputItem outputItem : outputItems) {
            ArrItemData item = outputItem.getItem();
            Iterator<ArrItemData> iterator = dataItemsToCompare.iterator();
            while (iterator.hasNext()) {
                ArrItemData next = iterator.next();
                if (EqualsBuilder.reflectionEquals(next, item)) {
                    iterator.remove();
                    break;
                } else if (next instanceof IArrItemStringValue && item instanceof IArrItemStringValue) {
                    // pokud se jedná o textové hodnoty atributu, porovnávám na úrovni textové hodnoty a specifikace
                    String nextValue = ((IArrItemStringValue) next).getValue();
                    String itemValue = ((IArrItemStringValue) item).getValue();
                    if (ObjectUtils.equals(nextValue, itemValue) && ObjectUtils.equals(next.getSpec(), item.getSpec())) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }

        // pokud není seznam prázdný, existuje alespoň jedna změna
        return dataItemsToCompare.size() != 0;
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
     *  @param outputDefinition pojmenovaný výstup
     * @param fundVersion      verze AS
     * @param itemType         typ atributu
     * @param strict
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public void switchOutputCalculating(final ArrOutputDefinition outputDefinition,
                                        @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                        final RulItemType itemType,
                                        final Boolean strict) {
        Assert.notNull(outputDefinition, "Neplatný výstup");
        Assert.notNull(fundVersion, "Neplatná verze fondu");
        Assert.notNull(itemType, "Neplatný typ atributu");

        List<OutputState> allowStates = Collections.singletonList(OutputState.OPEN);
        if (!allowStates.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstup, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        ArrItemSettings itemSettings = itemSettingsRepository.findOneByOutputDefinitionAndItemType(outputDefinition, itemType);

        ArrChange change = arrangementService.createChange(null);

        if (itemSettings == null) {
            if (strict) {
                throw new BusinessException("Nelze přepnout způsob vyplňování, protože neexistuje žádná hodnota generovaná funkcí", OutputCode.CANNOT_SWITCH_CALCULATING).level(Level.WARNING);
            }

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
            boolean changed = storeResults(fundVersion, change, nodes, nodes, outputDefinition, itemType);

            if (strict && !changed) {
                throw new BusinessException("Nelze přepnout způsob vyplňování, protože neexistuje žádná hodnota generovaná funkcí", OutputCode.CANNOT_SWITCH_CALCULATING).level(Level.WARNING);
            }
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
        List<RulItemTypeAction> itemTypeActionList = itemTypeActionRepository.findOneByItemTypeCode(itemType.getCode());
        if (itemTypeActionList != null && !itemTypeActionList.isEmpty()) {
            ArrItemSettings itemSettings = itemSettingsRepository.findOneByOutputDefinitionAndItemType(outputDefinition, itemType);
            if (itemSettings == null || !itemSettings.getBlockActionResult()) {
                throw new BusinessException("Tento atribut je počítán automaticky a nemůže být ručně editován", OutputCode.ITEM_TYPE_CALC);
            }
        }
    }

    /**
     * Vyhledá typy atributů ručně smazané a mají dopočítávanou hodnotu.
     */
    public List<RulItemTypeExt> findHiddenItemTypes(final ArrFundVersion version,
                                                    final ArrOutputDefinition outputDefinition,
                                                    final List<RulItemTypeExt> itemTypes,
                                                    final List<ArrOutputItem> outputItems) {
        List<RulItemTypeExt> itemTypesResult = new ArrayList<>(itemTypes);
        Iterator<RulItemTypeExt> itemTypeIterator = itemTypesResult.iterator();

        final Set<ArrNode> nodes = outputDefinition.getOutputNodes().stream()
                .filter(nodeOutput -> nodeOutput.getDeleteChange() == null)
                .map(ArrNodeOutput::getNode)
                .collect(Collectors.toSet());

        List<RulItemType> rulItemTypes = new ArrayList<>();

        if (nodes.size() != 0) {
            List<ArrBulkActionRun> bulkActionsByNodes;
            bulkActionsByNodes = bulkActionService.findBulkActionsByNodes(version, nodes);

            // získám kódy hromadných akcí
            List<String> actionCodes = new ArrayList<>(bulkActionsByNodes.size());
            for (ArrBulkActionRun bulkActionRun : bulkActionsByNodes) {
                actionCodes.add(bulkActionRun.getBulkActionCode());
            }

            if (!actionCodes.isEmpty()) {

                List<RulAction> actionByCodes = bulkActionService.getBulkActionByCodes(actionCodes);

                if (!actionByCodes.isEmpty()) {
                    rulItemTypes = itemTypeActionRepository.findByAction(actionByCodes);
                }
            }
        }

        // ručně vypnuté typy atributů
        List<ArrItemSettings> itemSettingses = itemSettingsRepository.findByOutputDefinition(outputDefinition);
        for (ArrItemSettings itemSettingse : itemSettingses) {
            if (itemSettingse.getBlockActionResult()) {
                rulItemTypes.add(itemSettingse.getItemType());
            }
        }

        List<RulItemType> rulItemTypesExists = new ArrayList<>();
        for (ArrOutputItem outputItem : outputItems) {
            rulItemTypesExists.add(outputItem.getItemType());
        }

        while (itemTypeIterator.hasNext()) {
            RulItemTypeExt itemType = itemTypeIterator.next();
            if (RECOMMENDED.equals(itemType.getType()) || REQUIRED.equals(itemType.getType())) {
                itemTypeIterator.remove();
            } else if (!rulItemTypes.contains(itemType)) {
                itemTypeIterator.remove();
            } else if (rulItemTypesExists.contains(itemType)) {
                itemTypeIterator.remove();
            }
        }

        return itemTypesResult;
    }

    /**
     * Nastavení hodnoty atributu na "Nezjištěno".
     *
     * @param outputItemTypeId        identifikátor typu atributu
     * @param outputDefinitionId      identifikátor výstupu
     * @param outputDefinitionVersion verze výstupu
     * @param fundVersionId           identifikátor verze fondu
     * @param outputItemSpecId        identifikátor specifikace atributu
     * @param outputItemObjectId      identifikátor atributu
     * @return atribut s "Nezjištěnou" hodnotou
     */
    public ArrOutputItem setNotIdentifiedDescItem(final Integer outputItemTypeId,
                                                  final Integer outputDefinitionId,
                                                  final Integer outputDefinitionVersion,
                                                  final Integer fundVersionId,
                                                  final Integer outputItemSpecId,
                                                  final Integer outputItemObjectId) {
        ArrOutputDefinition outputDefinition = outputDefinitionRepository.findOne(outputDefinitionId);
        if (outputDefinition == null) {
            throw new ObjectNotFoundException("Nebyl nalezen výstup s ID=" + outputDefinitionId, OutputCode.OUTPUT_NOT_EXISTS).set("id", outputDefinitionId);
        }

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        if (fundVersion == null) {
            throw new ObjectNotFoundException("Nebyla nalezena verze AS s ID=" + fundVersionId, ArrangementCode.FUND_VERSION_NOT_FOUND).set("id", fundVersionId);
        }

        List<RulItemTypeExt> outputItemTypes = ruleService.getOutputItemTypes(outputDefinition);
        RulItemType outputItemType = outputItemTypes.stream().filter(rulItemTypeExt -> rulItemTypeExt.getItemTypeId().equals(outputItemTypeId)).findFirst().orElse(null);
        if (outputItemType == null) {
            throw new ObjectNotFoundException("Nebyla nalezen typ atributu s ID=" + outputItemTypeId, ArrangementCode.ITEM_TYPE_NOT_FOUND).set("id", outputItemTypeId);
        }

        if (!outputItemType.getIndefinable()) {
            throw new BusinessException("Položku není možné nastavit jako '" + ArrangementService.UNDEFINED + "'", ArrangementCode.CANT_SET_INDEFINABLE);
        }

        RulItemSpec outputItemSpec = null;
        if (outputItemSpecId != null) {
            outputItemSpec = itemSpecRepository.findOne(outputItemSpecId);
            if (outputItemSpec == null) {
                throw new ObjectNotFoundException("Nebyla nalezena specifikace atributu s ID=" + outputItemSpecId, ArrangementCode.ITEM_SPEC_NOT_FOUND).set("id", outputItemSpecId);
            }
        }

        ArrChange change = arrangementService.createChange(null);

        if (outputItemObjectId != null) {
            ArrOutputItem openOutputItem = outputItemRepository.findOpenOutputItem(outputItemObjectId);
            if (openOutputItem == null) {
                throw new ObjectNotFoundException("Nebyla nalezena hodnota atributu s OBJID=" + outputItemObjectId, ArrangementCode.DATA_NOT_FOUND).set("descItemObjectId", outputItemObjectId);
            } else if (openOutputItem.getUndefined()) {
                throw new BusinessException("Položka již je nastavená jako '" + ArrangementService.UNDEFINED + "'", ArrangementCode.ALREADY_INDEFINABLE);
            }

            openOutputItem.setDeleteChange(change);
            outputItemRepository.save(openOutputItem);
        }

        outputDefinition.setVersion(outputDefinitionVersion);
        saveOutputDefinition(outputDefinition);

        ArrOutputItem outputItem = new ArrOutputItem();
        outputItem.setItemType(outputItemType);
        outputItem.setItemSpec(outputItemSpec);
        outputItem.setOutputDefinition(outputDefinition);
        outputItem.setCreateChange(change);
        outputItem.setDeleteChange(null);
        outputItem.setDescItemObjectId(outputItemObjectId == null ? arrangementService.getNextDescItemObjectId() : outputItemObjectId);
        outputItem.setUndefined(true);

        ArrOutputItem outputItemCreated = createOutputItem(outputItem, fundVersion, change);

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkCalculatingAttribute(outputDefinition, outputItem.getItemType());
        outputItemCreated.setItem(descItemFactory.createItemByType(outputItemType.getDataType()));

        // sockety
        publishChangeOutputItem(fundVersion, outputItemCreated);
        return outputItemCreated;
    }
}
