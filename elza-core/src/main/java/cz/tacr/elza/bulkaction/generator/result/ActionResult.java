package cz.tacr.elza.bulkaction.generator.result;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Implementace {@link cz.tacr.elza.api.vo.result.ActionResult}
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ActionResult implements cz.tacr.elza.api.vo.result.ActionResult {



}
