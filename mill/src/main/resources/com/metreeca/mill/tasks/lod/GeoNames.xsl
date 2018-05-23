<?xml version="1.0" encoding="UTF-8"?>

<!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Copyright © 2013-2018 Metreeca srl. All rights reserved.

  This file is part of Metreeca.

  Metreeca is free software: you can redistribute it and/or modify it under the terms
  of the GNU Affero General Public License as published by the Free Software Foundation,
  either version 3 of the License, or(at your option) any later version.

  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License along with Metreeca.
  If not, see <http://www.gnu.org/licenses/>.
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->

<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
		xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
		xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
		xmlns:skos="http://www.w3.org/2004/02/skos/core#">

	<!-- !!!  <adminCode1 ISO3166-2="CUN">33</adminCode1> -->

	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>


	<xsl:variable name="wikipedia" select="'http://en.wikipedia.org/wiki/'"/>
	<xsl:variable name="dbpedia" select="'http://dbpedia.org/resource/'"/>


	<xsl:template match="/geonames">
		<xsl:apply-templates select="geoname"/>
	</xsl:template>

	<xsl:template match="geoname">
		<GeoNames rdf:about="http://www.geonames.org/{string(geonameId)}">
			<rank rdf:datatype="http://www.w3.org/2001/XMLSchema#integer">
				<xsl:value-of select="position()"/>
			</rank>
			<xsl:apply-templates/>
		</GeoNames>
	</xsl:template>

	<xsl:template match="lat|lng|elevation[string() != '']|score|bbox/*">
		<xsl:copy>
			<xsl:attribute name="rdf:datatype">http://www.w3.org/2001/XMLSchema#decimal</xsl:attribute>
			<xsl:value-of select="string()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="population">
		<xsl:copy>
			<xsl:attribute name="rdf:datatype">http://www.w3.org/2001/XMLSchema#integer</xsl:attribute>
			<xsl:value-of select="string()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="bbox">
		<xsl:copy>
			<xsl:attribute name="rdf:parseType">Resource</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="timezone">
		<xsl:copy>
			<xsl:attribute name="rdf:parseType">Resource</xsl:attribute>
			<name>
				<xsl:value-of select="string()"/>
			</name>
			<dstOffset>
				<xsl:value-of select="@dstOffset"/>
			</dstOffset>
			<gmtOffset>
				<xsl:value-of select="@gmtOffset"/>
			</gmtOffset>
		</xsl:copy>

	</xsl:template>

	<xsl:template match="alternateNames"/>

	<xsl:template match="officialName[@lang]|alternateName[@lang]">

		<!-- !!! handle isPreferredName/isShortName attributes -->

		<xsl:copy>
			<xsl:attribute name="xml:lang">
				<xsl:value-of select="@lang"/>
			</xsl:attribute>
			<xsl:value-of select="string()"/>
		</xsl:copy>

	</xsl:template>

	<xsl:template match="alternateName[@lang='link']" priority="1">
		<rdfs:seeAlso rdf:resource="{string()}"/>
	</xsl:template>

	<xsl:template match="alternateName[@lang='link' and starts-with(string(), $wikipedia)]" priority="2">
		<rdfs:seeAlso rdf:resource="{string()}"/>
		<skos:exactMatch rdf:resource="{concat($dbpedia, substring-after(string(), $wikipedia))}"/>
	</xsl:template>

	<xsl:template match="*">
		<xsl:if test="node()">
			<xsl:copy>
				<xsl:apply-templates select="@*|node()"/>
			</xsl:copy>
		</xsl:if>
	</xsl:template>

	<xsl:template match="text()">
		<xsl:copy/>
	</xsl:template>

</xsl:transform>
