<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/">
	<xsl:output method="xml" encoding="utf-8" indent="yes"/>
	<xsl:strip-space elements="*"/>

	<xsl:template match="OAI-PMH">
		<OAI-PMH xmlns:oai-id="http://www.openarchives.org/OAI/2.0/oai-identifier"
			xmlns="http://www.openarchives.org/OAI/2.0/"
			xmlns:etd_ms="http://www.ndltd.org/standards/metadata/etdms/1.0/"
			xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd
			http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd
			http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd
			http://www.ndltd.org/standards/metadata/etdms/1.0/ http://www.ndltd.org/standards/metadata/etdms/1-0/etdms.xsd">
			<xsl:apply-templates select="@* | node()"/>
		</OAI-PMH>
	</xsl:template>

	<xsl:template match="@* | node()">
		<xsl:copy>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="oai_dc:dc">
		<oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
			xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
			<xsl:apply-templates select="*"/>
		</oai_dc:dc>
	</xsl:template>

	<xsl:template match="oai_dc:dc/*">
		<xsl:copy>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
