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

import javax.servlet.ServletOutputStream;

/**
 * A custom servlet output stream that stores its data in a buffer, rather than sending it directly to the client.
 *
 * @author Eric M. Burke
 */
public class BufferedServletOutputStream extends ServletOutputStream {
    // the actual buffer
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();

    /**
     * @return the contents of the buffer.
     */
    public byte[] getBuffer() {
        return this.bos.toByteArray();
    }

    /**
     * This method must be defined for custom servlet output streams.
     */
    @Override
    public void write(final int data) {
        this.bos.write(data);
    }

    // BufferedHttpResponseWrapper calls this method
    /**
     * The reset method.
     */
    public void reset() {
        this.bos.reset();
    }

    // BufferedHttpResponseWrapper calls this method
    /**
     * The setBufferSize method.
     * @param size
     */
    public void setBufferSize(final int size) {
        // no way to resize an existing ByteArrayOutputStream
        this.bos = new ByteArrayOutputStream(size);
    }
}
