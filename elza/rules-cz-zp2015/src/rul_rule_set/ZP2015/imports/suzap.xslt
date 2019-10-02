<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:ap="http://www.mvcr.cz/archivy/evidence-nad/ap">

<xsl:key name="evidencni_jednotky" match="ap:eviJednotka" use="concat(normalize-space(./ap:druhEVJ),normalize-space(./ap:cisloEVJ))"/>

<xsl:template match="ap:lokatorR">
    <record><xsl:value-of select="../@aID"/></record>
</xsl:template>

<xsl:template name="format">
    <xsl:param name="datum"/>
    <xsl:choose>
  <xsl:when test="string-length($datum) &lt; 5">Y</xsl:when>
  <xsl:when test="string-length($datum) &lt; 8">YM</xsl:when>
  <xsl:when test="string-length($datum) &lt; 11">YMD</xsl:when>
  <xsl:otherwise>error</xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template name="printISO8601">
    <xsl:param name="aNormDat"/>
    <xsl:param name="begin"/>
    <xsl:choose>
  <xsl:when test="$begin='1'">
            <xsl:choose>
    <xsl:when test="string-length($aNormDat) = 4"><xsl:value-of select="concat($aNormDat,'-01-01T00:00:00Z')"/></xsl:when>
    <xsl:when test="string-length($aNormDat) = 7"><xsl:value-of select="concat($aNormDat,'-01T00:00:00Z')"/></xsl:when>
    <xsl:when test="string-length($aNormDat) = 10"><xsl:value-of select="concat($aNormDat,'T00:00:00Z')"/></xsl:when>
    <xsl:otherwise>error</xsl:otherwise>
      </xsl:choose>
  </xsl:when>
  <xsl:otherwise>
            <xsl:choose>
    <xsl:when test="string-length($aNormDat) = 4"><xsl:value-of select="concat($aNormDat,'-12-31T23:59:59Z')"/></xsl:when>
    <xsl:when test="string-length($aNormDat) = 7"><xsl:value-of select="concat($aNormDat,'-31T23:59:59Z')"/></xsl:when>
    <xsl:when test="string-length($aNormDat) = 10"><xsl:value-of select="concat($aNormDat,'T23:59:59Z')"/></xsl:when>
    <xsl:otherwise>error</xsl:otherwise>
      </xsl:choose>
  </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template match="ap:datum">
    <desc-item-unit-date calendar-type-code="GREGORIAN">
  <xsl:choose>
      <xsl:when test="@aTypDat='priorum'">
    <xsl:attribute name="desc-item-type-code">ZP2015_UNIT_DATE_PRE</xsl:attribute>
      </xsl:when>
      <xsl:when test="@aTypDat='posteriorum'">
    <xsl:attribute name="desc-item-type-code">ZP2015_UNIT_DATE_POST</xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
    <xsl:attribute name="desc-item-type-code">ZP2015_UNIT_DATE</xsl:attribute>
      </xsl:otherwise>
  </xsl:choose>
  <position>1</position>
  <xsl:choose>
      <xsl:when test="substring-before(./@aNormDat,'/')">
    <value-from><xsl:call-template name="printISO8601"><xsl:with-param name="aNormDat" select="substring-before(./@aNormDat,'/')"/><xsl:with-param name="begin">1</xsl:with-param></xsl:call-template></value-from>
      </xsl:when>
      <xsl:otherwise>
    <value-from><xsl:call-template name="printISO8601"><xsl:with-param name="aNormDat" select="./@aNormDat"/><xsl:with-param name="begin">1</xsl:with-param></xsl:call-template></value-from>
      </xsl:otherwise>
  </xsl:choose>
  <xsl:choose>
      <xsl:when test="substring-after(./@aNormDat,'/')">
    <value-to><xsl:call-template name="printISO8601"><xsl:with-param name="aNormDat" select="substring-after(./@aNormDat,'/')"/></xsl:call-template></value-to>
      </xsl:when>
      <xsl:otherwise>
    <value-to><xsl:call-template name="printISO8601"><xsl:with-param name="aNormDat" select="./@aNormDat"/></xsl:call-template></value-to>
      </xsl:otherwise>
  </xsl:choose>
  <xsl:choose>
      <xsl:when test="@aOdhadDat='ano'">
    <value-from-estimated>true</value-from-estimated>
    <value-to-estimated>true</value-to-estimated>
      </xsl:when>
      <xsl:when test="@aOdhadDat='ano-ne'">
    <value-from-estimated>true</value-from-estimated>
    <value-to-estimated>false</value-to-estimated>
      </xsl:when>
      <xsl:when test="@aOdhadDat='ne-ano'">
    <value-from-estimated>false</value-from-estimated>
    <value-to-estimated>true</value-to-estimated>
      </xsl:when>
      <xsl:otherwise>
    <value-from-estimated>false</value-from-estimated>
    <value-to-estimated>false</value-to-estimated>
      </xsl:otherwise>
  </xsl:choose>

  <xsl:choose>
      <xsl:when test="contains(./@aNormDat,'/')">
    <format><xsl:if test="substring-before(./@aNormDat,'/')"><xsl:call-template name="format"><xsl:with-param name="datum"><xsl:value-of select="substring-before(./@aNormDat,'/')"/></xsl:with-param></xsl:call-template></xsl:if>-<xsl:if test="substring-after(./@aNormDat,'/')"><xsl:call-template name="format"><xsl:with-param name="datum"><xsl:value-of select="substring-after(./@aNormDat,'/')"/></xsl:with-param></xsl:call-template></xsl:if></format>
      </xsl:when>
      <xsl:otherwise>
    <format><xsl:call-template name="format"><xsl:with-param name="datum"><xsl:value-of select="./@aNormDat"/></xsl:with-param></xsl:call-template></format>
      </xsl:otherwise>
  </xsl:choose>
    </desc-item-unit-date>
