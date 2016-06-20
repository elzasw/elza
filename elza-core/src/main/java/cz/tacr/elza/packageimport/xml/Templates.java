package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * VO Templates.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "templates")
@XmlType(name = "templates")
public class Templates {

    @XmlElement(name = "template", required = true)
    private List<Template> templates;

    public List<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(final List<Template> templates) {
        this.templates = templates;
    }
}
