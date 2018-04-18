package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
public class AbstractApAccessPoint {
}
