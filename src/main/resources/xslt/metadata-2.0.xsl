<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:oai_etdms="http://www.ndltd.org/standards/metadata/etdms/1.0/">
	<xsl:output method="xml" encoding="utf-8" indent="yes"/>
	
	<xsl:strip-space elements="*"/>
	

	<xsl:template match="@* | node()">
		<xsl:copy copy-namespaces="no" inherit-namespaces="no">
			<xsl:if test="local-name()='OAI-PMH'">
				<xsl:namespace name="oai_dc">http://www.openarchives.org/OAI/2.0/oai_dc</xsl:namespace>
				<xsl:namespace name="oai-id">http://www.openarchives.org/OAI/2.0/oai-identifier</xsl:namespace>
				<xsl:namespace name="oai_etdms">http://www.openarchives.org/OAI/2.0/oai_etdms</xsl:namespace>
				<xsl:attribute name="xsi:schemaLocation">http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd
					http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates select="node()"/>
		</xsl:copy>
	</xsl:template>
	

	<xsl:template match="oai_dc:dc">
		<xsl:copy copy-namespaces="no" xpath-default-namespace="http://www.openarchives.org/OAI/2.0/oai_dc/">
			<xsl:namespace name="oai_dc">http://www.openarchives.org/OAI/2.0/oai_dc/</xsl:namespace>
			<xsl:namespace name="dc">http://purl.org/dc/elements/1.1/</xsl:namespace>
			<xsl:namespace name="xsi">http://www.w3.org/2001/XMLSchema-instance</xsl:namespace>
			<xsl:attribute name="xsi:schemaLocation">http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd</xsl:attribute>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>
	
	
	<!--<xsl:template match="oai_etdms:thesis">
		<xsl:copy copy-namespaces="no">
			<xsl:namespace name="oai_etdms">"http://www.ndltd.org/standards/metadata/etdms/1.0/"</xsl:namespace>
			<xsl:namespace name="thesis">http://www.ndltd.org/standards/metadata/etdms/1.0/</xsl:namespace>
			<xsl:namespace name="xsi">http://www.w3.org/2001/XMLSchema-instance</xsl:namespace>
			<xsl:attribute name="xsi:schemaLocation">http://www.ndltd.org/standards/metadata/etdms/1.0/ http://www.ndltd.org/standards/metadata/etdms/1-0/etdms.xsd</xsl:attribute>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>-->


</xsl:stylesheet>
