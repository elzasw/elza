package cz.tacr.elza.service;

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

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.ArrOutputRestrictionScopeVO;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrOutputRestrictionScope;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.OutputRestrictionScopeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import org.apache.commons.collections4.CollectionUtils;
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
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
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
import cz.tacr.elza.domain.ArrOutput.OutputState;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.ArrOutputTemplate;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulActionRecommended;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulTemplate;
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
import cz.tacr.elza.repository.OutputFileRepository;
import cz.tacr.elza.repository.OutputItemRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.repository.OutputTemplateRepository;
import cz.tacr.elza.repository.OutputTypeRepository;
import cz.tacr.elza.repository.TemplateRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.output.OutputRequestStatus;

import static cz.tacr.elza.domain.RulItemType.Type.RECOMMENDED;
import static cz.tacr.elza.domain.RulItemType.Type.REQUIRED;
import static cz.tacr.elza.repository.ExceptionThrow.ap;
import static cz.tacr.elza.repository.ExceptionThrow.output;
import static cz.tacr.elza.repository.ExceptionThrow.outputType;
import static cz.tacr.elza.repository.ExceptionThrow.scope;
import static cz.tacr.elza.repository.ExceptionThrow.template;
import static cz.tacr.elza.repository.ExceptionThrow.version;

@Service
public class OutputService {

    private static final Logger logger = LoggerFactory.getLogger(OutputService.class);

    @Autowired
    private OutputRepository outputRepository;

    @Autowired
    private OutputTemplateRepository outputTemplateRepository;

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

    @Autowired
    private OutputRestrictionScopeRepository outputRestrictionScopeRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;

    public ArrOutput getOutput(int outputId) {
        return outputServiceInternal.getOutput(outputId);
    }

    public OutputRequestStatus addRequest(int outputId, ArrFundVersion fundVersion, boolean checkBulkActions) {
        return outputServiceInternal.addRequest(outputId, fundVersion, checkBulkActions);
    }

