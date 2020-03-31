package cz.tacr.elza.dataexchange.input.context;

import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.StructObjValueService;

public class ImportInitHelper {

    private final GroovyScriptService groovyScriptService;

    private final InstitutionRepository institutionRepository;

    private final InstitutionTypeRepository institutionTypeRepository;

    private final ArrangementService arrangementService;

    private final LevelRepository levelRepository;

    private final ApAccessPointRepository apRepository;

    private final ApNameRepository apNameRepository;

    private final ApDescriptionRepository apDescRepository;

    private final ApExternalIdRepository apEidRepository;

    private final PartyRepository partyRepository;

    private final PartyNameRepository nameRepository;

    private final PartyNameComplementRepository nameComplementRepository;

    private final PartyGroupIdentifierRepository groupIdentifierRepository;

    private final UnitdateRepository unitdateRepository;

    private final StructObjValueService structObjService;

    private final AccessPointService accessPointService;

    private final DmsService dmsService;

    private final ApStateRepository apStateRepository;

    public ImportInitHelper(final GroovyScriptService groovyScriptService,
                            final InstitutionRepository institutionRepository,
                            final InstitutionTypeRepository institutionTypeRepository,
                            final ArrangementService arrangementService,
                            final LevelRepository levelRepository,
                            final ApAccessPointRepository apRepository,
                            final ApNameRepository apNameRepository,
                            final ApDescriptionRepository apDescRepository,
                            final ApExternalIdRepository apEidRepository,
                            final PartyRepository partyRepository,
                            final PartyNameRepository nameRepository,
                            final PartyNameComplementRepository nameComplementRepository,
                            final PartyGroupIdentifierRepository groupIdentifierRepository,
                            final UnitdateRepository unitdateRepository,
                            final StructObjValueService structObjService,
                            final AccessPointService accessPointService,
                            final DmsService dmsService,
                            final ApStateRepository apStateRepository) {
        this.groovyScriptService = groovyScriptService;
        this.institutionRepository = institutionRepository;
        this.institutionTypeRepository = institutionTypeRepository;
        this.arrangementService = arrangementService;
        this.levelRepository = levelRepository;
        this.apRepository = apRepository;
        this.apNameRepository = apNameRepository;
        this.apDescRepository = apDescRepository;
        this.apEidRepository = apEidRepository;
        this.partyRepository = partyRepository;
        this.nameRepository = nameRepository;
        this.nameComplementRepository = nameComplementRepository;
        this.groupIdentifierRepository = groupIdentifierRepository;
        this.unitdateRepository = unitdateRepository;
        this.structObjService = structObjService;
        this.accessPointService = accessPointService;
        this.dmsService = dmsService;
        this.apStateRepository = apStateRepository;
    }

    public DmsService getDmsService() {
        return dmsService;
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

    public ApAccessPointRepository getApRepository() {
        return apRepository;
    }

    public ApNameRepository getApNameRepository() {
        return apNameRepository;
    }

    public ApDescriptionRepository getApDescRepository() {
        return apDescRepository;
    }

    public ApExternalIdRepository getApEidRepository() {
        return apEidRepository;
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

    public StructObjValueService getStructObjService() {
        return structObjService;
    }

    public AccessPointService getAccessPointService() {
        return accessPointService;
    }

    public ApStateRepository getApStateRepository() {
        return apStateRepository;
    }
}
