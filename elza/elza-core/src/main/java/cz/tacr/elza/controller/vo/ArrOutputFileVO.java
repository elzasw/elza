package cz.tacr.elza.controller.vo;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.repository.OutputResultRepository;

import static cz.tacr.elza.repository.ExceptionThrow.outputResult;

/**
 * ArrOutputFile Value object
 *
 * 
 * @since 13.3.2016
 */
public class ArrOutputFileVO extends DmsFileVO {

    private Integer outputResultId;

    public ArrOutputFileVO() {

    }

    public ArrOutputFileVO(ArrOutputFile srcFile) {
        super(srcFile);

        outputResultId = srcFile.getOutputResult().getOutputResultId();
    }

    public Integer getOutputResultId() {
        return outputResultId;
    }

    public void setOutputResultId(Integer outputResultId) {
        this.outputResultId = outputResultId;
    }

    public ArrOutputFile createEntity(OutputResultRepository outputResultRepository) {
        ArrOutputFile result = new ArrOutputFile();

        ArrOutputResult dbResult = outputResultRepository.findById(outputResultId)
                .orElseThrow(outputResult(outputResultId));

        copyTo(result);
        return result;
    }

    public static ArrOutputFileVO newInstance(ArrOutputFile srcFile) {
        ArrOutputFileVO result = new ArrOutputFileVO(srcFile);

        return result;
    }
}
