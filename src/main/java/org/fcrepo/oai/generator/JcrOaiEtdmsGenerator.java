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

import java.net.URI;
import static org.fcrepo.oai.generator.JcrOaiDcGenerator.LICENSE_PROMPT;

//import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.kernel.api.models.Container;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter;
import org.fcrepo.oai.rdf.LanguageRdf;

import org.ndltd.standards.metadata.etdms._1.AuthorityType;
import org.ndltd.standards.metadata.etdms._1.ControlledTextType;
import org.ndltd.standards.metadata.etdms._1.FreeTextType;
import org.ndltd.standards.metadata.etdms._1.ObjectFactory;
import org.ndltd.standards.metadata.etdms._1.Thesis;
import org.ndltd.standards.metadata.etdms._1.Thesis.Contributor;
import org.ndltd.standards.metadata.etdms._1.Thesis.Degree;
import org.ndltd.standards.metadata.etdms._1.Thesis.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The JcrOaiEtdmsGenerator class.
 *
 * @author <a href="mailto:piyapong.charoenwattana@gmail.com">Piyapong Charoenwattana</a>
 * @version $Revision$ $Date$
 */
public class JcrOaiEtdmsGenerator extends JcrOaiGenerator {

    private static final Logger log = LoggerFactory.getLogger(JcrOaiEtdmsGenerator.class);

    private static final ObjectFactory etdmsFactory = new ObjectFactory();

    private static final Pattern slashPattern = Pattern.compile("\\/");

    private String lacIdFormat;

    private String pdfUrlFormat;

    private ValueConverter valueConverter = null;

    @Autowired
    private LanguageRdf languageRdf;

