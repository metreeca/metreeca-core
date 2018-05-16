<?xml version="1.0" encoding="UTF-8"?>

<!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Copyright © 2013-2018 Metreeca srl. All rights reserved.

  This file is part of Metreeca.

  Metreeca is free software: you can redistribute it and/or modify it
  under the terms of the GNU Affero General Public License as published
  by the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Metreeca is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty
  of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->

<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
		xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
>

	<xsl:template match="/">
		<xsl:apply-templates select="GeocodeResponse/result"/>
	</xsl:template>

	<xsl:template match="result">
		<GeoGoogle>
			<xsl:apply-templates/>
		</GeoGoogle>
	</xsl:template>

	<xsl:template match="result/*[not(*)]">
		<xsl:copy>
			<xsl:value-of select="string()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="result/*[*]">
		<xsl:copy>
			<xsl:attribute name="rdf:parseType">Resource</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="result/geometry" priority="1"/>

	<xsl:template match="*">
		<xsl:copy>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="text()">
		<xsl:copy/>
	</xsl:template>

</xsl:transform>
