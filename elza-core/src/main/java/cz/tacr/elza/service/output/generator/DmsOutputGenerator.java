package cz.tacr.elza.service.output.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.TempFileProvider;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.exception.ProcessException;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.output.OutputParams;

public abstract class DmsOutputGenerator implements OutputGenerator {

    protected final EntityManager em;

    protected final DmsService dmsService;

    protected final TempFileProvider tempFileProvider = new TempFileProvider("elza-output-");

    protected OutputParams params;

    private Path tmpResultFile;

    protected DmsOutputGenerator(EntityManager em, DmsService dmsService) {
        this.em = em;
        this.dmsService = dmsService;
    }

    @Override
    public void init(OutputParams params) {
        this.params = params;
    }

    @Override
    public final ArrOutputResult generate() {
        try (OutputStream os = openNewResultFile()) {
            generate(os);
            os.close();
            return storeResult();
        } catch (IOException e) {
            throw new ProcessException(params.getOutputId(), "Failed to create output result", e);
        } finally {
            tempFileProvider.close();
        }
    }

    protected abstract void generate(OutputStream os) throws IOException;

    private OutputStream openNewResultFile() throws IOException {
        Validate.isTrue(tmpResultFile == null);

        tmpResultFile = tempFileProvider.createTempFile();
        return Files.newOutputStream(tmpResultFile, StandardOpenOption.WRITE);
    }

    private ArrOutputResult storeResult() throws IOException {
        ArrOutputResult result = new ArrOutputResult();
        result.setChange(params.getChange());
        result.setTemplate(params.getTemplate());
        result.setOutput(params.getOutput());

        em.persist(result);

        ArrOutputFile outputFile = createOutputFile(result);

        try (InputStream is = Files.newInputStream(tmpResultFile, StandardOpenOption.READ)) {
            dmsService.createFile(outputFile, is);
        }
        return result;
    }

    private ArrOutputFile createOutputFile(ArrOutputResult result) {
        String definitionName = params.getOutput().getName();
        RulTemplate template = params.getTemplate();

        ArrOutputFile file = new ArrOutputFile();
        file.setName(definitionName);
        file.setFileName(definitionName + "." + template.getExtension());
        file.setMimeType(template.getMimeType());
        file.setFileSize(0); // DmsService will set real value after write
        
        result.addOutputFile(file);
        return file;
    }
}
