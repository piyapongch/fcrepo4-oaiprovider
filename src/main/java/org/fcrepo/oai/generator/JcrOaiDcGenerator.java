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
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang3.StringUtils;
import org.fcrepo.kernel.api.models.Container;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter;

import org.openarchives.oai._2_0.oai_dc.OaiDcType;
import org.purl.dc.elements._1.ObjectFactory;
import org.purl.dc.elements._1.SimpleLiteral;


/**
 * The type Jcr properties oai_dc generator.
 *
 * @author Piyapong Charoenwattana
 */
public class JcrOaiDcGenerator extends JcrOaiGenerator {

    public static final String LICENSE_PROMPT = "I am required to use/link to a publisher's license";

    private static final ObjectFactory dcFactory = new ObjectFactory();
    private static final org.openarchives.oai._2_0.oai_dc.ObjectFactory oaiDcFactory =
        new org.openarchives.oai._2_0.oai_dc.ObjectFactory();
    private static final String uofa = "University of Alberta";

    private ValueConverter valueConverter = null;

    /**
     * Generate dC.
     *
     * @param obj the obj
     * @param name name
     * @param valueConverter convert triple object values to string literals
     * @return the jAXB element
     * @throws RepositoryException if repository exception occurred
     * @throws IllegalStateException illegal state exception
     */
    public JAXBElement<OaiDcType> generate(
            final Container obj,
            final String name,
            final ValueConverter valueConverter
        ) throws RepositoryException, IllegalStateException {

        this.valueConverter = valueConverter;

        final OaiDcType oaidc = oaiDcFactory.createOaiDcType();
        Value[] values;
        final Node node = getJcrNode(obj);

        // creator and date
        final boolean isThesis = isThesis(node);
        if (isThesis) {
            // thesis dc:creator
            values = obj.hasProperty("ual:dissertant") ? node.getProperty("ual:dissertant").getValues() : null;
            addCreator(oaidc, values);
            // thesis dc:date
            values =
                obj.hasProperty("dcterms:dateAccepted") ? node.getProperty("dcterms:dateAccepted").getValues() : null;
            addDate(oaidc, values);
            // dc:type
            values = obj.hasProperty("dcterms:type") ? node.getProperty("dcterms:type").getValues() : null;
            if (values != null) {
                addType(oaidc, values);
            } else {
                addThesisType(oaidc, obj);
            }
        } else {
            // non-thesis dc:creator
            values = obj.hasProperty("dc:creator") ? node.getProperty("dc:creator").getValues() : null;
            addCreator(oaidc, values);
            // non-thesis dc:date
            values = obj.hasProperty("dcterms:created") ? node.getProperty("dcterms:created").getValues() : null;
            addDate(oaidc, values);
            // dc:type
            values = obj.hasProperty("dcterms:type") ? node.getProperty("dcterms:type").getValues() : null;
            addType(oaidc, values);
        }

        // dc:publisher (concatenate grantor and discipline/department contents)
        final Value[] depts =
            obj.hasProperty("ual:department") ? node.getProperty("ual:department").getValues() : null;
        //
        final Value[] ddgs = obj.hasProperty("swrc:institution")
                ? node.getProperty("swrc:institution").getValues() : null;

        final StringBuilder pub = new StringBuilder();

        // If both institution and department are present
        if ((ddgs != null) && (depts != null)) {
            final String tmp = valueConverter.convert(ddgs[0]).asLiteral().getString();
            final String humanDdgs = (institutionMap.containsKey(tmp)) ? institutionMap.get(tmp) : tmp;
            pub.append(humanDdgs + ". ");
            for (int i = 0; i < depts.length; i++) {
                pub.append(i == 0 ? "" : "; ").append(valueConverter.convert(depts[i]).asLiteral().getString());
            }
            pub.append(depts.length == 1 ? "." : "");

            // If only department is present
        } else if ((ddgs == null) && (depts != null)) {
            pub.append(uofa);
            for (int i = 0; i < depts.length; i++) {
                pub.append(i == 0 ? "; " : ", ").append(valueConverter.convert(depts[i]).asLiteral().getString());
            }
            pub.append(depts.length == 1 ? "." : "");

            // Otherwise, print only institution (no punctuation)
        } else if (ddgs != null) {
            final String tmp = valueConverter.convert(ddgs[0]).asLiteral().getString();
            final String humanDdgs = (institutionMap.containsKey(tmp)) ? institutionMap.get(tmp) : tmp;
            pub.append(humanDdgs);
        }
        pub.append(pub.toString().trim().length() == 0 ? uofa : "");

        final SimpleLiteral sim = dcFactory.createSimpleLiteral();
        sim.getContent().add(pub.toString());
        oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createPublisher(sim));

