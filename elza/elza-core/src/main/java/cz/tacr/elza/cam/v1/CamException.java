package cz.tacr.elza.cam.v1;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.cam.v1.client.ApiException;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.SystemException;

public class CamException {

    public static AbstractException prepareExtSystemException(ApiException e) {
        return new SystemException("Došlo k chybě při komunikaci s externím systémem.", e)
                .set("responseBody", e.getResponseBody())
                .set("responseCode", e.getCode())
                .set("responseHeaders", e.getResponseHeaders());
    }

    public static String getApiExceptionInfo(ApiException e) {
        StringBuilder sb = new StringBuilder(e.getMessage());
        sb.append(", code: ").append(e.getCode());
        String body = e.getResponseBody();
        if (StringUtils.isNotEmpty(body)) {
            sb.append(", response: ").append(body);
        }
        return sb.toString();
    }
}
