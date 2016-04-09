/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.oai.service;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static java.util.Collections.emptyMap;
import static org.fcrepo.kernel.impl.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.fcrepo.http.api.FedoraLdp;
import org.fcrepo.http.api.FedoraNodes;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.FedoraJcrTypes;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.models.Container;
import org.fcrepo.kernel.models.FedoraBinary;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.services.BinaryService;
import org.fcrepo.kernel.services.ContainerService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.oai.generator.JcrOaiDcGenerator;
import org.fcrepo.oai.generator.JcrOaiEtdmsGenerator;
import org.fcrepo.oai.http.ResumptionToken;
import org.fcrepo.oai.jersey.XmlDeclarationStrippingInputStream;
import org.fcrepo.oai.rdf.PropertyPredicate;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.query.QueryResult;
import org.ndltd.standards.metadata.etdms._1.Thesis;
import org.openarchives.oai._2.DeletedRecordType;
import org.openarchives.oai._2.DescriptionType;
import org.openarchives.oai._2.GetRecordType;
import org.openarchives.oai._2.GranularityType;
import org.openarchives.oai._2.HeaderType;
import org.openarchives.oai._2.IdentifyType;
import org.openarchives.oai._2.ListIdentifiersType;
import org.openarchives.oai._2.ListMetadataFormatsType;
import org.openarchives.oai._2.ListRecordsType;
import org.openarchives.oai._2.ListSetsType;
import org.openarchives.oai._2.MetadataFormatType;
import org.openarchives.oai._2.MetadataType;
import org.openarchives.oai._2.OAIPMHerrorType;
import org.openarchives.oai._2.OAIPMHerrorcodeType;
import org.openarchives.oai._2.OAIPMHtype;
import org.openarchives.oai._2.ObjectFactory;
import org.openarchives.oai._2.RecordType;
import org.openarchives.oai._2.RequestType;
import org.openarchives.oai._2.ResumptionTokenType;
import org.openarchives.oai._2.SetType;
import org.openarchives.oai._2.VerbType;
import org.openarchives.oai._2_0.oai_dc.OaiDcType;
import org.openarchives.oai._2_0.oai_identifier.OaiIdentifierType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * The type OAI provider service.
 *
 * @author lsitu
 * @author Frank Asseg
 * @author Piyapong Charoenwattana
 */
public class OAIProviderService {

    private static final Logger log = LoggerFactory.getLogger(OAIProviderService.class);

    private static final ObjectFactory oaiFactory = new ObjectFactory();

    private static final org.openarchives.oai._2_0.oai_identifier.ObjectFactory idFactory =
        new org.openarchives.oai._2_0.oai_identifier.ObjectFactory();

    private final DatatypeFactory dataFactory;

    private String rootPath;

    private String propertyHasModel;

    private String propertyHasCollectionId;

    private String propertyOaiRepositoryName;

    private String propertyOaiDescription;

    private String propertyOaiAdminEmail;

    private String propertyOaiBaseUrl;

    private Map<String, String> namespaces;

    private boolean setsEnabled;

    private boolean searchEnabled;

    private Map<String, String> descriptiveContent;

    private Map<String, MetadataFormat> metadataFormats;

