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

        // dc:identifier
        final ElementType valId = dcFactory.createElementType();
        valId.setValue(escape(converter.toDomain(obj.getPath()).getURI()));
        oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createIdentifier(valId));

        // dc:date
        final ElementType valCreated = dcFactory.createElementType();
        valCreated.setValue(escape(obj.getCreatedDate().toString()));
        oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createDate(valCreated));

        // dc:creator
        values = obj.hasProperty("dc:creator") ? obj.getProperty("dc:creator").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            final ElementType type = dcFactory.createElementType();
            type.setValue(escape(values[i].getString()));
            oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createCreator(type));
        }

        // dc:title
        values = obj.hasProperty("dc:title") ? obj.getProperty("dc:title").getValues() : null;
        for (int i = 0; values != null && i < values.length; i++) {
            final ElementType type = dcFactory.createElementType();
            type.setValue(escape(values[i].getString()));
            oaidc.getTitleOrCreatorOrSubject().add(dcFactory.createTitle(type));
        }

        // TODO: add all dc elements

        return oaiDcFactory.createDc(oaidc);
    }

    private String escape(final String orig) {
        return StringEscapeUtils.escapeXml(orig);
    }
}