</xsl:template>

<xsl:template match="ap:datace">
    <xsl:choose>
  <xsl:when test="./ap:datum[@aNormDat and string-length(normalize-space(@aNormDat))]">
      <xsl:apply-templates select="./ap:datum"/>
  </xsl:when>
  <xsl:when test="./ap:datum">
      <desc-item-string desc-item-type-code="ZP2015_UNIT_DATE_TEXT">
    <position>1</position>
    <value><xsl:value-of select="./ap:datum"/></value>
      </desc-item-string>
  </xsl:when>
  <xsl:otherwise>
      <desc-item-string desc-item-type-code="ZP2015_UNIT_DATE_TEXT">
    <position>1</position>
    <value><xsl:value-of select="."/></value>
      </desc-item-string>
  </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template match="ap:nadpisIS">
    <desc-item-string>
  <xsl:attribute name="desc-item-type-code">ZP2015_TITLE</xsl:attribute>
  <position>1</position>
  <value><xsl:value-of select=".//ap:obsah"/></value>
    </desc-item-string>
    <desc-item-enum>
       <xsl:attribute name="desc-item-type-code">ZP2015_LEVEL_TYPE</xsl:attribute>
       <xsl:attribute name="desc-item-spec-code">ZP2015_LEVEL_SERIES</xsl:attribute>
       <position>1</position>
    </desc-item-enum>
</xsl:template>

<xsl:template match="ap:obsah">
    <desc-item-string>
  <xsl:attribute name="desc-item-type-code">ZP2015_TITLE</xsl:attribute>
  <position>1</position>
  <value><xsl:value-of select="."/></value>
    </desc-item-string>
</xsl:template>