    /**
     * The generate method.
     *
     * @param obj object container
     * @param name name
     * @param valueConverter convert triple object values to string literals
     * @param fileBinaryList object containing the attached file information
     * @param converter triple lookup value converter
     * @return a JaxB Thesis root object
     * @throws RepositoryException general repository exception
     */
    public Thesis generate(
        final Container obj,
            final String name,
            final ValueConverter valueConverter,
            final HttpResourceConverter converter,
            final List<FedoraBinary> fileBinaryList
    )
        throws RepositoryException {

        this.valueConverter = valueConverter;

        String handle = null;
        final Node node = getJcrNode(obj);

        final Thesis thesis = etdmsFactory.createThesis();

        // degree element
        final Degree degree = etdmsFactory.createThesisDegree();

        final String nsUal = jcrNamespaceMap.get("ual");
        final String nsDc = jcrNamespaceMap.get("dc");
        final String nsDcTerms = jcrNamespaceMap.get("dcterms");
        final String nsBibo = jcrNamespaceMap.get("bibo");
        final String nsPrism = jcrNamespaceMap.get("prism");
        final String nsSwrc = jcrNamespaceMap.get("swrc");

        // case "dcterms:type":
        if (obj.hasProperty(nsDcTerms + "type")) {
            for (final Value v : node.getProperty(nsDcTerms + "type").getValues()) {
                addType(v, thesis.getType());
            }
        }

        // case "dcterms:creator":
        if (obj.hasProperty(nsDcTerms + "creator")) {
            for (final Value v : node.getProperty(nsDcTerms + "creator").getValues()) {
                addAuthorityType(v, thesis.getCreator());
            }
        }

        // case "ual:dissertant":
        if (obj.hasProperty(nsUal + "dissertant")) {
            for (final Value v : node.getProperty(nsUal + "dissertant").getValues()) {
                addAuthorityType(v, thesis.getCreator());
            }
        }

        // case "dc:contributor":
        if (obj.hasProperty(nsDc + "contributor")) {
            for (final Value v : node.getProperty(nsDc + "contributor").getValues()) {
                addContributor(v, thesis.getContributor(), null);
            }
        }

        // case "ual:supervisor":
        if (obj.hasProperty(nsUal + "supervisor")) {
            for (final Value v : node.getProperty(nsUal + "supervisor").getValues()) {
                addContributor(v, thesis.getContributor(), "advisor");
            }
        }

        // case "ual:commiteeMember":
        if (obj.hasProperty(nsUal + "commiteeMember")) {
            for (final Value v : node.getProperty(nsUal + "commiteeMember").getValues()) {
                addContributor(v, thesis.getContributor(), "committeemember");
            }
        }

        // case "swrc:institution":
        if (obj.hasProperty(nsSwrc + "institution")) {
            for (final Value v : node.getProperty(nsSwrc + "institution").getValues()) {
                addInstitutionType(v, degree.getGrantor());
            }
        }

        // case "ual:department":
        if (obj.hasProperty(nsUal + "department")) {
            for (final Value v : node.getProperty(nsUal + "department").getValues()) {
                addFreeTextType(v, degree.getDiscipline());
            }
        }

        // case "dc:subject":
        if (obj.hasProperty(nsDc + "subject")) {
            for (final Value v : node.getProperty(nsDc + "subject").getValues()) {
                addControlledTextType(v, thesis.getSubject());
            }
        }

        // case "dcterms:temporal":
        if (obj.hasProperty(nsDcTerms + "temporal")) {
            for (final Value v : node.getProperty(nsDcTerms + "temporal").getValues()) {
                addControlledTextType(v, thesis.getSubject());
            }
        }

        // case "dcterms:spatial":
        if (obj.hasProperty(nsDcTerms + "spatial")) {
            for (final Value v : node.getProperty(nsDcTerms + "spatial").getValues()) {
                addControlledTextType(v, thesis.getSubject());
            }
        }

        // case "ual:specialization":
        if (obj.hasProperty(nsUal + "specialization")) {
            for (final Value v : node.getProperty(nsUal + "specialization").getValues()) {
                addDescription(v, thesis.getDescription(), "Specialization: ");
            }
        }

        // case "dcterms:dateAccepted":
        if (obj.hasProperty(nsDcTerms + "dateAccepted")) {
            for (final Value v : node.getProperty(nsDcTerms + "dateAccepted").getValues()) {
                addDate(v, thesis);
            }
        }

        // case "dcterms:title":
        if (obj.hasProperty(nsDcTerms + "title")) {
            for (final Value v : node.getProperty(nsDcTerms + "title").getValues()) {
                addFreeTextType(v, thesis.getTitle());
            }
        }

        // case "dcterms:alternative":
        if (obj.hasProperty(nsDcTerms + "alternative")) {
            for (final Value v : node.getProperty(nsDcTerms + "alternative").getValues()) {
                addFreeTextType(v, thesis.getAlternativeTitle());
            }
        }

        // case "bibo:degree":
        if (obj.hasProperty(nsBibo + "degree")) {
            for (final Value v : node.getProperty(nsBibo + "degree").getValues()) {
                addFreeTextType(v, degree.getName());
            }
        }

        // case "ual:thesisLevel":
        if (obj.hasProperty(nsUal + "thesisLevel")) {
            for (final Value v : node.getProperty(nsUal + "thesisLevel").getValues()) {
                addString(v, degree.getLevel());
            }
        }

        // case "dcterms:identifier":
        if (obj.hasProperty(nsDcTerms + "identifier")) {
            for (final Value v : node.getProperty(nsDcTerms + "identifier").getValues()) {
                addString(v, thesis.getIdentifier());
            }
        }

        // case "prism:doi":
        if (obj.hasProperty(nsPrism + "doi")) {
            for (final Value v : node.getProperty(nsPrism + "doi").getValues()) {
                //addString(v, thesis.getIdentifier());
                addUalid(v, thesis.getIdentifier());
            }
        }

        // case "ual:fedora3Handle":
        if (obj.hasProperty(nsUal + "fedora3Handle")) {
            for (final Value v : node.getProperty(nsUal + "fedora3Handle").getValues()) {
                addString(v, thesis.getIdentifier());
                handle = StringUtils.isEmpty(v.getString())
                  ? null : valueConverter.convert(v).asLiteral().getString();
            }
        }

        // case "dcterms:description":
        if (obj.hasProperty(nsDcTerms + "description")) {
            addLongDescription(thesis, node.getProperty(nsDcTerms + "description"), null);
        }

        // case "dcterms:abstract":
        if (obj.hasProperty(nsDcTerms + "abstract")) {
            addLongDescription(thesis, node.getProperty(nsDcTerms + "abstract"), "Abstract: ");
        }

        // case "dcterms:language":
        if (obj.hasProperty(nsDcTerms + "language")) {
            for (final Value v : node.getProperty(nsDcTerms + "language").getValues()) {
                addLanguage(v, thesis.getLanguage());
            }
        }

        // case "dc:rights":
        if (obj.hasProperty(nsDc + "rights")) {
            for (final Value v : node.getProperty(nsDc + "rights").getValues()) {
                addFreeTextType(v, thesis.getRights());
            }
        }

        // case "dcterms:license":
        if (obj.hasProperty(nsDcTerms + "license")) {
            for (final Value v : node.getProperty(nsDcTerms + "license").getValues()) {
                if (!v.getString().equals(LICENSE_PROMPT)) {
                    addFreeTextType(v, thesis.getRights());
                }
            }
        }

        // case "dcterms:format":
        if (obj.hasProperty(nsDcTerms + "format")) {
            for (final Value v : node.getProperty(nsDcTerms + "format").getValues()) {
                addFreeTextType(v, thesis.getFormat());
            }
        }


        thesis.setDegree(degree);

        // LAC unique identifier
        try {
            if (handle != null) {
                final String[] h = slashPattern.split(handle);

                // add 2000 if it is thesisdeposit handle
                thesis.getIdentifier().add(String.format(lacIdFormat,
                    h[4].indexOf("era.") < 0 ? h[4] : NumberUtils.toInt(h[4].substring(4)) + 2000));
            }
        } catch (final Exception e) {
            // could not generate the identifier
            log.error(e.toString());
        }

        // era identifier
        thesis.getIdentifier().add(String.format(eraIdFormat, name));

        // rdf:type
        addThesisType(obj, thesis.getType());

        // download file
        for (final FedoraBinary fileItem : fileBinaryList) {
            final String fileSet = getFileSetFromPath(fileItem.getPath());
            try {
                thesis.getIdentifier().add(
                        String.format(
                                pdfUrlFormat,
                                name,
                                fileSet,
                                URLEncoder.encode(fileItem.getFilename()), "UTF-8")
                        );
            } catch (final Exception e) {
                log.error(e.toString());
                throw new RepositoryException(e);
            }
        }

        return thesis;
    }

