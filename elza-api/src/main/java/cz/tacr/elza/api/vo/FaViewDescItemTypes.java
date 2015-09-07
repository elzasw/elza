package cz.tacr.elza.api.vo;

import java.util.List;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 9. 2015
 */
public interface FaViewDescItemTypes<RDIT, RFV> {

    RFV getRulFaView();

    void setRulFaView(RFV rulFaView);

    List<RDIT> getDescItemTypes();

    void setDescItemTypes(List<RDIT> descItemTypes);
}
