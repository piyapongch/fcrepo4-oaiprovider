/**
 * Piyapong Charoenwattana
 * Project: fcrepo4-oaiprovider
 * $Id$
 */

package org.fcrepo.oai.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * The MetadataHttpResponseWrapper class.
 *
 * @author <a href="mailto:piyapong.charoenwattana@gmail.com">Piyapong Charoenwattana</a>
 * @version $Revision$ $Date$
 */
public class MetadataHttpResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream bos;

    /**
     * The MetadataHttpResponseWrapper class constructor.
     * @param response
     */
    public MetadataHttpResponseWrapper(final HttpServletResponse response) {
        super(response);
        this.bos = new ByteArrayOutputStream();
    }

    /**
     * The getOutput method.
     * @return
     */
    public byte[] getBuffer() {
        return bos.toByteArray();
    }

    /**
     *
     * @see javax.servlet.ServletResponseWrapper#getOutputStream()
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {

            @Override
            public void write(final int b) throws IOException {
                bos.write(b);
                
            }
            
            @Override
            public void setWriteListener(WriteListener wl) {
            }
            
            @Override
            public boolean isReady() { return false; }
            
        };
    }

}
