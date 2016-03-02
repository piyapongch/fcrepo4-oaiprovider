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
package org.fcrepo.oai.dublincore;

import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;

import org.apache.commons.lang3.StringUtils;
import org.fcrepo.kernel.models.Container;
import org.openarchives.oai._2_0.oai_dc.OaiDcType;
import org.purl.dc.elements._1.ObjectFactory;
import org.purl.dc.elements._1.SimpleLiteral;

/**
 * The type Jcr properties generator.
 *
 * @author Frank Asseg
 * @author Piyapong Charoenwattana
 */
public class JcrPropertiesGenerator {
    private static final ObjectFactory dcFactory = new ObjectFactory();
    private static final org.openarchives.oai._2_0.oai_dc.ObjectFactory oaiDcFactory =
        new org.openarchives.oai._2_0.oai_dc.ObjectFactory();

    /**
     * Generate dC.
     *
     * @param session the session
     * @param obj the obj
     * @param uriInfo the uri info
     * @return the jAXB element
     * @throws RepositoryException if repository exception occurred
     * @throws IllegalStateException
     */
    public JAXBElement<OaiDcType> generateDc(final Session session, final Container obj, final UriInfo uriInfo)
        throws RepositoryException, IllegalStateException {

        final OaiDcType oaidc = oaiDcFactory.createOaiDcType();
        Value[] values;
        boolean isThesis = false;

        // dc:type
        values = obj.hasProperty("dcterms:type") ? obj.getProperty("dcterms:type").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createType(simple));
            }
        }

        // using stream api to find dcterms:type Thesis
        final List<Value> vl = Arrays.asList(values);
        isThesis = vl.stream().anyMatch(v -> {

            // getString() throws exceptions need to catch them
            try {
                return v.getString().equalsIgnoreCase("Thesis");
            } catch (final Exception e) {
                return false;
            }
        });

        if (isThesis) {

            // thesis dc:creator
            values = obj.hasProperty("marcrel:dis") ? obj.getProperty("marcrel:dis").getValues() : null;
            for (int i = 0; values != null && i < values.length; i++) {
                if (!StringUtils.isEmpty(values[i].getString())) {
                    final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                    simple.getContent().add(values[i].getString());
                    oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createCreator(simple));
                }
            }

            // thesis dc:date
            values =
                obj.hasProperty("dcterms:dateAccepted") ? obj.getProperty("dcterms:dateAccepted").getValues() : null;
            for (int i = 0; values != null && i < values.length; i++) {
                if (!StringUtils.isEmpty(values[i].getString())) {
                    final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                    simple.getContent().add(values[i].getString());
                    oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDate(simple));
                }
            }
        } else {

            // non-thesis dc:creator
            values = obj.hasProperty("dcterms:creator") ? obj.getProperty("dcterms:creator").getValues() : null;
            for (int i = 0; values != null && i < values.length; i++) {
                if (!StringUtils.isEmpty(values[i].getString())) {
                    final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                    simple.getContent().add(values[i].getString());
                    oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createCreator(simple));
                }
            }

            // non-thesis dc:date
            values = obj.hasProperty("dcterms:created") ? obj.getProperty("dcterms:created").getValues() : null;
            for (int i = 0; values != null && i < values.length; i++) {
                if (!StringUtils.isEmpty(values[i].getString())) {
                    final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                    simple.getContent().add(values[i].getString());
                    oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDate(simple));
                }
            }
        }

        // dc:contributor
        values = obj.hasProperty("dcterms:contributor") ? obj.getProperty("dcterms:contributor").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createContributor(simple));
            }
        }
        values = obj.hasProperty("marcrel:ths") ? obj.getProperty("marcrel:ths").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createContributor(simple));
            }
        }
        values = obj.hasProperty("ualterms:thesiscommitteemember")
            ? obj.getProperty("ualterms:thesiscommitteemember").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createContributor(simple));
            }
        }

        // dc:publisher (concatenate grantor and discipline/department contents)
        final Value[] depts =
            obj.hasProperty("vivo:AcademicDepartment") ? obj.getProperty("vivo:AcademicDepartment").getValues() : null;
        final Value[] ddgs = obj.hasProperty("marcrel:dgg") ? obj.getProperty("marcrel:dgg").getValues() : null;
        final StringBuilder pub = new StringBuilder();

        // If both marcrel:dgg and vivo:AcademicDepartment are present
        if ((ddgs != null) && (depts != null)) {
            pub.append(ddgs[0].getString());
            for (int i = 0; i < depts.length; i++) {
                pub.append(i == 0 ? "; " : ", ").append(depts[i].getString());
            }
            pub.append(depts.length == 1 ? "." : "");

            // If only vivo:AcademicDepartment is present
        } else if ((ddgs == null) && (depts != null)) {
            pub.append("University of Alberta");
            for (int i = 0; i < depts.length; i++) {
                pub.append(i == 0 ? "; " : ", ").append(depts[i].getString());
            }
            pub.append(depts.length == 1 ? "." : "");

            // If none of marcrel:dgg and vivo:AcademicDepartment are present
        } else if ((ddgs == null) && (depts == null)) {
            pub.append("University of Alberta");

            // Otherwise, print only marcrel:dgg (no punctuation)
        } else if (ddgs != null) {
            pub.append(ddgs[0].getString());
        }
        pub.append(pub.toString().trim().length() == 0 ? "University of Alberta" : "");
        final SimpleLiteral sim = dcFactory.createSimpleLiteral();
        sim.getContent().add(pub.toString());
        oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createPublisher(sim));

        // dc:subject
        values = obj.hasProperty("dcterms:subject") ? obj.getProperty("dcterms:subject").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createSubject(simple));
            }
        }
        values = obj.hasProperty("dcterms:temporal") ? obj.getProperty("dcterms:temporal").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createSubject(simple));
            }
        }
        values = obj.hasProperty("dcterms:spatial") ? obj.getProperty("dcterms:spatial").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createSubject(simple));
            }
        }

        // dc:description (add prefix in content: "Specialization: ")
        values = obj.hasProperty("uatermsid:specialization") ? obj.getProperty("uatermsid:specialization").getValues()
            : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add("Specialization: " + values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDescription(simple));
            }
        }

        // dc:title
        values = obj.hasProperty("dcterms:title") ? obj.getProperty("dcterms:title").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createTitle(simple));
            }
        }

        // dc:description (add prefix in content: "Degree: ")
        values = obj.hasProperty("bibo:ThesisDegree") ? obj.getProperty("bibo:ThesisDegree").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add("Degree: " + values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDescription(simple));
            }
        }

        // dc:identifier
        values = obj.hasProperty("dcterms:identifier") ? obj.getProperty("dcterms:identifier").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(simple));
            }
        }
        values = obj.hasProperty("ualterms:trid") ? obj.getProperty("ualterms:trid").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(simple));
            }
        }
        values = obj.hasProperty("ualterms:ser") ? obj.getProperty("ualterms:ser").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(simple));
            }
        }
        values =
            obj.hasProperty("ualterms:fedora3handle") ? obj.getProperty("ualterms:fedora3handle").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(simple));
            }
        }

        // dc:description
        values = obj.hasProperty("dcterms:description") ? obj.getProperty("dcterms:description").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDescription(simple));
            }
        }

        // dc:description (add prefix in content: "Abstract: ")
        values = obj.hasProperty("dcterms:abstract") ? obj.getProperty("dcterms:abstract").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add("Abstract: " + values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDescription(simple));
            }
        }

        // dc:language
        values = obj.hasProperty("dcterms:language") ? obj.getProperty("dcterms:language").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createLanguage(simple));
            }
        }

        // dc:relation
        values = obj.hasProperty("dcterms:relation") ? obj.getProperty("dcterms:relation").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRelation(simple));
            }
        }
        values = obj.hasProperty("dcterms:isVersionOf") ? obj.getProperty("dcterms:isVersionOf").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRelation(simple));
            }
        }

        // dc:source
        values = obj.hasProperty("dcterms:source") ? obj.getProperty("dcterms:source").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createSource(simple));
            }
        }

        // dc:rights
        values = obj.hasProperty("dcterms:rights") ? obj.getProperty("dcterms:rights").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRights(simple));
            }
        }
        values = obj.hasProperty("dcterms:license") ? obj.getProperty("dcterms:license").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())
                && !values[i].getString().equals("I am required to use/link to a publisher's license")) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRights(simple));
            }
        }

        // dc:format
        values = obj.hasProperty("dcterms:format") ? obj.getProperty("dcterms:format").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            if (!StringUtils.isEmpty(values[i].getString())) {
                final SimpleLiteral simple = dcFactory.createSimpleLiteral();
                simple.getContent().add(values[i].getString());
                oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createFormat(simple));
            }
        }

        return oaiDcFactory.createDc(oaidc);
    }
}
