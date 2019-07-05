/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.tacr.elza.factory;

import cz.tacr.elza.destructransferrequest.service.DescructTransferRequestService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.WorkspaceItemService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Abstraktní service pro získání instance pro práci s databází
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 5.07.2019.
 */
public class ContentServiceFactoryImpl extends ContentServiceFactory {

    @Autowired(required = true)
    private List<DSpaceObjectService<? extends DSpaceObject>> dSpaceObjectServices;

    @Autowired(required = true)
    private List<DSpaceObjectLegacySupportService<? extends DSpaceObject>> dSpaceObjectLegacySupportServices;

    @Autowired(required = true)
    private WorkspaceItemService workspaceItemService;

    @Autowired(required = true)
    private DescructTransferRequestService descructTransferRequestService;

    @Override
    public List<DSpaceObjectService<? extends DSpaceObject>> getDSpaceObjectServices() {
        return dSpaceObjectServices;
    }

    @Override
    public List<DSpaceObjectLegacySupportService<? extends DSpaceObject>> getDSpaceObjectLegacySupportServices() {
        return dSpaceObjectLegacySupportServices;
    }

    @Override
    public WorkspaceItemService getWorkspaceItemService() {
        return workspaceItemService;
    }

    @Override
    public DescructTransferRequestService getDescructTransferRequestService() {
        return descructTransferRequestService;
    }
    }

