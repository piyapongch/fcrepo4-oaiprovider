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

import javax.jcr.Session;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.models.Container;
import org.ndltd.standards.metadata.etdms._1.FreeTextType;
import org.ndltd.standards.metadata.etdms._1.ObjectFactory;
import org.ndltd.standards.metadata.etdms._1.Thesis;

/**
 * The JcrOaiEtdmsGenerator class.
 *
 * @author <a href="mailto:piyapong.charoenwattana@gmail.com">Piyapong Charoenwattana</a>
 * @version $Revision$ $Date$
 */
public class JcrOaiEtdmsGenerator {

    private static final ObjectFactory etdmsFactory = new ObjectFactory();

    /**
     * The JcrOaiEtdmsGenerator class constructor.
     */
    public JcrOaiEtdmsGenerator() {
        // TODO: Implement this constructor.
    }

    /**
     * The generate method.
     * @param session
     * @param obj
     * @param uriInfo
     * @return
     */
    public Thesis generate(final Session session, final Container obj, final UriInfo uriInfo) {
        final Thesis thesis = etdmsFactory.createThesis();
        final FreeTextType title = etdmsFactory.createFreeTextType();
        title.setValue("test");
        thesis.getTitle().add(title);
        return thesis;
    }

}
