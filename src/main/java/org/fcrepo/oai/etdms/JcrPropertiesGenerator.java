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
package org.fcrepo.oai.etdms;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.StringEscapeUtils;
import org.fcrepo.kernel.models.Container;
import org.openarchives.oai._2_0.oai_dc.OaiDcType;

/**
 * The type Jcr properties generator.
 *
 * @author Frank Asseg
 */
public class JcrPropertiesGenerator {

    /**
     * Generate OAI ETD-MS
     *
     * @param session the session
     * @param obj the obj
     * @param uriInfo the uri info
     * @return the jAXB element
     * @throws RepositoryException if repository exception occurred
     */
    public JAXBElement<OaiDcType> generateEtdms(final Session session, final Container obj, final UriInfo uriInfo)
        throws RepositoryException {
        return null;
    }

    private String escape(final String orig) {
        return StringEscapeUtils.escapeXml(orig);
    }
}
