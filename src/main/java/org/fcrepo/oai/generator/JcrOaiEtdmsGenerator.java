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

import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.fcrepo.kernel.models.Container;
import org.ndltd.standards.metadata.etdms._1.AuthorityType;
import org.ndltd.standards.metadata.etdms._1.ControlledTextType;
import org.ndltd.standards.metadata.etdms._1.FreeTextType;
import org.ndltd.standards.metadata.etdms._1.ObjectFactory;
import org.ndltd.standards.metadata.etdms._1.Thesis;
import org.ndltd.standards.metadata.etdms._1.Thesis.Contributor;
import org.ndltd.standards.metadata.etdms._1.Thesis.Degree;
import org.ndltd.standards.metadata.etdms._1.Thesis.Description;

/**
 * The JcrOaiEtdmsGenerator class.
 *
 * @author <a href="mailto:piyapong.charoenwattana@gmail.com">Piyapong Charoenwattana</a>
 * @version $Revision$ $Date$
 */
public class JcrOaiEtdmsGenerator {

    private static final ObjectFactory etdmsFactory = new ObjectFactory();

    private String lacIdFormat;

    /**
     * The generate method.
     * 
     * @param session
     * @param obj
     * @param uriInfo
     * @return
     * @throws RepositoryException
     */
    public Thesis generate(final Session session, final Container obj, final UriInfo uriInfo)
        throws RepositoryException {
        final Thesis thesis = etdmsFactory.createThesis();

        // LAC identifier
        thesis.getIdentifier().add(String.format(lacIdFormat, obj.getNode().getName()));

        // degree element
        final Degree degree = etdmsFactory.createThesisDegree();

        final PropertyIterator props = obj.getNode().getProperties();
        while (props.hasNext()) {
            final Property prop = (Property) props.next();
            switch (prop.getName()) {

            case "dcterms:type":
                for (final Value v : prop.getValues()) {
                    addFreeTextType(v, thesis.getType());
                }
                break;

            case "marcrel:dis":
                for (final Value v : prop.getValues()) {
                    addAuthorityType(v, thesis.getCreator());
                }
                break;

            case "dcterms:contributor":
                for (final Value v : prop.getValues()) {
                    addContributor(v, thesis.getContributor(), null);
                }
                break;

            case "marcrel:ths":
                for (final Value v : prop.getValues()) {
                    addContributor(v, thesis.getContributor(), "advisor");
                }
                break;

            case "uatermsid:thesiscommitteemember":
                for (final Value v : prop.getValues()) {
                    addContributor(v, thesis.getContributor(), "committeemember");
                }
                break;

            case "marcrel:dgg":
                for (final Value v : prop.getValues()) {
                    addAuthorityType(v, degree.getGrantor());
                }
                break;

            case "vivo:AcademicDepartment":
                for (final Value v : prop.getValues()) {
                    addFreeTextType(v, degree.getDiscipline());
                }
                break;

            case "dcterms:subject":
                for (final Value v : prop.getValues()) {
                    addControlledTextType(v, thesis.getSubject());
                }
                break;

            case "dcterms:temporal":
                for (final Value v : prop.getValues()) {
                    addControlledTextType(v, thesis.getSubject());
                }
                break;

            case "dcterms:spatial":
                for (final Value v : prop.getValues()) {
                    addControlledTextType(v, thesis.getSubject());
                }
                break;

            case "uatermsid:specialization":
                for (final Value v : prop.getValues()) {
                    addDescription(v, thesis.getDescription(), "Specialization: ");
                }
                break;

            case "dcterms:dateAccepted":
                for (final Value v : prop.getValues()) {
                    thesis.setDate(StringUtils.isEmpty(v.getString()) ? null : v.getString());
                }
                break;

            case "dcterms:title":
                for (final Value v : prop.getValues()) {
                    addFreeTextType(v, thesis.getTitle());
                }
                break;

            case "dcterms:alternative":
                for (final Value v : prop.getValues()) {
                    addFreeTextType(v, thesis.getAlternativeTitle());
                }
                break;

            case "bibo:ThesisDegree":
                for (final Value v : prop.getValues()) {
                    addFreeTextType(v, degree.getName());
                }
                break;

            case "uatermsid:thesislevel":
                for (final Value v : prop.getValues()) {
                    addString(v, degree.getLevel());
                }
                break;

            case "dcterms:identifier":
                for (final Value v : prop.getValues()) {
                    addString(v, thesis.getIdentifier());
                }
                break;

            case "uatermsid:fedora3handle":
                for (final Value v : prop.getValues()) {
                    addString(v, thesis.getIdentifier());
                }
                break;

            case "dcterms:description":
                for (final Value v : prop.getValues()) {
                    addDescription(v, thesis.getDescription(), null);
                }
                break;

            case "dcterms:abstract":
                for (final Value v : prop.getValues()) {
                    addDescription(v, thesis.getDescription(), "Abstract: ");
                }
                break;

            case "dcterms:language":
                for (final Value v : prop.getValues()) {
                    addString(v, thesis.getLanguage());
                }
                break;

            case "dcterms:rights":
                for (final Value v : prop.getValues()) {
                    addFreeTextType(v, thesis.getRights());
                }
                break;

            case "dcterms:license":
                for (final Value v : prop.getValues()) {
                    if (!v.getString().equals("I am required to use/link to a publisher's license")) {
                        addFreeTextType(v, thesis.getRights());
                    }
                }
                break;

            case "dcterms:format":
                for (final Value v : prop.getValues()) {
                    addFreeTextType(v, thesis.getFormat());
                }
                break;

            default:
                break;
            }
        }
        thesis.setDegree(degree);
        return thesis;
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
            final Description desc = etdmsFactory.createThesisDescription();
            desc.setValue(prefix == null ? v.getString() : prefix + v.getString());
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
            text.setValue(v.getString());
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
            level.add(v.getString());
        }
    }

    /**
     * The addContributor method.
     * 
     * @param v
     * @param contributor
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addContributor(final Value v, final List<Contributor> conts, final String role)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            final Contributor cont = new Thesis.Contributor();
            cont.setValue(v.getString());
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
     * @param creator
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addAuthorityType(final Value v, final List<AuthorityType> auths)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            final AuthorityType auth = etdmsFactory.createAuthorityType();
            auth.setValue(v.getString());
            auths.add(auth);
        }
    }

    /**
     * The addFreeTextType method.
     * 
     * @param v
     * @param type
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws ValueFormatException
     */
    private void addFreeTextType(final Value v, final List<FreeTextType> texts)
        throws ValueFormatException, IllegalStateException, RepositoryException {
        if (StringUtils.isNotEmpty(v.getString())) {
            final FreeTextType text = etdmsFactory.createFreeTextType();
            text.setValue(v.getString());
            texts.add(text);
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

}
