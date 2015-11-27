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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.StringEscapeUtils;
import org.fcrepo.http.api.FedoraNodes;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.kernel.models.Container;
import org.openarchives.oai._2_0.oai_dc.OaiDcType;
import org.purl.dc.elements._1.ElementType;
import org.purl.dc.elements._1.ObjectFactory;

/**
 * The type Jcr properties generator.
 *
 * @author Frank Asseg
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
     */
    public JAXBElement<OaiDcType> generateDC(final Session session, final Container obj, final UriInfo uriInfo)
        throws RepositoryException {

        final HttpResourceConverter converter =
            new HttpResourceConverter(session, uriInfo.getBaseUriBuilder().clone().path(FedoraNodes.class));
        final OaiDcType oaidc = oaiDcFactory.createOaiDcType();
        Value[] values;

        // dc:title
        values = obj.hasProperty("dc:title") ? obj.getProperty("dc:title").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            final ElementType type = dcFactory.createElementType();
            type.setValue(escape(values[i].getString()));
            oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createTitle(type));
        }

        // // dc:type
        // values = obj.hasProperty("dcterms:type") ? obj.getProperty("dcterms:type").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createType(type));
        // }
        //
        // // dc:creator
        // values = obj.hasProperty("dcterms:creator") ? obj.getProperty("dcterms:creator").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createCreator(type));
        // }
        // values = obj.hasProperty("marcrel:dis") ? obj.getProperty("marcrel:dis").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createCreator(type));
        // }
        //
        // // dc:contributor
        // values = obj.hasProperty("dcterms:contributor") ? obj.getProperty("dcterms:contributor").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createContributor(type));
        // }
        // values = obj.hasProperty("marcrel:ths") ? obj.getProperty("marcrel:ths").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createContributor(type));
        // }
        // values = obj.hasProperty("ualterms:thesiscommitteemember")
        // ? obj.getProperty("ualterms:thesiscommitteemember").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createContributor(type));
        // }
        //
        // // dc:publisher (concatenate grantor and discipline/department contents)
        // final Value[] vals =
        // obj.hasProperty("vivo:AcademicDepartment") ? obj.getProperty("vivo:AcademicDepartment").getValues() : null;
        // values = obj.hasProperty("marcrel:dgg") ? obj.getProperty("marcrel:dgg").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(
        // escape(values[i].getString() + vals != null && vals[i] != null ? " " + vals[i].getString() : ""));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createPublisher(type));
        // }
        //
        // // dc:subject
        // values = obj.hasProperty("dcterms:subject") ? obj.getProperty("dcterms:subject").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createSubject(type));
        // }
        // values = obj.hasProperty("dcterms:temporal") ? obj.getProperty("dcterms:temporal").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createSubject(type));
        // }
        // values = obj.hasProperty("dcterms:spatial") ? obj.getProperty("dcterms:spatial").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createSubject(type));
        // }
        //
        // // dc:description (add prefix in content: "Specialization: ")
        // values =
        // obj.hasProperty("ualterms:specialization") ? obj.getProperty("ualterms:specialization").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Specialization: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDescription(type));
        // }
        //
        // // dc:date
        // values = obj.hasProperty("dcterms:created") ? obj.getProperty("dcterms:created").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDate(type));
        // }
        // values = obj.hasProperty("dcterms:dateAccepted") ? obj.getProperty("dcterms:dateAccepted").getValues() :
        // null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDate(type));
        // }
        //
        // // dc:title
        // values = obj.hasProperty("dcterms:title") ? obj.getProperty("dcterms:title").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createTitle(type));
        // }
        //
        // // dc:description (add prefix in content: "Degree: ")
        // values = obj.hasProperty("bibo:ThesisDegree") ? obj.getProperty("bibo:ThesisDegree").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Degree: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDescription(type));
        // }
        //
        // // dc:identifier
        // values = obj.hasProperty("dcterms:identifier") ? obj.getProperty("dcterms:identifier").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Degree: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(type));
        // }
        // values = obj.hasProperty("ualterms:trid") ? obj.getProperty("ualterms:trid").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Degree: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(type));
        // }
        // values = obj.hasProperty("ualterms:ser") ? obj.getProperty("ualterms:ser").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Degree: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(type));
        // }
        // values =
        // obj.hasProperty("ualterms:fedora3handle") ? obj.getProperty("ualterms:fedora3handle").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Degree: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(type));
        // }
        //
        // // dc:description
        // values = obj.hasProperty("dcterms:description") ? obj.getProperty("dcterms:description").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Degree: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDescription(type));
        // }
        //
        // // dc:description (add prefix in content: "Abstract: ")
        // values = obj.hasProperty("dcterms:abstract") ? obj.getProperty("dcterms:abstract").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Abstract: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDescription(type));
        // }
        //
        // // dc:language
        // values = obj.hasProperty("dcterms:language") ? obj.getProperty("dcterms:language").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Abstract: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createLanguage(type));
        // }
        //
        // // dc:relation
        // values = obj.hasProperty("dcterms:relation") ? obj.getProperty("dcterms:relation").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape(values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRelation(type));
        // }
        // values = obj.hasProperty("dcterms:isVersionOf") ? obj.getProperty("dcterms:isVersionOf").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Abstract: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRelation(type));
        // }
        //
        // // dc:source
        // values = obj.hasProperty("dcterms:source") ? obj.getProperty("dcterms:source").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Abstract: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createSource(type));
        // }
        //
        // // dc:rights
        // values = obj.hasProperty("dcterms:rights") ? obj.getProperty("dcterms:rights").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Abstract: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRights(type));
        // }
        // values = obj.hasProperty("dcterms:license") ? obj.getProperty("dcterms:license").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Abstract: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRights(type));
        // }
        //
        // // dc:format
        // values = obj.hasProperty("dcterms:format") ? obj.getProperty("dcterms:format").getValues() : null;
        // for (int i = 0; values != null && i < values.length; i++) {
        // final ElementType type = dcFactory.createElementType();
        // type.setValue(escape("Abstract: " + values[i].getString()));
        // oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createRights(type));
        // }

        return oaiDcFactory.createDc(oaidc);
    }

    private String escape(final String orig) {
        return StringEscapeUtils.escapeXml(orig);
    }
}
