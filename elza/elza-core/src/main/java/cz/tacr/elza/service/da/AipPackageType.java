package cz.tacr.elza.service.da;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.domain.DaAipState;
import gov.loc.mets.v1_11.schema.MetsType;

/**
 * Type of an AIP as its METS declares it: the type of its content and its profile.
 *
 * @param contentType {@code csip:CONTENTINFORMATIONTYPE}, or {@code csip:OTHERCONTENTINFORMATIONTYPE}
 *            when the former is OTHER (e.g. NSESSS); null when the METS declares none
 * @param profile the METS PROFILE (e.g. https://stands.nacr.cz/da/2023/aip.xml); null when none
 */
public record AipPackageType(@Nullable String contentType, @Nullable String profile) {

    private static final Logger logger = LoggerFactory.getLogger(AipPackageType.class);

    public static final String CSIP_NAMESPACE = "https://DILCIS.eu/XML/METS/CSIPExtensionMETS";

    static final QName CONTENT_INFORMATION_TYPE = new QName(CSIP_NAMESPACE, "CONTENTINFORMATIONTYPE");
    static final QName OTHER_CONTENT_INFORMATION_TYPE = new QName(CSIP_NAMESPACE, "OTHERCONTENTINFORMATIONTYPE");

    /** Value of CONTENTINFORMATIONTYPE that defers to OTHERCONTENTINFORMATIONTYPE. */
    static final String OTHER = "OTHER";

    /** Length of the columns the values are stored in. */
    private static final int MAX_LENGTH = 250;

    /**
     * A package that declares OTHER without saying what it is keeps OTHER - the type is only
     * informative, the package is not refused for it.
     *
     * @throws AipProblemException when a value is too long to be stored
     */
    public static AipPackageType of(MetsType mets) {
        String contentType = StringUtils.trimToNull(mets.getOtherAttributes().get(CONTENT_INFORMATION_TYPE));
        if (OTHER.equals(contentType)) {
            String other = StringUtils.trimToNull(mets.getOtherAttributes().get(OTHER_CONTENT_INFORMATION_TYPE));
            if (other != null) {
                contentType = other;
            } else {
                logger.warn("METS balíčku {} uvádí CONTENTINFORMATIONTYPE=OTHER bez OTHERCONTENTINFORMATIONTYPE",
                            mets.getOBJID());
            }
        }
        return new AipPackageType(checkLength(contentType, "typ obsahu"),
                checkLength(StringUtils.trimToNull(mets.getPROFILE()), "profil"));
    }

    public void applyTo(DaAipState aipState) {
        aipState.setContentType(contentType);
        aipState.setProfile(profile);
    }

    @Nullable
    private static String checkLength(@Nullable String value, String what) {
        if (value != null && value.length() > MAX_LENGTH) {
            throw AipProblemException.metadata("METS balíčku uvádí " + what + " delší než " + MAX_LENGTH
                    + " znaků: '" + StringUtils.abbreviate(value, 80) + "'", "METS.xml", null);
        }
        return value;
    }
}
