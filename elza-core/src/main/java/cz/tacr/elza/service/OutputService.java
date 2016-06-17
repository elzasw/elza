package cz.tacr.elza.service;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
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
     * @param fundVersion verze AS
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
     * @param fundVersion verze AS
     * @param name        název výstupu
     * @param internalCode        kód výstupu
     * @param temporary   dočasný výstup?
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
     * @param change      změna
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
     * @param output pojmenovaný výstup
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
     * @param fundVersion verze AS
     * @param output      pojmenovaný výstup
     * @param name        název výstupu
     * @param internalCode        kód výstupu
     * @return upravený výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrOutputDefinition updateNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                                 final ArrOutput output,
                                                 final String name,
                                                 final String internalCode) {
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

        outputDefinitionRepository.save(outputDefinition);

        Integer[] outputIds = outputDefinition.getOutputs().stream().map(ArrOutput::getOutputId).toArray(size -> new Integer[size]);
        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, outputIds);
        eventNotificationService.publishEvent(event);

        return outputDefinition;
    }

    /**
     * Kontrola AS u verze a výstupu.
     *
     * @param fundVersion verze AS
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
}
