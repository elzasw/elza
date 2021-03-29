package cz.tacr.elza.dataexchange.input.context;

import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.*;

public class ImportInitHelper {

    private final GroovyScriptService groovyScriptService;

    private final InstitutionRepository institutionRepository;

    private final InstitutionTypeRepository institutionTypeRepository;

    private final ArrangementService arrangementService;
    
    private final ArrangementInternalService arrangementInternalService;

    private final LevelRepository levelRepository;

    private final ApAccessPointRepository apRepository;

    private final ApBindingRepository bindingRepository;

    private final ApBindingStateRepository bindingStateRepository;

    private final ApPartRepository apPartRepository;

    private final ApItemRepository apItemRepository;

    private final StructObjValueService structObjService;

    private final AccessPointService accessPointService;

    private final AccessPointItemService accessPointItemService;

    private final DmsService dmsService;

    private final ApStateRepository apStateRepository;

    public ImportInitHelper(final GroovyScriptService groovyScriptService,
                            final InstitutionRepository institutionRepository,
                            final InstitutionTypeRepository institutionTypeRepository,
                            final ArrangementService arrangementService,
                            final ArrangementInternalService arrangementInternalService,
                            final LevelRepository levelRepository,
                            final ApAccessPointRepository apRepository,
                            final ApBindingRepository bindingRepository,
                            final StructObjValueService structObjService,
                            final AccessPointService accessPointService,
                            final DmsService dmsService,
                            final ApStateRepository apStateRepository,
                            final ApPartRepository apPartRepository,
                            final ApItemRepository apItemRepository,
                            final AccessPointItemService accessPointItemService,
                            final ApBindingStateRepository bindingStateRepository) {
        this.groovyScriptService = groovyScriptService;
        this.institutionRepository = institutionRepository;
        this.institutionTypeRepository = institutionTypeRepository;
        this.arrangementService = arrangementService;
        this.arrangementInternalService = arrangementInternalService;
        this.levelRepository = levelRepository;
        this.apRepository = apRepository;
        this.bindingRepository = bindingRepository;
        this.structObjService = structObjService;
        this.accessPointService = accessPointService;
        this.dmsService = dmsService;
        this.apStateRepository = apStateRepository;
        this.apPartRepository = apPartRepository;
        this.apItemRepository = apItemRepository;
        this.accessPointItemService = accessPointItemService;
        this.bindingStateRepository = bindingStateRepository;
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

    public ArrangementInternalService getArrangementInternalService() {
        return arrangementInternalService;
    }

    public LevelRepository getLevelRepository() {
        return levelRepository;
    }

    public ApAccessPointRepository getApRepository() {
        return apRepository;
    }

    public ApBindingRepository getBindingRepository() {
        return bindingRepository;
    }

    public ApBindingStateRepository getBindingStateRepository() {
        return bindingStateRepository;
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

    public ApPartRepository getApPartRepository() {
        return apPartRepository;
    }

    public ApItemRepository getApItemRepository() {
        return apItemRepository;
    }

    public AccessPointItemService getAccessPointItemService() {
        return accessPointItemService;
    }
}