    /**
     * Add the thesis date
     *
     *
     * @param thesis
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addDate(final Value v, final Thesis thesis)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        thesis.setDate(StringUtils.isEmpty(v.getString()) ? null : valueConverter.convert(v).asLiteral().getString());
    }


  /**
     * The addDescription method for long descriptions.
     *
     * In the context of the Fedora 4.2 & HydraNorth stack,
     * if dcterms:description property (in jcr/xml) is of a
     * substantial length (tested with 6760 characters) the property
     * type switches to `Binary` with subsequent edits appending a new value to the
     * list of values for the property (instead of replacing).
     * This workaround chooses the last value assuming the previous
     * values are old versions (to avoid returning all values of the
     * property, including the obsolete). 2017-05-12
     *
     * @param thesis
     * @param prop
     * @param prefix
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addLongDescription(final Thesis thesis, final Property prop, final String prefix)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        final Value[] vals = prop.getValues();
        final int len = java.lang.Math.toIntExact(vals.length);
        if (len > 0) {
            final Value lastValue = (len > 0) ? vals[len - 1] : null;
            addDescription(lastValue, thesis.getDescription(), prefix);
        }
    }


    /**
     * The addDescription method.
     *
     * @param v
     * @param description
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addDescription(final Value v, final List<Description> description, final String prefix)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            final String tmp = valueConverter.convert(v).asLiteral().getString();
            final Description desc = etdmsFactory.createThesisDescription();
            desc.setValue(prefix == null ? tmp : prefix + tmp);
            description.add(desc);
        }
    }

    /**
     * The addControlledTextType method.
     *
     * @param v
     * @param subject
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addControlledTextType(final Value v, final List<ControlledTextType> subject)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            final ControlledTextType text = etdmsFactory.createControlledTextType();
            text.setValue(valueConverter.convert(v).asLiteral().getString());
            subject.add(text);
        }
    }

    /**
     * The addString method.
     *
     * @param v
     * @param level
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addString(final Value v, final List<String> level)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            level.add(valueConverter.convert(v).asLiteral().getString());
        }
    }

    /**
     * The addString method.
     *
     * @param v
     * @param level
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addUalid(final Value v, final List<String> level)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            addString(formatUalidDoi(valueConverter.convert(v).asLiteral().getString()), level);
        }
    }


    /**
     * The addString method.
     *
     * @param v
     * @param level
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addString(final String v, final List<String> level)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v)) {
            level.add(v);
        }
    }

    /**
     * The addContributor method.
     *
     * @param v
     * @param contributor
     * @param role
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addContributor(final Value v, final List<Contributor> conts, final String role)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            final Contributor cont = new Thesis.Contributor();
            cont.setValue(valueConverter.convert(v).asLiteral().getString());
            if (role != null) {
                cont.setRole(role);
            }
            conts.add(cont);
        }
    }

    /**
     * The addAuthorityType method.
     *
     * @param v
     * @param auths
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAuthorityType(final Value v, final List<AuthorityType> auths)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            final AuthorityType auth = etdmsFactory.createAuthorityType();
            auth.setValue(valueConverter.convert(v).asLiteral().getString());
            auths.add(auth);
        }
    }

    /**
     * The addAuthorityType method.
     *
     * @param v
     * @param auths
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addInstitutionType(final Value v, final List<AuthorityType> auths)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            // use map to convert to human readable string
            final String tmp = valueConverter.convert(v).asLiteral().getString();
            final String humanStr = (institutionMap.containsKey(tmp)) ? institutionMap.get(tmp) : tmp;
            final AuthorityType auth = etdmsFactory.createAuthorityType();
            auth.setValue(humanStr);
            auths.add(auth);
        }
    }

    /**
     * The addFreeTextType method.
     *
     * @param v
     * @param texts
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addFreeTextType(final Value v, final List<FreeTextType> texts)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            final FreeTextType text = etdmsFactory.createFreeTextType();
            text.setValue(valueConverter.convert(v).asLiteral().getString());
            texts.add(text);
        }
    }

    /**
     * The a type method.
     *
     * @param v
     * @param texts
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addType(final Value v, final List<FreeTextType> texts)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            final String validDcType =
                getDcTypeValue(valueConverter.convert(v).asLiteral().getString());
            if (validDcType != null) {
                final FreeTextType text = etdmsFactory.createFreeTextType();
                text.setValue(validDcType);
                texts.add(text);
            }
        }
    }


    /** add the dc:type for a Thesis
     *
     * @param oaidc the output object
     * @param obj a container object
     *
     */
    private void addThesisType(final Container obj, final List<FreeTextType> freeTextTypes) {
        for (URI v : obj.getTypes()) {
            final String validDcType = getDcTypeValue(v.toString());
            if (validDcType != null) {
                final FreeTextType freeTextType = etdmsFactory.createFreeTextType();
                freeTextType.setValue(validDcType);
                freeTextTypes.add(freeTextType);
            }
        }
    }

    /**
     * The addLanguage method.
     *
     * @param v
     * @param level
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addLanguage(final Value v, final List<String> level)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            final String langStr = languageRdf.getLiteralFromUrl(valueConverter.convert(v).asLiteral().getString());
            if (langStr != null) {
                level.add(langStr);
            }
        }
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
     * The setPdfUrlFormat setter method.
     *
     * @param pdfUrlFormat the pdfUrlFormat to set
     */
    public final void setPdfUrlFormat(final String pdfUrlFormat) {
        this.pdfUrlFormat = pdfUrlFormat;
    }

}
