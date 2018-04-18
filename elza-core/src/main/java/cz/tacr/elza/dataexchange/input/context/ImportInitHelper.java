package cz.tacr.elza.dataexchange.input.context;

import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.GroovyScriptService;

public class ImportInitHelper {

    private final ApExternalSystemRepository externalSystemRepository;

    private final GroovyScriptService groovyScriptService;

    private final InstitutionRepository institutionRepository;

    private final InstitutionTypeRepository institutionTypeRepository;

    private final ArrangementService arrangementService;

    private final LevelRepository levelRepository;

    private final ApAccessPointRepository accessPointRepository;

    private final PartyRepository partyRepository;

    private final PartyNameRepository nameRepository;

    private final PartyNameComplementRepository nameComplementRepository;

    private final PartyGroupIdentifierRepository groupIdentifierRepository;

    private final UnitdateRepository unitdateRepository;

    private final ApNameRepository apNameRepository;

    private final AccessPointDataService accessPointDataService;

    public ImportInitHelper(ApExternalSystemRepository externalSystemRepository,
                            GroovyScriptService groovyScriptService,
                            InstitutionRepository institutionRepository,
                            InstitutionTypeRepository institutionTypeRepository,
                            ArrangementService arrangementService,
                            LevelRepository levelRepository,
                            ApAccessPointRepository accessPointRepository,
                            PartyRepository partyRepository,
                            PartyNameRepository nameRepository,
                            PartyNameComplementRepository nameComplementRepository,
                            PartyGroupIdentifierRepository groupIdentifierRepository,
                            UnitdateRepository unitdateRepository, ApNameRepository apNameRepository, AccessPointDataService accessPointDataService) {
        this.externalSystemRepository = externalSystemRepository;
        this.groovyScriptService = groovyScriptService;
        this.institutionRepository = institutionRepository;
        this.institutionTypeRepository = institutionTypeRepository;
        this.arrangementService = arrangementService;
        this.levelRepository = levelRepository;
        this.accessPointRepository = accessPointRepository;
        this.partyRepository = partyRepository;
        this.nameRepository = nameRepository;
        this.nameComplementRepository = nameComplementRepository;
        this.groupIdentifierRepository = groupIdentifierRepository;
        this.unitdateRepository = unitdateRepository;
        this.apNameRepository = apNameRepository;
        this.accessPointDataService = accessPointDataService;
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

    public ApAccessPointRepository getAccessPointRepository() {
        return accessPointRepository;
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

    public ApNameRepository getApNameRepository() {
        return apNameRepository;
    }

    public AccessPointDataService getAccessPointDataService() {
        return accessPointDataService;
    }
}
