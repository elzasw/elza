package cz.tacr.elza.dataexchange.output.aps;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
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
        ApOutputStream os = context.getBuilder().openAccessPointsOutputStream();
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
                    if (!apInfo.isPartyAp()) {
                        os.addAccessPoint(apInfo);
                    }
                }
            };
            loader.addRequest(apId, dispatcher);
        }
        loader.flush();
    }
}
