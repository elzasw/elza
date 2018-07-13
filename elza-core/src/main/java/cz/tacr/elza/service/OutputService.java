package cz.tacr.elza.service;

import static cz.tacr.elza.domain.RulItemType.Type.RECOMMENDED;
import static cz.tacr.elza.domain.RulItemType.Type.REQUIRED;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

import cz.tacr.elza.core.data.StaticDataProvider;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.bulkaction.BulkActionService;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.controller.vo.OutputSettingsVO;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.core.security.AuthParam.Type;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItemSettings;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulActionRecommended;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.OutputCode;
import cz.tacr.elza.repository.ActionRecommendedRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSettingsRepository;
import cz.tacr.elza.repository.ItemTypeActionRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
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
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.output.OutputRequestStatus;

@Service
public class OutputService {

    private static final Logger logger = LoggerFactory.getLogger(OutputService.class);

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
    private NodeRepository nodeRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OutputResultRepository outputResultRepository;

    @Autowired
    private OutputFileRepository outputFileRepository;

    @Autowired
    private OutputServiceInternal outputServiceInternal;

    @Autowired
    private BulkActionService bulkActionService;

    @Autowired
    private ActionRecommendedRepository actionRecommendedRepository;

    @Autowired
    private ItemSettingsRepository itemSettingsRepository;

    @Autowired
    private ItemTypeActionRepository itemTypeActionRepository;

    @Autowired
    private StaticDataService staticDataService;

    public ArrOutputDefinition getOutputDefinition(int outputDefinitionId) {
        return outputServiceInternal.getOutputDefinition(outputDefinitionId);
    }

