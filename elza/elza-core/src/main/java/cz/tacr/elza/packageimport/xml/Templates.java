package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

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
    private List<TemplateXml> templates;

    public List<TemplateXml> getTemplates() {
        return templates;
    }

    public void setTemplates(final List<TemplateXml> templates) {
        this.templates = templates;
    }
}
