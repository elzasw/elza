package cz.tacr.elza.service.importnodes;

import com.google.common.base.Objects;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.StructureDataRepository;
import cz.tacr.elza.service.ArrMoveLevelService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.importnodes.vo.File;
import cz.tacr.elza.service.importnodes.vo.ImportParams;
import cz.tacr.elza.service.importnodes.vo.ImportSource;
import cz.tacr.elza.service.importnodes.vo.Structured;
import cz.tacr.elza.service.importnodes.vo.Scope;
import cz.tacr.elza.service.importnodes.vo.ValidateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
    private StructureDataRepository structureDataRepository;

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
        Set<String> scopeCodes = source.getScopes().stream().map(Scope::getCode).collect(Collectors.toSet());
        result.setScopeError(scopeCodes.size() > 0 && !scopeCodesFund.containsAll(scopeCodes));
        if (result.isScopeError()) {
            Set<String> missedScopesCodes = new HashSet<>(scopeCodes);
            missedScopesCodes.removeAll(scopeCodesFund);
            result.setScopeErrors(missedScopesCodes);
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
        List<? extends Structured> structuredFund = structureDataRepository.findByFundAndDeleteChangeIsNull(targetFundVersion.getFund());
        // zjištění používaných obalů v podstromech vybraných JP
        Set<? extends Structured> structuredList = source.getStructuredList();
        result.setStructuredConflict(structuredList.size() > 0 && structuredFund.stream().anyMatch(p -> {
            for (Structured structured : structuredList) {
                if (Objects.equal(structured.getValue(), p.getValue())) {
                    Collection<String> structuredConflicts = result.getStructuredConflicts();
                    if (structuredConflicts == null) {
                        structuredConflicts = new ArrayList<>();
                        result.setStructuredConflicts(structuredConflicts);
                    }
                    structuredConflicts.add(structured.getValue());
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
