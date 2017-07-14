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
package org.fcrepo.oai.generator;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import javax.ws.rs.core.UriInfo;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.fcrepo.kernel.models.Container;

// generated-sources
import org.w3._2005.atom.CategoryType;
import org.w3._2005.atom.DateTimeType;
import org.w3._2005.atom.EntryType;
import org.w3._2005.atom.GeneratorType;
import org.w3._2005.atom.IdType;
import org.w3._2005.atom.LinkType;
import org.w3._2005.atom.ObjectFactory;
import org.w3._2005.atom.PersonType;
import org.w3._2005.atom.TextType;
import org.w3._2005.atom.UriType;

import org.openarchives.ore.atom.Triples;
import org.purl.dc.terms.ConformsTo;
import org.w3._1999._02._22_rdf_syntax_ns_.Description;
import org.w3._1999._02._22_rdf_syntax_ns_.Type;
import org.w3._2000._01.rdf_schema_.IsDefinedBy;



/**
 * The JcrOaiOreGenerator class.
 *
 * Build JAXB classes to output XML for harvesting ORE via LAC
 * Based on JcrOaiEtdmsGenerator
 *
 * @author DI
 * @version $Revision$ $Date$
 */
public class JcrOaiOreGenerator extends JcrOaiGenerator {

    private static final ObjectFactory oreFactory = new ObjectFactory();

    private static final Pattern slashPattern = Pattern.compile("\\/");

    private String lacIdFormat;

    private String pdfUrlFormat;

    private String oreSourceGenerator;

    private String oreSourceAuthorName;

    private String oreSourceAuthorUri;

    private String oaiUrlFormat;

    private String htmlUrlFormat;

    /**
     * The generate method.
     *
     * @param session
     * @param obj
     * @param uriInfo
     * @return
     * @throws RepositoryException
     */
    public JAXBElement<EntryType> generate
        (final Session session, final Container obj, final String name, final UriInfo uriInfo, final String identifier)
        throws RepositoryException {

        final EntryType entry = oreFactory.createEntryType();

        final PropertyIterator props = obj.getNode().getProperties();
        while (props.hasNext()) {
            final Property prop = (Property) props.next();
            switch (prop.getName()) {

                case "bibo:ThesisDegree":
                    break;

                case "dcterms:abstract":
                    break;
                case "dcterms:alternative":
                    break;
                case "dcterms:creator":
                    // <!-- dcterms:creator / http://id.loc.gov/vocabulary/relators/dis (thesis) -->
                    addAtomAuthor(entry, prop);
                    break;
                case "dcterms:contributor":
                    // <!-- dcterms:contributor (optional)-->/
                    addAtomContributor(entry, prop);
                    break;
                case "dcterms:dateAccepted":
                case "dcterms:created":
                    break;
                case "dcterms:description":
                    break;
                case "dcterms:format":
                    break;
                case "dcterms:identifier":
                    addIdentifier(entry, prop);
                    break;
                case "dcterms:language":
                    break;
                case "dcterms:license":
                    break;
                case "dcterms:rights":
                    break;
                case "dcterms:spatial":
                    break;
                case "dcterms:subject":
                    break;
                case "dcterms:temporal":
                    break;
                case "dcterms:title":
                    //<!-- dcterms:title -->
                    addAtomTitle(entry, prop);
                    break;
                case "dcterms:type":

                    break;

                case "marcrel:dis":
                    break;
                case "marcrel:dgg":
                    break;
                case "marcrel:ths":
                    break;

                case "model:downloadFilename":
                    addFilenameIdentifier(entry, prop, name);
                    break;

                case "ualid:doi":
                    addIdentifier(entry, prop);
                    addUalidDoiIdentifier(entry, prop);
                    break;

                case "ualid:fedora3handle":
                    addIdentifier(entry, prop);
                    addLacIdentifier(entry, prop);
                    break;

                case "ualrole:thesiscommitteemember":
                    break;

                case "ualthesis:specialization":
                    break;
                case "ualthesis:thesislevel":
                    break;

                case "vivo:AcademicDepartment":
                    break;

                default:
                    break;
            }

        }

        // get href values used repetedly
        try {
            final String htmlHref = String.format(htmlUrlFormat, URLEncoder.encode(name, "UTF-8"));
            final String oreHref  = htmlHref.concat("/ore.xml");
            final String oaiHref  = String.format(oaiUrlFormat, URLEncoder.encode(name, "UTF-8"));

            addIdentifier(entry, name);
            addEraIdentifier(entry, name);

            addAtomCategory(entry, obj);
            addAtomPublishedDate(entry, obj);
            addAtomSource(entry, obj.getProperty("ualid:doi").getValues());
            addAggregatedResources(entry, obj, name, identifier);
            addAtomTriples(entry, obj, name, identifier, htmlHref, oreHref, oaiHref);
        } catch (final UnsupportedEncodingException e) {
            throw new RepositoryException(e);
        }

        return oreFactory.createEntry(entry);
    }

