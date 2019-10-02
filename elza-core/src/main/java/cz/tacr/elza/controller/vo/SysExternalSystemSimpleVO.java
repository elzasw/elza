package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * VO pro externí systém.
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class SysExternalSystemSimpleVO
        extends BaseCodeVo {

}
