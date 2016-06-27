package cz.tacr.elza.controller.vo.nodes.descitems;


import cz.tacr.elza.controller.vo.ArrFileVO;


/**
 * VO hodnoty atributu - file.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 27.6.16
 */
public class ArrItemFileRefVO extends ArrItemVO {

    /**
     * obal
     */
    private Integer value;

    private ArrFileVO file;

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    public ArrFileVO getFile() {
        return file;
    }

    public void setFile(final ArrFileVO file) {
        this.file = file;
    }
}