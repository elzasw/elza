package cz.tacr.elza.dataexchange.input.context;

import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.repository.ApRecordRepository;
import cz.tacr.elza.repository.ApVariantRecordRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.StructObjService;

/**
 * Helper object with references to all repositories and services
 * 
 *
 */
public class ImportInitHelper {

    private final ApExternalSystemRepository externalSystemRepository;

    private final GroovyScriptService groovyScriptService;

    private final InstitutionRepository institutionRepository;

    private final InstitutionTypeRepository institutionTypeRepository;

    private final ArrangementService arrangementService;

    private final StructObjService structObjService;

    private final LevelRepository levelRepository;

    private final ApRecordRepository recordRepository;

    private final ApVariantRecordRepository variantRecordRepository;

    private final PartyRepository partyRepository;

    private final PartyNameRepository nameRepository;

    private final PartyNameComplementRepository nameComplementRepository;

    private final PartyGroupIdentifierRepository groupIdentifierRepository;

    private final UnitdateRepository unitdateRepository;

    public ImportInitHelper(ApExternalSystemRepository externalSystemRepository,
                            GroovyScriptService groovyScriptService,
                            InstitutionRepository institutionRepository,
                            InstitutionTypeRepository institutionTypeRepository,
                            ArrangementService arrangementService,
                            LevelRepository levelRepository,
                            ApRecordRepository recordRepository,
                            ApVariantRecordRepository variantRecordRepository,
                            PartyRepository partyRepository,
                            PartyNameRepository nameRepository,
                            PartyNameComplementRepository nameComplementRepository,
                            PartyGroupIdentifierRepository groupIdentifierRepository,
                            UnitdateRepository unitdateRepository,
                            StructObjService structObjService) {
        this.externalSystemRepository = externalSystemRepository;
        this.groovyScriptService = groovyScriptService;
        this.institutionRepository = institutionRepository;
        this.institutionTypeRepository = institutionTypeRepository;
        this.arrangementService = arrangementService;
        this.levelRepository = levelRepository;
        this.recordRepository = recordRepository;
        this.variantRecordRepository = variantRecordRepository;
        this.partyRepository = partyRepository;
        this.nameRepository = nameRepository;
        this.nameComplementRepository = nameComplementRepository;
        this.groupIdentifierRepository = groupIdentifierRepository;
        this.unitdateRepository = unitdateRepository;
        this.structObjService = structObjService;
    }

    public ApExternalSystemRepository getExternalSystemRepository() {
        return externalSystemRepository;
    }

    public GroovyScriptService getGroovyScriptService() {
        return groovyScriptService;
    }

    public InstitutionRepository getInstitutionRepository() {
        return institutionRepository;
    }

    public InstitutionTypeRepository getInstitutionTypeRepository() {
        return institutionTypeRepository;
    }

    public ArrangementService getArrangementService() {
        return arrangementService;
    }

    public LevelRepository getLevelRepository() {
        return levelRepository;
    }

    public ApRecordRepository getRecordRepository() {
        return recordRepository;
    }

    public ApVariantRecordRepository getVariantRecordRepository() {
        return variantRecordRepository;
    }

    public PartyRepository getPartyRepository() {
        return partyRepository;
    }

    public PartyNameRepository getNameRepository() {
        return nameRepository;
    }

    public PartyNameComplementRepository getNameComplementRepository() {
        return nameComplementRepository;
    }

    public PartyGroupIdentifierRepository getGroupIdentifierRepository() {
        return groupIdentifierRepository;
    }

    public UnitdateRepository getUnitdateRepository() {
        return unitdateRepository;
    }

    public StructObjService getStructObjService() {
        return structObjService;
    }
}
