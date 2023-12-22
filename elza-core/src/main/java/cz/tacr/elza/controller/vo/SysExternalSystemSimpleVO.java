package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém.
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class SysExternalSystemSimpleVO
        extends BaseCodeVo {

    protected SysExternalSystemSimpleVO(final SysExternalSystem src) {
        setCode(src.getCode());
        setId(src.getExternalSystemId());
        setName(src.getName());
    }
}
