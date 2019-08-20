
package cz.tacr.elza.daoimport.schema.dao;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the cz.tacr.elza.daoimport.schema.dao package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Dao_QNAME = new QName("http://elza.tacr.cz/xsd/dspace/dao", "dao");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: cz.tacr.elza.daoimport.schema.dao
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Dao }
     * 
     */
    public Dao createDao() {
        return new Dao();
    }

    /**
     * Create an instance of {@link Meta }
     * 
     */
    public Meta createMeta() {
        return new Meta();
    }

    /**
     * Create an instance of {@link Page }
     * 
     */
    public Page createPage() {
        return new Page();
    }

    /**
     * Create an instance of {@link Pages }
     * 
     */
    public Pages createPages() {
        return new Pages();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Dao }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Dao }{@code >}
     */
    @XmlElementDecl(namespace = "http://elza.tacr.cz/xsd/dspace/dao", name = "dao")
    public JAXBElement<Dao> createDao(Dao value) {
        return new JAXBElement<Dao>(_Dao_QNAME, Dao.class, null, value);
    }

}