    public OutputRequestStatus addRequest(int outputDefinitionId, ArrFundVersion fundVersion, boolean checkBulkActions) {
        return outputServiceInternal.addRequest(outputDefinitionId, fundVersion, checkBulkActions);
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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(outputDefinition, "Definice výstupu musí být vyplněna");

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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(outputDefinition, "Definice výstupu musí být vyplněna");

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
        // reset previous error
        outputDefinition.setError(null);

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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(originalOutputDef, "Musí být vyplněna definice výstupu");

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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(output, "Výstup musí být vyplněn");
        Assert.notNull(change, "Změna musí být vyplněna");

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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(name, "Název musí být vyplněn");
        Assert.notNull(temporary, "Musí být vyplněno");
        Assert.notNull(outputTypeId, "Identifikátor typu vystupu musí být vyplněn");

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
        Assert.notNull(type, "Typ musí být vyplněn");
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
        Assert.notNull(outputDefinition, "Definice výstupu musí být vyplněna");
        Assert.notNull(change, "Změna musí být vyplněna");

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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(output, "Výstup musí být vyplněn");
        Assert.notEmpty(nodeIds, "Musí být vyplněna alespoň jedna JP");
        Assert.notNull(change, "Změna musí být vyplněna");

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

        // get current live nodes
        List<ArrNodeOutput> outputNodes = nodeOutputRepository.findByOutputDefinitionAndDeleteChangeIsNull(outputDefinition);

        // mark nodes as deleted
        List<Integer> remainingNodeIds = new ArrayList<>();
        List<ArrNodeOutput> removedNodes = new ArrayList<>();
        for (ArrNodeOutput nodeOutput : outputNodes) {
            if (nodeIds.contains(nodeOutput.getNodeId())) {
                    nodeOutput.setDeleteChange(change);
                removedNodes.add(nodeOutput);
            } else {
                remainingNodeIds.add(nodeOutput.getNodeId());
                }
            }

        if (nodeIds.size() != removedNodes.size()) {
            throw new BusinessException("Byl předán seznam s neplatným identifikátorem uzlu: " + nodeIds, ArrangementCode.NODE_NOT_FOUND).set("id", nodeIds);
        }

        nodeOutputRepository.save(removedNodes);

        updateCalculatedItems(fundVersion, change, remainingNodeIds, outputDefinition);

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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(output, "Výstup musí být vyplněn");
        Assert.notNull(name, "Název musí být vyplněn");

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
     * @param fundVersion    verze AS
     * @param output         pojmenovaný výstup
     * @param connectNodeIds seznam identifikátorů přidávaných uzlů
     * @param change         změna
     */
    private void addNodesNamedOutput(final ArrFundVersion fundVersion, final ArrOutput output,
                                     final List<Integer> connectNodeIds, final ArrChange change) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(output, "Výstup musí být vyplněn");
        Assert.notEmpty(connectNodeIds, "Musí být vyplněna alespoň jedna JP");
        Assert.notNull(change, "Změna musí být vyplněna");

        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze přidat uzly k výstupu v uzavřené verzi AS",
                    ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        if (output.getLockChange() != null) {
            throw new BusinessException("Nelze přidat uzly u zamčeného výstupu", OutputCode.LOCKED);
        }

        final ArrOutputDefinition outputDefinition = output.getOutputDefinition();

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze přidat uzly k výstupu, který není ve stavu otevřený",
                    OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkFund(fundVersion, outputDefinition);

        // seznam aktuálních (nesmazaných) uzlů
        List<ArrNodeOutput> currOutputNodes = nodeOutputRepository
                .findByOutputDefinitionAndDeleteChangeIsNull(outputDefinition);
        Set<Integer> currNodeIds = currOutputNodes.stream().map(ArrNodeOutput::getNodeId).collect(Collectors.toSet());

        for (Integer nodeIdAdd : connectNodeIds) {
            if (currNodeIds.contains(nodeIdAdd)) {
                throw new BusinessException("Nelze přidat již přidaný uzel. (ID=" + nodeIdAdd + ")",
                        ArrangementCode.ALREADY_ADDED);
                }
            }

        List<ArrNode> nodes = nodeRepository.findAll(connectNodeIds);

        if (nodes.size() != connectNodeIds.size()) {
            throw new BusinessException("Byl předán seznam s neplatným identifikátorem uzlu: " + connectNodeIds,
                    ArrangementCode.NODE_NOT_FOUND).set("id", connectNodeIds);
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

        currNodeIds.addAll(connectNodeIds);
        updateCalculatedItems(fundVersion, change, currNodeIds, outputDefinition);

        Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId)
                .toArray(Integer[]::new);
        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion,
                outputIds);
        eventNotificationService.publishEvent(event);
    }

    /**
     * Update counted results after assigned nodes were updated
     *
     * @param fundVersion verze AS
     * @param change      změna překlopení
     * @param nodeIds    kompletní seznam uzlů
     */
    private void updateCalculatedItems(final ArrFundVersion fundVersion, final ArrChange change,
                                          final Collection<Integer> nodeIds,
                                          final ArrOutputDefinition outputDefinition) {
        // nalezeni automaticky vypoctenych hodnot a jejich vymazani

        // get recommended actions -> calculated item type
        List<RulItemTypeAction> itemTypeLinks = itemTypeActionRepository
                .findByOutputType(outputDefinition.getOutputType());

        // remove itemtypes which do not have extra settings
        List<ArrItemSettings> itemSettings = this.itemSettingsRepository.findByOutputDefinition(outputDefinition);
        // Collection of item types to not delete
        Set<Integer> preserveItemTypeIds = itemSettings.stream()
                .filter(is -> Boolean.TRUE.equals(is.getBlockActionResult())).map(is -> is.getItemTypeId())
                .collect(Collectors.toSet());
        // delete item types
        for (RulItemTypeAction ria : itemTypeLinks) {
            Integer itemTypeId = ria.getItemTypeId();
            if (!preserveItemTypeIds.contains(itemTypeId)) {
                outputServiceInternal.deleteOutputItemsByType(fundVersion, outputDefinition, itemTypeId, change);
            }
        }

        // check if nodes are connected
        if (nodeIds.size() == 0) {
            return;
        }

    private boolean storeResults(final ArrFundVersion fundVersion,
                                 final ArrChange change,
                                 final Collection<Integer> nodes,
                                 final ArrOutputDefinition outputDefinition,
                                 final RulItemType itemType)
    {

        List<ArrBulkActionRun> bulkActionRunList = bulkActionService.findFinishedBulkActionsByNodeIds(fundVersion, nodes);
        List<RulActionRecommended> actionRecommendeds = actionRecommendedRepository.findByOutputType(outputDefinition.getOutputType());

        // create item connector
        OutputItemConnector connector = outputServiceInternal.createItemConnector(fundVersion, outputDefinition);
        connector.setChangeSupplier(() -> change);

        storeResults(fundVersion, nodeIds, outputDefinition, connector);
    }

    /**
     *
     * @param fundVersion
     * @param nodes
     * @param outputDefinition
     * @return Return true if some item was modified
     */
    private boolean storeResults(final ArrFundVersion fundVersion,
                                 final Collection<Integer> nodes,
                                 final ArrOutputDefinition outputDefinition,
                                 final OutputItemConnector connector) {

        List<ArrBulkActionRun> bulkActionRunList = bulkActionService.findFinishedBulkActionsByNodeIds(fundVersion,
                                                                                                      nodes);
        List<RulActionRecommended> actionRecommendeds = actionRecommendedRepository
                .findByOutputType(outputDefinition.getOutputType());

        for (ArrBulkActionRun bulkActionRun : bulkActionRunList) {
            RulAction action = bulkActionService.getBulkActionByCode(bulkActionRun.getBulkActionCode());
            for (RulActionRecommended actionRecommended : actionRecommendeds) {
                // process only recommended actions
                if (actionRecommended.getAction().equals(action)) {

                    // read results
                    Result resultObj = null;
                    try {
                        resultObj = bulkActionRun.getResult();
                    } catch (Exception e) {
                        // Due to updates it might happen that we are not
                        // able to deserialize results
                        // In such case we are ignoring / skipping these results
                        logger.error("Failed to deserialize results", e);
                    }
                    if (resultObj != null && resultObj.getResults() != null) {
                        // process all results
                        for (ActionResult result : resultObj.getResults()) {
                            result.createOutputItems(connector);
                        }
                    }
                }
            }
        }

        return connector.getModifiedItemTypeIds().size() > 0;
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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(output, "Výstup musí být vyplněn");
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
        return outputTypeRepository.findByRuleSet(version.getRuleSet());
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
        Assert.notNull(item, "Hodnota musí být vyplněna");
        Assert.notNull(outputDefinitionId, "Identifikátor definice výstupu musí být vyplněn");
        Assert.notNull(outputDefinitionVersion, "Verze definice výstupu musí být vyplněna");

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);

        ArrOutputDefinition outputDefinition = outputDefinitionRepository.findOne(outputDefinitionId);
        Assert.notNull(outputDefinition, "Definice výstupu musí být vyplněna");
        outputDefinition.setVersion(outputDefinitionVersion);
        saveOutputDefinition(outputDefinition);

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkCalculatingAttribute(outputDefinition, item.getItemType());
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
        outputServiceInternal.publishOutputItemChanged(outputItemCreated, fundVersion.getFundVersionId());

        return outputItemCreated;
    }

    /**
     * Vytvoření hodnoty atributu pro výstup.
     *
     * @param outputItem  hodnota atributu
     * @param fundVersion verze AS
     * @param change      změna
     * @return vytvořená hodnota atributu
     */
    private ArrOutputItem createOutputItem(final ArrOutputItem outputItem,
                                           final ArrFundVersion fundVersion,
                                           final ArrChange change) {
        Assert.notNull(outputItem, "Výstup musí být vyplněn");
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(change, "Změna musí být vyplněna");

        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze vytvořit prvek popisu pro výstup v uzavřené verzi.", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        // kontrola validity typu a specifikace
        StaticDataProvider sdp = staticDataService.getData();
        itemService.checkValidTypeAndSpec(sdp, outputItem);

        int maxPosition = outputItemRepository.findMaxItemPosition(outputItem.getItemType(), outputItem.getOutputDefinition());

        if (outputItem.getPosition() == null || (outputItem.getPosition() > maxPosition)) {
            outputItem.setPosition(maxPosition + 1);
        }

        // načtení hodnot, které je potřeba přesunout níž
        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItemsAfterPosition(
                outputItem.getItemType(),
                outputItem.getOutputDefinition(),
                outputItem.getPosition() - 1);

        // posun prvků
        for (ArrOutputItem item : outputItems) {
            itemService.copyItem(item, change, item.getPosition() + 1);
        }

        outputItem.setCreateChange(change);
        return itemService.save(outputItem);
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
        Assert.notNull(descItemObjectId, "Nebyl vyplněn jednoznačný identifikátor descItem");
        Assert.notNull(outputVersion, "Verze výstupu není vyplněna");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

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
        Assert.notNull(outputItem, "Výstup musí být vyplněn");
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(change, "Změna musí být vyplněna");

        // pro mazání musí být verze otevřená
        if (version.getLockChange() != null) {
            throw new BusinessException("Nelze smazat prvek popisu u výstupu v uzavřené verzi.", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        if (moveAfter) {
            // načtení hodnot, které je potřeba přesunout výš
            List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItemsAfterPosition(
                    outputItem.getItemType(),
                    outputItem.getOutputDefinition(),
                    outputItem.getPosition());
            for (ArrOutputItem item : outputItems) {
                itemService.copyItem(item, change, item.getPosition() - 1);
        }
        }

        outputItem.setDeleteChange(change);

        // sockety
        outputServiceInternal.publishOutputItemChanged(outputItem, version.getFundVersionId());

        return outputItemRepository.save(outputItem);
    }

    /**
     * Úprava hodnoty atributu.
     *
     * @param outputItem              hodnota atributu
     * @param outputDefinitionVersion verze outputu
     * @param fundVersionId           identifikátor verze fondu
     * @return upravená hodnota atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputItem updateOutputItem(final ArrOutputItem outputItem,
                                          final Integer outputDefinitionVersion,
                                          @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        Assert.notNull(outputItem, "Výstup musí být vyplněn");
        Assert.notNull(outputItem.getPosition(), "Pozice musí být vyplněna");
        Assert.notNull(outputItem.getDescItemObjectId(), "Unikátní identifikátor hodnoty atributu musí být vyplněna");
        Assert.notNull(outputDefinitionVersion, "Verze definice výstupu musí být vyplněna");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

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
        Assert.notNull(outputDefinition, "Definice výstupu musí být vyplněna");

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkCalculatingAttribute(outputDefinition, outputItemDB.getItemType());

            outputDefinition.setVersion(outputDefinitionVersion);

            // uložení uzlu (kontrola optimistických zámků)
            saveOutputDefinition(outputDefinition);

            // vytvoření změny
            change = arrangementService.createChange(null);

        ArrOutputItem outputItemUpdated = updateOutputItem(outputItem, outputItemDB, fundVersion, change);

        return outputItemUpdated;
    }

    /**
     * Úprava hodnoty atributu.
     *
     * @param outputItem       hodnota atributu
     * @param outputItemDB     hodnota atributu z DB
     * @param version          verze AS
     * @param change           změna pro úpravu
     * @return upravená hodnota
     */
    public ArrOutputItem updateOutputItem(final ArrOutputItem outputItem,
                                          final ArrOutputItem outputItemDB,
                                          final ArrFundVersion version,
                                          final ArrChange change) {
        Validate.notNull(change);

        if (version.getLockChange() != null) {
            throw new BusinessException("Nelze aktualizovat prvek popisu pro výstup v uzavřené verzi.", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

			Integer positionOrig = outputItemDB.getPosition();
            Integer positionNew = outputItem.getPosition();

            // změnila pozice, budou se provádět posuny
            if (positionOrig != positionNew) {

			int maxPosition = outputItemRepository.findMaxItemPosition(outputItemDB.getItemType(), outputItemDB.getOutputDefinition());

                if (outputItem.getPosition() == null || (outputItem.getPosition() > maxPosition)) {
                    outputItem.setPosition(maxPosition + 1);
                }

                List<ArrOutputItem> outputItemsMove;
                Integer diff;

                if (positionNew < positionOrig) {
                    diff = 1;
					outputItemsMove = findOutputItemsBetweenPosition(outputItemDB, positionNew, positionOrig - 1);
                } else {
                    diff = -1;
					outputItemsMove = findOutputItemsBetweenPosition(outputItemDB, positionOrig + 1, positionNew);
                }

            for (ArrOutputItem item : outputItemsMove) {
                itemService.copyItem(item, change, item.getPosition() + diff);
            }
        }

            try {
				ArrOutputItem descItemNew = new ArrOutputItem(outputItemDB);

				outputItemDB.setDeleteChange(change);
			itemService.save(outputItemDB);

                descItemNew.setItemId(null);
                descItemNew.setCreateChange(change);
                descItemNew.setPosition(positionNew);
                descItemNew.setData(outputItem.getData());

            ArrOutputItem outputItemUpdated = itemService.save(descItemNew);

            outputServiceInternal.publishOutputItemChanged(outputItemUpdated, version.getFundVersionId());
            return outputItemUpdated;

            } catch (Exception e) {
                throw new SystemException(e);
            }
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
     * Vyhledání hodnot atributu výstupu.
     *
     * @param fundVersion      verze AS
     * @param outputDefinition pojmenovaný výstup
     * @return seznam hodnot atrubutů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrOutputItem> getOutputItems(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                              final ArrOutputDefinition outputDefinition) {
        return outputServiceInternal.getOutputItems(outputDefinition, fundVersion.getLockChange());
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
        Assert.notNull(outputItems, "Výstupy musí být vyplněny");
        Assert.notEmpty(outputItems, "Musí být zadán alespoň jeden výstup");
        Assert.notNull(outputDefinitionId, "Identifikátor definice výstupu musí být vyplněn");
        Assert.notNull(outputDefinitionVersion, "Verze definice výstupu musí být vyplněna");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        ArrOutputDefinition outputDefinition = outputServiceInternal.getOutputDefinition(outputDefinitionId);

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
            outputServiceInternal.publishOutputItemChanged(created, version.getFundVersionId());
        }

        return createdItems;
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
    @Transactional
    @AuthMethod(permission= {Permission.FUND_OUTPUT_WR, Permission.FUND_OUTPUT_WR_ALL, Permission.FUND_ADMIN})
    public ArrOutputDefinition deleteOutputItemsByType(@AuthParam(type=Type.FUND_VERSION) final Integer fundVersionId,
                                                       final Integer outputDefinitionId,
                                                       final Integer outputDefinitionVersion,
                                                       final Integer itemTypeId) {

        ArrChange change = arrangementService.createChange(null);
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        Validate.notNull(fundVersion, "Verze archivní pomůcky neexistuje");

        StaticDataProvider sdp = staticDataService.getData();
        RuleSystemItemType itemType = sdp.getItemTypeById(itemTypeId);

        Validate.notNull(itemType, "Typ atributu neexistuje");

        ArrOutputDefinition outputDefinition = outputDefinitionRepository.findOne(outputDefinitionId);
        Validate.notNull(outputDefinition, "Definice výstupu musí být vyplněna");

        List<OutputState> allowStates = Collections.singletonList(OutputState.OPEN);
        if (!allowStates.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstup, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkCalculatingAttribute(outputDefinition, itemType.getEntity());

        outputDefinition.setVersion(outputDefinitionVersion);
        saveOutputDefinition(outputDefinition);


        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(itemTypeId, outputDefinition);

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

            List<ArrOutputItem> items = outputItemRepository.findOpenOutputItems(itemType.getItemTypeId(), outputDefinition);
            for (ArrOutputItem item : items) {
                outputServiceInternal.publishOutputItemChanged(item, fundVersion.getFundVersionId());
            }
        } else {
            itemSettingsRepository.delete(itemSettings);
            // delete old items
            outputServiceInternal.deleteOutputItemsByType(fundVersion, outputDefinition, itemType.getItemTypeId(), change);

            // get current nodes for output
            List<ArrNodeOutput> nodes = nodeOutputRepository.findByOutputDefinitionAndDeleteChangeIsNull(outputDefinition);
            boolean changed = false;
            if(nodes.size()>0) {
                List<Integer> nodeIds = nodes.stream().map(n -> n.getNodeId()).collect(Collectors.toList());

                // create item connector
                OutputItemConnector connector = outputServiceInternal.createItemConnector(fundVersion,
                                                                                          outputDefinition);
                connector.setChangeSupplier(() -> change);
                // set item type as filter if present
                if (itemType != null) {
                    connector.setItemTypeFilter(itemType.getItemTypeId());
                }

                changed = storeResults(fundVersion, nodeIds, outputDefinition, connector);
            }

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

        final Set<Integer> nodeIds = outputDefinition.getOutputNodes().stream()
                .filter(nodeOutput -> nodeOutput.getDeleteChange() == null)
                .map(ArrNodeOutput::getNodeId)
                .collect(Collectors.toSet());

        List<RulItemType> rulItemTypes = new ArrayList<>();

        if (nodeIds.size() != 0) {
            List<ArrBulkActionRun> bulkActionsByNodes;
            bulkActionsByNodes = bulkActionService.findFinishedBulkActionsByNodeIds(version, nodeIds);

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

        StaticDataProvider sdp = staticDataService.getData();
        RuleSystemItemType itemType = sdp.getItemTypeById(outputItemTypeId);
        if (itemType == null) {
            throw new ObjectNotFoundException("Nebyla nalezen typ atributu s ID=" + outputItemTypeId, ArrangementCode.ITEM_TYPE_NOT_FOUND).set("id", outputItemTypeId);
        }

        RulItemSpec outputItemSpec = null;
        if (outputItemSpecId != null) {
            outputItemSpec = itemType.getItemSpecById(outputItemSpecId);
            if (outputItemSpec == null) {
                throw new ObjectNotFoundException("Nebyla nalezena specifikace atributu s ID=" + outputItemSpecId, ArrangementCode.ITEM_SPEC_NOT_FOUND).set("id", outputItemSpecId);
            }
        }

        ArrChange change = arrangementService.createChange(null);

        if (outputItemObjectId != null) {
            ArrOutputItem openOutputItem = outputItemRepository.findOpenOutputItem(outputItemObjectId);
            if (openOutputItem == null) {
                throw new ObjectNotFoundException("Nebyla nalezena hodnota atributu s OBJID=" + outputItemObjectId, ArrangementCode.DATA_NOT_FOUND).set("descItemObjectId", outputItemObjectId);
            } else if (openOutputItem.getData() == null) {
                throw new BusinessException("Položka již je nastavená jako '" + ArrangementService.UNDEFINED + "'", ArrangementCode.ALREADY_INDEFINABLE);
            }

            openOutputItem.setDeleteChange(change);
            outputItemRepository.save(openOutputItem);
        }

        outputDefinition.setVersion(outputDefinitionVersion);
        saveOutputDefinition(outputDefinition);

        ArrOutputItem outputItem = new ArrOutputItem();
        outputItem.setItemType(itemType.getEntity());
        outputItem.setItemSpec(outputItemSpec);
        outputItem.setOutputDefinition(outputDefinition);
        outputItem.setCreateChange(change);
        outputItem.setDeleteChange(null);
        outputItem.setDescItemObjectId(outputItemObjectId == null ? arrangementService.getNextDescItemObjectId() : outputItemObjectId);

        ArrOutputItem outputItemCreated = createOutputItem(outputItem, fundVersion, change);

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(outputDefinition.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkCalculatingAttribute(outputDefinition, outputItem.getItemType());
        //outputItemCreated.setItem(descItemFactory.createItemByType(outputItemType.getDataType()));

        // sockety
        outputServiceInternal.publishOutputItemChanged(outputItemCreated, fundVersion.getFundVersionId());
        return outputItemCreated;
    }

    public void setOutputSettings(OutputSettingsVO outputConfig, Integer outputId) throws JsonProcessingException {
        ArrOutputDefinition outputDefinition = outputDefinitionRepository.findByOutputId(outputId);
        ObjectMapper mapper = new ObjectMapper();
        String s = mapper.writeValueAsString(outputConfig);
        outputDefinition.setOutputSettings(s);
        outputDefinitionRepository.save(outputDefinition);
    }
}
