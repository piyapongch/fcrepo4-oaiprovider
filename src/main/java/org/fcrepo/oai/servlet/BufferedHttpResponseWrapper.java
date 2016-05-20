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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * The BufferedHttpResponseWrapper class.
 *
 * @author <a href="mailto:piyapong.charoenwattana@gmail.com">Piyapong Charoenwattana</a>
 * @version $Revision$ $Date$
 */
public class BufferedHttpResponseWrapper extends HttpServletResponseWrapper {

    private final BufferedServletOutputStream bufferedServletOut = new BufferedServletOutputStream();
    private PrintWriter printWriter = null;
    private ServletOutputStream outputStream = null;

    /**
     * The BufferedHttpResponseWrapper class constructor.
     * @param origResponse
     */
    public BufferedHttpResponseWrapper(final HttpServletResponse origResponse) {
        super(origResponse);
    }

    /**
     * The getBuffer method.
     * @return
     */
    public byte[] getBuffer() {
        return this.bufferedServletOut.getBuffer();
    }

    /**
     *
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.outputStream != null) {
            throw new IllegalStateException(
                "The Servlet API forbids calling getWriter() after getOutputStream() has been called");
        }

        if (this.printWriter == null) {
            this.printWriter = new PrintWriter(this.bufferedServletOut);
        }
        return this.printWriter;
    }

    /**
     *
     * @see javax.servlet.ServletResponseWrapper#getOutputStream()
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.printWriter != null) {
            throw new IllegalStateException(
                "The Servlet API forbids calling getOutputStream() after getWriter() has been called");
        }

        if (this.outputStream == null) {
            this.outputStream = this.bufferedServletOut;
        }
        return this.outputStream;
    }

    /**
     *
     * @see javax.servlet.ServletResponseWrapper#flushBuffer()
     */
    @Override
    public void flushBuffer() throws IOException {
        if (this.outputStream != null) {
            this.outputStream.flush();
        } else if (this.printWriter != null) {
            this.printWriter.flush();
        }
    }

    /**
     *
     * @see javax.servlet.ServletResponseWrapper#getBufferSize()
     */
    @Override
    public int getBufferSize() {
        return this.bufferedServletOut.getBuffer().length;
    }

    /**
     *
     * @see javax.servlet.ServletResponseWrapper#reset()
     */
    @Override
    public void reset() {
        this.bufferedServletOut.reset();
    }

    /**
     *
     * @see javax.servlet.ServletResponseWrapper#resetBuffer()
     */
    @Override
    public void resetBuffer() {
        this.bufferedServletOut.reset();
    }

    /**
     *
     * @see javax.servlet.ServletResponseWrapper#setBufferSize(int)
     */
    @Override
    public void setBufferSize(final int size) {
        this.bufferedServletOut.setBufferSize(size);
    }
}