<xsl:template match="ap:eviJednotka">
  <xsl:choose>
    <xsl:when test="./ap:druhEVJ='karton' or ./ap:druhEVJ='N'">
    <desc-item-packet-ref>
      <xsl:attribute name="desc-item-type-code">ZP2015_STORAGE_ID</xsl:attribute>
      <xsl:attribute name="packet-id">
        <xsl:value-of select="./ap:cisloEVJ"/>
      </xsl:attribute>
      <position>1</position>
    </desc-item-packet-ref>
    <desc-item-enum>
      <xsl:attribute name="desc-item-type-code">ZP2015_LEVEL_TYPE</xsl:attribute>
      <xsl:attribute name="desc-item-spec-code">ZP2015_LEVEL_FOLDER</xsl:attribute>
      <position>1</position>
    </desc-item-enum>
    <desc-item-enum>
      <xsl:attribute name="desc-item-type-code">ZP2015_FOLDER_TYPE</xsl:attribute>
      <xsl:attribute name="desc-item-spec-code">ZP2015_FOLDER_UNITS</xsl:attribute>
      <position>1</position>
    </desc-item-enum>
    </xsl:when>
    <xsl:otherwise>
      <desc-item-enum>
        <xsl:attribute name="desc-item-type-code">ZP2015_LEVEL_TYPE</xsl:attribute>
        <xsl:attribute name="desc-item-spec-code">ZP2015_LEVEL_ITEM</xsl:attribute>
        <position>1</position>
      </desc-item-enum>
      <xsl:choose>
         <xsl:when test="./ap:druhEVJ='úřední kniha' or ./ap:druhEVJ='K'">
           <desc-item-enum>
             <xsl:attribute name="desc-item-type-code">ZP2015_UNIT_TYPE</xsl:attribute>
             <xsl:attribute name="desc-item-spec-code">ZP2015_UNIT_TYPE_UKN</xsl:attribute>
             <position>1</position>
           </desc-item-enum>
        </xsl:when>
        <xsl:when test="./ap:druhEVJ='plán'">
           <desc-item-enum>
             <xsl:attribute name="desc-item-type-code">ZP2015_UNIT_TYPE</xsl:attribute>
             <xsl:attribute name="desc-item-spec-code">ZP2015_UNIT_TYPE_TVY</xsl:attribute>
             <position>1</position>
           </desc-item-enum>
        </xsl:when>
        <xsl:when test="./ap:druhEVJ='M' or ./ap:druhEVJ='mapa'">
           <desc-item-enum>
             <xsl:attribute name="desc-item-type-code">ZP2015_UNIT_TYPE</xsl:attribute>
             <xsl:attribute name="desc-item-spec-code">ZP2015_UNIT_TYPE_MAP</xsl:attribute>
             <position>1</position>
           </desc-item-enum>
        </xsl:when>
        <xsl:when test="./ap:druhEVJ='Y'">
           <desc-item-enum>
             <xsl:attribute name="desc-item-type-code">ZP2015_UNIT_TYPE</xsl:attribute>
             <xsl:attribute name="desc-item-spec-code">ZP2015_UNIT_TYPE_KTT</xsl:attribute>
             <position>1</position>
           </desc-item-enum>
        </xsl:when>
        <xsl:when test="./ap:druhEVJ='fotografie'">
           <desc-item-enum>
             <xsl:attribute name="desc-item-type-code">ZP2015_UNIT_TYPE</xsl:attribute>
             <xsl:attribute name="desc-item-spec-code">ZP2015_UNIT_TYPE_FSN</xsl:attribute>
             <position>1</position>
           </desc-item-enum>
        </xsl:when>
        <xsl:when test="./ap:druhEVJ='grafické listy'">
           <desc-item-enum>
             <xsl:attribute name="desc-item-type-code">ZP2015_UNIT_TYPE</xsl:attribute>
             <xsl:attribute name="desc-item-spec-code">ZP2015_UNIT_TYPE_GLI</xsl:attribute>
             <position>1</position>
           </desc-item-enum>
        </xsl:when>
        <xsl:otherwise>
           <desc-item-enum>
             <xsl:attribute name="desc-item-type-code">ZP2015_UNIT_TYPE</xsl:attribute>
             <xsl:attribute name="desc-item-spec-code">ZP2015_UNIT_TYPE_JIN</xsl:attribute>
             <position>1</position>
           </desc-item-enum>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="ap:radekIS">
    <xsl:apply-templates select="./ap:obsah"/>
    <xsl:apply-templates select="./ap:datace"/>
    <xsl:apply-templates select="./ap:eviJednotka"/>
    <xsl:choose>
      <xsl:when test="./ap:eviJednotka">

      </xsl:when>
      <xsl:otherwise>
        <desc-item-enum>
          <xsl:attribute name="desc-item-type-code">ZP2015_LEVEL_TYPE</xsl:attribute>
          <xsl:attribute name="desc-item-spec-code">ZP2015_LEVEL_SERIES</xsl:attribute>
        </desc-item-enum>
      </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template match="ap:castIS" mode="child">
<level>
<position><xsl:value-of select="position()"/></position>
<uuid/>
<xsl:if test="./ap:castIS">
<sub-level-list>
    <xsl:apply-templates select="./ap:castIS" mode="child"/>
</sub-level-list>
</xsl:if>
<desc-item-list>
    <xsl:apply-templates select="./ap:nadpisIS"/>
    <xsl:apply-templates select="./ap:radekIS"/>
</desc-item-list>
<xsl:if test="//ap:lokatorR[@aIDRef=current()/@aID]">
<record-list>
    <xsl:apply-templates select="//ap:lokatorR[@aIDRef=current()/@aID]"/>
</record-list>
</xsl:if>
</level>
</xsl:template>

<xsl:template match="ap:castIS" mode="root">
<root-level>
<position><xsl:value-of select="position()"/></position>
<uuid/>
<xsl:if test="./ap:castIS">
<sub-level-list>
    <xsl:apply-templates select="./ap:castIS" mode="child"/>
</sub-level-list>
</xsl:if>
<desc-item-list>
    <xsl:apply-templates select="./ap:nadpisIS"/>
    <xsl:apply-templates select="./ap:radekIS"/>
