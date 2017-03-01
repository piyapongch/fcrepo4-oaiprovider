/**
 * Piyapong Charoenwattana
 * Project: fcrepo-oaiprovider
 * $Id$
 */

package org.fcrepo.oai.generator;

import org.apache.commons.lang3.StringUtils;

/**
 * The JcrOaiGenerator class.
 *
 * @author <a href="mailto:piyapong.charoenwattana@gmail.com">Piyapong Charoenwattana</a>
 * @version $Revision$ $Date$
 */
public class JcrOaiGenerator {

    protected String eraIdFormat;
    protected String doiFullUriFormat;

    /**
     * The setEraIdFormat setter method.
     *
     * @param eraIdFormat the eraIdFormat to set
     */
    public final void setEraIdFormat(final String eraIdFormat) {
        this.eraIdFormat = eraIdFormat;
    }

    /**
     * The setFullDoiUrl setter method.
     *
     */
    public final void setDoiFullUriFormat(final String s) {
        this.doiFullUriFormat = s;
    }

    /**
     * Given the JCR DOI (doi:10.7939/FEXDSF) convert to the full DOI URI https://doi.org/10.7939/FEXDSF
     *
     * @param ualidDoi the ualid:doi field within JCR
     *
     */
    protected final String formatUalidDoi(final String ualibDoi) {
        String fullDoiUrl = null;
        if (StringUtils.isNotEmpty(ualibDoi) && ualibDoi.length() >= 4) {
            fullDoiUrl = String.format(doiFullUriFormat, ualibDoi.substring(4));
        }
        return fullDoiUrl;
    }

}