    /**
     * Smazat pojmenovaný výstup.
     *
     * @param fundVersion verze AS
     * @param output pojmenovaný výstup
     * @return smazaný pojmenovaný výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutput deleteNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final ArrOutput output) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(output, "Výstup musí být vyplněn");

        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze smazat výstup v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        checkFund(fundVersion, output);

        if (output.getDeleteChange() != null) {
            throw new BusinessException("Nelze smazat již smazaný výstup", OutputCode.ALREADY_DELETED);
        }

        List<OutputState> allowedState = Arrays.asList(OutputState.OPEN, OutputState.FINISHED, OutputState.OUTDATED);

        if (!allowedState.contains(output.getState())) {
            throw new BusinessException("Nelze smazat výstup v tomto stavu: " + output.getState(),
                    OutputCode.CANNOT_DELETED_IN_STATE).set("state", output.getState());
        }

        ArrChange change = arrangementService.createChange(null);
        output.setDeleteChange(change);

        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, output.getOutputId());
        eventNotificationService.publishEvent(event);

        return outputRepository.save(output);
    }

    /**
     * Vrátí stav do Otevřeno
     *
     * @param fundVersion verze AS
     * @param output pojmenovaný výstup
     * @return pojmenovaný výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutput revertToOpenState(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                       final ArrOutput output) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(output, "Výstup musí být vyplněn");

        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze vrátit do přípravy výstup v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        checkFund(fundVersion, output);

        if (output.getDeleteChange() != null) {
            throw new BusinessException("Nelze vrátit do přípravy smazaný výstup", OutputCode.CANNOT_CHANGE_STATE);
        }

        List<OutputState> allowedState = Arrays.asList(OutputState.FINISHED, OutputState.OUTDATED);
        if (!allowedState.contains(output.getState())) {
            throw new BusinessException("Do stavu \"V přípravě\" lze vrátit pouze \"Neaktuální\" či \"Vygenerované\" výstupy", OutputCode.CANNOT_CHANGE_STATE);
        }

        output.setState(OutputState.OPEN);
        // reset previous error
        output.setError(null);

        ArrOutputResult outputResult = output.getOutputResult();
        if (outputResult != null) {
            List<ArrOutputFile> outputFiles = outputResult.getOutputFiles();
            if (outputFiles != null && !outputFiles.isEmpty()) {
                outputFileRepository.deleteAll(outputFiles);
            }
            outputResultRepository.delete(outputResult);
        }

        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, output.getOutputId());
        eventNotificationService.publishEvent(event);

        return outputRepository.save(output);
    }

    /**
     * Vytvoří kopii výstupu s nody bez resultů
     *
     * @param fundVersion verze AS
     * @param originalOutput pojmenovaný výstup originál
     * @return pojmenovaný výstup kopie
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutput cloneOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                 final ArrOutput originalOutput) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(originalOutput, "Výstup musí být vyplněn");

        checkFund(fundVersion, originalOutput);

        if (originalOutput.getDeleteChange() != null) {
            throw new BusinessException("Nelze klonovat smazaný výstup", OutputCode.CANNOT_CLONE_DELETED);
        }

        String copy = " - kopie";
        String newName = originalOutput.getName() + copy;
        if (outputRepository.existsByName(newName)) {
            int num = 1;
            String newNameWithNum;
            do {
                newNameWithNum = newName + " " + num++;
            } while (outputRepository.existsByName(newNameWithNum));
            newName = newNameWithNum;
        }

        final ArrOutput newOutput = createOutput(fundVersion,
                newName,
                originalOutput.getInternalCode(),
                originalOutput.getOutputType().getOutputTypeId(),
                originalOutput.getTemplate() != null ? originalOutput.getTemplate().getTemplateId() : null
        );

        final ArrChange change = newOutput.getCreateChange();
        final ArrayList<ArrNodeOutput> newNodes = new ArrayList<>();
        originalOutput.getOutputNodes().forEach(node -> {
            if (node.getDeleteChange() == null) {
                ArrNodeOutput newNode = new ArrNodeOutput();
                newNode.setCreateChange(change);
                newNode.setNode(node.getNode());
                newNode.setOutput(newOutput);
                newNodes.add(newNode);
            }
        });

        nodeOutputRepository.saveAll(newNodes);

        return newOutput;
    }

    /**
     * Vytvoření pojmenovaného výstupu.
     *
     * @param fundVersion verze AS
     * @param name název výstupu
     * @param internalCode kód výstupu
     * @param templateId id šablony
     * @return vytvořený výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutput createOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                  final String name,
                                  final String internalCode,
                                  final Integer outputTypeId,
                                  final Integer templateId) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(name, "Název musí být vyplněn");
        Assert.notNull(outputTypeId, "Identifikátor typu vystupu musí být vyplněn");

        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze vytvořit výstup v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        ArrOutput output = new ArrOutput();
        output.setFund(fundVersion.getFund());
        output.setName(name);
        output.setInternalCode(internalCode);
        output.setState(OutputState.OPEN);

        RulOutputType type = outputTypeRepository.findById(outputTypeId)
                .orElseThrow(outputType(outputTypeId));
        output.setOutputType(type);

        ArrOutputTemplate templates = new ArrOutputTemplate();
        templates.setOutput(output);

        if (templateId != null) {
        	RulTemplate template = templateRepository.findById(templateId).orElseThrow(template(templateId));
        	templates.setTemplate(template);
        	output.setTemplate(template);
        } else {
        	templates.setTemplate(null);
        	output.setTemplate(null);
        }

        ArrChange change = arrangementService.createChange(null);
        output.setCreateChange(change);
        output.setDeleteChange(null);

        outputRepository.save(output);
        outputTemplateRepository.save(templates);

        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, output.getOutputId());
        eventNotificationService.publishEvent(event);

        return output;
    }

    /**
     * Odstranění uzlů z výstupu.
     *
     * @param fundVersion verze AS
     * @param output výstup
     * @param nodeIds seznam identifikátorů uzlů
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
     * @param output pojmenovaný výstup
     * @param nodeIds seznam identifikátorů uzlů
     * @param change změna
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

        if (output.getDeleteChange() != null) {
            throw new BusinessException("Nelze odebrat uzly u smazaného výstupu", OutputCode.DELETED);
        }

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(output.getState())) {
            throw new BusinessException("Nelze odebrat uzly z výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkFund(fundVersion, output);

        // get current live nodes
        List<ArrNodeOutput> outputNodes = nodeOutputRepository.findByOutputAndDeleteChangeIsNull(output);

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

        nodeOutputRepository.saveAll(removedNodes);

        updateCalculatedItems(fundVersion, change, remainingNodeIds, output);

        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, output.getOutputId());
        eventNotificationService.publishEvent(event);
    }

    /**
     * Upravení výstupu.
     *
     * @param fundVersion verze AS
     * @param output pojmenovaný výstup
     * @param name název výstupu
     * @param internalCode kód výstupu
     * @param templateId id šablony
     * @param anonymizedAp id anonymizovaného přístupového bodu
     * @return upravený výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutput updateNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                       final ArrOutput output,
                                       final String name,
                                       final String internalCode,
                                       final Integer templateId,
                                       final ApAccessPointVO anonymizedAp) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(output, "Výstup musí být vyplněn");
        Assert.notNull(name, "Název musí být vyplněn");

        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze upravovat výstup v uzavřené verzi AS", ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        if (output.getDeleteChange() != null) {
            throw new BusinessException("Nelze upravit smazaný výstup", OutputCode.DELETED);
        }

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(output.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        output.setName(name);
        output.setInternalCode(internalCode);
        if (templateId != null) {
            //output.setTemplate(templateRepository.findById(templateId).orElseThrow(template(templateId)));
        } else {
            //output.setTemplate(null);
        }

        if (anonymizedAp != null) {
            ApAccessPoint accessPoint = apAccessPointRepository.findById(anonymizedAp.getId())
                    .orElseThrow(ap(anonymizedAp.getId()));
            output.setAnonymizedAp(accessPoint);
        } else {
            output.setAnonymizedAp(null);
        }

        outputRepository.save(output);

        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, output.getOutputId());
        eventNotificationService.publishEvent(event);

        return output;
    }

    /**
     * Kontrola AS u verze a výstupu.
     *
     * @param fundVersion verze AS
     * @param output pojmenovaný výstup
     */
    private void checkFund(final ArrFundVersion fundVersion, final ArrOutput output) {
        if (!output.getFund().equals(fundVersion.getFund())) {
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

        if (output.getDeleteChange() != null) {
            throw new BusinessException("Nelze přidat uzly u smazaného výstupu", OutputCode.DELETED);
        }

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(output.getState())) {
            throw new BusinessException("Nelze přidat uzly k výstupu, který není ve stavu otevřený",
                    OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkFund(fundVersion, output);

        // seznam aktuálních (nesmazaných) uzlů
        List<ArrNodeOutput> currOutputNodes = nodeOutputRepository
                .findByOutputAndDeleteChangeIsNull(output);
        Set<Integer> currNodeIds = currOutputNodes.stream().map(ArrNodeOutput::getNodeId).collect(Collectors.toSet());

        for (Integer nodeIdAdd : connectNodeIds) {
            if (currNodeIds.contains(nodeIdAdd)) {
                throw new BusinessException("Nelze přidat již přidaný uzel. (ID=" + nodeIdAdd + ")",
                        ArrangementCode.ALREADY_ADDED);
            }
        }

        List<ArrNode> nodes = nodeRepository.findAllById(connectNodeIds);

        if (nodes.size() != connectNodeIds.size()) {
            throw new BusinessException("Byl předán seznam s neplatným identifikátorem uzlu: " + connectNodeIds,
                    ArrangementCode.NODE_NOT_FOUND).set("id", connectNodeIds);
        }

        List<ArrNodeOutput> nodeOutputs = new ArrayList<>(nodes.size());
        for (ArrNode node : nodes) {
            ArrNodeOutput nodeOutput = new ArrNodeOutput();
            nodeOutput.setNode(node);
            nodeOutput.setOutput(output);
            nodeOutput.setCreateChange(change);
            nodeOutputs.add(nodeOutput);
        }

        nodeOutputRepository.saveAll(nodeOutputs);

        currNodeIds.addAll(connectNodeIds);
        updateCalculatedItems(fundVersion, change, currNodeIds, output);

        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, output.getOutputId());
        eventNotificationService.publishEvent(event);
    }

    /**
     * Update counted results after assigned nodes were updated
     *
     * @param fundVersion verze AS
     * @param change změna překlopení
     * @param nodeIds kompletní seznam uzlů
     */
    private void updateCalculatedItems(final ArrFundVersion fundVersion, final ArrChange change,
                                       final Collection<Integer> nodeIds,
                                       final ArrOutput output) {
        // nalezeni automaticky vypoctenych hodnot a jejich vymazani

        // get recommended actions -> calculated item type
        List<RulItemTypeAction> itemTypeLinks = itemTypeActionRepository
                .findByOutputType(output.getOutputType());

        // remove itemtypes which do not have extra settings
        List<ArrItemSettings> itemSettings = this.itemSettingsRepository.findByOutput(output);
        // Collection of item types to not delete
        Set<Integer> preserveItemTypeIds = itemSettings.stream()
                .filter(is -> Boolean.TRUE.equals(is.getBlockActionResult())).map(is -> is.getItemTypeId())
                .collect(Collectors.toSet());
        // delete item types
        for (RulItemTypeAction ria : itemTypeLinks) {
            Integer itemTypeId = ria.getItemTypeId();
            if (!preserveItemTypeIds.contains(itemTypeId)) {
                outputServiceInternal.deleteOutputItemsByType(fundVersion, output, itemTypeId, change);
            }
        }

        // check if nodes are connected
        if (nodeIds.size() == 0) {
            return;
        }
    }

    /**
     * @return Return true if some item was modified
     */
    private boolean storeResults(final ArrFundVersion fundVersion,
                                 final Collection<Integer> nodes,
                                 final ArrOutput output,
                                 final OutputItemConnector connector) {

        List<ArrBulkActionRun> bulkActionRunList = bulkActionService.findFinishedBulkActionsByNodeIds(fundVersion,
                nodes);
        List<RulActionRecommended> actionRecommendeds = actionRecommendedRepository
                .findByOutputType(output.getOutputType());

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
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR,
            /*UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR*/})
    public List<ArrOutput> getSortedOutputs(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        List<ArrOutput> outputs = outputRepository.findByFundVersionSorted(fundVersion);
        return outputs;
    }