        // era identifier
        final SimpleLiteral simple = dcFactory.createSimpleLiteral();
        simple.getContent().add(String.format(eraIdFormat, name));
        oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(simple));

        final PropertyIterator props = node.getProperties();
        while (props.hasNext()) {
            final Property prop = (Property) props.next();
            switch (prop.getName()) {

            case "dc:contributor":
                addContributor(oaidc, prop);
                break;

            case "ual:supervisor":
                addContributor(oaidc, prop);
                break;

            case "ual:commiteeMember":
                addContributor(oaidc, prop);
                break;

            case "dc:subject":
                addSubject(oaidc, prop);
                break;

            case "dcterms:temporal":
                addSubject(oaidc, prop);
                break;

            case "dcterms:spatial":
                addSubject(oaidc, prop);
                break;

            case "ual:specialization":
                addDescription(oaidc, prop, "Specialization: ");
                break;

            case "dcterms:title":
                addTitle(oaidc, prop);
                break;

            case "bibo:degree":
                addDescription(oaidc, prop, "Degree: ");
                break;

            case "dcterms:identifier":
                addIdentifier(oaidc, prop);
                break;

            case "prism:doi":
                addIdentifier(oaidc, prop);
                addIdentifierDoi(oaidc, prop, dcFactory);
                break;

            case "ual:fedora3Handle":
                addIdentifier(oaidc, prop);
                break;

            case "dcterms:description":
                addLongDescription(oaidc, prop, null);
                break;

            case "dcterms:abstract":
                addLongDescription(oaidc, prop, "Abstract: ");
                break;

            case "dcterms:language":
                addLanguage(oaidc, prop);
                break;

            case "dcterms:relation":
                addRelation(oaidc, prop);
                break;

            case "dcterms:isVersionOf":
                addRelation(oaidc, prop);
                break;

            case "dcterms:source":
                addSource(oaidc, prop);
                break;

            case "dc:rights":
                addRights(oaidc, prop);
                break;

            case "dcterms:license":
                addRights(oaidc, prop);
                break;

            case "dcterms:format":
                addFormat(oaidc, prop);
                break;

            default:
                break;
            }
        }
        return oaiDcFactory.createDc(oaidc);
    }

    /**
     * The addFormat method.
     *
     * @param oaidc
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addFormat(final OaiDcType oaidc, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(v).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createFormat(simple));
            }
        }
    }

    /**
     * The addRights method.
     *
     * @param oaidc
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addRights(final OaiDcType oaidc, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            final String tmp = valueConverter.convert(v).asLiteral().getString();
            if (StringUtils.isNotEmpty(tmp) && !tmp.equals(LICENSE_PROMPT)) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(tmp);
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRights(simple));
            }
        }
    }

    /**
     * The addSource method.
     *
     * @param oaidc
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addSource(final OaiDcType oaidc, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(v).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createSource(simple));
            }
        }
    }

    /**
     * The addRelation method.
     *
     * @param oaidc
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addRelation(final OaiDcType oaidc, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(v).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRelation(simple));
            }
        }
    }

    /**
     * The addLanguage method.
     *
     * @param oaidc
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addLanguage(final OaiDcType oaidc, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(v).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createLanguage(simple));
            }
        }
    }

    /**
     * The addIdentifier method.
     *
     * @param oaidc root of the OAI DC response
     * @param prop list of properties to add to the DC response
     * @throws RepositoryException general repository exception
     * @throws IllegalStateException illegal state exception
     */
    private void addIdentifier(final OaiDcType oaidc, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(v).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(simple));
            }
        }
    }

    /**
     * The addIdentifier method - modify the string to include the full DOI URL.
     *
     * @param oaidc root of the OAI DC response
     * @param prop list of properties to add to the DC response
     * @param dcFactory factory to build the DC formatted response
     * @throws RepositoryException general repository exception
     * @throws IllegalStateException illegal state exception
     * @throws ValueFormatException content format exception
     */
    protected void addIdentifierDoi(final OaiDcType oaidc, final Property prop, final ObjectFactory dcFactory)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(formatUalidDoi(valueConverter.convert(v).asLiteral().getString()));
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(simple));
            }
        }
    }

    /**
     * The addTitle method.
     *
     * @param oaidc
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addTitle(final OaiDcType oaidc, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(v).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createTitle(simple));
            }
        }
    }

    /**
     * The addDescription method.
     *
     * @param oaidc
     * @param prop
     * @param prefix
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addDescription(final OaiDcType oaidc, final Property prop, final String prefix)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                final String tmp = valueConverter.convert(v).asLiteral().getString();
                simple.getContent().add(prefix == null ? tmp : prefix + tmp);
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDescription(simple));
            }
        }
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
     * @param oaidc
     * @param prop
     * @param prefix
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addLongDescription(final OaiDcType oaidc, final Property prop, final String prefix)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        final Value[] vals = prop.getValues();
        final int len = java.lang.Math.toIntExact(vals.length);
        if (len > 0) {
            final Value lastValue = (len > 0) ? vals[len - 1] : null;
            if (StringUtils.isNotEmpty(lastValue.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                final String tmp = valueConverter.convert(lastValue).asLiteral().getString();
                simple.getContent().add(prefix == null ? tmp : prefix + tmp);
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDescription(simple));
            }
        }
    }

    /**
     * The addSubject method.
     *
     * @param oaidc
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addSubject(final OaiDcType oaidc, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(v).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createSubject(simple));
            }
        }
    }

    /**
     * The addContributor method.
     *
     * @param oaidc
     * @param prop
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addContributor(final OaiDcType oaidc, final Property prop)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (final Value v : prop.getValues()) {
            if (StringUtils.isNotEmpty(v.getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(v).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createContributor(simple));
            }
        }
    }

    /**
     * The addDate method.
     *
     * @param oaidc
     * @param values
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addDate(final OaiDcType oaidc, final Value[] values)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(values[i]).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDate(simple));
            }
        }
    }

    /**
     * The addCreator method.
     *
     * @param oaidc
     * @param values
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addCreator(final OaiDcType oaidc, final Value[] values)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(values[i]).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createCreator(simple));
            }
        }
    }

    /**
     * The isThesis method.
     *
     * @param node Object node
     * @return true if thesis, false otherwise
     */
    private boolean isThesis(final Node node) throws RepositoryException {
        final Value[] values
                = node.hasProperty("model:hasModel") ? node.getProperty("model:hasModel").getValues() : null;
        if (values != null) {
            // using stream api to find dcterms:type Thesis
            final List<Value> vl = Arrays.asList(values);
            return vl.stream().anyMatch(v -> {

                // getString() throws exceptions need to catch them
                try {
                    return v.getString().equalsIgnoreCase(org.fcrepo.oai.service.OAIProviderService.modelIRThesis);
                } catch (final Exception e) {
                    return false;
                }
            });
        }
        return false;
    }

    /**
     * The createType method.
     *
     * @param values
     * @param oaidc
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addType(final OaiDcType oaidc, final Value[] values)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(valueConverter.convert(values[i]).asLiteral().getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createType(simple));
            }
        }
    }

    /** add the dc:type for a Thesis
     *
     * @param oaidc the output object
     * @param obj a container object
     *
     */
    private void addThesisType(final OaiDcType oaidc, final Container obj) {
        for (URI v : obj.getTypes()) {
            final SimpleLiteral simple = dcFactory.createSimpleLiteral();
            simple.getContent().add(v.toString());
            oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createType(simple));
        }
    }
}
