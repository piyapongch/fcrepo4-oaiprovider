<?xml version="1.0" encoding="UTF-8"?>
<!--
  * using as input the http://id.loc.gov/vocabulary/iso639-2.rdf
  * output a Java spring map xml structure with org.fcrepo.oai.rdf.LanguageRdf
  * <map><entry key="" value="" />...</map>
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:madsrdf="http://www.loc.gov/mads/rdf/v1#"
  version="2.0"
  >
  <xsl:output 
    method="xml"
    encoding="UTF-8"
    omit-xml-declaration="yes"
    indent="yes"
    />

  <xsl:template match="/">
    <xsl:element name="map">
      <xsl:apply-templates select="rdf:RDF/madsrdf:MADSScheme/madsrdf:hasTopMemberOfMADSScheme/madsrdf:Authority[madsrdf:authoritativeLabel/@xml:lang='en']" />
    </xsl:element>
  </xsl:template>


  <!-- convert to map format -->
  <xsl:template match="madsrdf:Authority">
    <xsl:element name="entry">
      <xsl:attribute name="key">
        <xsl:value-of select="@rdf:about" />
      </xsl:attribute> 
      <xsl:attribute name="value">
        <xsl:value-of select="madsrdf:authoritativeLabel/text()" />
      </xsl:attribute> 
    </xsl:element>
  </xsl:template>

</xsl:stylesheet>
