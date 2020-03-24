package cz.tacr.elza.controller.vo.ap;

import cz.tacr.elza.domain.ApFragment;

import javax.annotation.Nullable;

/**
 * @since 18.07.2018
 */
public class ApFragmentVO {

    /**
     * Identifikátor fragmentu.
     */
    private Integer id;

    /**
     * Hodnota fragmentu.
     */
    private String value;

    /**
     * Stav fragmentu.
     */
    private ApStateVO state;

    /**
     * Identifikátor typu fragmentu.
     */
    private Integer typeId;

    /**
     * Chyby ve fragmentu.
     */
    @Nullable
    private String errorDescription;

    /**
     * Strukturovaná data fragmentu.
     */
    @Nullable
    private ApFormVO form;

    /**
     * Identifikátor nadřazeného fragmentu.
     */
    @Nullable
    private Integer fragmentParentId;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public ApStateVO getState() {
        return state;
    }

    public void setState(final ApStateVO state) {
        this.state = state;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(final Integer typeId) {
        this.typeId = typeId;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(final String errorDescription) {
        this.errorDescription = errorDescription;
    }

    @Nullable
    public ApFormVO getForm() {
        return form;
    }

    public void setForm(@Nullable final ApFormVO form) {
        this.form = form;
    }

    @Nullable
    public Integer getFragmentParentId() {
        return fragmentParentId;
    }

    public void setFragmentParentId(@Nullable Integer fragmentParentId) {
        this.fragmentParentId = fragmentParentId;
    }

    /**
     * Creates value object from AP fragment.
     */
    public static ApFragmentVO newInstance(ApFragment src) {
        ApFragmentVO vo = new ApFragmentVO();
        vo.setId(src.getFragmentId());
        vo.setValue(src.getValue());
        vo.setTypeId(src.getFragmentType().getStructuredTypeId());
        vo.setState(ApStateVO.valueOf(src.getState().name()));
        vo.setErrorDescription(src.getErrorDescription());
        vo.setFragmentParentId(src.getParentFragment() != null ? src.getParentFragment().getFragmentId() : null);
        return vo;
    }
}
