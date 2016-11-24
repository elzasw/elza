package org.tempuri;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * This class was generated by Apache CXF 3.0.11
 * 2016-11-23T20:44:57.693+01:00
 * Generated source version: 3.0.11
 * 
 */
@WebService(targetNamespace = "http://tempuri.org", name = "wssoapSoap")
@XmlSeeAlso({ObjectFactory.class})
public interface WssoapSoap {

    @WebMethod(action = "http://tempuri.org/cust.interpi.ws.soap.authUsers")
    @RequestWrapper(localName = "authUsers", targetNamespace = "http://tempuri.org", className = "org.tempuri.AuthUsers")
    @ResponseWrapper(localName = "authUsersResponse", targetNamespace = "http://tempuri.org", className = "org.tempuri.AuthUsersResponse")
    @WebResult(name = "authUsersResult", targetNamespace = "http://tempuri.org")
    public java.lang.String authUsers(
        @WebParam(name = "sAuthUser", targetNamespace = "http://tempuri.org")
        java.lang.String sAuthUser,
        @WebParam(name = "sAuthUserPwd", targetNamespace = "http://tempuri.org")
        java.lang.String sAuthUserPwd,
        @WebParam(name = "sUser", targetNamespace = "http://tempuri.org")
        java.lang.String sUser,
        @WebParam(name = "sPwd", targetNamespace = "http://tempuri.org")
        java.lang.String sPwd
    );

    @WebMethod(action = "http://tempuri.org/cust.interpi.ws.soap.findData")
    @RequestWrapper(localName = "findData", targetNamespace = "http://tempuri.org", className = "org.tempuri.FindData")
    @ResponseWrapper(localName = "findDataResponse", targetNamespace = "http://tempuri.org", className = "org.tempuri.FindDataResponse")
    @WebResult(name = "findDataResult", targetNamespace = "http://tempuri.org")
    public java.lang.String findData(
        @WebParam(name = "sQuery", targetNamespace = "http://tempuri.org")
        java.lang.String sQuery,
        @WebParam(name = "sQuerySort", targetNamespace = "http://tempuri.org")
        java.lang.String sQuerySort,
        @WebParam(name = "sFrom", targetNamespace = "http://tempuri.org")
        java.lang.String sFrom,
        @WebParam(name = "sTo", targetNamespace = "http://tempuri.org")
        java.lang.String sTo,
        @WebParam(name = "sUser", targetNamespace = "http://tempuri.org")
        java.lang.String sUser,
        @WebParam(name = "sPwd", targetNamespace = "http://tempuri.org")
        java.lang.String sPwd
    );

    @WebMethod(action = "http://tempuri.org/cust.interpi.ws.soap.writeOneRecord")
    @RequestWrapper(localName = "writeOneRecord", targetNamespace = "http://tempuri.org", className = "org.tempuri.WriteOneRecord")
    @ResponseWrapper(localName = "writeOneRecordResponse", targetNamespace = "http://tempuri.org", className = "org.tempuri.WriteOneRecordResponse")
    @WebResult(name = "writeOneRecordResult", targetNamespace = "http://tempuri.org")
    public java.lang.String writeOneRecord(
        @WebParam(name = "sT001", targetNamespace = "http://tempuri.org")
        java.lang.String sT001,
        @WebParam(name = "sData", targetNamespace = "http://tempuri.org")
        java.lang.String sData,
        @WebParam(name = "sUser", targetNamespace = "http://tempuri.org")
        java.lang.String sUser,
        @WebParam(name = "sPwd", targetNamespace = "http://tempuri.org")
        java.lang.String sPwd
    );

    @WebMethod(action = "http://tempuri.org/cust.interpi.ws.soap.getOneRecord")
    @RequestWrapper(localName = "getOneRecord", targetNamespace = "http://tempuri.org", className = "org.tempuri.GetOneRecord")
    @ResponseWrapper(localName = "getOneRecordResponse", targetNamespace = "http://tempuri.org", className = "org.tempuri.GetOneRecordResponse")
    @WebResult(name = "getOneRecordResult", targetNamespace = "http://tempuri.org")
    public java.lang.String getOneRecord(
        @WebParam(name = "sT001", targetNamespace = "http://tempuri.org")
        java.lang.String sT001,
        @WebParam(name = "sUser", targetNamespace = "http://tempuri.org")
        java.lang.String sUser,
        @WebParam(name = "sPwd", targetNamespace = "http://tempuri.org")
        java.lang.String sPwd
    );
}