    /**
     * Získání výstupů podle verze AS.
     *
     * @param fundVersion verze AS
     * @param state stav outputu
     * @return seznam výstupů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR,
            /*UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR*/})
    public List<ArrOutput> getSortedOutputsByState(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final OutputState state) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        return outputRepository.findByFundVersionAndStateSorted(fundVersion, state);
    }

    /**
     * Získání pojmenovaného výstupu.
     *
     * @param fundVersion verze AS
     * @param output výstup
     * @return pojmenovaný výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR,
            /*UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR*/})
    public ArrOutput getOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final ArrOutput output) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(output, "Výstup musí být vyplněn");
        checkFund(fundVersion, output);
        return output;
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
     * @param item data pro hodnotu atributu
     * @param outputId identifikátor výstupu
     * @param outputVersion verze výstupu
     * @param fundVersionId identifikátor verze fondu
     * @return vytvořená hodnota atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputItem createOutputItem(final ArrOutputItem item,
                                          final Integer outputId,
                                          final Integer outputVersion,
                                          @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        Assert.notNull(item, "Hodnota musí být vyplněna");
        Assert.notNull(outputId, "Identifikátor výstupu musí být vyplněn");
        Assert.notNull(outputVersion, "Verze výstupu musí být vyplněna");

        ArrFundVersion fundVersion = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));

        ArrOutput output = getOutput(outputId);
        output.setVersion(outputVersion);
        saveOutput(output);

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(output.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkCalculatingAttribute(output, item.getItemType());
        return createOutputItem(item, output, fundVersion, null);
    }

    /**
     * Uložení výstupu - zámky.
     *
     * @param output pojmenovaný výstup
     * @return pojmenovaný výstup
     */
    private ArrOutput saveOutput(final ArrOutput output) {
        output.setLastUpdate(LocalDateTime.now());
        outputRepository.save(output);
        outputRepository.flush();
        return output;
    }

    /**
     * Vytvoření výstupu.
     *
     * @param outputItem hodnota atributu
     * @param output pojmenovaný výstup
     * @param fundVersion verze AS
     * @param createChange použitá změna
     */
    public ArrOutputItem createOutputItem(final ArrOutputItem outputItem,
                                          final ArrOutput output,
                                          final ArrFundVersion fundVersion,
                                          @Nullable final ArrChange createChange) {
        ArrChange change = createChange == null ? arrangementService.createChange(null) : createChange;

        outputItem.setOutput(output);

        outputItem.setOutput(output);
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
     * @param outputItem hodnota atributu
     * @param fundVersion verze AS
     * @param change změna
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

        int maxPosition = outputItemRepository.findMaxItemPosition(outputItem.getItemType(), outputItem.getOutput());

        if (outputItem.getPosition() == null || (outputItem.getPosition() > maxPosition)) {
            outputItem.setPosition(maxPosition + 1);
        }

        // načtení hodnot, které je potřeba přesunout níž
        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItemsAfterPosition(
                outputItem.getItemType(),
                outputItem.getOutput(),
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
     * @param outputVersion verze výstupu
     * @param fundVersionId identifikátor verze fondu
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
        ArrFundVersion fundVersion = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));
        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(descItemObjectId);

        if (outputItems.size() > 1) {
            throw new SystemException("Hodnota musí být právě jedna", BaseCode.DB_INTEGRITY_PROBLEM);
        } else if (outputItems.size() == 0) {
            throw new SystemException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }

        ArrOutputItem outputItem = outputItems.get(0);
        ArrOutput output = outputItem.getOutput();

        if (output.getState() != OutputState.OPEN) {
            throw new IllegalArgumentException("Nelze upravit výstupu, který není ve stavu otevřený");
        }

        checkCalculatingAttribute(output, outputItem.getItemType());

        output.setVersion(outputVersion);

        saveOutput(output);

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(output.getState())) {
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
                    outputItem.getOutput(),
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
     * @param outputItem hodnota atributu
     * @param outputVersion verze outputu
     * @param fundVersionId identifikátor verze fondu
     * @return upravená hodnota atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputItem updateOutputItem(final ArrOutputItem outputItem,
                                          final Integer outputVersion,
                                          @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        Validate.notNull(outputItem, "Výstup musí být vyplněn");
        Validate.notNull(outputItem.getPosition(), "Pozice musí být vyplněna");
        Validate.notNull(outputItem.getDescItemObjectId(), "Unikátní identifikátor hodnoty atributu musí být vyplněna");
        Validate.notNull(outputVersion, "Verze výstupu musí být vyplněna");
        Validate.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        ArrChange change = null;
        ArrFundVersion fundVersion = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));

        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(outputItem.getDescItemObjectId());

        if (outputItems.size() > 1) {
            throw new SystemException("Hodnota musí být právě jedna", BaseCode.DB_INTEGRITY_PROBLEM);
        } else if (outputItems.size() == 0) {
            throw new SystemException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }
        ArrOutputItem outputItemDB = outputItems.get(0);

        final ArrOutput output = outputItemDB.getOutput();
        Validate.notNull(output, "Výstup musí být vyplněn");

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(output.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        // check itemTypes
        if (outputItem.getItemTypeId() != null && !outputItemDB.getItemTypeId().equals(outputItem.getItemTypeId())) {
            throw new BusinessException("Received item has different itemType from item in DB",
                    OutputCode.NOT_PROCESS_IN_STATE)
                    .set("dbItemTypeId", outputItemDB.getItemTypeId())
                    .set("receivedItemTypeId", outputItem.getItemTypeId());
        }

        checkCalculatingAttribute(output, outputItemDB.getItemType());

        output.setVersion(outputVersion);

        // uložení uzlu (kontrola optimistických zámků)
        saveOutput(output);

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
            throw new BusinessException("Nelze aktualizovat prvek popisu pro výstup v uzavřené verzi.",
                    ArrangementCode.VERSION_ALREADY_CLOSED);
        }

        Integer positionOrig = outputItemDB.getPosition();
        Integer positionNew = outputItem.getPosition();

        // změnila pozice, budou se provádět posuny
        if (positionOrig != positionNew) {

            int maxPosition = outputItemRepository.findMaxItemPosition(outputItemDB.getItemType(),
                    outputItemDB.getOutput());

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
            // Copy new value
            ArrOutputItem descItemNew = new ArrOutputItem(outputItemDB);

            // save old value with deleteChange
            outputItemDB.setDeleteChange(change);
            itemService.save(outputItemDB);

            descItemNew.setItemId(null);
            descItemNew.setCreateChange(change);
            descItemNew.setPosition(positionNew);
            // update specification
            descItemNew.setItemSpec(outputItem.getItemSpec());
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
                outputItem.getOutput(), positionFrom, positionTo);
        return descItems;
    }

    /**
     * Vyhledání hodnot atributu výstupu.
     *
     * @param fundVersion verze AS
     * @param output pojmenovaný výstup
     * @return seznam hodnot atrubutů
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR,
            /*UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR*/})
    public List<ArrOutputItem> getOutputItems(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion, final ArrOutput output) {
        return outputServiceInternal.getOutputItems(output, fundVersion.getLockChange());
    }

    /**
     * Vytvoření hodnoty atributu výstupu.
     *
     * @param outputItems seznam hodnot
     * @param outputId identifikátor výstupu
     * @param outputVersion verze výstupu
     * @param fundVersionId identifikátor verze fondu
     * @return seznam hodnot
     */
    public List<ArrOutputItem> createOutputItems(final List<ArrOutputItem> outputItems,
                                                 final Integer outputId,
                                                 final Integer outputVersion,
                                                 final Integer fundVersionId) {
        Assert.notNull(outputItems, "Výstupy musí být vyplněny");
        Assert.notEmpty(outputItems, "Musí být zadán alespoň jeden výstup");
        Assert.notNull(outputId, "Identifikátor výstupu musí být vyplněn");
        Assert.notNull(outputVersion, "Verze výstupu musí být vyplněna");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        ArrFundVersion version = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));

        ArrOutput output = getOutput(outputId);

        // uložení uzlu (kontrola optimistických zámků)
        output.setVersion(outputVersion);
        saveOutput(output);

        return createOutputItems(outputItems, output, version, null);
    }

    /**
     * Vytvoření hodnoty atributu výstupu.
     *
     * @param outputItems seznam hodnot
     * @param output pojmenovaný výstup
     * @param version verze AS
     * @param createChange použitá změna
     * @return seznam hodnot
     */
    public List<ArrOutputItem> createOutputItems(final List<ArrOutputItem> outputItems,
                                                 final ArrOutput output,
                                                 final ArrFundVersion version,
                                                 @Nullable final ArrChange createChange) {
        List<OutputState> allowStates = Collections.singletonList(OutputState.OPEN);
        if (!allowStates.contains(output.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }
        ArrChange change = createChange == null ? arrangementService.createChange(null) : createChange;
        List<ArrOutputItem> createdItems = new ArrayList<>();
        for (ArrOutputItem outputItem : outputItems) {
            outputItem.setOutput(output);
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
     * @param fundVersionId identifikátor verze fondu
     * @param outputId identifikátor výstupu
     * @param outputVersion verze výstupu
     * @param itemTypeId identifikátor typu atributu
     * @return výstup
     */
    @Transactional
    @AuthMethod(permission = {Permission.FUND_OUTPUT_WR, Permission.FUND_OUTPUT_WR_ALL, Permission.FUND_ADMIN})
    public ArrOutput deleteOutputItemsByType(@AuthParam(type = Type.FUND_VERSION) final Integer fundVersionId,
                                             final Integer outputId,
                                             final Integer outputVersion,
                                             final Integer itemTypeId) {

        ArrChange change = arrangementService.createChange(null);
        ArrFundVersion fundVersion = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));

        StaticDataProvider sdp = staticDataService.getData();
        ItemType itemType = sdp.getItemTypeById(itemTypeId);
        Validate.notNull(itemType, "Typ atributu neexistuje");

        ArrOutput output = getOutput(outputId);

        List<OutputState> allowStates = Collections.singletonList(OutputState.OPEN);
        if (!allowStates.contains(output.getState())) {
            throw new BusinessException("Nelze upravit výstup, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkCalculatingAttribute(output, itemType.getEntity());

        output.setVersion(outputVersion);
        saveOutput(output);


        List<ArrOutputItem> outputItems = outputItemRepository.findOpenOutputItems(itemTypeId, output);

        if (outputItems.size() == 0) {
            throw new IllegalStateException("Nebyla nalezena žádná hodnota atributu ke smazání");
        }

        ArrItemSettings outputAndItemType = itemSettingsRepository.findOneByOutputAndItemType(output, itemType.getEntity());
        if (outputAndItemType != null) {
            itemSettingsRepository.delete(outputAndItemType);
        }

        List<ArrOutputItem> outputItemsDeleted = new ArrayList<>(outputItems.size());
        outputItemsDeleted.addAll(outputItems.stream()
                .map(descItem -> deleteOutputItem(descItem, fundVersion, change, false))
                .collect(Collectors.toList()));

        return output;
    }

    /**
     * Změnit typ kalkulace typu atributu - uživatelsky/automaticky.
     *
     * @param output pojmenovaný výstup
     * @param fundVersion verze AS
     * @param itemType typ atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public boolean switchOutputCalculating(final ArrOutput output,
                                        @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                        final RulItemType itemType,
                                        final Boolean strict) {
        Assert.notNull(output, "Neplatný výstup");
        Assert.notNull(fundVersion, "Neplatná verze fondu");
        Assert.notNull(itemType, "Neplatný typ atributu");

        List<OutputState> allowStates = Collections.singletonList(OutputState.OPEN);
        if (!allowStates.contains(output.getState())) {
            throw new BusinessException("Nelze upravit výstup, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        ArrItemSettings itemSettings = itemSettingsRepository.findOneByOutputAndItemType(output, itemType);

        ArrChange change = arrangementService.createChange(null);

        if (itemSettings == null) {
            if (strict) {
                return false;
            }

            itemSettings = new ArrItemSettings();
            itemSettings.setBlockActionResult(true);
            itemSettings.setItemType(itemType);
            itemSettings.setOutput(output);
            itemSettingsRepository.save(itemSettings);

            List<ArrOutputItem> items = outputItemRepository.findOpenOutputItems(itemType.getItemTypeId(), output);
            for (ArrOutputItem item : items) {
                outputServiceInternal.publishOutputItemChanged(item, fundVersion.getFundVersionId());
            }
        } else {
            itemSettingsRepository.delete(itemSettings);
            // delete old items
            outputServiceInternal.deleteOutputItemsByType(fundVersion, output, itemType.getItemTypeId(), change);

            // get current nodes for output
            List<ArrNodeOutput> nodes = nodeOutputRepository.findByOutputAndDeleteChangeIsNull(output);
            boolean changed = false;
            if (nodes.size() > 0) {
                List<Integer> nodeIds = nodes.stream().map(ArrNodeOutput::getNodeId).collect(Collectors.toList());

                // create item connector
                OutputItemConnector connector = outputServiceInternal.createItemConnector(fundVersion, output);
                connector.setChangeSupplier(() -> change);
                connector.setItemTypeFilter(itemType.getItemTypeId());

                changed = storeResults(fundVersion, nodeIds, output, connector);
            }

            if (strict && !changed) {
                return false;
            }
        }
        return true;
    }

    /**
     * Kontrola, že neměníme typ atributu, který je počítán automaticky.
     *
     * @param output pojmenovaný výstup
     * @param itemType typ atributu
     */
    private void checkCalculatingAttribute(final ArrOutput output,
                                           final RulItemType itemType) {
        List<RulItemTypeAction> itemTypeActionList = itemTypeActionRepository.findOneByItemTypeCode(itemType.getCode());
        if (itemTypeActionList != null && !itemTypeActionList.isEmpty()) {
            ArrItemSettings itemSettings = itemSettingsRepository.findOneByOutputAndItemType(output, itemType);
            if (itemSettings == null || !itemSettings.getBlockActionResult()) {
                throw new BusinessException("Tento atribut je počítán automaticky a nemůže být ručně editován", OutputCode.ITEM_TYPE_CALC);
            }
        }
    }

    /**
     * Vyhledá typy atributů ručně smazané a mají dopočítávanou hodnotu.
     */
    public List<RulItemTypeExt> findHiddenItemTypes(final ArrFundVersion version,
                                                    final ArrOutput output,
                                                    final List<RulItemTypeExt> itemTypes,
                                                    final List<ArrOutputItem> outputItems) {
        List<RulItemTypeExt> itemTypesResult = new ArrayList<>(itemTypes);
        Iterator<RulItemTypeExt> itemTypeIterator = itemTypesResult.iterator();

        final Set<Integer> nodeIds = output.getOutputNodes().stream()
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
        List<ArrItemSettings> itemSettingses = itemSettingsRepository.findByOutput(output);
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
     * @param outputItemTypeId identifikátor typu atributu
     * @param outputId identifikátor výstupu
     * @param outputVersion verze výstupu
     * @param fundVersionId identifikátor verze fondu
     * @param outputItemSpecId identifikátor specifikace atributu
     * @param outputItemObjectId identifikátor atributu
     * @return atribut s "Nezjištěnou" hodnotou
     */
    public ArrOutputItem setNotIdentifiedDescItem(final Integer outputItemTypeId,
                                                  final Integer outputId,
                                                  final Integer outputVersion,
                                                  final Integer fundVersionId,
                                                  final Integer outputItemSpecId,
                                                  final Integer outputItemObjectId) {
        ArrOutput output = outputRepository.findById(outputId)
                .orElseThrow(output(outputId));

        ArrFundVersion fundVersion = fundVersionRepository.findById(fundVersionId)
                .orElseThrow(version(fundVersionId));

        StaticDataProvider sdp = staticDataService.getData();
        ItemType itemType = sdp.getItemTypeById(outputItemTypeId);
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

        output.setVersion(outputVersion);
        saveOutput(output);

        ArrOutputItem outputItem = new ArrOutputItem();
        outputItem.setItemType(itemType.getEntity());
        outputItem.setItemSpec(outputItemSpec);
        outputItem.setOutput(output);
        outputItem.setCreateChange(change);
        outputItem.setDeleteChange(null);
        outputItem.setDescItemObjectId(outputItemObjectId == null ? arrangementService.getNextDescItemObjectId() : outputItemObjectId);

        ArrOutputItem outputItemCreated = createOutputItem(outputItem, fundVersion, change);

        List<OutputState> allowedState = Collections.singletonList(OutputState.OPEN);
        if (!allowedState.contains(output.getState())) {
            throw new BusinessException("Nelze upravit výstupu, který není ve stavu otevřený", OutputCode.NOT_PROCESS_IN_STATE);
        }

        checkCalculatingAttribute(output, outputItem.getItemType());
        //outputItemCreated.setItem(descItemFactory.createItemByType(outputItemType.getDataType()));

        // sockety
        outputServiceInternal.publishOutputItemChanged(outputItemCreated, fundVersion.getFundVersionId());
        return outputItemCreated;
    }

    public void setOutputSettings(OutputSettingsVO outputConfig, Integer outputId) throws JsonProcessingException {
        ArrOutput output = outputRepository.findByOutputId(outputId);
        ObjectMapper mapper = new ObjectMapper();
        String s = mapper.writeValueAsString(outputConfig);
        output.setOutputSettings(s);
        outputRepository.save(output);
    }

    public ArrOutputRestrictionScopeVO addRestrictedScope(Integer outputId, Integer scopeId) {
        Assert.notNull(scopeId, "Identifikátor třídy musí být vyplněna");
        Assert.notNull(outputId, "Identifikátor výstupu musí být vyplněn");
        ArrOutput output = outputRepository.findByOutputId(outputId);
        if (output == null) {
            throw new ObjectNotFoundException("Nebyl nalezen výstup s ID=" + outputId, OutputCode.OUTPUT_NOT_EXISTS).set("id", outputId);
        }

        ApScope scope = scopeRepository.findById(scopeId)
                .orElseThrow(scope(scopeId));

        ArrOutputRestrictionScope restrictionScope = new ArrOutputRestrictionScope();
        restrictionScope.setOutput(output);
        restrictionScope.setScope(scope);
        outputRestrictionScopeRepository.save(restrictionScope);

        ArrFundVersion fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(output.getFundId());
        eventNotificationService.publishEvent(EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, output.getOutputId()));

        return createArrOutputRestrictionScopeVO(restrictionScope);
    }

    private ArrOutputRestrictionScopeVO createArrOutputRestrictionScopeVO(ArrOutputRestrictionScope restrictionScope) {
        ArrOutputRestrictionScopeVO outputRestrictionScopeVO = new ArrOutputRestrictionScopeVO();
        outputRestrictionScopeVO.setId(restrictionScope.getRestrictionId());
        outputRestrictionScopeVO.setOutputId(restrictionScope.getOutput() != null ? restrictionScope.getOutput().getOutputId() : null);
        outputRestrictionScopeVO.setScopeId(restrictionScope.getScope() != null ? restrictionScope.getScope().getScopeId() : null);
        return outputRestrictionScopeVO;
    }

    public void deleteRestrictedScope(Integer outputId, Integer scopeId) {
        Assert.notNull(scopeId, "Identifikátor třídy musí být vyplněna");
        Assert.notNull(outputId, "Identifikátor výstupu musí být vyplněn");

        ArrOutput output = outputRepository.findByOutputId(outputId);
        ArrFundVersion fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(output.getFundId());
        outputRestrictionScopeRepository.deleteByOutputAndScope(outputId, scopeId);
        eventNotificationService.publishEvent(EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, outputId));

    }

}