</desc-item-list>
</root-level>
</xsl:template>


<xsl:template match="ap:hesloRej">
    <xsl:param name="typeCode"/>
    <xsl:param name="local"/>
<record>
    <xsl:attribute name="record-id"><xsl:value-of select="./@aID"/></xsl:attribute>
    <xsl:attribute name="register-type-code"><xsl:value-of select="$typeCode"/></xsl:attribute>
    <xsl:attribute name="local"><xsl:value-of select="$local"/></xsl:attribute>
    <xsl:attribute name="external-id"><xsl:value-of select="./@aID"/></xsl:attribute>
    <preferred-name><xsl:value-of select="./ap:zahlaviR"/></preferred-name>
    <characteristics><xsl:value-of select="./ap:zahlaviR"/></characteristics>
    <xsl:if test="./ap:hesloRej">
  <sub-record-list>
    <xsl:apply-templates select="./ap:hesloRej">
      <xsl:with-param name="typeCode" select="$typeCode"/>
      <xsl:with-param name="local" select="$local"/>
    </xsl:apply-templates>
  </sub-record-list>
    </xsl:if>
</record>
</xsl:template>

<xsl:template match="ap:rejstrik">
    <xsl:variable name="typeCode">
    <xsl:choose>
  <xsl:when test="@aTypRej='vseobecny'">TERM_GENERAL</xsl:when>
  <xsl:when test="@aTypRej='jmenny'">TERM_PERSON</xsl:when>
  <xsl:when test="@aTypRej='predmetovy'">TERM_GENERAL</xsl:when>
  <xsl:when test="@aTypRej='zemepisny'">GEO_UNIT</xsl:when>
  <xsl:when test="@aTypRej='nazvovy'">TERM_GENERAL</xsl:when>
  <xsl:when test="@aTypRej='ciselKodu'">EVENT_EVENT</xsl:when>
  <xsl:when test="@aTypRej='autorsky'">ARTWORK_ARTWORK</xsl:when>
  <xsl:otherwise>UNKNOWN</xsl:otherwise>
    </xsl:choose>
    </xsl:variable>
    <xsl:apply-templates select="./ap:hesloRej">
  <xsl:with-param name="typeCode" select="$typeCode"/>
  <xsl:with-param name="local">true</xsl:with-param>
    </xsl:apply-templates>
</xsl:template>

<!-- Main match -->
<xsl:template match="/ap:dokumentArchPomucky">
<elza:xml-import xmlns:elza="http://v1_0_0.import.elza.tacr.cz">
    <fund>
      <xsl:attribute name="arr-type-code">INV</xsl:attribute>
      <xsl:attribute name="rule-set-code">ZP2015</xsl:attribute>
      <name><xsl:value-of select="./ap:metaData/ap:oPomucce/ap:nazevPomucky[@aTyp='nazev']"/></name>
        <xsl:choose>
          <xsl:when test="count(./ap:pomucka/ap:inventSeznam/ap:castIS)>1">
            <root-level>
              <position><xsl:value-of select="position()"/></position>
              <uuid/>
                <sub-level-list>
                  <xsl:apply-templates select="./ap:pomucka/ap:inventSeznam/ap:castIS" mode="child"/>
                </sub-level-list>
              <desc-item-list/>
            </root-level>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="./ap:pomucka/ap:inventSeznam/ap:castIS" mode="root"/>
          </xsl:otherwise>
        </xsl:choose>
    </fund>
    <record-list>
  <xsl:apply-templates select="./ap:pomucka/ap:rejstrik"/>
    </record-list>
    <party-list>
    </party-list>
    <packet-list>
        <xsl:for-each select=".//ap:eviJednotka[generate-id() = generate-id(key('evidencni_jednotky',concat(normalize-space(./ap:druhEVJ),normalize-space(./ap:cisloEVJ)))[1])]">
                <xsl:choose>
                  <xsl:when test="./ap:druhEVJ='karton' or ./ap:druhEVJ='N'">
                    <packet>
                      <xsl:attribute name="packet-type-code">BOX</xsl:attribute>
                      <xsl:attribute name="invalid">false</xsl:attribute>
                      <xsl:attribute name="state">OPEN</xsl:attribute>
                      <xsl:attribute name="storage-number">
                        <xsl:value-of select="./ap:cisloEVJ"/>
                      </xsl:attribute>
                      <xsl:attribute name="state">OPEN</xsl:attribute>
                    </packet>
                  </xsl:when>
                </xsl:choose>
        </xsl:for-each>
    </packet-list>
</elza:xml-import>
</xsl:template>

</xsl:stylesheet>