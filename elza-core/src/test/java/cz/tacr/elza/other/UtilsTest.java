package cz.tacr.elza.other;

import java.io.File;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.packageimport.PackageService;


/**
 * @author Martin Šlapa
 * @since 16.2.2016
 */
@Service
public class UtilsTest {

    @Autowired
    private PackageService packageService;

    @Transactional
    public void importPackage(final File file) {
        packageService.importPackage(file);
    }

    public List<RulPackage> getPackages() {
        return packageService.getPackages();
    }
}
