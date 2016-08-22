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
package org.fcrepo.oai.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.openarchives.oai._2.VerbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

/**
 * The MetadataXsltFilter class is a servlet filter that transforms oai response metadata using xslt to add namespaces
 * on the metadata elements.
 *
 * @author Piyapong Charoenwattana
 */
@WebFilter(filterName = "MetadataXsltFilter", urlPatterns = { "/rest/oai" }, initParams = {
    @WebInitParam(name = "xslPath", value = "/xslt/metadata.xsl") })
public class MetadataXsltFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(MetadataXsltFilter.class);
    private String xslPath;
    private TransformerFactory factory;
    private Templates templates;

    /**
     *
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        factory = TransformerFactory.newInstance();
        xslPath = filterConfig.getInitParameter("xslPath");
        try {
            log.debug("creating templates using " + factory.toString() + "...");
            templates = factory.newTemplates(new StreamSource(this.getClass().getResourceAsStream(xslPath)));
        } catch (final Exception e) {
            throw new ServletException("Could not initialize filter!", e);
        }
    }

    /**
     *
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
     *      javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        final String vb = request.getParameter("verb");
        final String mp = request.getParameter("metadataPrefix");
        if (vb.equals(VerbType.LIST_RECORDS.value()) || vb.equals(VerbType.GET_RECORD.value())) {
            final MetadataHttpResponseWrapper wrapper = new MetadataHttpResponseWrapper((HttpServletResponse) response);
            chain.doFilter(request, wrapper);
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            final PrintWriter out = response.getWriter();
            final Source xmlSource = new StreamSource(new ByteArrayInputStream(wrapper.getBuffer()));
            try {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final StreamResult result = new StreamResult(bos);
                final Stopwatch timer = Stopwatch.createStarted();
                final Transformer transformer = templates.newTransformer();
                transformer.transform(xmlSource, result);
                log.debug(vb.toString() + (mp == null ? "" : " / " + mp) + " transformation took: " + timer);
                response.setContentLength(bos.size());
                out.write(bos.toString());
            } catch (final Exception e) {
                log.error("Could not transform OAI response!", e);
                out.write(new String(wrapper.getBuffer()));
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     *
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }
}
