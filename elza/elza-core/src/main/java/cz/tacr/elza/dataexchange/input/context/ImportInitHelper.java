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

    private final ApExternalIdRepository apEidRepository;

    private final ApPartRepository apPartRepository;

    private final ApItemRepository apItemRepository;

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
                            final ApExternalIdRepository apEidRepository,
                            final StructObjValueService structObjService,
                            final AccessPointService accessPointService,
                            final DmsService dmsService,
                            final ApStateRepository apStateRepository,
                            final ApPartRepository apPartRepository,
                            final ApItemRepository apItemRepository) {
        this.groovyScriptService = groovyScriptService;
        this.institutionRepository = institutionRepository;
        this.institutionTypeRepository = institutionTypeRepository;
        this.arrangementService = arrangementService;
        this.levelRepository = levelRepository;
        this.apRepository = apRepository;
        this.apEidRepository = apEidRepository;
        this.structObjService = structObjService;
        this.accessPointService = accessPointService;
        this.dmsService = dmsService;
        this.apStateRepository = apStateRepository;
        this.apPartRepository = apPartRepository;
        this.apItemRepository = apItemRepository;
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

    public ApExternalIdRepository getApEidRepository() {
        return apEidRepository;
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

    public ApPartRepository getApPartRepository() { return apPartRepository; }

    public ApItemRepository getApItemRepository() {
        return apItemRepository;
    }
}
