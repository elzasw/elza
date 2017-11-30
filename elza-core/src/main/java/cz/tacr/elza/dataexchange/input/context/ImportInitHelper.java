package cz.tacr.elza.dataexchange.input.context;

import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegCoordinatesRepository;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegVariantRecordRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.GroovyScriptService;

public class ImportInitHelper {

    private final RegExternalSystemRepository externalSystemRepository;

    private final GroovyScriptService groovyScriptService;

    private final InstitutionRepository institutionRepository;

    private final InstitutionTypeRepository institutionTypeRepository;

    private final ArrangementService arrangementService;

    private final LevelRepository levelRepository;

    private final RegRecordRepository recordRepository;

    private final RegCoordinatesRepository coordinatesRepository;

    private final RegVariantRecordRepository variantRecordRepository;

    private final PartyRepository partyRepository;

    private final PartyNameRepository nameRepository;

    private final PartyNameComplementRepository nameComplementRepository;

    private final PartyGroupIdentifierRepository groupIdentifierRepository;

    private final UnitdateRepository unitdateRepository;

    public ImportInitHelper(RegExternalSystemRepository externalSystemRepository,
                            GroovyScriptService groovyScriptService,
                            InstitutionRepository institutionRepository,
                            InstitutionTypeRepository institutionTypeRepository,
                            ArrangementService arrangementService,
                            LevelRepository levelRepository,
                            RegRecordRepository recordRepository,
                            RegCoordinatesRepository coordinatesRepository,
                            RegVariantRecordRepository variantRecordRepository,
                            PartyRepository partyRepository,
                            PartyNameRepository nameRepository,
                            PartyNameComplementRepository nameComplementRepository,
                            PartyGroupIdentifierRepository groupIdentifierRepository,
                            UnitdateRepository unitdateRepository) {
        this.externalSystemRepository = externalSystemRepository;
        this.groovyScriptService = groovyScriptService;
        this.institutionRepository = institutionRepository;
        this.institutionTypeRepository = institutionTypeRepository;
        this.arrangementService = arrangementService;
        this.levelRepository = levelRepository;
        this.recordRepository = recordRepository;
        this.coordinatesRepository = coordinatesRepository;
        this.variantRecordRepository = variantRecordRepository;
        this.partyRepository = partyRepository;
        this.nameRepository = nameRepository;
        this.nameComplementRepository = nameComplementRepository;
        this.groupIdentifierRepository = groupIdentifierRepository;
        this.unitdateRepository = unitdateRepository;
    }

    public RegExternalSystemRepository getExternalSystemRepository() {
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

    public RegRecordRepository getRecordRepository() {
        return recordRepository;
    }

    public RegCoordinatesRepository getCoordinatesRepository() {
        return coordinatesRepository;
    }

    public RegVariantRecordRepository getVariantRecordRepository() {
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
}