    private final DateTimeFormatter dateFormat = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC);

    private int maxListSize;

    private String baseUrl;

    private String scheme;

    private String repositoryIdentifier;

    private String delimiter;

    private String sampleIdentifier;

    private String idFormat;

    // public item, webacl:agent "http://projecthydra.org/ns/auth/group#public^^URI"
    private final String publicAgent =
        new String(Base64.decodeBase64("aHR0cDovL3Byb2plY3RoeWRyYS5vcmcvbnMvYXV0aC9ncm91cCNwdWJsaWMYXl4YVVJJ"));

    // public collection, ualindentifier:is_official "true^^http://www.w3.org/2001/XMLSchema#boolean"
    private final String booleanTrue =
        new String(Base64.decodeBase64("dHJ1ZRheXhhodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSNib29sZWFu"));

    private static final Pattern slashPattern = Pattern.compile("\\/");

    private static final Pattern pathPattern = Pattern.compile("(?<=\\G..)");

    @Autowired
    private BinaryService binaryService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ContainerService containerService;

    @Autowired
    private JcrOaiDcGenerator jcrOaiDcGenerator;

    @Autowired
    private JcrOaiEtdmsGenerator jcrOaiEtdmsGenerator;

    /**
     * Sets property has set spec.
     *
     * @param propertyHasSetSpec the property has set spec
     */
    public void setPropertyHasSetSpec(final String propertyHasSetSpec) {
    }

    /**
     * Sets property set name.
     *
     * @param propertySetName the property set name
     */
    public void setPropertySetName(final String propertySetName) {
    }

    /**
     * Sets property has sets.
     *
     * @param propertyHasSets the property has sets
     */
    public void setPropertyHasSets(final String propertyHasSets) {
    }

    /**
     * The setPropertyHasModel setter method.
     * 
     * @param propertyHasModel the propertyHasModel to set
     */
    public void setPropertyHasModel(final String propertyHasModel) {
        this.propertyHasModel = propertyHasModel;
    }

    /**
     * The setIdFormat setter method.
     * 
     * @param idFormat the idFormat to set
     */
    public final void setIdFormat(final String idFormat) {
        this.idFormat = idFormat;
    }

    /**
     * Sets max list size.
     *
     * @param maxListSize the max list size
     */
    public void setMaxListSize(final int maxListSize) {
        this.maxListSize = maxListSize;
    }

    /**
     * Sets property is part of set.
     *
     * @param propertyHasCollectionId the property has colletion id
     */
    public void setPropertyHasCollectionId(final String propertyHasCollectionId) {
        this.propertyHasCollectionId = propertyHasCollectionId;
    }

    /**
     * Set propertyOaiRepositoryName
     *
     * @param propertyOaiRepositoryName the oai repository name
     */
    public void setPropertyOaiRepositoryName(final String propertyOaiRepositoryName) {
        this.propertyOaiRepositoryName = propertyOaiRepositoryName;
    }

    /**
     * Set propertyOaiDescription
     *
     * @param propertyOaiDescription the oai description
     */
    public void setPropertyOaiDescription(final String propertyOaiDescription) {
        this.propertyOaiDescription = propertyOaiDescription;
    }

    /**
     * Set propertyOaiAdminEmail
     *
     * @param propertyOaiAdminEmail the oai admin email
     */
    public void setPropertyOaiAdminEmail(final String propertyOaiAdminEmail) {
        this.propertyOaiAdminEmail = propertyOaiAdminEmail;
    }

    /**
     * The setNamespaces setter method.
     *
     * @param namespaces the namespaces to set
     */
    public void setNamespaces(final Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    /**
     * Sets sets root path.
     *
     * @param rootPath the oai root path
     */
    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Sets sets enabled.
     *
     * @param setsEnabled the sets enabled
     */
    public void setSetsEnabled(final boolean setsEnabled) {
        this.setsEnabled = setsEnabled;
    }

    /**
     * Sets metadata formats.
     *
     * @param metadataFormats the metadata formats
     */
    public void setMetadataFormats(final Map<String, MetadataFormat> metadataFormats) {
        this.metadataFormats = metadataFormats;
    }

    /**
     * Sets descriptive content.
     *
     * @param descriptiveContent the descriptive content
     */
    public void setDescriptiveContent(final Map<String, String> descriptiveContent) {
        this.descriptiveContent = descriptiveContent;
    }

    /**
     * The setPropertyOaiBaseUrl setter method.
     * 
     * @param propertyOaiBaseUrl the propertyOaiBaseUrl to set
     */
    public void setPropertyOaiBaseUrl(final String propertyOaiBaseUrl) {
        this.propertyOaiBaseUrl = propertyOaiBaseUrl;
    }

    /**
     * The setSearchEnabled setter method.
     * 
     * @param searchEnabled the searchEnabled to set
     */
    public void setSearchEnabled(final boolean searchEnabled) {
        this.searchEnabled = searchEnabled;
    }

    /**
     * Service intitialization
     *
     * @throws RepositoryException the repository exception
     */
    @PostConstruct
    public void init() throws RepositoryException {
        /* check if set root node exists */
        final Session session = sessionFactory.getInternalSession();

        final NamespaceRegistry namespaceRegistry =
            (org.modeshape.jcr.api.NamespaceRegistry) session.getWorkspace().getNamespaceRegistry();

        // Register namespaces
        log.info("Registering namespaces...");
        final Set<Entry<String, String>> entries = namespaces.entrySet();
        for (final Entry<String, String> entry : entries) {
            if (namespaceRegistry.isRegisteredPrefix(entry.getKey())) {
                namespaceRegistry.unregisterNamespace(entry.getKey());
            }
            namespaceRegistry.registerNamespace(entry.getKey(), entry.getValue());
        }

        final Container root;
        if (!nodeService.exists(session, rootPath)) {
            log.info("Initializing OAI root {} ...", rootPath);
            root = containerService.findOrCreate(session, rootPath);
        } else {
            log.info("Updating OAI root {} ...", rootPath);
            root = containerService.findOrCreate(session, rootPath);
        }

        final String repositoryName = descriptiveContent.get("repositoryName");
        final String description = descriptiveContent.get("description");
        final String adminEmail = descriptiveContent.get("adminEmail");
        baseUrl = descriptiveContent.get("baseUrl");
        scheme = descriptiveContent.get("scheme");
        repositoryIdentifier = descriptiveContent.get("repositoryIdentifier");
        delimiter = descriptiveContent.get("delimiter");
        sampleIdentifier = descriptiveContent.get("sampleIdentifier");

        // save data in oai node
        root.getNode().setProperty(getPropertyName(session, createProperty(propertyOaiRepositoryName)), repositoryName);
        root.getNode().setProperty(getPropertyName(session, createProperty(propertyOaiDescription)), description);
        root.getNode().setProperty(getPropertyName(session, createProperty(propertyOaiAdminEmail)), adminEmail);
        root.getNode().setProperty(getPropertyName(session, createProperty(propertyOaiBaseUrl)), baseUrl);
        session.save();

        // rootPath.replaceAll(".*/", "");
    }

    /**
     * Instantiates a new OAI provider service.
     *
     * @throws DatatypeConfigurationException the datatype configuration exception
     * @throws JAXBException the jAXB exception
     */
    public OAIProviderService() throws DatatypeConfigurationException, JAXBException {
        dataFactory = DatatypeFactory.newInstance();
        final JAXBContext ctx = JAXBContext.newInstance(OAIPMHtype.class, IdentifyType.class, SetType.class);
        ctx.createUnmarshaller();
    }

    /**
     * Identify jAXB element.
     *
     * @param session the session
     * @param uriInfo the uri info
     * @return the jAXB element
     * @throws RepositoryException the repository exception
     * @throws JAXBException the jAXB exception
     */
    public JAXBElement<OAIPMHtype> identify(final Session session, final UriInfo uriInfo)
        throws RepositoryException, JAXBException {
        final HttpResourceConverter converter =
            new HttpResourceConverter(session, uriInfo.getBaseUriBuilder().clone().path(FedoraNodes.class));

        final FedoraResource root = nodeService.find(session, rootPath);

        final IdentifyType id = oaiFactory.createIdentifyType();
        id.setEarliestDatestamp(dateFormat.print(root.getCreatedDate().getTime()));
        id.setProtocolVersion("2.0");

        // repository name, project version
        RdfStream triples =
            root.getTriples(converter, PropertiesRdfContext.class)
                .filter(new PropertyPredicate(propertyOaiRepositoryName));
        id.setRepositoryName(triples.next().getObject().getLiteralValue().toString());

        // base url
        triples =
            root.getTriples(converter, PropertiesRdfContext.class).filter(new PropertyPredicate(propertyOaiBaseUrl));
        final String baseUrl = triples.next().getObject().getLiteralValue().toString();
        id.setBaseURL(baseUrl);

        // admin email
        triples =
            root.getTriples(converter, PropertiesRdfContext.class).filter(new PropertyPredicate(propertyOaiAdminEmail));
        id.getAdminEmail().add(0, triples.next().getObject().getLiteralValue().toString());

        // granularity
        id.setGranularity(GranularityType.YYYY_MM_DD_THH_MM_SS_Z);

        // deleteRecord
        id.setDeletedRecord(DeletedRecordType.NO);

        // oai-identifier description
        final OaiIdentifierType oaiIdType = idFactory.createOaiIdentifierType();
        oaiIdType.setScheme(scheme);
        oaiIdType.setRepositoryIdentifier(repositoryIdentifier);
        oaiIdType.setDelimiter(delimiter);
        oaiIdType.setSampleIdentifier(sampleIdentifier);
        final DescriptionType desc = oaiFactory.createDescriptionType();
        desc.setAny(idFactory.createOaiIdentifier(oaiIdType));
        id.getDescription().add(desc);

        // request header
        final RequestType req = oaiFactory.createRequestType();
        req.setVerb(VerbType.IDENTIFY);
        req.setValue(uriInfo.getRequestUri().toASCIIString());

        final OAIPMHtype oai = oaiFactory.createOAIPMHtype();
        oai.setIdentify(id);
        oai.setResponseDate(dataFactory.newXMLGregorianCalendar(dateFormat.print(new Date().getTime())));
        oai.setRequest(req);
        return oaiFactory.createOAIPMH(oai);
    }

    /**
     * List metadata formats.
     *
     * @param session the session
     * @param uriInfo the uri info
     * @param identifier the identifier
     * @return the jAXB element
     * @throws RepositoryException the repository exception
     */
    public JAXBElement<OAIPMHtype> listMetadataFormats(final Session session, final UriInfo uriInfo,
        final String identifier) throws RepositoryException {

        final ListMetadataFormatsType listMetadataFormats = oaiFactory.createListMetadataFormatsType();
        final HttpResourceConverter converter =
            new HttpResourceConverter(session, uriInfo.getBaseUriBuilder().clone().path(FedoraNodes.class));

        /* check which formats are available on top of oai_dc for this object */
        if (identifier != null && !identifier.isEmpty()) {
            final String path = identifier.substring(this.baseUrl.length());
            if (path != null && !path.isEmpty()) {
                /* generate metadata format response for a single pid */
                if (!nodeService.exists(session, path)) {
                    return error(VerbType.LIST_METADATA_FORMATS, identifier, null,
                        OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "The object does not exist");
                }
                final Container obj = containerService.find(session, path);
                for (final MetadataFormat mdf : metadataFormats.values()) {
                    if (mdf.getPrefix().equals("oai_dc")) {
                        listMetadataFormats.getMetadataFormat().add(mdf.asMetadataFormatType());
                    } else if (mdf.getPrefix().equals("oai_etdms")) {
                        listMetadataFormats.getMetadataFormat().add(mdf.asMetadataFormatType());
                    } else {
                        final RdfStream triples =
                            obj.getTriples(converter, PropertiesRdfContext.class)
                                .filter(new PropertyPredicate(mdf.getPropertyName()));
                        if (triples.hasNext()) {
                            listMetadataFormats.getMetadataFormat().add(mdf.asMetadataFormatType());
                        }
                    }
                }
            }
        } else {
            /* generate a general metadata format response */
            listMetadataFormats.getMetadataFormat().addAll(listAvailableMetadataFormats());
        }

        final RequestType req = oaiFactory.createRequestType();
        req.setVerb(VerbType.LIST_METADATA_FORMATS);
        req.setValue(uriInfo.getRequestUri().toASCIIString());

        final OAIPMHtype oai = oaiFactory.createOAIPMHtype();
        oai.setResponseDate(dataFactory.newXMLGregorianCalendar(dateFormat.print(new Date().getTime())));
        oai.setListMetadataFormats(listMetadataFormats);
        oai.setRequest(req);
        return oaiFactory.createOAIPMH(oai);
    }

    private List<MetadataFormatType> listAvailableMetadataFormats() {
        final List<MetadataFormatType> types = new ArrayList<>(metadataFormats.size());
        for (final MetadataFormat mdf : metadataFormats.values()) {
            final MetadataFormatType mdft = oaiFactory.createMetadataFormatType();
            mdft.setMetadataPrefix(mdf.getPrefix());
            mdft.setMetadataNamespace(mdf.getNamespace());
            mdft.setSchema(mdf.getSchemaUrl());
            types.add(mdft);
        }
        return types;
    }

    /**
     * Gets record.
     *
     * @param session the session
     * @param uriInfo the uri info
     * @param identifier the identifier
     * @param metadataPrefix the metadata prefix
     * @return the record
     * @throws RepositoryException the repository exception
     */
    public JAXBElement<OAIPMHtype> getRecord(final Session session, final UriInfo uriInfo, final String identifier,
        final String metadataPrefix) throws RepositoryException {
        final MetadataFormat format = metadataFormats.get(metadataPrefix);
        if (format == null) {
            return error(VerbType.GET_RECORD, identifier, metadataPrefix, OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT,
                "The metadata format is not available");
        }

        final String noid;
        try {
            noid = slashPattern.split(identifier)[2].substring(2);
        } catch (final Exception e) {
            return error(VerbType.GET_RECORD, identifier, metadataPrefix, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST,
                "The requested identifier does not exist");
        }
        final StringBuilder jql = new StringBuilder();
        jql.append("SELECT res.[jcr:path] AS path FROM [fedora:Resource] AS res");
        jql.append(" JOIN [fedora:Resource] AS per ON res.[jcr:uuid] = per.[webacl:accessTo_ref] ");
        jql.append("WHERE res.[mode:localName] = '").append(noid).append("'");
        jql.append(" AND per.[model:hasModel] = 'Hydra::AccessControls::Permission'");
        jql.append(" AND per.[webacl:agent] = CAST('" + publicAgent + "' AS BINARY)");

        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final RowIterator result = executeQuery(queryManager, jql.toString());
        if (!result.hasNext()) {
            return error(VerbType.GET_RECORD, identifier, metadataPrefix, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST,
                "The requested identifier does not exist");
        }
        final String path = result.nextRow().getValue("path").getString();
        final Container obj = containerService.find(session, path);

        final OAIPMHtype oai = oaiFactory.createOAIPMHtype();
        oai.setResponseDate(dataFactory.newXMLGregorianCalendar(dateFormat.print(new Date().getTime())));
        final RequestType req = oaiFactory.createRequestType();
        req.setVerb(VerbType.GET_RECORD);
        req.setValue(uriInfo.getRequestUri().toASCIIString());
        req.setMetadataPrefix(metadataPrefix);
        oai.setRequest(req);

        final GetRecordType getRecord = oaiFactory.createGetRecordType();
        final RecordType record;
        try {
            record = createRecord(session, format, obj.getPath(), noid, uriInfo);
            getRecord.setRecord(record);
            oai.setGetRecord(getRecord);
            return oaiFactory.createOAIPMH(oai);
        } catch (final IOException e) {
            log.error("Unable to create OAI record for object " + obj.getPath());
            return error(VerbType.GET_RECORD, identifier, metadataPrefix, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST,
                "The requested OAI record does not exist for object " + obj.getPath());
        }
    }

    private JAXBElement<OaiDcType> generateOaiDc(final Session session, final Container obj, final String name,
        final UriInfo uriInfo) throws RepositoryException {
        return jcrOaiDcGenerator.generate(session, obj, name, uriInfo);
    }

    private Thesis generateOaiEtdms(final Session session, final Container obj, final String name,
        final UriInfo uriInfo) throws RepositoryException {
        return jcrOaiEtdmsGenerator.generate(session, obj, name, uriInfo);
    }

    private JAXBElement<String> fetchOaiResponse(final Container obj, final Session session,
        final MetadataFormat format, final UriInfo uriInfo) throws RepositoryException, IOException {

        final HttpResourceConverter converter =
            new HttpResourceConverter(session, uriInfo.getBaseUriBuilder().clone().path(FedoraNodes.class));
        final RdfStream triples =
            obj.getTriples(converter, PropertiesRdfContext.class)
                .filter(new PropertyPredicate(format.getPropertyName()));

        if (!triples.hasNext()) {
            log.error("There is no OAI record of type " + format.getPrefix() + " associated with the object "
                + obj.getPath());
            return null;
        }

        final String recordPath = triples.next().getObject().getLiteralValue().toString();
        final FedoraBinary bin = binaryService.findOrCreate(session, "/" + recordPath);

        try (final InputStream src = new XmlDeclarationStrippingInputStream(bin.getContent())) {
            return new JAXBElement<String>(new QName(format.getPrefix()), String.class, IOUtils.toString(src));
        }
    }

    /**
     * Creates a OAI error response for JAX-B
     *
     * @param verb the verb
     * @param identifier the identifier
     * @param metadataPrefix the metadata prefix
     * @param errorCode the error code
     * @param msg the msg
     * @return the jAXB element
     */
    public static JAXBElement<OAIPMHtype> error(final VerbType verb, final String identifier,
        final String metadataPrefix, final OAIPMHerrorcodeType errorCode, final String msg) {
        final OAIPMHtype oai = oaiFactory.createOAIPMHtype();
        final RequestType req = oaiFactory.createRequestType();
        req.setVerb(verb);
        req.setIdentifier(identifier);
        req.setMetadataPrefix(metadataPrefix);
        oai.setRequest(req);

        final OAIPMHerrorType error = oaiFactory.createOAIPMHerrorType();
        error.setCode(errorCode);
        error.setValue(msg);
        oai.getError().add(error);
        return oaiFactory.createOAIPMH(oai);
    }

    /**
     * List identifiers.
     *
     * @param session the session
     * @param uriInfo the uri info
     * @param metadataPrefix the metadata prefix
     * @param from the from
     * @param until the until
     * @param set the set
     * @param offset the offset
     * @return the jAXB element
     * @throws RepositoryException the repository exception
     */
    public JAXBElement<OAIPMHtype> listIdentifiers(final Session session, final UriInfo uriInfo,
        final String metadataPrefix, final String from, final String until, final String set, final int offset)
        throws RepositoryException {

        if (metadataPrefix == null) {
            return error(VerbType.LIST_IDENTIFIERS, null, null, OAIPMHerrorcodeType.BAD_ARGUMENT,
                "metadataprefix is invalid");
        }

        final MetadataFormat mdf = metadataFormats.get(metadataPrefix);
        if (mdf == null) {
            return error(VerbType.LIST_IDENTIFIERS, null, metadataPrefix, OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT,
                "Unavailable metadata format");
        }

        if (StringUtils.isNotBlank(set) && !setsEnabled) {
            return error(VerbType.LIST_RECORDS, null, metadataPrefix, OAIPMHerrorcodeType.NO_SET_HIERARCHY,
                "Sets are not enabled");
        }

        // dateTime format validation
        try {
            validateDateTimeFormat(from);
            validateDateTimeFormat(until);
        } catch (final IllegalArgumentException e) {
            return error(VerbType.LIST_IDENTIFIERS, null, metadataPrefix, OAIPMHerrorcodeType.BAD_ARGUMENT,
                e.getMessage());
        }

        final HttpResourceConverter converter =
            new HttpResourceConverter(session, uriInfo.getBaseUriBuilder().clone().path(FedoraNodes.class));
        final ValueConverter valueConverter = new ValueConverter(session, converter);

        final String jql =
            listResourceQuery(session, FedoraJcrTypes.FEDORA_CONTAINER, metadataPrefix, from, until, set, maxListSize,
                offset);
        try {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final RowIterator result = executeQuery(queryManager, jql);

            if (!result.hasNext()) {
                return error(VerbType.LIST_IDENTIFIERS, null, metadataPrefix, OAIPMHerrorcodeType.NO_RECORDS_MATCH,
                    "No record found");
            }

            final OAIPMHtype oai = oaiFactory.createOAIPMHtype();
            oai.setResponseDate(dataFactory.newXMLGregorianCalendar(dateFormat.print(new Date().getTime())));
            final ListIdentifiersType ids = oaiFactory.createListIdentifiersType();

            while (result.hasNext()) {
                // fix JCR-SQL2 LIMIT bug in 4.2.0
                if (result.getPosition() < maxListSize) {
                    final HeaderType h = oaiFactory.createHeaderType();
                    final Row sol = result.nextRow();
                    final Resource sub = valueConverter.convert(sol.getValue("sub")).asResource();
                    final String path = converter.convert(sub).getPath();

                    // get base url
                    final FedoraResource root = nodeService.find(session, rootPath);
                    RdfStream triples =
                        root.getTriples(converter, PropertiesRdfContext.class)
                            .filter(new PropertyPredicate(propertyOaiBaseUrl));
                    h.setIdentifier(createId(converter.asString(sub)));

                    final Container obj = containerService.find(session, path);
                    h.setDatestamp(dateFormat.print(obj.getLastModifiedDate().getTime()));
                    triples =
                        obj.getTriples(converter, PropertiesRdfContext.class)
                            .filter(new PropertyPredicate(propertyHasCollectionId));
                    while (triples.hasNext()) {
                        h.getSetSpec().add(triples.next().getObject().getLiteralValue().toString());
                    }
                    ids.getHeader().add(h);
                } else {
                    break;
                }
            }

            final RequestType req = oaiFactory.createRequestType();
            if (ids.getHeader().size() == maxListSize) {
                final ResumptionTokenType token = oaiFactory.createResumptionTokenType();
                token.setValue(encodeResumptionToken(VerbType.LIST_IDENTIFIERS.value(), metadataPrefix, from, until,
                    set, offset + maxListSize));
                token.setCursor(new BigInteger(String.valueOf(offset)));
                token.setCompleteListSize(new BigInteger(String.valueOf(result.getSize())));
                ids.setResumptionToken(token);
            }
            req.setVerb(VerbType.LIST_IDENTIFIERS);
            req.setMetadataPrefix(metadataPrefix);
            req.setFrom(StringUtils.isEmpty(from) ? null : from);
            req.setUntil(StringUtils.isEmpty(until) ? null : until);
            req.setSet(StringUtils.isEmpty(set) ? null : set);
            oai.setRequest(req);
            oai.setListIdentifiers(ids);
            return oaiFactory.createOAIPMH(oai);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        }
    }

    /**
     * Encode resumption token.
     *
     * @param verb the verb
     * @param metadataPrefix the metadata prefix
     * @param from the from
     * @param until the until
     * @param set the set
     * @param offset the offset
     * @return the string
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static String encodeResumptionToken(final String verb, final String metadataPrefix, final String from,
        final String until, final String set, final int offset) throws UnsupportedEncodingException {

        final String[] data =
            new String[] { urlEncode(verb), urlEncode(metadataPrefix), urlEncode(from != null ? from : ""),
                urlEncode(until != null ? until : ""), urlEncode(set != null ? set : ""),
                urlEncode(String.valueOf(offset)) };
        return Base64.encodeBase64URLSafeString(StringUtils.join(data, ':').getBytes("UTF-8"));
    }

    /**
     * Url encode.
     *
     * @param value the value
     * @return the string
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static String urlEncode(final String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }

    /**
     * Url decode.
     *
     * @param value the value
     * @return the string
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static String urlDecode(final String value) throws UnsupportedEncodingException {
        return URLDecoder.decode(value, "UTF-8");
    }

    /**
     * Decode resumption token.
     *
     * @param token the token
     * @return the resumption token
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static ResumptionToken decodeResumptionToken(final String token) throws UnsupportedEncodingException {
        final String[] data = StringUtils.splitPreserveAllTokens(new String(Base64.decodeBase64(token)), ':');
        final String verb = urlDecode(data[0]);
        final String metadataPrefix = urlDecode(data[1]);
        final String from = urlDecode(data[2]);
        final String until = urlDecode(data[3]);
        final String set = urlDecode(data[4]);
        final int offset = Integer.parseInt(urlDecode(data[5]));
        return new ResumptionToken(verb, metadataPrefix, from, until, offset, set);
    }

    /**
     * List sets.
     *
     * @param session the session
     * @param uriInfo the uri info
     * @param offset the offset
     * @return the jAXB element
     * @throws RepositoryException the repository exception
     */
    public JAXBElement<OAIPMHtype> listSets(final Session session, final UriInfo uriInfo, final int offset)
        throws RepositoryException {

        // TODO: support offset
        final HttpResourceConverter converter =
            new HttpResourceConverter(session, uriInfo.getBaseUriBuilder().clone().path(FedoraLdp.class));
        final ValueConverter valueConverter = new ValueConverter(session, converter);

        try {
            if (!setsEnabled) {
                return error(VerbType.LIST_SETS, null, null, OAIPMHerrorcodeType.NO_SET_HIERARCHY,
                    "Set are not enabled");
            }

            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final OAIPMHtype oai = oaiFactory.createOAIPMHtype();
            oai.setResponseDate(dataFactory.newXMLGregorianCalendar(dateFormat.print(new Date().getTime())));
            final ListSetsType sets = oaiFactory.createListSetsType();

            // query from collection object model
            final StringBuilder jql = new StringBuilder();
            jql.append("SELECT col.[mode:localName] AS spec, col.[dcterms:title] AS name,")
                .append(" col.[dcterms:description] AS desc, com.[dcterms:title] as cname ");
            jql.append("FROM [").append(FedoraJcrTypes.FEDORA_RESOURCE).append("] as col ").append("JOIN [")
                .append(FedoraJcrTypes.FEDORA_RESOURCE).append("] as com ")
                .append(" ON col.[uatermsid:belongsToCommunity] = com.[mode:localName] ")
                .append("WHERE col.[model:hasModel] = 'Collection' AND col.[uatermsid:is_community] IS NULL ")
                .append(" AND col.[uatermsid:is_official] = CAST('" + booleanTrue + "' AS BINARY)")
                .append(" AND com.[uatermsid:is_community] = CAST('" + booleanTrue + "' AS BINARY)");
            final RowIterator setResult = executeQuery(queryManager, jql.toString());
            if (!setResult.hasNext()) {
                return error(VerbType.LIST_SETS, null, null, OAIPMHerrorcodeType.NO_RECORDS_MATCH, "No record found");
            }
            while (setResult.hasNext()) {
                final SetType set = oaiFactory.createSetType();
                final Row sol = setResult.nextRow();
                final String setName =
                    valueConverter.convert(sol.getValue("cname")).asLiteral().getString() + " / "
                        + valueConverter.convert(sol.getValue("name")).asLiteral().getString();
                set.setSetSpec(valueConverter.convert(sol.getValue("spec")).asLiteral().getString());
                set.setSetName(setName);
                sets.getSet().add(set);
            }

            oai.setListSets(sets);
            return oaiFactory.createOAIPMH(oai);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        }
    }

    /**
     * List records.
     *
     * @param session the session
     * @param uriInfo the uri info
     * @param metadataPrefix the metadata prefix
     * @param from the from
     * @param until the until
     * @param set the set
     * @param offset the offset
     * @return the jAXB element
     * @throws RepositoryException the repository exception
     */
    public JAXBElement<OAIPMHtype> listRecords(final Session session, final UriInfo uriInfo,
        final String metadataPrefix, final String from, final String until, final String set, final int offset)
        throws RepositoryException {

        final HttpResourceConverter converter =
            new HttpResourceConverter(session, uriInfo.getBaseUriBuilder().clone().path(FedoraNodes.class));
        final ValueConverter valueConverter = new ValueConverter(session, converter);

        if (metadataPrefix == null) {
            return error(VerbType.LIST_RECORDS, null, null, OAIPMHerrorcodeType.BAD_ARGUMENT,
                "metadataprefix is invalid");
        }
        final MetadataFormat mdf = metadataFormats.get(metadataPrefix);
        if (mdf == null) {
            return error(VerbType.LIST_RECORDS, null, metadataPrefix, OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT,
                "Unavailable metadata format");
        }

        // dateTime format validation
        try {
            validateDateTimeFormat(from);
            validateDateTimeFormat(until);
        } catch (final IllegalArgumentException e) {
            return error(VerbType.LIST_IDENTIFIERS, null, metadataPrefix, OAIPMHerrorcodeType.BAD_ARGUMENT,
                e.getMessage());
        }

        if (StringUtils.isNotBlank(set) && !setsEnabled) {
            return error(VerbType.LIST_RECORDS, null, metadataPrefix, OAIPMHerrorcodeType.NO_SET_HIERARCHY,
                "Sets are not enabled");
        }

        final String jql =
            listResourceQuery(session, FedoraJcrTypes.FEDORA_CONTAINER, metadataPrefix, from, until, set, maxListSize,
                offset);
        try {

            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final RowIterator result = executeQuery(queryManager, jql);

            if (!result.hasNext()) {
                return error(VerbType.LIST_RECORDS, null, metadataPrefix, OAIPMHerrorcodeType.NO_RECORDS_MATCH,
                    "No record found");
            }

            final OAIPMHtype oai = oaiFactory.createOAIPMHtype();
            oai.setResponseDate(dataFactory.newXMLGregorianCalendar(dateFormat.print(new Date().getTime())));

            final ListRecordsType records = oaiFactory.createListRecordsType();
            while (result.hasNext()) {
                // fix JCR-SQL2 LIMIT bug in 4.2.0
                if (result.getPosition() < maxListSize) {
                    final Row solution = result.nextRow();
                    final Resource subjectUri = valueConverter.convert(solution.getValue("sub")).asResource();
                    final String name = valueConverter.convert(solution.getValue("name")).asLiteral().getString();
                    final RecordType record = createRecord(session, mdf, converter.asString(subjectUri), name, uriInfo);
                    records.getRecord().add(record);
                } else {
                    break;
                }
            }

            final RequestType req = oaiFactory.createRequestType();
            if (records.getRecord().size() == maxListSize) {
                final ResumptionTokenType token = oaiFactory.createResumptionTokenType();
                token.setValue(encodeResumptionToken(VerbType.LIST_RECORDS.value(), metadataPrefix, from, until, set,
                    offset + maxListSize));
                token.setCursor(new BigInteger(String.valueOf(offset)));
                token.setCompleteListSize(new BigInteger(String.valueOf(result.getSize() + offset)));
                records.setResumptionToken(token);
            }
            req.setVerb(VerbType.LIST_RECORDS);
            req.setMetadataPrefix(metadataPrefix);
            req.setFrom(StringUtils.isEmpty(from) ? null : from);
            req.setUntil(StringUtils.isEmpty(until) ? null : until);
            req.setSet(StringUtils.isEmpty(set) ? null : set);
            oai.setRequest(req);
            oai.setListRecords(records);
            return oaiFactory.createOAIPMH(oai);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        }
    }

    private RecordType createRecord(final Session session, final MetadataFormat mdf, final String path,
        final String name, final UriInfo uriInfo) throws IOException, RepositoryException {

        final HttpResourceConverter converter =
            new HttpResourceConverter(session, uriInfo.getBaseUriBuilder().clone().path(FedoraNodes.class));
        final HeaderType h = oaiFactory.createHeaderType();

        // using spring bean property, descriptiveContent.baseUrl
        h.setIdentifier(createId(path));

        final Container obj = containerService.find(session, path);
        h.setDatestamp(dateFormat.print(obj.getLastModifiedDate().getTime()));

        // set setSpecs
        final RdfStream triples =
            obj.getTriples(converter, PropertiesRdfContext.class)
                .filter(new PropertyPredicate(propertyHasCollectionId));
        while (triples.hasNext()) {
            h.getSetSpec().add(triples.next().getObject().getLiteralValue().toString());
        }

        // get the metadata record from fcrepo
        final MetadataType md = oaiFactory.createMetadataType();
        if (mdf.getPrefix().equals("oai_dc")) {
            /* generate a OAI DC reponse using the DC Generator from fcrepo4 */
            md.setAny(generateOaiDc(session, obj, name, uriInfo));
        } else if (mdf.getPrefix().equals("oai_etdms")) {
            /* generate a OAI ETDMS reponse using the DC Generator from fcrepo4 */
            md.setAny(generateOaiEtdms(session, obj, name, uriInfo));
        } else {
            /* generate a OAI response from the linked Binary */
            md.setAny(fetchOaiResponse(obj, session, mdf, uriInfo));
        }

        final RecordType record = oaiFactory.createRecordType();
        record.setMetadata(md);
        record.setHeader(h);
        return record;
    }

    /**
     * The createId method.
     * 
     * @param path
     * @return
     */
    private String createId(final String path) {
        final String noid = slashPattern.split(path)[6];
        final String id = String.format(idFormat, noid);
        return id;
    }

    private String listResourceQuery(final Session session, final String mixinTypes, final String metadataPrefix,
        final String from, final String until, final String set, final int limit, final int offset)
        throws RepositoryException {

        final String propJcrPath = getPropertyName(session, createProperty(RdfLexicon.JCR_NAMESPACE + "path"));
        final String propHasMixinType = getPropertyName(session, RdfLexicon.HAS_MIXIN_TYPE);
        final String propJcrUuid = getPropertyName(session, createProperty(RdfLexicon.JCR_NAMESPACE + "uuid"));
        final String propJcrLastModifiedDate = getPropertyName(session, RdfLexicon.LAST_MODIFIED_DATE);
        final String propHasModel = getPropertyName(session, createProperty(propertyHasModel));
        final String propAccessTo =
            getPropertyName(session, createProperty("http://www.w3.org/ns/auth/acl#accessTo_ref"));
        final String propAgent = getPropertyName(session, createProperty("http://www.w3.org/ns/auth/acl#agent"));
        final String propHasCollectionId = getPropertyName(session, createProperty(propertyHasCollectionId));

        final StringBuilder jql = new StringBuilder();
        jql.append("SELECT res.[" + propJcrPath + "] AS sub, res.[mode:localName] AS name ");
        jql.append("FROM [" + FedoraJcrTypes.FEDORA_RESOURCE + "] AS [res]");
        jql.append(" JOIN [" + FedoraJcrTypes.FEDORA_RESOURCE + "] AS [per]");
        jql.append(" ON res.[" + propJcrUuid + "] = per.[" + propAccessTo + "] ");
        jql.append("WHERE ");

        // mixin type constraint
        jql.append("res.[" + propHasMixinType + "] = '" + mixinTypes + "'");

        // items
        jql.append(" AND");
        jql.append(" res.[" + propHasModel + "] = 'GenericFile'");

        // permission
        jql.append(" AND");
        jql.append(" per.[" + propHasModel + "] = 'Hydra::AccessControls::Permission'");

        // public item, cast to binary and compare with xs:base64binary string property
        jql.append(" AND");
        jql.append(" per.[" + propAgent + "] = CAST('" + publicAgent + "' AS BINARY)");

        // start datetime constraint
        if (StringUtils.isNotBlank(from)) {
            jql.append(" AND");
            jql.append(" res.[" + propJcrLastModifiedDate + "] >= CAST('" + from + "' AS DATE)");
        }

        // end datetime constraint
        if (StringUtils.isNotBlank(until)) {
            jql.append(" AND");
            jql.append(" res.[" + propJcrLastModifiedDate + "] <= CAST('" + until + "' AS DATE)");
        }

        // etdms for thesis only
        if (metadataPrefix.equals("oai_etdms")) {
            jql.append(" AND");
            jql.append(" res.[dcterms:type] = 'Thesis'");
        }

        // set constraint
        if (StringUtils.isNotBlank(set)) {
            jql.append(" AND");
            jql.append(" res.[" + propHasCollectionId + "] = '" + set + "'");
        }

        if (limit > 0) {
            // bug in 4.2.0 fixed in 4.5.0
            // jql.append(" LIMIT ").append(maxListSize);
            jql.append(" OFFSET ").append(offset);
        }

        log.debug(jql.toString());
        return jql.toString();
    }

    private RowIterator executeQuery(final QueryManager queryManager, final String jql) throws RepositoryException {
        final Query query = queryManager.createQuery(jql, Query.JCR_SQL2);
        final QueryResult results = (QueryResult) query.execute();
        log.debug(results.getPlan());
        return results.getRows();
    }

    private void validateDateTimeFormat(final String dateTime) {
        if (StringUtils.isNotBlank(dateTime)) {
            dateFormat.parseDateTime(dateTime);
        }
    }

    /**
     * Get a property name for an RDF predicate
     *
     * @param session
     * @param predicate
     * @return property name from the given predicate
     * @throws RepositoryException
     */
    private String getPropertyName(final Session session, final Property predicate) throws RepositoryException {

        final NamespaceRegistry namespaceRegistry =
            (org.modeshape.jcr.api.NamespaceRegistry) session.getWorkspace().getNamespaceRegistry();
        final Map<String, String> namespaceMapping = emptyMap();
        return getPropertyNameFromPredicate(namespaceRegistry, predicate, namespaceMapping);
    }

    private String searchResourceQuery(final Session session, final String mixinTypes, final String property,
        final String value, final int limit, final int offset) throws RepositoryException {

        final String propHasMixinType = getPropertyName(session, RdfLexicon.HAS_MIXIN_TYPE);
        final String propJcrPath = getPropertyName(session, createProperty(RdfLexicon.JCR_NAMESPACE + "path"));
        final String propJcrUuid = getPropertyName(session, createProperty(RdfLexicon.JCR_NAMESPACE + "uuid"));
        // final String propJcrLastModifiedDate = getPropertyName(session, RdfLexicon.LAST_MODIFIED_DATE);
        final String propHasModel = getPropertyName(session, createProperty(propertyHasModel));
        final String propAccessTo =
            getPropertyName(session, createProperty("http://www.w3.org/ns/auth/acl#accessTo_ref"));
        // final String propAgent = getPropertyName(session, createProperty("http://www.w3.org/ns/auth/acl#agent"));
        final StringBuilder jql = new StringBuilder();
        jql.append("SELECT res.[" + propJcrPath + "] AS sub ");
        jql.append("FROM [" + FedoraJcrTypes.FEDORA_RESOURCE + "] AS [res]");
        jql.append(" JOIN [" + FedoraJcrTypes.FEDORA_RESOURCE + "] AS [per]");
        jql.append(" ON res.[" + propJcrUuid + "] = per.[" + propAccessTo + "] ");
        jql.append("WHERE ");

        // mixin type constraint
        jql.append("res.[" + propHasMixinType + "] = '" + mixinTypes + "'");

        // items
        jql.append(" AND");
        jql.append(" res.[" + propHasModel + "] = 'GenericFile'");

        // permission
        jql.append(" AND");
        jql.append(" per.[" + propHasModel + "] = 'Hydra::AccessControls::Permission'");

        // public item, cast to binary and compare with xs:base64binary string property
        // jql.append(" AND");
        // jql.append(" per.[" + propAgent + "] = CAST('" + publicAgent + "' AS BINARY)");

        // search criteria
        jql.append(" AND");
        jql.append(" res.[").append(property).append("] LIKE '%").append(value).append("%'");

        // limit and offset
        if (limit > 0) {
            jql.append(" LIMIT ").append(maxListSize);
            jql.append(" OFFSET ").append(offset);
        }

        log.debug(jql.toString());
        log.debug(jql.toString());
        return jql.toString();
    }

    /**
     * Search records.
     *
     * @param session the session
     * @param uriInfo the uri info
     * @param metadataPrefix the metadata prefix
     * @param property
     * @param value
     * @param offset
     * @return the jAXB element
     * @throws RepositoryException the repository exception
     */
    public JAXBElement<OAIPMHtype> search(final Session session, final UriInfo uriInfo, final String metadataPrefix,
        final String property, final String value, final int offset) throws RepositoryException {
        if (!searchEnabled) {
            return error(VerbType.LIST_RECORDS, null, null, OAIPMHerrorcodeType.NO_SET_HIERARCHY,
                "Search is not enabled");
        }

        final HttpResourceConverter converter =
            new HttpResourceConverter(session, uriInfo.getBaseUriBuilder().clone().path(FedoraNodes.class));
        final ValueConverter valueConverter = new ValueConverter(session, converter);

        if (metadataPrefix == null) {
            return error(VerbType.LIST_RECORDS, null, null, OAIPMHerrorcodeType.BAD_ARGUMENT,
                "metadataprefix is invalid");
        }
        final MetadataFormat mdf = metadataFormats.get(metadataPrefix);
        if (mdf == null) {
            return error(VerbType.LIST_RECORDS, null, metadataPrefix, OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT,
                "Unavailable metadata format");
        }

        final String jql =
            searchResourceQuery(session, FedoraJcrTypes.FEDORA_CONTAINER, property, value, maxListSize, offset);
        try {

            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final RowIterator result = executeQuery(queryManager, jql);

            if (!result.hasNext()) {
                return error(VerbType.LIST_RECORDS, null, metadataPrefix, OAIPMHerrorcodeType.NO_RECORDS_MATCH,
                    "No record found");
            }

            final OAIPMHtype oai = oaiFactory.createOAIPMHtype();
            oai.setResponseDate(dataFactory.newXMLGregorianCalendar(dateFormat.print(new Date().getTime())));
            final ListRecordsType records = oaiFactory.createListRecordsType();
            while (result.hasNext()) {
                final Row solution = result.nextRow();
                final Resource subjectUri = valueConverter.convert(solution.getValue("sub")).asResource();
                final String name = valueConverter.convert(solution.getValue("name")).asLiteral().getString();
                final RecordType record = createRecord(session, mdf, converter.asString(subjectUri), name, uriInfo);
                records.getRecord().add(record);
            }

            final RequestType req = oaiFactory.createRequestType();
            if (records.getRecord().size() == maxListSize) {
                final ResumptionTokenType token = oaiFactory.createResumptionTokenType();
                token.setValue(encodeResumptionToken(VerbType.LIST_RECORDS.value(), metadataPrefix, null, null, null,
                    offset + maxListSize));
                token.setCursor(new BigInteger(String.valueOf(offset)));
                token.setCompleteListSize(new BigInteger(String.valueOf(result.getSize() + offset)));
                records.setResumptionToken(token);
            }
            req.setVerb(VerbType.LIST_RECORDS);
            req.setMetadataPrefix(metadataPrefix);
            req.setFrom(null);
            req.setUntil(null);
            req.setSet(null);
            oai.setRequest(req);
            oai.setListRecords(records);
            return oaiFactory.createOAIPMH(oai);
        } catch (final Exception e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        }
    }

    /**
     * The delete method.
     * 
     * @param path
     * @return
     * @throws RepositoryException
     */
    public JAXBElement<OAIPMHtype> delete(final String path) throws RepositoryException {
        final Session session = sessionFactory.getInternalSession();
        if (nodeService.exists(session, path)) {
            try {
                log.debug("Deleting resource ...", path);
                final Container con = containerService.find(session, path);
                con.delete();
                session.save();
                return error(VerbType.GET_RECORD, null, null, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST,
                    "Resource has been deleted");
            } catch (final Exception e) {
                return error(VerbType.GET_RECORD, null, null, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, e.getMessage());
            }
        } else {
            return error(VerbType.GET_RECORD, null, null, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST,
                "Resource does not exist");
        }
    }
}
