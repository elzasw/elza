package cz.tacr.elza.service.importnodes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.importnodes.vo.ImportParams;
import cz.tacr.elza.service.importnodes.vo.ImportSource;
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
    private StructuredObjectRepository structureDataRepository;

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
		for (ApScope scope : source.getScopes()) {
			if (!scopeCodesFund.contains(scope.getCode())) {
				result.addMissingScope(scope.getCode());
			}
        }

        // zjištění existujících názvů souborů v cílovém archivním souboru
        List<ArrFile> fundFiles = fundFileRepository.findByFund(targetFundVersion.getFund());
		Set<String> fundfileNames = fundFiles.stream().map(ArrFile::getName)
		        .collect(Collectors.toCollection(() -> new HashSet<>()));
        // zjištění používaných souborů v podstromech vybraných JP
		List<ArrFile> srcFiles = source.getFiles();
		for (ArrFile srcFile : srcFiles) {
			// check if exists
			if (fundfileNames.contains(srcFile.getName())) {
				result.addFileConflict(srcFile.getName());
			}
		}

        // zjištění existujících obalů v cílovém archivním souboru
        List<ArrStructuredObject> packetsFund = structureDataRepository
                .findByFundAndDeleteChangeIsNull(targetFundVersion.getFund());
        // zjištění používaných obalů v podstromech vybraných JP
        List<ArrStructuredObject> packets = source.getStructuredList();
        for (ArrStructuredObject srcPacket : packets) {
            for (ArrStructuredObject currPacket : packetsFund) {

				//TODO: compare packet id instead of objects
                if (StringUtils.equalsIgnoreCase(srcPacket.getValue(), currPacket.getValue())) {
					// we found a conflict
                    result.addStructObjConflicts(srcPacket);
					break;
				}
			}
		}
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
                           final FundLevelService.AddLevelDirection selectedDirection) {
        ImportProcess importProcess = createImportProcess();
        importProcess.init(source, params, targetFundVersion, targetNode, targetStaticParentNode, selectedDirection);
        importProcess.run();
    }

}
