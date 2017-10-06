package cz.tacr.elza.service.importnodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.ArrMoveLevelService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.importnodes.vo.File;
import cz.tacr.elza.service.importnodes.vo.ImportParams;
import cz.tacr.elza.service.importnodes.vo.ImportSource;
import cz.tacr.elza.service.importnodes.vo.Packet;
import cz.tacr.elza.service.importnodes.vo.ValidateResult;

/**
 * Servisní třída pro importování uzlů do stromu z různých zdrojů.
 *
 * @since 19.07.2017
 */
@Service
@Configuration
public class ImportNodesFromSource {

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private PacketRepository packetRepository;

    /**
     * Validace dat před samotným importem.
     *
     * @param source            zdrojová data pro import
     * @param targetFundVersion cílová verze AS
     * @return výsledek validace
     */
    public ValidateResult validateData(final ImportSource source,
                                       final ArrFundVersion targetFundVersion) {
        Assert.notNull(source, "Nebyl předán vstup");
        arrangementService.isValidAndOpenVersion(targetFundVersion);

        ValidateResult result = new ValidateResult();

        // zjištění podporovaných scope cílového archivního souboru
        Set<String> scopeCodesFund = scopeRepository.findCodesByFund(targetFundVersion.getFund());
        // zjištění používaných scope v podstromech vybraných JP
		for (RegScope scope : source.getScopes()) {
			if (!scopeCodesFund.contains(scope.getCode())) {
				result.addMissingScope(scope.getCode());
			}
        }

        // zjištění existujících názvů souborů v cílovém archivním souboru
        List<ArrFile> fundFiles = fundFileRepository.findByFund(targetFundVersion.getFund());
        Set<String> fileNamesFund = fundFiles.stream().map(ArrFile::getName).collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
        // zjištění používaných souborů v podstromech vybraných JP
        Set<String> fileNames = source.getFiles().stream().map(File::getName).collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
        result.setFileConflict(fileNames.size() > 0 && fileNamesFund.stream().anyMatch(fileNames::contains));
        if (result.isFileConflict()) {
            Set<String> conflictedFileNames = new HashSet<>(fileNamesFund);
            conflictedFileNames.retainAll(fileNames);
            result.setFileConflicts(conflictedFileNames);
        }

        // zjištění existujících obalů v cílovém archivním souboru
        List<? extends Packet> packetsFund = packetRepository.findByFund(targetFundVersion.getFund(), Lists.newArrayList(ArrPacket.State.OPEN, ArrPacket.State.CLOSED));
        // zjištění používaných obalů v podstromech vybraných JP
        Set<? extends Packet> packets = source.getPackets();
        result.setPacketConflict(packets.size() > 0 && packetsFund.stream().anyMatch(p -> {
            for (Packet packet : packets) {
                if ((packet.getStorageNumber() == null && p.getStorageNumber() == null) ||
                        (packet.getStorageNumber() != null && packet.getStorageNumber().equalsIgnoreCase(p.getStorageNumber()))
                        /*&& Objects.equal(packet.getPacketType(), p.getPacketType())*/) {
                    Collection<String> packetConflicts = result.getPacketConflicts();
                    if (packetConflicts == null) {
                        packetConflicts = new ArrayList<>();
                        result.setPacketConflicts(packetConflicts);
                    }
                    packetConflicts.add(packet.getStorageNumber());
                    return true;
                }
            }
            return false;
        }));

        return result;
    }

    /**
     * @return nová instance importního procesu
     */
    @Bean
    @org.springframework.context.annotation.Scope("prototype")
    public ImportProcess createImportProcess() {
        return new ImportProcess();
    }

    /**
     * @return nová instance importu z AS
     */
    @Bean
    @org.springframework.context.annotation.Scope("prototype")
    public ImportFromFund createImportFromFund() {
        return new ImportFromFund();
    }

    /**
     * Import dat ze zdroje do cílového AS.
     *
     * @param source                 zdroj importu
     * @param params                 parametry importu
     * @param targetFundVersion      verze AS do které kopírujeme
     * @param targetNode             uzel, vůči kterému kopírujeme
     * @param targetStaticParentNode rodič uzlu, vůči kterému kopírujeme
     * @param selectedDirection      směr kopírování
     */
    public void importData(final ImportSource source,
                           final ImportParams params,
                           final ArrFundVersion targetFundVersion,
                           final ArrNode targetNode,
                           final ArrNode targetStaticParentNode,
                           final ArrMoveLevelService.AddLevelDirection selectedDirection) {
        ImportProcess importProcess = createImportProcess();
        importProcess.init(source, params, targetFundVersion, targetNode, targetStaticParentNode, selectedDirection);
        importProcess.run();
    }

}
