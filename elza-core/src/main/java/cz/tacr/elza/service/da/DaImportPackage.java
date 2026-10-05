package cz.tacr.elza.service.da;

import javax.annotation.Nullable;

/**
 * The package being imported, as the DA_IMPORT script of the rules sees it (variable
 * {@code PACKAGE}): what kind of package it is and how the user asked to import it.
 */
public class DaImportPackage {

    private final String contentType;
    private final String profile;
    private final boolean fileplanAsRoot;

    /**
     * @param fileplanAsRoot whether the file plan the package comes from is imported as the root
     *            series; by default it is skipped and only the hierarchy below it is imported
     */
    public DaImportPackage(@Nullable String contentType, @Nullable String profile, boolean fileplanAsRoot) {
        this.contentType = contentType;
        this.profile = profile;
        this.fileplanAsRoot = fileplanAsRoot;
    }

    /** @see AipPackageType#contentType() */
    @Nullable
    public String getContentType() {
        return contentType;
    }

    /** @see AipPackageType#profile() */
    @Nullable
    public String getProfile() {
        return profile;
    }

    public boolean isFileplanAsRoot() {
        return fileplanAsRoot;
    }
}
