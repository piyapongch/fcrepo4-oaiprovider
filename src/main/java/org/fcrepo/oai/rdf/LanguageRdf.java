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

package org.fcrepo.oai.rdf;

import java.util.Map;

/**
 * Handler for ISO 639-2 RDF - Language codes
 * 
 * @author J
 */

public class LanguageRdf {

    private Map<String, String> languageMap;


    /**
     * language url to literal mapping
     *
     * @param langMap a Map object containing the url to literal mapping
     */
    public void setLanguageMap(final Map<String, String> langMap) {
      this.languageMap = langMap;
    }

    /**
     * language url to literal mapping
     *
     * @param url iso 639-2 url
     * @return the string literal or null if not present
     */
    public String getLiteralFromUrl(final String url) {
      String ret = null;
      if (url != null && this.languageMap != null) {
        ret = this.languageMap.get(url);
      }
      return ret;
    }

}
