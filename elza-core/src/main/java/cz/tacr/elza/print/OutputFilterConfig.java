package cz.tacr.elza.print;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import cz.tacr.elza.dataexchange.output.filters.FilterConfig;
import cz.tacr.elza.dataexchange.output.filters.conditions.And;
import cz.tacr.elza.dataexchange.output.filters.conditions.EntityProperties;
import cz.tacr.elza.dataexchange.output.filters.conditions.Not;
import cz.tacr.elza.dataexchange.output.filters.conditions.PartCondition;
import cz.tacr.elza.exception.SystemException;

public class OutputFilterConfig extends FilterConfig {

    /**
     * Reads output filter configuration from its YAML file.
     */
    public static OutputFilterConfig load(Path outputFilterFile) {
        Constructor yamlCtor = new Constructor(new LoaderOptions());
        yamlCtor.addTypeDescription(new TypeDescription(OutputFilterConfig.class, "!OutputFilterConfig"));
        yamlCtor.addTypeDescription(new TypeDescription(EntityProperties.class, "!EntityProperties"));
        yamlCtor.addTypeDescription(new TypeDescription(And.class, "!And"));
        yamlCtor.addTypeDescription(new TypeDescription(Not.class, "!Not"));
        yamlCtor.addTypeDescription(new TypeDescription(PartCondition.class, "!Part"));
        Yaml yamlLoader = new Yaml(yamlCtor);

        try (InputStream inputStream = new FileInputStream(outputFilterFile.toFile())) {
            return yamlLoader.load(inputStream);
        } catch (IOException e) {
            throw new SystemException("Failed to read output filter file: " + outputFilterFile, e);
        }
    }
}
