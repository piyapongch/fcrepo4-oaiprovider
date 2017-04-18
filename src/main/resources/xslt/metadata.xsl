<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<xsl:output method="xml" encoding="utf-8" indent="yes"/>
	

	<xsl:strip-space elements="*"/>
	

	<xsl:template match="@* | node()">
		<xsl:copy copy-namespaces="no">
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>
	
	
	<xsl:template match="*[local-name() = 'OAI-PMH']">
		<xsl:element name="OAI-PMH" namespace="http://www.openarchives.org/OAI/2.0/">
			<xsl:attribute name="xsi:schemaLocation">http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd</xsl:attribute>
			<xsl:apply-templates select="node()"/>
		</xsl:element>
	</xsl:template>
	
	
	<xsl:template match="*[local-name() = 'oai-identifier']">
		<xsl:copy copy-namespaces="no">
			<xsl:namespace name="oai-id">http://www.openarchives.org/OAI/2.0/oai-identifier</xsl:namespace>
			<xsl:attribute name="xsi:schemaLocation">http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd</xsl:attribute>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>


    <xsl:template match="*[local-name()='dc']">
    	<xsl:element name="oai_dc:dc" namespace="http://www.openarchives.org/OAI/2.0/oai_dc/">
		    <xsl:namespace name="oai_dc">http://www.openarchives.org/OAI/2.0/oai_dc/</xsl:namespace>
			<xsl:namespace name="dc">http://purl.org/dc/elements/1.1/</xsl:namespace>
		    <xsl:namespace name="xsi2">http://www.w3.org/2001/XMLSchema-instance</xsl:namespace>
		    <xsl:attribute name="xsi2:schemaLocation" namespace="http://www.w3.org/2001/XMLSchema-instance">http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd</xsl:attribute>
			<xsl:apply-templates select="node()"/>
		</xsl:element>
	</xsl:template>
	

    <xsl:template match="*[local-name()='thesis']">
		<xsl:copy copy-namespaces="no">
		    <xsl:namespace name="etd_ms">http://www.ndltd.org/standards/metadata/etdms/1.0/</xsl:namespace>
			<xsl:namespace name="xsi2">http://www.w3.org/2001/XMLSchema-instance</xsl:namespace>
		    <xsl:attribute name="xsi2:schemaLocation" namespace="http://www.w3.org/2001/XMLSchema-instance">http://www.ndltd.org/standards/metadata/etdms/1.0/ http://www.ndltd.org/standards/metadata/etdms/1.0/etdms.xsd</xsl:attribute>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>
	
	
	<xsl:template match="//@xsi:*">
		<xsl:attribute name="other:{local-name()}" namespace="{namespace-uri()}">
			<xsl:value-of select="."/>
		</xsl:attribute>
	</xsl:template>
	
	
	<!-- Replace full datetime with year -->
	<xsl:template match="*[local-name()='date']/text()">
		<xsl:choose>
			<xsl:when test="matches(.,'T\d\d:\d\d:\d\dZ')">
				<xsl:value-of select="replace(.,'(.*)T\d\d:\d\d:\d\dZ','$1')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="replace(.,'/','-')"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	

</xsl:stylesheet>
