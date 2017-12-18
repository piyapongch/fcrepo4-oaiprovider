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

import com.google.common.xml.XmlEscapers;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import javax.ws.rs.core.UriInfo;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.fcrepo.kernel.models.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(JcrOaiOreGenerator.class);

    private String lacIdFormat;

    private String pdfUrlFormat;

    private String oreSourceGenerator;

    private String oreSourceAuthorName;

    private String oreSourceAuthorUri;

    private String oaiUrlFormat;

    private String etdmsUrlFormat;

    private String oreUrlFormat;

    private String htmlUrlFormat;

    /**
     * The generate method.
     *
     * @param session
     * @param obj
     * @param name Name of the object (id)
     * @param uriInfo
     * @param identifier
     * 
     * @return JAXB element containing the metadata
     * @throws RepositoryException
     */
    public JAXBElement<EntryType> generate
        (final Session session, final Container obj, final String name, final UriInfo uriInfo, final String identifier)
        throws RepositoryException {

        final EntryType entry = oreFactory.createEntryType();

        // get href values used repetedly
        try {
            final String htmlHref = String.format(htmlUrlFormat, URLEncoder.encode(name, "UTF-8"));
            final String oreHref  = htmlHref.concat("/ore.xml");
            final String oaiHref  = String.format(oaiUrlFormat, URLEncoder.encode(identifier, "UTF-8"));
            final String etdmsHref  = String.format(etdmsUrlFormat, URLEncoder.encode(identifier, "UTF-8"));
            final String oreRef  = String.format(oreUrlFormat, URLEncoder.encode(identifier, "UTF-8"));

            // <!-- Atom Specific; No ORE Semantics -->
            addAtomIdentifiers(entry, obj, name);

            // <!-- Resource map metadata -->
            addResourceMapMetadata(entry, obj, oreRef);

            // <!-- Aggregation metadata -->
            addAggregationMetadata(entry, obj);

            //<!-- Categories for the Aggregation (rdf:type) (repeatable for multifile resources) -->
            addAtomCategory(entry, obj, name);

            // <!-- Aggregated Resources -->
            addAggregatedResources(entry, obj, name, oaiHref);

            // <!-- Additional properties pertaining to Aggregated Resources and Aggregation -->
            addAtomTriples(entry, obj, name, identifier, htmlHref, oreHref, oaiHref, etdmsHref);
        } catch (final UnsupportedEncodingException e) {
            throw new RepositoryException(e);
        }

        return oreFactory.createEntry(entry);
    }

  /**
     * The add multi-valued identifier method.
     *
     * @param et entryType class
     * @param prop JCR/Fedora property
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
     * @param prop JCR/Fedora property
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
     * @param prop JCR/Fedora property
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
     * @param prop JCR/Fedora property
     * @param name Name of the object (id)
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
            throw new ValueFormatException(e);
        }
    }

    /**
     * The add ERA Id method.
     *
     * @param et entryType class
     * @param name Name of the object (id)
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
     * @param prop JCR/Fedora property
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
            throw new ValueFormatException(e);
        }
    }

    /**
     * The add Title method.
     *
     * @param et entryType class
     * @param prop JCR/Fedora property
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
     * @param prop JCR/Fedora property
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
     * @param prop JCR/Fedora property
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

        // atom:source/atom:generator - <!-- publisher of record -->
        final GeneratorType generator = oreFactory.createGeneratorType();
        generator.setValue(oreSourceGenerator);
        source.getContent().add(oreFactory.createSourceTypeGenerator(generator));

        // atom:source/atom:update - <!-- timestamp -->
        try {
            final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
            final DateTimeType dateTime = oreFactory.createDateTimeType();
            final XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    GregorianCalendar.from(java.time.ZonedDateTime.now(ZoneOffset.UTC)));
            dateTime.setValue(xgc);
            source.getContent().add(oreFactory.createSourceTypeUpdated(dateTime));
        } catch (DatatypeConfigurationException e) {
            throw new ValueFormatException(e);
        }

        // atom:source/atom:id - <!-- identifier -->
        if (values != null) {
            final int len = java.lang.Math.toIntExact(values.length);
            if (len > 0) {
                final Value lastValue = (len > 0) ? values[len - 1] : null;
                if (lastValue != null && StringUtils.isNotEmpty(lastValue.getString())) {
                    final IdType id = oreFactory.createIdType();
                    id.setValue(formatUalidDoi(lastValue.getString()));
                    source.getContent().add(oreFactory.createSourceTypeId(id));
                }
            }
        }
        et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeSource(source));
    }

    /**
     * The add Atom published data method
     *
     * @param et entryType class
     * @param obj JCR object properties in a container
     * @param oreHref url for the ORE endpoint
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addResourceMapMetadata(final EntryType entry, final Container obj, final String oreHref)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        // metadata link
        // <!-- this ReM is serialized in Atom -->
        final LinkType atomLink = oreFactory.createLinkType();
        atomLink.setHref(oreHref);
        atomLink.setRel("self");
        atomLink.setType("application/atom+xml");
        entry.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeLink(atomLink));

        // <!-- This ReM is an ReM -->
        final LinkType oreLink = oreFactory.createLinkType();
        oreLink.setHref(oreHref);
        oreLink.setRel("http://www.openarchives.org/ore/terms/describes");
        entry.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeLink(oreLink));

        // add atom:source
        final Value[] dois = obj.hasProperty("ualid:doi") ? obj.getProperty("ualid:doi").getValues() : null;
        addAtomSource(entry, dois);

        // atom:published
        final Value[] values = returnDateValues(obj);

        for (final Value v : values) {
            try {
                if (StringUtils.isNotEmpty(v.getString())) {
                    final XMLGregorianCalendar xgc
                            = DatatypeFactory.newInstance().newXMLGregorianCalendar(v.getString());
                    // atom:published is a xs:dateTime thus only populate if data and time present
                    if (xgc.getXMLSchemaType() == DatatypeConstants.DATETIME) {
                        final DateTimeType dateTime = oreFactory.createDateTimeType();
                        dateTime.setValue(xgc);
                        entry.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypePublished(dateTime));
                    } else {
                        throw new ValueFormatException();
                    }
                }
            } catch (Exception e) {
                // throw new ValueFormatException();
            }
        }
    }

    /**
     * Identifiers
     *
     * @param et entryType class
     * @param obj JCR object properties
     * @param name Name of the object (id)
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomIdentifiers(final EntryType entry, final Container obj, final String name)
        throws ValueFormatException, IllegalStateException, RepositoryException {

           // identifiers
            addIdentifier(entry, name);
            addEraIdentifier(entry, name);
            if (obj.hasProperty("dcterms:identifier")) {
                addIdentifier(entry, obj.getProperty("dcterms:identifier"));
            }
            if (obj.hasProperty("model:downloadFilename")) {
                addFilenameIdentifier(entry, obj.getProperty("model:downloadFilename"), name);
            }
            if (obj.hasProperty("ualid:doi")) {
                addIdentifier(entry, obj.getProperty("ualid:doi"));
                addUalidDoiIdentifier(entry, obj.getProperty("ualid:doi"));
            }
            if (obj.hasProperty("ualid:fedora3handle")) {
                addIdentifier(entry, obj.getProperty("ualid:fedora3handle"));
                addLacIdentifier(entry, obj.getProperty("ualid:fedora3handle"));
            }
    }

    /**
     * Aggregation metadata
     *
     * @param et entryType class
     * @param obj JCR object properties
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAggregationMetadata(final EntryType entry, final Container obj)
        throws ValueFormatException, IllegalStateException, RepositoryException {

            // <!-- dcterms:creator / http://id.loc.gov/vocabulary/relators/dis (thesis) -->
            // marcrel:dis maps to creator
            if (obj.hasProperty("marcrel:dis")) {
                addAtomAuthor(entry, obj.getProperty("marcrel:dis"));
            } else if (obj.hasProperty("dcterms:creator")) {
                addAtomAuthor(entry, obj.getProperty("dcterms:creator"));
            }
            // <!-- dcterms:contributor (optional)-->/
            if (obj.hasProperty("dcterms:contributor")) {
                addAtomContributor(entry, obj.getProperty("dcterms:contributor"));
            }
            // supervisor
            //  if (obj.hasProperty("marcrel:ths")) {
            //      addAtomContributor(entry, obj.getProperty("marcrel:ths"));
            //  }
            // committee - assume include "marcrel:ths" value
            if (obj.hasProperty("ualrole:thesiscommitteemember")) {
                addAtomContributor(entry, obj.getProperty("ualrole:thesiscommitteemember"));
            }
            // <!-- dcterms:title -->
            if (obj.hasProperty("dcterms:title")) {
                addAtomTitle(entry, obj.getProperty("dcterms:title"));
            }
    }

    /**
     * The add Category method.
     *
     * @param et entryType class
     * @param prop JCR object properties
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomCategory(final EntryType et, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final CategoryType category = oreFactory.createCategoryType();
                final String tmp = XmlEscapers.xmlAttributeEscaper().escape(v.getString());
                category.setTerm(tmp);
                category.setLabel(tmp);
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
     * @param name Name of the object (id)
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAtomCategory(final EntryType et, final Container obj, final String name)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        // <!-- Creation and Modification date/time of the Aggregation (rdf literals) -->
        final Value[] values = returnDateValues(obj);

        // get last date
        final int len = (values != null) ? java.lang.Math.toIntExact(values.length) : 0;
        final Value v = (len > 0) ? values[len - 1] : null;
        if (v != null && StringUtils.isNotEmpty(v.getString())) {
            try {
                final XMLGregorianCalendar xgc
                        = DatatypeFactory.newInstance().newXMLGregorianCalendar(v.getString());
                addModifiedDate(v.getString(), et);
            } catch (Exception e) {
                // disregard malformed dates
                log.warn("Invalid date on object: " + name + " - value: " + v.getString());
                // kludge to fix date in format of "[1999]", "c1999", or "[1999?]"
                final String modDate = v.getString().replaceAll("[^\\d.]", "");
                addModifiedDate(modDate, et);
                // throw new ValueFormatException();
            }
        }

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
     * add modified date
     * 
     * @param v String representation of a valid date
     * @param et And EntryType container object
     */
    private void addModifiedDate(final String v, final EntryType et) {
        final CategoryType modified = oreFactory.createCategoryType();
        modified.setTerm(v);
        modified.setScheme("http://www.openarchives.org/ore/atom/modified");
        et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeCategory(modified));
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
            final String oaiHref)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        try {
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
                linkFile.setTitle(
                        XmlEscapers.xmlAttributeEscaper().escape(
                                findLastPropertyValue(obj.getProperty("premis:hasOriginalName")).getString()
                        )
                );
            } else if (fileStr != null) {
                linkFile.setTitle(XmlEscapers.xmlAttributeEscaper().escape(fileStr));
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
                linkHtml.setTitle(XmlEscapers.xmlAttributeEscaper().escape(titleStr));
            }
            linkHtml.setHref(String.format(htmlUrlFormat, URLEncoder.encode(name, "UTF-8")));
            et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeLink(linkHtml));

            // add OAI-PMH
            final LinkType linkOai = oreFactory.createLinkType();
            linkOai.setRel("http://www.openarchives.org/ore/terms/aggregates");
            if (obj.hasProperty("dcterms:title")) {
                final String titleStr = findLastPropertyValue(obj.getProperty("dcterms:title")).getString();
                linkOai.setTitle(XmlEscapers.xmlAttributeEscaper().escape(titleStr));
            }
            linkOai.setHref(oaiHref);
            linkOai.setType("application/xml");
            et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeLink(linkOai));

        } catch (final UnsupportedEncodingException e) {
            throw new ValueFormatException(e);
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
            final String identifier,
            final String htmlHref, final String oreHref, final String oaiHref,
            final String etdmsHref
            )
        throws ValueFormatException, IllegalStateException, RepositoryException {

        final org.openarchives.ore.atom.ObjectFactory oreAtomFactory
                = new org.openarchives.ore.atom.ObjectFactory();

        final org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory oreRdfFactory
                = new org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory();

        final Triples triples = oreAtomFactory.createTriples();

        // <!-- Properties pertaining to aggregation -->
        addTriplePropAgg(et, obj, oreRdfFactory, oreHref, triples);
        // <!-- Properties pertaining to the aggregated binary (can be repeated for multifile resources) -->
        addTriplePropAggBinary(et, obj, oreRdfFactory, triples, name);
        // <!-- Properties pertaining to the aggregated resource splash page-->
        addTriplePropSplashPage(et, oreRdfFactory, htmlHref, triples);
        // <!-- asserts the relationship between the oai_pmh record and the ore record -->
        addTriplePropOreRecord(et, oreRdfFactory, oaiHref, etdmsHref, triples);

        et.getAuthorOrCategoryOrContent().add(triples);
    }

    /**
     * Add triple properties pertaining to aggregation
     *
     * @param et entryType class
     * @param obj JCR/Fedora object
     * @param oreRdfFactory Object to create ORE RDF metadata
     * @param oreHref URL for the ORE
     * @param triples Object to create the ORE Triples section
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
                final String modifiedDate =
                        findLastPropertyValue(obj.getProperty("dcterms:modified")).getString();
                // Todo: is there a better way?
                // dcterms:modified is a String in the form
                // "YYYY-MM-DDTHH:MM:SS:xxxZ ^^http://www.w3.org/2001/XMLSchema#dateTime"
                if (StringUtils.isNotEmpty(modifiedDate)) {
                    final String trimmedDate =
                            modifiedDate.replaceAll(".{4,4}http://www.w3.org/2001/XMLSchema#dateTime", "");
                    final XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(trimmedDate);
                    description.setModified(xgc);
                }
            } catch (Exception e) {
                throw new ValueFormatException(e);
            }
        }

        if (obj.hasProperty("dcterms:rights")) {
            description.setRights(findLastPropertyValue(obj.getProperty("dcterms:rights")).getString());
        }
        if (obj.hasProperty("dcterms:license")) {
            description.setLicense(findLastPropertyValue(obj.getProperty("dcterms:license")).getString());
        }
        if (obj.hasProperty("dcterms:isVersionOf")) {
            description.setIsVersionOf(findLastPropertyValue(obj.getProperty("dcterms:isVersionOf")).getString());
        }

        triples.getDescription().add(description);
    }

    /**
     * Properties pertaining to the aggregated binary (can be repeated for multifile resources)
     *
     * @param et EntryType class instance
     * @param name Name of the object (id)
     * @param oreRdfFactory Object to create ORE RDF metadata
     * @param oreHref URL for the ORE
     * @param triples Object to create the ORE Triples section
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
            throw new ValueFormatException(e);
        }
    }

    /**
     * Properties pertaining to the aggregated resource splash page
     *
     * @param et EntryType class instance
     * @param oreRdfFactory Object to create ORE RDF metadata
     * @param href URL for the splash page
     * @param triples Object to create the ORE Triples section
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
     * @param et EntryType class instance
     * @param oreRdfFactory Object to create ORE RDF metadata
     * @param href URL for the object
     * @param triples Object to create the ORE Triples section
     */
    private void addTriplePropOreRecord(
        final EntryType et, final org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory oreRdfFactory,
        final String href_dc,
        final String href_etdms,
        final Triples triples)
        throws ValueFormatException, IllegalStateException, RepositoryException {

        // link to DC record
        final org.w3._2000._01.rdf_schema_.ObjectFactory oreRdfsFactory
                = new org.w3._2000._01.rdf_schema_.ObjectFactory();

        final org.purl.dc.terms.ObjectFactory dctermsFactory
                = new org.purl.dc.terms.ObjectFactory();

        final Description description_dc = oreRdfFactory.createDescription();
        description_dc.setAbout(href_dc);
        final Type rdfType_dc = oreRdfFactory.createType();
        rdfType_dc.setResource("info:eu-repo/semantics/descriptiveMetadata");
        description_dc.setType(rdfType_dc);
        final ConformsTo conformsTo_dc = dctermsFactory.createConformsTo();
        conformsTo_dc.setResource("http://www.openarchives.org/OAI/2.0/oai_dc/");
        description_dc.setConformsTo(conformsTo_dc);
        triples.getDescription().add(description_dc);

        final Description description_etdms = oreRdfFactory.createDescription();
        description_etdms.setAbout(href_etdms);
        final Type rdfType_etdms = oreRdfFactory.createType();
        rdfType_etdms.setResource("info:eu-repo/semantics/descriptiveMetadata");
        description_etdms.setType(rdfType_etdms);
        final ConformsTo conformsTo_etdms = dctermsFactory.createConformsTo();
        conformsTo_etdms.setResource("http://www.ndltd.org/standards/metadata/etdms/1.0/");
        description_etdms.setConformsTo(conformsTo_etdms);
        triples.getDescription().add(description_etdms);

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
     * @param values type of object
     * 
     * @return true if Thesis otherwise false or null
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
     * Find the date Value
     * 
     * @param obj Container
     * 
     * @return Value array or null if none found
     */
    public final Value[] returnDateValues(final Container obj)
        throws ValueFormatException, RepositoryException {

        Value[] values;

        // get date depending on dc:type
        final Value[] dcType = obj.hasProperty("dcterms:type") ? obj.getProperty("dcterms:type").getValues() : null;
        final boolean isThesis = isThesis(dcType);

        //  <!-- dcterms:created | dcterms:dateAccepted (thesis)  -->
        if (isThesis) {
            values = obj.hasProperty("dcterms:dateAccepted")
                    ? obj.getProperty("dcterms:dateAccepted").getValues() : null;
        } else {
            values = obj.hasProperty("dcterms:created") ? obj.getProperty("dcterms:created").getValues() : null;
        }

        return values;
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
     * The etdmsUrlFormat setter method - from Bean.
     *
     * @param etdmsUrlFormat the OreSourceAuthorUri to set
     */
    public final void setEtdmsUrlFormat(final String etdmsUrlFormat) {
        this.etdmsUrlFormat = etdmsUrlFormat;
    }

    /**
     * The oreUrlFormat setter method - from Bean.
     *
     * @param oreUrlFormat the OreSourceAuthorUri to set
     */
    public final void setOreUrlFormat(final String oreUrlFormat) {
        this.oreUrlFormat = oreUrlFormat;
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
