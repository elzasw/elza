package cz.tacr.elza.service;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNamedOutput;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.repository.NamedOutputRepository;
import cz.tacr.elza.repository.NodeOutputRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.OutputRepository;
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
    private NamedOutputRepository namedOutputRepository;

    @Autowired
    private OutputRepository outputRepository;

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
     * @param namedOutput pojmenovaný výstup
     * @return smazaný pojmenovaný výstup
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN,
            UsrPermission.Permission.FUND_OUTPUT_WR_ALL, UsrPermission.Permission.FUND_OUTPUT_WR})
    public ArrNamedOutput deleteNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                            final ArrNamedOutput namedOutput) {
        Assert.notNull(fundVersion);
        Assert.notNull(namedOutput);

        if (fundVersion.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze smazat výstup v uzavřené verzi AS");
        }

        checkFund(fundVersion, namedOutput);

        if (namedOutput.getDeleted()) {
            throw new IllegalArgumentException("Nelze smazat již smazaný výstup");
        }

        namedOutput.setDeleted(true);

        Integer[] outputIds = namedOutput.getOutputs().stream().map(ArrOutput::getOutputId).toArray(size -> new Integer[size]);
        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, outputIds);
        eventNotificationService.publishEvent(event);

        return namedOutputRepository.save(namedOutput);
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

        checkFund(fundVersion, output.getNamedOutput());

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
    public ArrNamedOutput createNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                            final String name,
                                            final String internalCode,
                                            final Boolean temporary) {
        Assert.notNull(fundVersion);
        Assert.notNull(name);
        Assert.notNull(temporary);

        if (fundVersion.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze vytvořit výstup v uzavřené verzi AS");
        }

        ArrNamedOutput namedOutput = new ArrNamedOutput();
        namedOutput.setFund(fundVersion.getFund());
        namedOutput.setName(name);
        namedOutput.setInternalCode(internalCode);
        namedOutput.setDeleted(false);
        namedOutput.setTemporary(temporary);

        namedOutputRepository.save(namedOutput);

        ArrChange change = arrangementService.createChange();
        ArrOutput output = createOutput(namedOutput, change);

        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES, fundVersion, output.getOutputId());
        eventNotificationService.publishEvent(event);

        return namedOutput;
    }

    /**
     * Vytvoření výstupu.
     *
     * @param namedOutput pojmenovaný výstup
     * @param change      změna
     * @return vytvořený výstup
     */
    private ArrOutput createOutput(final ArrNamedOutput namedOutput,
                                   final ArrChange change) {
        Assert.notNull(namedOutput);
        Assert.notNull(change);

        ArrOutput output = new ArrOutput();
        output.setCreateChange(change);
        output.setNamedOutput(namedOutput);

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

        ArrNamedOutput namedOutput = output.getNamedOutput();

        checkFund(fundVersion, namedOutput);

        Set<ArrNodeOutput> nodeOutputs = namedOutput.getOutputNodes().stream()
                .filter(nodeOutput -> nodeOutput.getDeleteChange() == null && nodeIds.contains(nodeOutput.getNode().getNodeId()))
                .collect(Collectors.toSet());

        if (nodeOutputs.size() > 0) {
            nodeOutputs.stream().forEach(arrNodeOutput -> arrNodeOutput.setDeleteChange(change));
            nodeOutputRepository.save(nodeOutputs);

            Integer[] outputIds = namedOutput.getOutputs().stream().map(ArrOutput::getOutputId).toArray(size -> new Integer[size]);
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
    public ArrNamedOutput updateNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
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

        ArrNamedOutput namedOutput = output.getNamedOutput();

        namedOutput.setName(name);
        namedOutput.setInternalCode(internalCode);

        namedOutputRepository.save(namedOutput);

        Integer[] outputIds = namedOutput.getOutputs().stream().map(ArrOutput::getOutputId).toArray(size -> new Integer[size]);
        EventIdsInVersion event = EventFactory.createIdsInVersionEvent(EventType.OUTPUT_CHANGES_DETAIL, fundVersion, outputIds);
        eventNotificationService.publishEvent(event);

        return namedOutput;
    }

    /**
     * Kontrola AS u verze a výstupu.
     *
     * @param fundVersion verze AS
     * @param namedOutput pojmenovaný výstup
     */
    private void checkFund(final ArrFundVersion fundVersion, final ArrNamedOutput namedOutput) {
        if (!namedOutput.getFund().equals(fundVersion.getFund())) {
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

        ArrNamedOutput namedOutput = output.getNamedOutput();

        checkFund(fundVersion, namedOutput);

        Set<Integer> nodesIdsDb = namedOutput.getOutputNodes().stream()
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
                        nodeOutput.setNamedOutput(namedOutput);
                        nodeOutput.setCreateChange(change);
                        return nodeOutput;
                    }).collect(Collectors.toList());

            nodeOutputRepository.save(nodeOutputs);

            Integer[] outputIds = namedOutput.getOutputs().stream().map(ArrOutput::getOutputId).toArray(size -> new Integer[size]);
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
    public ArrNamedOutput getNamedOutput(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                         final ArrOutput output) {
        Assert.notNull(fundVersion);
        Assert.notNull(output);
        checkFund(fundVersion, output.getNamedOutput());
        return output.getNamedOutput();
    }
}
