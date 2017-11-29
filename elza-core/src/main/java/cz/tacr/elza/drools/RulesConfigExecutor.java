package cz.tacr.elza.drools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;


/**
 * Serviska má na starosti spouštění pravidel přes Drools.
 *
 * @author Martin Šlapa
 * @since 26.11.2015
 */
@Service
public class RulesConfigExecutor {

    /**
     * Název složky v drools.
     */
    public static final String FOLDER = "drools";

    /**
     * Název složky v groovies.
     */
    public static final String GROOVY_FOLDER = "groovies";

    /**
     * Cesta adresáře pro konfiguraci pravidel.
     */
    @Value("${elza.packagesDir}")
    private String packagesDir;

    public String getPackagesDir() {
        return packagesDir;
    }

    /**
     * Vrací úplnou cestu k adresáři drools podle balíčku.
     *
     *
     * @param packageCode
     * @param ruleCode kód pravidel
     * @return cesta k adresáři drools
     */
    public String getDroolsDir(final String packageCode, final String ruleCode) {
        return packagesDir + File.separator + packageCode + File.separator + ruleCode + File.separator + FOLDER;
    }

    /**
     * Vrací úplnou cestu k adresáři drools podle balíčku.
     *
     *
     * @param packageCode
     * @param ruleCode kód pravidel
     * @return cesta k adresáři drools
     */
    public String getGroovyDir(final String packageCode, final String ruleCode) {
        return packagesDir + File.separator + packageCode + File.separator + ruleCode + File.separator + GROOVY_FOLDER;
    }

}