  /**
     * The add multi-valued identifier method.
     *
     * @param et entryType class
     * @param prop JCR property output
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addIdentifier(final EntryType et, final String identifier) {
        if (StringUtils.isNotEmpty(identifier)) {
            final IdType id = oreFactory.createIdType();
            id.setValue(identifier);
            et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeId(id));
        }
    }

    /**
     * The add multi-valued identifier method.
     *
     * @param et entryType class
     * @param prop JCR property output
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addIdentifier(final EntryType et, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            addIdentifier(et, v.getString());
        }
    }

    /**
     * The add multi-valued UAL DOI method.
     *
     * @param et entryType class
     * @param prop JCR property output
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addUalidDoiIdentifier(final EntryType et, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            addIdentifier(et, formatUalidDoi(v.getString()));
        }
    }

    /**
     * The add multi-valued Filename Id method.
     *
     * @param et entryType class
     * @param prop JCR property output
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addFilenameIdentifier(final EntryType et, final Property prop, final String name)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        try {
            for (final Value v : prop.getValues()) {
                if (StringUtils.isNotEmpty(v.getString())) {
                    final LinkType link = oreFactory.createLinkType();
                    link.setHref(String.format(pdfUrlFormat, name, URLEncoder.encode(v.getString(), "UTF-8")));
                    link.setRel("alternate");
                    et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeLink(link));
                }
            }
        } catch (final UnsupportedEncodingException e) {
            throw new RepositoryException(e);
        }
    }


    /**
     * The add ERA Id method.
     *
     * @param et entryType class
     * @param name
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addEraIdentifier(final EntryType et, final String name)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(name)) {
            final LinkType link = oreFactory.createLinkType();
            link.setHref(String.format(eraIdFormat, name));
            link.setRel("alternate");
            et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeLink(link));
        }
    }

   /**
     * The add multi-valued LAC Id method.
     *
     * @param et entryType class
     * @param prop JCR property output
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addLacIdentifier(final EntryType et, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        try {
            for (final Value v : prop.getValues()) {
                if (StringUtils.isNotEmpty(v.getString())) {
                    // LAC unique identifier
                    final String[] h = slashPattern.split(v.getString());
                    // add 2000 if it is thesisdeposit handle
                    final String tmp = String.format(lacIdFormat,
                        h[4].indexOf("era.") < 0
                        ? h[4] : NumberUtils.toInt(h[4].substring(4)) + 2000);
                    final IdType id = oreFactory.createIdType();
                    id.setValue(tmp);
                    et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeId(id));
                }
            }
        } catch (final Exception e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * The add Title method.
     *
     * @param et entryType class
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomTitle(final EntryType et, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final TextType title = oreFactory.createTextType();
                title.getContent().add(v.getString());
                et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeTitle(title));
            }
        }
    }

    /**
     * The add Author method.
     *
     * @param et entryType class
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomAuthor(final EntryType et, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final JAXBElement<String> authorName = oreFactory.createPersonTypeName(v.getString());
                final PersonType author = oreFactory.createPersonType();
                author.getNameOrUriOrEmail().add(authorName);
                et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeAuthor(author));
            }
        }
    }

    /**
     * The add Contributor method.
     *
     * @param et entryType class
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomContributor(final EntryType et, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final JAXBElement<String> authorName = oreFactory.createPersonTypeName(v.getString());
                final PersonType author = oreFactory.createPersonType();
                author.getNameOrUriOrEmail().add(authorName);
                et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeContributor(author));
            }
        }
    }

    /**
     * The add Atom Source section method
     *
     * @param et entryType class
     * @param values array of property values
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomSource(final EntryType et, final Value[] values)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        final TextType source = oreFactory.createTextType();

        // atom:source/atom:author - <!-- name of record creator -->
        final PersonType author = oreFactory.createPersonType();
        // add author uri
        final UriType authorUri = oreFactory.createUriType();
        authorUri.setValue(oreSourceAuthorUri);
        author.getNameOrUriOrEmail().add(oreFactory.createPersonTypeUri(authorUri));
        // add author name
        author.getNameOrUriOrEmail().add(oreFactory.createPersonTypeName(oreSourceAuthorName));
        source.getContent().add(oreFactory.createSourceTypeAuthor(author));

        // atom:source/atom:generator - <!-- publisher of recod -->
        final GeneratorType generator = oreFactory.createGeneratorType();
        generator.setValue(oreSourceGenerator);
        source.getContent().add(oreFactory.createSourceTypeGenerator(generator));

        // atom:source/atom:update - <!-- timestamp -->
        final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        final DateTimeType dateTime = oreFactory.createDateTimeType();
        generator.setValue(formatter.format(java.time.Instant.now()));
        source.getContent().add(oreFactory.createSourceTypeUpdated(dateTime));

        // atom:source/atom:id - <!-- identifier -->
        final int len = java.lang.Math.toIntExact(values.length);
        if (len > 0) {
            final Value lastValue = (len > 0) ? values[len - 1] : null;
            if (StringUtils.isNotEmpty(lastValue.getString())) {
                final IdType id = oreFactory.createIdType();
                id.setValue(formatUalidDoi(lastValue.getString()));
                source.getContent().add(oreFactory.createSourceTypeId(id));
            }
        }

        //final TextType text = oreFactory.createTextType();
        //text.getContent().add(source);
        et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeSource(source));
    }

    /**
     * The add Atom published data method
     *
     * @param et entryType class
     * @param obj JCR object properties
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomPublishedDate(final EntryType et, final Container obj)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        Value[] values;

        // dc:type
        final Value[] dcType = obj.hasProperty("dcterms:type") ? obj.getProperty("dcterms:type").getValues() : null;
        final boolean isThesis = isThesis(dcType);

        //  <!-- dcterms:created | dcterms:dateAccepted (thesis)  -->
        if (isThesis) {
            values = obj.hasProperty("dcterms:dateAccepted")
                    ? obj.getProperty("dcterms:dateAccepted").getValues() : null;
        } else {
            values = obj.hasProperty("dcterms:created") ? obj.getProperty("dcterms:created").getValues() : null;
        }

        for (final Value v : values) {
            final DateTimeType dateTime = oreFactory.createDateTimeType();
            try {
                final XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(v.getString());
                dateTime.setValue(xgc);
                et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypePublished(dateTime));
            } catch (DatatypeConfigurationException e) {
                throw new RepositoryException(e);
            }

        }
    }

    /**
     * The add Category method.
     *
     * @param et entryType class
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomCategory(final EntryType et, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final CategoryType category = oreFactory.createCategoryType();
                category.setTerm(v.getString());
                category.setLabel(v.getString());
                category.setScheme("http://purl.org/ontology/bibo/");
                et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeCategory(category));
            }
        }
    }

    /**
     * The add Atom Category method
     *
     * @param et entryType class
     * @param obj JCR object properties
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomCategory(final EntryType et, final Container obj)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        // <!-- Creation and Modification date/time of the Aggregation (rdf literals) -->
        final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        final String dateStr = formatter.format(java.time.Instant.now());
        final CategoryType modified = oreFactory.createCategoryType();
        modified.setTerm(dateStr);
        modified.setScheme("http://www.openarchives.org/ore/atom/modified");
        et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeCategory(modified));

        // <!-- Categories for the Aggregation (rdf:type) (repeatable for multifile resources) -->
        final CategoryType catAgg = oreFactory.createCategoryType();
        catAgg.setTerm("http://www.openarchives.org/ore/terms/Aggregation");
        catAgg.setScheme("http://www.openarchives.org/ore/terms/");
        catAgg.setLabel("Aggregation");
        et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeCategory(catAgg));

        final CategoryType catFedora = oreFactory.createCategoryType();
        catFedora.setTerm("http://fedora.info/definitions/v4/repository");
        catFedora.setScheme("http://fedora.info/definitions/v4/repository#resource");
        catFedora.setLabel("Fedora Resource");
        et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeCategory(catFedora));
        // <!-- dcterms:type -->
        final Property prop = obj.hasProperty("dcterms:type") ? obj.getProperty("dcterms:type") : null;
        addAtomCategory(et, prop);
    }

    /**
     * The add aggregated resource section method
     *
     * @param et entryType class
     * @param values array of property values
     * @param name identifier of the object
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAggregatedResources(final EntryType et, final Container obj, final String name,
            final String identifier)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        try {
            // <!-- Aggregated Resources -->
            // <!-- info:fedora/fedora-system:def/model#downloadFilename |
            // premis:hasOriginalName | fedora:mimetype | premis:hasSize -->

            /* TODO: remove */
            /*
            final Value[] files = obj.hasProperty("model:downloadFilename")
                ? obj.getProperty("model:downloadFilename").getValues() : null;
            final Value[] premisName = obj.hasProperty("premis:hasOriginalName")
                ? obj.getProperty("model:downloadFilename").getValues() : null;
            final Value[] premisSize = obj.hasProperty("premis:hasSize")
                ? obj.getProperty("premis:hasSize").getValues() : null;
            final Value[] mimetype = obj.hasProperty("fedora:mimetype")
                ? obj.getProperty("fedora:mimetype").getValues() : null;
            for (int i = 0; i < files.length; i++) {
                if (StringUtils.isNotEmpty(files[i].getString())) {
                    final String hrefStr = String.format(
                        pdfUrlFormat, name, URLEncoder.encode(files[i].getString(), "UTF-8"));
                    final String titleStr = premisName[i].getString();
                    final String mimetypeStr = mimetype[i].getString();
                    final BigInteger len = new BigInteger(premisSize[1].getString());

                    final LinkType link = oreFactory.createLinkType();
                    link.setRel("http://www.openarchives.org/ore/terms/aggregates");
                    if (titleStr != null ) {
                        link.setTitle(titleStr);
                    }
                    if (hrefStr != null ) {
                        link.setHref(hrefStr);
                    }
                    if (hrefStr != null ) {
                        link.setType(mimetypeStr);
                    }
                    if (len != null ) {
                        link.setLength(len);
                    }
                    et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeLink(link));
                }
            }
            */

