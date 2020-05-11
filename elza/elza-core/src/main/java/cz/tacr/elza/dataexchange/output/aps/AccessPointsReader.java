package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.UserService;

/**
 * Reads access points specified by context.<br>
 * <i>Note: Implementation does not export coordinates, which will be most
 * likely removed in
 * future.</i>
 */
public class AccessPointsReader implements ExportReader {

    private final Set<Integer> authorizedScopeIds = new HashSet<>();

    private final ExportContext context;

    private final EntityManager em;

    private final UserService userService;

    public AccessPointsReader(ExportContext context, ExportInitHelper initHelper) {
        this.context = context;
        this.em = initHelper.getEm();
        this.userService = initHelper.getUserService();
    }

    /**
     * Reads all access points. Party access points are passed to
     * <code>PartiesReader</code>.
     */
    @Override
    public void read() {
        ApOutputStream os = context.getBuilder().openAccessPointsOutputStream(context);
        try {
            readAccessPoints(os);
            os.processed();
        } finally {
            authorizedScopeIds.clear();
            os.close();
        }
    }

    private void readAccessPoints(ApOutputStream os) {
        ApInfoLoader loader = new ApInfoLoader(context, em, userService);
        for (Integer apId : context.getApIds()) {
            ApInfoDispatcher dispatcher = new ApInfoDispatcher(context.getStaticData()) {
                @Override
                protected void onCompleted() {
                    ApInfo apInfo = getApInfo();
                    if(apInfo==null) {
                    	throw new SystemException("Entity not found.", BaseCode.ID_NOT_EXIST)
                    		.set("ID", apId);
                    }
                    if (!apInfo.isPartyAp()) {
                        ItemDispatcher itd = new ItemDispatcher(context.getStaticData()) {
                            @Override
                            protected void onCompleted() {
                                apInfo.setItems(this.getPartItemsMap());
                            }
                        };
                        ItemLoader itemLoader = new ItemLoader(context, em, 1000);
                        List<ApPart> parts = new ArrayList<>(apInfo.getParts());
                        for(ApPart part : parts) {
                            itemLoader.addRequest(part.getPartId(), itd);
                        }
                        itemLoader.flushItem();
                        os.addAccessPoint(apInfo);
                    }
                }
            };
            loader.addRequest(apId, dispatcher);
        }
        loader.flush();
    }
}
