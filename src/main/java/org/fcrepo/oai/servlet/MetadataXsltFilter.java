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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.openarchives.oai._2.VerbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

/**
 * The MetadataXsltFilter class.
 *
 * @author Piyapong Charoenwattana
 */
@WebFilter(filterName = "MetadataXsltFilter", urlPatterns = { "/rest/oai" }, initParams = {
    @WebInitParam(name = "xslPath", value = "/xslt/metadata-2.0.xsl") })
public class MetadataXsltFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(MetadataXsltFilter.class);
    private String xslPath;
    private TransformerFactory factory;
    private StreamSource xslSource;
    private Transformer transformer;

    /**
     *
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        this.factory = TransformerFactory.newInstance();
        this.xslPath = filterConfig.getInitParameter("xslPath");
        this.xslSource = new StreamSource(this.getClass().getResourceAsStream(xslPath));
        try {
            transformer = factory.newTransformer(xslSource);
        } catch (final TransformerConfigurationException e) {
            log.error("Could not create transformer!", e);
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
        final String verb = request.getParameter("verb");
        if (verb.equals(VerbType.LIST_RECORDS.value()) || verb.equals(VerbType.GET_RECORD.value())) {
            final PrintWriter out = response.getWriter();
            final BufferedHttpResponseWrapper wrapper = new BufferedHttpResponseWrapper((HttpServletResponse) response);
            chain.doFilter(request, wrapper);
            final String resp = new String(wrapper.getBuffer());
            final Source xmlSource = new StreamSource(new StringReader(resp));
            try {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final StreamResult result = new StreamResult(bos);
                final Stopwatch timer = Stopwatch.createStarted();
                transformer.transform(xmlSource, result);
                log.debug("transformation took: " + timer);
                final String rs = new String(bos.toByteArray());
                response.setContentType("text/xml");
                response.setContentLength(rs.length());
                out.write(new String(rs));
            } catch (final Exception ex) {
                out.println(ex.toString());
                out.write(resp);
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
