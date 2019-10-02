package cz.tacr.elza.common;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper methods to prepare name of downloaded file
 * and set headers
 *
 */
public abstract class FileDownload {
    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    /**
     * Add instructions to the servlet to download file as attachment
     * 
     * @param response
     * @param filename
     */
    public static void addContentDispositionAsAttachment(HttpServletResponse response, String srcFilename) {
        StringBuilder sb = new StringBuilder();
        String incorrectChars = ",;\"`'\\/:+*|!<>";
        char[] chars = StringUtils.stripAccents(srcFilename).toCharArray();
        boolean onlyWhitespaces = true;
        for (char c : chars) {
            if (incorrectChars.indexOf(c) >= 0) {
                // skip 
                continue;
            }
            if (!Character.isWhitespace(c)) {
                onlyWhitespaces = false;
            } else {
                // skip leading whitespaces
                if (onlyWhitespaces) {
                    continue;
                }
            }
            sb.append(c);
        }
        // add default name
        if (onlyWhitespaces) {
            sb.append("download.bin");
        }
        response.setHeader("Content-Disposition", "attachment; filename=" + sb.toString());
    }
}
