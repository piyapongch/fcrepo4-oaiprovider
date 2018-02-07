/**
 * Piyapong Charoenwattana
 * Project: fcrepo-oaiprovider
 * $Id$
 */

package org.fcrepo.oai.generator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
     * @param s the DIO full uri format
     */
    public final void setDoiFullUriFormat(final String s) {
        this.doiFullUriFormat = s;
    }

    /**
     * Given the JCR DOI (doi:10.7939/FEXDSF) convert to the full DOI URI https://doi.org/10.7939/FEXDSF
     *
     * @param ualibDoi the ualid:doi field within JCR
     * @return the full DOI uri
     */
    protected final String formatUalidDoi(final String ualibDoi) {
        String fullDoiUrl = null;
        if (StringUtils.isNotEmpty(ualibDoi) && ualibDoi.length() >= 4) {
            fullDoiUrl = String.format(doiFullUriFormat, ualibDoi.substring(4));
        }
        return fullDoiUrl;
    }

    /**
     * given a FedoraBinary file path, return the PCDM fileSet portion
     * e.g., /dev/e9/87/8b/5e/e9878b5e-354f-4f77-b3b2-6caa0175b656/files/1bece7c9-d7d8-4d17-967e-d9836113ff0c
     * grab "e9878b5e-354f-4f77-b3b2-6caa0175b656"
     *
     * @param path FedoraBinary file path
     * @return String representing the FileSet UUID
     */
    protected final String getFileSetFromPath(final String path) {
        String ret = "";
        final Pattern fileSetPattern = Pattern.compile(".*\\/([^/]*)\\/files\\/.*");
        final Matcher matcher = fileSetPattern.matcher(path);
        while (matcher.find()) {
            ret = matcher.group(1);
        }
        return ret;
    }

}
