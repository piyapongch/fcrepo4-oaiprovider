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
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
//import java.time.Instant;
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
import org.w3._2005.atom.DateTimeType;
import org.w3._2005.atom.EntryType;
import org.w3._2005.atom.GeneratorType;
import org.w3._2005.atom.IdType;
import org.w3._2005.atom.LinkType;
import org.w3._2005.atom.ObjectFactory;
import org.w3._2005.atom.PersonType;
import org.w3._2005.atom.SourceType;
import org.w3._2005.atom.TextType;
import org.w3._2005.atom.UriType;


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
        (final Session session, final Container obj, final String name, final UriInfo uriInfo)
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
                case "dcterms:contributor":
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


        addEraIdentifier(entry, name);

        //addAtomPublishedDate(entry, obj);
        //addAtomSource(entry, obj.getProperty("ualid:doi").getValues());

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
    private void addIdentifier(final EntryType et, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final IdType id = oreFactory.createIdType();
                id.setValue(v.getString());
                et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeId(id));
            }
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
            if (StringUtils.isNotEmpty(v.getString())) {
                final IdType id = oreFactory.createIdType();
                id.setValue(formatUalidDoi(v.getString()));
                et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeId(id));
            }
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

        final SourceType source = oreFactory.createSourceType();

        // atom:source/atom:author - <!-- name of record creator -->
        final PersonType author = oreFactory.createPersonType();
        // add author uri
        final UriType authorUri = oreFactory.createUriType();
        authorUri.setValue(oreSourceAuthorUri);
        author.getNameOrUriOrEmail().add(authorUri);
        // add author name
        final JAXBElement<String> authorName = oreFactory.createPersonTypeName(oreSourceAuthorName);
        author.getNameOrUriOrEmail().add(authorName);
        source.getAuthorOrCategoryOrContributor().add(author);

        // atom:source/atom:generator - <!-- publisher of recod -->
        final GeneratorType generator = oreFactory.createGeneratorType();
        generator.setValue(oreSourceGenerator);
        source.getAuthorOrCategoryOrContributor().add(oreFactory.createSourceTypeGenerator(generator));

        // atom:source/atom:update - <!-- timestamp -->
        final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        final DateTimeType dateTime = oreFactory.createDateTimeType();
        generator.setValue(formatter.format(java.time.Instant.now()));
        source.getAuthorOrCategoryOrContributor().add(oreFactory.createSourceTypeUpdated(dateTime));

        // atom:source/atom:id - <!-- identifier -->
        final int len = java.lang.Math.toIntExact(values.length);
        if (len > 0) {
            final Value lastValue = (len > 0) ? values[len - 1] : null;
            if (StringUtils.isNotEmpty(lastValue.getString())) {
                final IdType id = oreFactory.createIdType();
                id.setValue(formatUalidDoi(lastValue.getString()));
                source.getAuthorOrCategoryOrContributor().add(oreFactory.createSourceTypeId(id));
            }
        }

        final TextType text = oreFactory.createTextType();
        text.getContent().add(source);
        et.getAuthorOrCategoryOrContent().add(oreFactory.createEntryTypeSource(text));
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
            values = obj.hasProperty("marcrel:dis") ? obj.getProperty("marcrel:dis").getValues() : null;
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





}
