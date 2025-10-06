package cz.tacr.elza.cam.v2;

import cz.tacr.cam.v2.client.ApiException;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.SystemException;

public class CamException {

    public static AbstractException prepareExtSystemException(ApiException e) {
        return new SystemException("Došlo k chybě při komunikaci s externím systémem.", e)
                .set("responseBody", e.getResponseBody())
                .set("responseCode", e.getCode())
                .set("responseHeaders", e.getResponseHeaders());
    }
}
