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
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.InProgressSubmissionService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.factory.WorkflowServiceFactory;

import java.util.List;

/**
 * Abstraktní service pro získání instance pro práci s databází
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 5.07.2019.
 */
public abstract class ContentServiceFactory {

    public abstract List<DSpaceObjectService<? extends DSpaceObject>> getDSpaceObjectServices();

    public abstract List<DSpaceObjectLegacySupportService<? extends DSpaceObject>> getDSpaceObjectLegacySupportServices();

    public abstract WorkspaceItemService getWorkspaceItemService();

    public abstract DescructTransferRequestService getDescructTransferRequestService();

    public InProgressSubmissionService getInProgressSubmissionService(InProgressSubmission inProgressSubmission)
    {
        if(inProgressSubmission instanceof WorkspaceItem)
        {
            return getWorkspaceItemService();
        }
        else
        {
            return WorkflowServiceFactory.getInstance().getWorkflowItemService();
        }
    }

    public<T extends DSpaceObject> DSpaceObjectService<T> getDSpaceObjectService(T dso)
    {
        // No need to worry when supressing, as long as our "getDSpaceObjectManager" method is properly implemented
        // no casting issues should occur
        @SuppressWarnings("unchecked")
        DSpaceObjectService<T> manager = getDSpaceObjectService(dso.getType());
        return manager;
    }

    public DSpaceObjectService getDSpaceObjectService(int type)
    {
        for (int i = 0; i < getDSpaceObjectServices().size(); i++) {
            DSpaceObjectService objectService = getDSpaceObjectServices().get(i);
            if(objectService.getSupportsTypeConstant() == type)
            {
                return objectService;
            }
        }
        throw new UnsupportedOperationException("Unknown DSpace type: " + type);
    }

    public DSpaceObjectLegacySupportService<? extends DSpaceObject> getDSpaceLegacyObjectService(int type)
    {
        for (int i = 0; i < getDSpaceObjectLegacySupportServices().size(); i++) {
            DSpaceObjectLegacySupportService<? extends DSpaceObject> objectLegacySupportService = getDSpaceObjectLegacySupportServices().get(i);
            if(objectLegacySupportService.getSupportsTypeConstant() == type)
            {
                return objectLegacySupportService;
            }

        }
        throw new UnsupportedOperationException("Unknown DSpace type: " + type);
    }

    public static ContentServiceFactory getInstance(){
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("contentServiceFactory", ContentServiceFactory.class);
    }

}
