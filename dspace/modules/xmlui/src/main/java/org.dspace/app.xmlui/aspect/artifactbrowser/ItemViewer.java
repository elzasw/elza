/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.sfx.factory.SfxServiceFactory;
import org.dspace.app.sfx.service.SFXFileReaderService;
import org.dspace.app.util.GoogleMetadata;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.content.service.ItemService;

import cz.tacr.elza.metadataconstants.MetadataEnum;
import cz.tacr.elza.ws.WsClient;
import cz.tacr.elza.destructransferrequest.service.ProcessingRequestService;

/**
 * Display a single item.
 *
 * @author Scott Phillips
 */
public class ItemViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.ItemViewer.trail");

    private static final Message T_show_simple =
        message("xmlui.ArtifactBrowser.ItemViewer.show_simple");

    private static final Message T_show_full =
        message("xmlui.ArtifactBrowser.ItemViewer.show_full");

    private static final Message T_head_parent_collections =
        message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");

    private static final Message T_withdrawn = message("xmlui.ArtifactBrowser.ItemViewer.withdrawn");

    private static final Message T_send_to_elza =
            message("xmlui.ArtifactBrowser.ItemViewer.send_item_to_elza");

    private static final Message T_view_in_elza =
            message("xmlui.ArtifactBrowser.ItemViewer.view_did_in_elza");

	/** Cached validity object */
	private SourceValidity validity = null;

	/** XHTML crosswalk instance */
	private DisseminationCrosswalk xHTMLHeadCrosswalk = null;

	private final String sfxFile = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir")
            + File.separator + "config" + File.separator + "sfx.xml";

    private static final Logger log = LoggerFactory.getLogger(ItemViewer.class);

    protected SFXFileReaderService sfxFileReaderService = SfxServiceFactory.getInstance().getSfxFileReaderService();



    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     * @return the key.
     */
    @Override
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
            {
                return "0"; // no item, something is wrong.
            }
            Request request = ObjectModelHelper.getRequest(objectModel);
            String show = request.getParameter("show");
            String sendToElza = request.getParameter("sendToElza");
            String viewInElza = request.getParameter("viewInElza");
            if (sendToElza != null && sendToElza.length() > 0) {
                return HashUtil.hash(dso.getHandle() + "sendToElza:" + sendItemToElza(objectModel));
            } else if (viewInElza != null && viewInElza.length() > 0) {
                return HashUtil.hash(dso.getHandle() + "viewInElza:" + viewItemInElza(objectModel));
            } else {
                return HashUtil.hash(dso.getHandle() + "full:" + showFullItem(objectModel));
            }

        }
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     *
     * The validity object will include the item being viewed,
     * along with all bundles and bitstreams.
     * @return validity.
     */
    @Override
    public SourceValidity getValidity()
    {
        DSpaceObject dso = null;

        if (this.validity == null)
    	{
	        try {
	            dso = HandleUtil.obtainHandle(objectModel);

	            DSpaceValidity newValidity = new DSpaceValidity();
	            newValidity.add(context, dso);
	            this.validity =  newValidity.complete();
	        }
	        catch (Exception e)
	        {
	            // Ignore all errors and just invalidate the cache.
	        }

    	}
    	return this.validity;
    }

    /** Matches Handle System URIs. */
    private static final Pattern handlePattern = Pattern.compile(
            "hdl:|https?://hdl\\.handle\\.net/", Pattern.CASE_INSENSITIVE);

    /** Matches DOI URIs. */
    private static final Pattern doiPattern = Pattern.compile(
            "doi:|https?://(dx\\.)?doi\\.org/", Pattern.CASE_INSENSITIVE);

    /**
     * Add the item's title and trail links to the page's metadata.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.app.xmlui.wing.WingException if a crosswalk fails.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
        {
            return;
        }

        Item item = (Item) dso;

        // Set the page title
        String title = item.getName();

        if (title != null)
        {
            pageMeta.addMetadata("title").addContent(title);
        }
        else
        {
            pageMeta.addMetadata("title").addContent(item.getHandle());
        }

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(context, item,pageMeta,contextPath);
        pageMeta.addTrail().addContent(T_trail);

        // Add SFX link
        String sfxserverUrl = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("sfx.server.url");
        if (sfxserverUrl != null && sfxserverUrl.length() > 0)
        {
            String sfxQuery = "";

            // parse XML file -> XML document will be build
            sfxQuery = sfxFileReaderService.loadSFXFile(sfxFile, item);

            // Remove initial &, if any
            if (sfxQuery.startsWith("&"))
            {
                sfxQuery = sfxQuery.substring(1);
            }
            sfxserverUrl = sfxserverUrl.trim() +"&" + sfxQuery.trim();
            pageMeta.addMetadata("sfx","server").addContent(sfxserverUrl);
        }
        
        // Add persistent identifiers
        /* Temporarily switch to using metadata directly.
         * FIXME Proper fix is to have IdentifierService handle all durable
         * identifiers, whether minted here or elsewhere.
        List<IdentifierProvider> idPs = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServicesByType(IdentifierProvider.class);
        for (IdentifierProvider idP : idPs)
        {
            log.debug("Looking up Item {} by IdentifierProvider {}",
                    dso.getID(), idP.getClass().getName());
            try {
                String id = idP.lookup(context, dso);
                log.debug("Found identifier {}", id);
                String idType;
                String providerName = idP.getClass().getSimpleName().toLowerCase();
                if (providerName.contains("handle"))
                    idType = "handle";
                else if (providerName.contains("doi"))
                    idType = "doi";
                else
                {
                    log.info("Unhandled provider {}", idP.getClass().getName());
                    continue;
                }
                log.debug("Adding identifier of type {}", idType);
                Metadata md = pageMeta.addMetadata("identifier", idType);
                md.addContent(id);
            } catch (IdentifierNotFoundException | IdentifierNotResolvableException ex) {
                continue;
            }
        }
        */
        String identifierField = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getPropertyAsType("altmetrics.field", "dc.identifier.uri");
        for (MetadataValue uri : ContentServiceFactory.getInstance().getDSpaceObjectService(dso).getMetadataByMetadataString(dso, identifierField))
        {
            String idType, idValue;
            Matcher handleMatcher = handlePattern.matcher(uri.getValue());
            Matcher doiMatcher = doiPattern.matcher(uri.getValue());
            if (handleMatcher.lookingAt())
            {
                idType = "handle";
                idValue = uri.getValue().substring(handleMatcher.end());
            }
            else if (doiMatcher.lookingAt())
            {
                idType = "doi";
                idValue = uri.getValue().substring(doiMatcher.end());
            }
            else
            {
                log.info("Unhandled identifier URI {}", uri.getValue());
                continue;
            }
            log.debug("Adding identifier of type {}", idType);
            Metadata md = pageMeta.addMetadata("identifier", idType);
            md.addContent(idValue);
        }

        String sfxserverImg = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("sfx.server.image_url");
        if (sfxserverImg != null && sfxserverImg.length() > 0)
        {
            pageMeta.addMetadata("sfx","image_url").addContent(sfxserverImg);
        }

        boolean googleEnabled = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty(
            "google-metadata.enable", false);

        if (googleEnabled)
        {
            // Add Google metadata field names & values to DRI
            GoogleMetadata gmd = new GoogleMetadata(context, item);

            for (Entry<String, String> m : gmd.getMappings())
            {
                pageMeta.addMetadata(m.getKey()).addContent(m.getValue());
            }
        }

        // Metadata for <head> element
        if (xHTMLHeadCrosswalk == null)
        {
            xHTMLHeadCrosswalk = (DisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(
              DisseminationCrosswalk.class, "XHTML_HEAD_ITEM");
        }

        // Produce <meta> elements for header from crosswalk
        try
        {
            List l = xHTMLHeadCrosswalk.disseminateList(context, item);
            StringWriter sw = new StringWriter();

            XMLOutputter xmlo = new XMLOutputter();
            xmlo.output(new Text("\n"), sw);
            for (int i = 0; i < l.size(); i++)
            {
                Element e = (Element) l.get(i);
                // FIXME: we unset the Namespace so it's not printed.
                // This is fairly yucky, but means the same crosswalk should
                // work for Manakin as well as the JSP-based UI.
                e.setNamespace(null);
                xmlo.output(e, sw);
                xmlo.output(new Text("\n"), sw);
            }
            pageMeta.addMetadata("xhtml_head_item").addContent(sw.toString());
        }
        catch (CrosswalkException ce)
        {
            // TODO: Is this the right exception class?
            throw new WingException(ce);
        }
    }

    /**
     * Display a single item
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
        {
            return;
        }

        Item item = (Item) dso;

        // Build the item viewer division.
        Division division = body.addDivision("item-view","primary");
        String title = item.getName();
        if (title != null)
        {
            division.setHead(title);
        }
        else
        {
            division.setHead(item.getHandle());
        }

        // Add Withdrawn Message if it is
        if(item.isWithdrawn()){
            Division div = division.addDivision("notice", "notice");
            Para p = div.addPara();
            p.addContent(T_withdrawn);
            //Set proper response. Return "404 Not Found"
            HttpServletResponse response = (HttpServletResponse)objectModel
                    .get(HttpEnvironment.HTTP_RESPONSE_OBJECT);   
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Para showfullPara = division.addPara(null, "item-view-toggle item-view-toggle-top");

        if (showFullItem(objectModel))
        {
            String link = contextPath + "/handle/" + item.getHandle();
            showfullPara.addXref(link).addContent(T_show_simple);
        }
        else
        {
            String link = contextPath + "/handle/" + item.getHandle()
                    + "?show=full";
            showfullPara.addXref(link).addContent(T_show_full);
        }

        ReferenceSet referenceSet;
        if (showFullItem(objectModel))
        {
            referenceSet = division.addReferenceSet("collection-viewer",
                    ReferenceSet.TYPE_DETAIL_VIEW);
        }
        else
        {
            referenceSet = division.addReferenceSet("collection-viewer",
                    ReferenceSet.TYPE_SUMMARY_VIEW);
        }

        // Reference the actual Item
        ReferenceSet appearsInclude = referenceSet.addReference(item).addReferenceSet(ReferenceSet.TYPE_DETAIL_LIST,null,"hierarchy");
        appearsInclude.setHead(T_head_parent_collections);

        // Reference all collections the item appears in.
        for (Collection collection : item.getCollections())
        {
            appearsInclude.addReference(collection);
        }

        showfullPara = division.addPara(null,"item-view-toggle item-view-toggle-bottom");

        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        MetadataEnum metaData = MetadataEnum.ISELZA;
        List<MetadataValue> metadata = itemService.getMetadata(item, metaData.getSchema(), metaData.getElement(), metaData.getQualifier(), Item.ANY);

        boolean isElza = false;
        if (!metadata.isEmpty()) {
            MetadataValue mv = metadata.iterator().next();
            String value = mv.getValue();
            isElza = Boolean.valueOf(value);
        }

//        if (!isElza) {
            Para sendToElzaPara = division.addPara(null, "item-view-send-to-elza");

            String linkToElza = contextPath + "/handle/" + item.getHandle()
                    + "?sendToElza=true" + "?idemId=" + item.getID();

            sendToElzaPara.addXref(linkToElza).addContent(T_send_to_elza);
//        }

        metaData = MetadataEnum.ELZADIDID;
        metadata = itemService.getMetadata(item, metaData.getSchema(), metaData.getElement(), metaData.getQualifier(), Item.ANY);
        String did = null;
        if (!metadata.isEmpty()) {
            MetadataValue mv = metadata.iterator().next();
            did = mv.getValue();
        }
        
        if (did != null) {
            Para viewInElzaPara = division.addPara(null, "did-view-in-elsa");

            // TODO upravit formát podle skutečné adresy v ELZA
            String baseUrl = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("elza.base.url");
            baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            String viewInElzaLink = baseUrl + "node/" + did; // poslat didId

            viewInElzaPara.addXref(viewInElzaLink, T_view_in_elza);
        }
    }

    /**
     * Determine if the full item should be referenced or just a summary.
     * @param objectModel to get the request.
     * @return true if the full item should be shown.
     */
    public static boolean showFullItem(Map objectModel)
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String show = request.getParameter("show");

        return show != null && show.length() > 0;
    }


    /**
     * send item to elza in package
     * @param objectModel to get the request.|
     */
    public boolean sendItemToElza(Map objectModel)
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String show = request.getParameter("sendToElza");

        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (!(dso instanceof Item))
            {
               return false;
            }

            Item item = (Item) dso;
            WsClient.sendItemToElza(item);

        } catch (SQLException e) {
            // Ignore all errors and just return that the component is not cachable.
            return false;
        }


        return show != null && show.length() > 0;
    }

    /**
     * view item in elza
     * @param objectModel to get the request.
     */
    public boolean viewItemInElza(Map objectModel)
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String show = request.getParameter("viewInElza");

        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (!(dso instanceof Item))
            {
               return false;
            }

            Item item = (Item) dso;
            WsClient.sendItemToElza(item);

        } catch (SQLException e) {
            // Ignore all errors and just return that the component is not cachable.
            return false;
        }


        return show != null && show.length() > 0;
    }

    @Override
    public void recycle() {
    	this.validity = null;
    	super.recycle();
    }
}