            // <!-- Aggregated Resources -->
            // <!-- info:fedora/fedora-system:def/model#downloadFilename |
            // premis:hasOriginalName | fedora:mimetype | premis:hasSize -->
            final LinkType linkFile = oreFactory.createLinkType();
            String fileStr = null;
            if (obj.hasProperty("model:downloadFilename")) {
                fileStr = findLastPropertyValue(obj.getProperty("model:downloadFilename")).getString();
                final String hrefStr = String.format(pdfUrlFormat, name, URLEncoder.encode(fileStr, "UTF-8"));
                linkFile.setHref(hrefStr);
            }
            if (obj.hasProperty("premis:hasOriginalName")) {
                linkFile.setTitle(findLastPropertyValue(obj.getProperty("premis:hasOriginalName")).getString());
            } else {
                linkFile.setTitle(fileStr);
            }
            if (obj.hasProperty("premis:hasSize")) {
                final BigInteger len
                        = new BigInteger(findLastPropertyValue(obj.getProperty("dcterms:title")).getString());
                linkFile.setLength(len);
            }
            if (obj.hasProperty("fedora:mimeType")) {
                linkFile.setType(findLastPropertyValue(obj.getProperty("dcterms:title")).getString());
            }
            linkFile.setRel("http://www.openarchives.org/ore/terms/aggregates");
            et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeLink(linkFile));

            // add html
            final LinkType linkHtml = oreFactory.createLinkType();
            linkHtml.setRel("http://www.openarchives.org/ore/terms/aggregates");
            linkHtml.setType("text/html");
            if (obj.hasProperty("dcterms:title")) {
                final String titleStr = findLastPropertyValue(obj.getProperty("dcterms:title")).getString();
                linkHtml.setTitle(titleStr);
            }
            linkHtml.setHref(String.format(htmlUrlFormat, URLEncoder.encode(name, "UTF-8")));
            et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeLink(linkHtml));

            // add OAI-PMH
            final LinkType linkOai = oreFactory.createLinkType();
            linkOai.setRel("http://www.openarchives.org/ore/terms/aggregates");
            if (obj.hasProperty("dcterms:title")) {
                final String titleStr = findLastPropertyValue(obj.getProperty("dcterms:title")).getString();
                linkOai.setTitle(titleStr);
            }
            linkOai.setHref(String.format(oaiUrlFormat, URLEncoder.encode(identifier, "UTF-8")));
            linkOai.setType("application/xml");
            et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeLink(linkOai));

        } catch (final UnsupportedEncodingException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * The add Atom triple section method
     *
     * @param et entryType class
     * @param values array of property values
     * @param name identifier of the object
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomTriples(final EntryType et, final Container obj, final String name,
            final String identifier, final String htmlHref, final String oreHref, final String oaiHref)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        final org.openarchives.ore.atom.ObjectFactory oreAtomFactory
                = new org.openarchives.ore.atom.ObjectFactory();

        final org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory oreRdfFactory
                = new org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory();


        // <!-- Properties pertaining to aggregation -->
        /*
        final Description description = oreRdfFactory.createDescription();
        description.setAbout(oreHref);
        final Type rdfType = oreRdfFactory.createType();
        rdfType.setResource("http://fedora.info/definitions/v4/repository#Resource");
        if (obj.hasProperty("dcterms:modified")) {
            try {
                final String modifiedDate = findLastPropertyValue(obj.getProperty("dcterms:modified")).getString();
                final XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(modifiedDate);
                description.setModified(xgc);
            } catch (DatatypeConfigurationException e) {
                throw new ValueFormatException(e);
            } catch (Exception e) {
                   throw new ValueFormatException(e);
            }
        }
        if (obj.hasProperty("dcterms:license")) {
            description.setLicense(findLastPropertyValue(obj.getProperty("dcterms:license")).getString());
        } else if (obj.hasProperty("dcterms:rights")) {
            description.setLicense(findLastPropertyValue(obj.getProperty("dcterms:rights")).getString());
        }
        if (obj.hasProperty("dcterms:isVersionOf")) {
            description.setLicense(findLastPropertyValue(obj.getProperty("dcterms:isVersionOf")).getString());
        }
*/

        //dctermsFactory.create





        final Triples triples = oreAtomFactory.createTriples();

        addTriplePropAgg(et, obj, oreRdfFactory, oreHref, triples);
        addTriplePropAggBinary(et, obj, oreRdfFactory, triples, name);
        addTriplePropSplashPage(et, oreRdfFactory, htmlHref, triples);
        addTriplePropOreRecord(et, oreRdfFactory, oreHref, triples);

        et.getAuthorOrCategoryOrContent().add(triples);

    }

    /**
     * Add triple properties pertaining to aggregation
     *
     *
     */
    private void addTriplePropAgg(
        final EntryType et, final Container obj, final org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory oreRdfFactory,
        final String oreHref, final Triples triples)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        final Description description = oreRdfFactory.createDescription();
        description.setAbout(oreHref);
        final Type rdfType = oreRdfFactory.createType();
        rdfType.setResource("http://fedora.info/definitions/v4/repository#Resource");
        description.setType(rdfType);
        if (obj.hasProperty("dcterms:modified")) {
            try {
                final String modifiedDate = findLastPropertyValue(obj.getProperty("dcterms:modified")).getString();
                //final XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(modifiedDate);
                //description.setModified(xgc);
            } catch (Exception e) {
                throw new ValueFormatException(e);
            }
        }
        if (obj.hasProperty("dcterms:license")) {
            description.setLicense(findLastPropertyValue(obj.getProperty("dcterms:license")).getString());
        } else if (obj.hasProperty("dcterms:rights")) {
            description.setLicense(findLastPropertyValue(obj.getProperty("dcterms:rights")).getString());
        }
        if (obj.hasProperty("dcterms:isVersionOf")) {
            description.setIsVersionOf(findLastPropertyValue(obj.getProperty("dcterms:isVersionOf")).getString());
        }

        triples.getDescription().add(description);
    }

    /**
     * Properties pertaining to the aggregated binary (can be repeated for multifile resources)
     *
     *
     */
    private void addTriplePropAggBinary(
        final EntryType et, final Container obj, final org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory oreRdfFactory,
        final Triples triples, final String name)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        try {
            final Description description = oreRdfFactory.createDescription();

            if (obj.hasProperty("model:downloadFilename")) {
                final String fileStr = findLastPropertyValue(obj.getProperty("model:downloadFilename")).getString();
                final String hrefStr = String.format(pdfUrlFormat, name, URLEncoder.encode(fileStr, "UTF-8"));
                description.setAbout(hrefStr);
            }

            final Type rdfType = oreRdfFactory.createType();
            rdfType.setResource("http://fedora.info/definitions/v4/repository#Binary");
            description.setType(rdfType);
            description.setDescription("ORIGINAL");

            triples.getDescription().add(description);
        } catch (final UnsupportedEncodingException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Properties pertaining to the aggregated resource splash page
     *
     *
     */
    private void addTriplePropSplashPage(
        final EntryType et, final org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory oreRdfFactory,
        final String href, final Triples triples)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        final org.w3._2000._01.rdf_schema_.ObjectFactory oreRdfsFactory
                = new org.w3._2000._01.rdf_schema_.ObjectFactory();

        final Description description = oreRdfFactory.createDescription();
        description.setAbout(href);
        final Type rdfType = oreRdfFactory.createType();
        description.setType(rdfType);
        rdfType.setResource("info:eu-repo/semantics/humanStartPage");
        triples.getDescription().add(description);

        final Description descStart = oreRdfFactory.createDescription();
        descStart.setAbout("info:eu-repo/semantics/humanStartPage");
        descStart.setLabel("humanStartPage");
        final IsDefinedBy isDefinedBy = oreRdfsFactory.createIsDefinedBy();
        isDefinedBy.setResource("info:eu-repo/semantics/");
        descStart.setIsDefinedBy(isDefinedBy);
        triples.getDescription().add(descStart);
    }

    /**
     * asserts the relationship between the oai_pmh record and the ore record
     *
     *
     */
    private void addTriplePropOreRecord(
        final EntryType et, final org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory oreRdfFactory,
        final String href, final Triples triples)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        final org.w3._2000._01.rdf_schema_.ObjectFactory oreRdfsFactory
                = new org.w3._2000._01.rdf_schema_.ObjectFactory();

        final org.purl.dc.terms.ObjectFactory dctermsFactory
                = new org.purl.dc.terms.ObjectFactory();

        final Description description = oreRdfFactory.createDescription();
        description.setAbout(href);
        final Type rdfType = oreRdfFactory.createType();
        rdfType.setResource("info:eu-repo/semantics/descriptiveMetadata");
        description.setType(rdfType);
        final ConformsTo conformsTo = dctermsFactory.createConformsTo();
        conformsTo.setResource("http://www.openarchives.org/OAI/2.0/oai_dc/");
        description.setConformsTo(conformsTo);
        triples.getDescription().add(description);


        final Description descMeta = oreRdfFactory.createDescription();
        descMeta.setAbout("info:eu-repo/semantics/descriptiveMetadata");
        descMeta.setLabel("descriptiveMetadata");
        final IsDefinedBy isDefinedBy = oreRdfsFactory.createIsDefinedBy();
        isDefinedBy.setResource("info:eu-repo/semantics/");
        descMeta.setIsDefinedBy(isDefinedBy);
        triples.getDescription().add(descMeta);
    }




    /**
     * The isThesis method.
     *
     * @param values
     * @return
     */
    private boolean isThesis(final Value[] values) {
        if (values != null) {

            // using stream api to find dcterms:type Thesis
            final List<Value> vl = Arrays.asList(values);
            return vl.stream().anyMatch(v -> {

                // getString() throws exceptions need to catch them
                try {
                    return v.getString().equalsIgnoreCase("Thesis");
                } catch (final Exception e) {
                    return false;
                }
            });
        }
        return false;
    }

    /**
     * The find last value method.
     *
     * @param prop a property
     */
    public final Value findLastPropertyValue(final Property prop)
        throws ValueFormatException, RepositoryException {
        final Value[] vals = prop.getValues();
        final int len = java.lang.Math.toIntExact(vals.length);
        final Value val = (len > 0) ? vals[len - 1] : null;
        return val;
    }

    /**
     * The setLacIdFormat setter method.
     *
     * @param lacIdFormat the lacIdFormat to set
     */
    public final void setLacIdFormat(final String lacIdFormat) {
        this.lacIdFormat = lacIdFormat;
    }


    /**
     * The setPdfUrlFormat setter method - from Bean.
     *
     * @param pdfUrlFormat the pdfUrlFormat to set
     */
    public final void setPdfUrlFormat(final String pdfUrlFormat) {
        this.pdfUrlFormat = pdfUrlFormat;
    }

    /**
     * The OreSourceGenerator setter method - from Bean.
     *
     * @param oreSourceGenerator the OreSourceGenerator to set
     */
    public final void setOreSourceGenerator(final String oreSourceGenerator) {
        this.oreSourceGenerator = oreSourceGenerator;
    }

    /**
     * The OreSourceAuthorName setter method - from Bean.
     *
     * @param oreSourceAuthorName the OreSourceAuthorName to set
     */
    public final void setOreSourceAuthorName(final String oreSourceAuthorName) {
        this.oreSourceAuthorName = oreSourceAuthorName;
    }

    /**
     * The OreSourceAuthorUri setter method - from Bean.
     *
     * @param oreSourceAuthorUri the OreSourceAuthorUri to set
     */
    public final void setOreSourceAuthorUri(final String oreSourceAuthorUri) {
        this.oreSourceAuthorUri = oreSourceAuthorUri;
    }

    /**
     * The oaiUrlFormat setter method - from Bean.
     *
     * @param oaiUrlFormat the OreSourceAuthorUri to set
     */
    public final void setOaiUrlFormat(final String oaiUrlFormat) {
        this.oaiUrlFormat = oaiUrlFormat;
    }

    /**
     * The htmlUrlFormat setter method - from Bean.
     *
     * @param htmlUrlFormat
     */
    public final void setHtmlUrlFormat(final String htmlUrlFormat) {
        this.htmlUrlFormat = htmlUrlFormat;
    }




}
