package cz.tacr.elza.controller.vo;

/**
 * ArrOutputFile Value object
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
public class ArrOutputFileVO extends DmsFileVO {

    private Integer outputResultId;

    public Integer getOutputResultId() {
        return outputResultId;
    }

    public void setOutputResultId(Integer outputResultId) {
        this.outputResultId = outputResultId;
    }
}
