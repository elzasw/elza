
package cz.tacr.elza.interpi.ws;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the cz.tacr.elza.interpi.ws package. 
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


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: cz.tacr.elza.interpi.ws
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link AuthUsers }
     * 
     */
    public AuthUsers createAuthUsers() {
        return new AuthUsers();
    }

    /**
     * Create an instance of {@link AuthUsersResponse }
     * 
     */
    public AuthUsersResponse createAuthUsersResponse() {
        return new AuthUsersResponse();
    }

    /**
     * Create an instance of {@link FindData }
     * 
     */
    public FindData createFindData() {
        return new FindData();
    }

    /**
     * Create an instance of {@link FindDataResponse }
     * 
     */
    public FindDataResponse createFindDataResponse() {
        return new FindDataResponse();
    }

    /**
     * Create an instance of {@link GetOneRecord }
     * 
     */
    public GetOneRecord createGetOneRecord() {
        return new GetOneRecord();
    }

    /**
     * Create an instance of {@link GetOneRecordResponse }
     * 
     */
    public GetOneRecordResponse createGetOneRecordResponse() {
        return new GetOneRecordResponse();
    }

    /**
     * Create an instance of {@link WriteOneRecord }
     * 
     */
    public WriteOneRecord createWriteOneRecord() {
        return new WriteOneRecord();
    }

    /**
     * Create an instance of {@link WriteOneRecordResponse }
     * 
     */
    public WriteOneRecordResponse createWriteOneRecordResponse() {
        return new WriteOneRecordResponse();
    }

}
