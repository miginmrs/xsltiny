<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
				xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:x="alias://xsl"
				xmlns:d="http://xsltiny.sf.net/document"
				xmlns:xslm="alias://xsl/pure"
				xmlns:xsli="alias://xsl/item"
				xmlns:xslt="alias://xsl/content"
				xmlns:ctx="http://xsltiny.sf.net/context"
				xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				xmlns:xs="http://www.w3.org/2001/XMLSchema"
				xsi:schemaLocation="
				alias://xsl ../../../org/w3/xslt20.xsd
				alias://xsl/content schema/xslcontent.xsd
				alias://xsl/item schema/xslitem.xsd
				alias://xsl/pure  schema/xslpure.xsd
				http://www.w3.org/1999/XSL/Transform ../../../org/w3/xslt20.xsd
				http://xsltiny.sf.net/context schema/context.xsd
				http://xsltiny.sf.net/document schema/document.xsd">

	<xsl:output method="xml" />

	<xsl:namespace-alias stylesheet-prefix="x" result-prefix="xsl"/>

	<xsl:variable name="document"/>

	<xsl:variable name="mappers" select="/d:document/d:define/d:mapper"/>

	<xsl:variable name="templates" select="/d:document/d:define/d:template"/>

	<xsl:variable name="fragments" select="/d:document/d:define/d:fragment"/>

	<xsl:function name="d:getbyname" as="item()*">
		<xsl:param name="name" as="xs:string"/>
		<xsl:param name="nodes" as="item()*"/>
		<xsl:sequence select="$nodes[@name=$name]"/>
	</xsl:function>

	<xsl:function name="d:context" as="item()*">
		<xsl:param name="node"/>
		<xsl:variable name="mutators" select="($node/ancestor::d:*)/(self::d:for/@name,self::d:with[d:getbyname(@name,$templates)[@target]/@target])"/>
		<xsl:choose>
			<xsl:when test="$mutators">
				<xsl:copy-of select="$document/ctx:context[@name=$mutators[last()]]"/>
			</xsl:when>
			<xsl:when test="not($mutators) and $node/ancestor-or-self::d:section[1]/@context">
				<xsl:copy-of select="$document/ctx:context[@name=$node/ancestor-or-self::d:section[1]/@context]"/>
			</xsl:when>
			<!-- The first context without parent is the default context -->
			<xsl:otherwise><xsl:copy-of select="$document/ctx:context[not(@parent)][1]"/></xsl:otherwise>
		</xsl:choose>
	</xsl:function>

	<xsl:function name="d:selector" as="xs:string">
		<xsl:param name="node"/>
		<xsl:variable name="context" select="$document/ctx:context[@name=$node/@name]"/>
		<xsl:variable name="current" select="d:context($node)"/>
        <xsl:variable name="case1" select="$context/not(@parent)"/>
        <xsl:variable name="case2" select="$case1 or ($node/@root and not($document/ctx:context[@name=$context/@parent]/@parent))"/>
		<xsl:variable name="output">
			<xsl:choose>
				<xsl:when test="$case1"><xsl:value-of select="$context/@path"/></xsl:when>
				<xsl:when test="not($case1) and $case2">
					<xsl:value-of select="$document/ctx:context[@name=$context/@parent]/@path"/>/<xsl:value-of select="$context/@path"/>
				</xsl:when>
				<xsl:when test="not($case2) and not($node/@parent='yes') and $current/@name=$context/@parent"><xsl:value-of select="$context/@path"/></xsl:when>
				<xsl:when test="not($case2) and $node/@parent='yes' and $current/@parent=$node/@name and $current/@length">ascendant::*[last()+1-<xsl:value-of select="$current/@length"/>]</xsl:when>
				<xsl:otherwise/>
			</xsl:choose>
		</xsl:variable>
		<xsl:value-of select="$output"/>
	</xsl:function>

	<xsl:function name="d:call" as="item()*">
		<xsl:param name="node"/>
        <xsl:variable name="name" select="$node/local-name()"/>
        <xsl:variable name="target" select="($document/ctx:default-context|d:context($node))/ctx:*[local-name()=$name and @name=$node/@name and (not(@section) or @section=$node/ancestor::d:section/@name)][last()]"/>
		<xsl:copy-of select="$target"/>
	</xsl:function>

	<xsl:function name="d:section" as="item()*">
		<xsl:param name="name" as="xs:string"/>
		<xsl:param name="node" as="item()*"/>
		<xsl:sequence select="$node/ancestor-or-self::d:section[1]/preceding-sibling::d:section[@name=$name and d:context(.)/@name=d:context($node)/@name]"/>
	</xsl:function>

	<xsl:function name="d:getns" as="item()*">
		<xsl:param name="node"/>
        <xsl:variable name="namespaces" select="$node/ancestor::d:document/namespace::* | $node/namespace::*"/>
        <xsl:for-each select="distinct-values($namespaces/name())">
            <xsl:variable name="prefix" select="."/>
            <xsl:copy-of select="$namespaces[name()=$prefix][last()]"/>
        </xsl:for-each>
	</xsl:function>

	<xsl:template match="/">
		<xsl:element name="d:document">
			<xsl:for-each select="d:document/d:section[not(@abstract)]">
				<xsl:element name="d:section">
					<xsl:attribute name="name" select="@name"/>
					<x:stylesheet version="2.0">
						<xsl:copy-of select="d:getns(.)"/>
						<x:output method="text" indent="yes"/>
						<x:variable name="doc_properties">
							<xsl:copy-of select="/d:document/d:properties/d:property"/>
						</x:variable>
						<x:variable name="ctx_properties">
							<xsl:copy-of select="$document/ctx:properties/d:property"/>
						</x:variable>
						<xsl:apply-templates mode="xslpure" select="/d:document/d:define/d:header/*"/>
						<x:template match="{d:context(.)/@path}">
							<xsl:apply-templates mode="section" select="."/>
						</x:template>
					</x:stylesheet>
				</xsl:element>
			</xsl:for-each>
		</xsl:element>
	</xsl:template>

	<xsl:template match="d:var" mode="normal" priority="1"><x:variable name="v_{@name}"><xsl:apply-templates mode="normal"/></x:variable></xsl:template>
	<xsl:template match="d:ivar" mode="normal" priority="1"><x:variable name="iv_{@name}"><xsl:apply-templates mode="normal"/></x:variable></xsl:template>
	<xsl:template match="d:lay" mode="normal" priority="1"><x:value-of select="$v_{@name}"/></xsl:template>
	<xsl:template match="d:ilay" mode="normal" priority="1"><x:copy-of select="$iv_{@name}"/></xsl:template>

	<xsl:template match="xslm:*" mode="xslpure" priority="1">
		<xsl:element name="xsl:{local-name()}">
			<xsl:copy-of select="d:getns(.)"/>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates mode="xslpure"/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="xsli:*" mode="xslitem" priority="1">
		<xsl:element name="xsl:{local-name()}">
			<xsl:copy-of select="d:getns(.)"/>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates mode="xslitem"/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="ctx:item" mode="xslitem" priority="1">
		<d:item>
			<xsl:copy-of select="d:getns(.)"/>
			<xsl:apply-templates mode="xslpure"/>
		</d:item>
	</xsl:template>

	<xsl:template match="xslt:content" mode="xslcontent" priority="2">
		<xsl:param name="content"/>
		<xsl:apply-templates select="$content" mode="normal"/>
	</xsl:template>

	<xsl:template match="xslm:variable" mode="xslcontent" priority="1">
        <xsl:apply-templates select="." mode="xslpure"/>
	</xsl:template>

	<xsl:template match="xslt:*" mode="xslcontent" priority="1">
		<xsl:param name="content"/>
		<xsl:element name="xsl:{local-name()}">
			<xsl:copy-of select="d:getns(.)"/>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates mode="xslcontent">
				<xsl:with-param name="content" select="$content"/>
			</xsl:apply-templates>
		</xsl:element>
	</xsl:template>

	<xsl:template match="d:map" mode="normal" priority="1">
		<x:variable name="param"><xsl:apply-templates mode="normal"/></x:variable>
		<xsl:variable name="name"><xsl:value-of select="@name"/></xsl:variable>
		<x:for-each select="$param">
			<xsl:apply-templates select="$mappers[@name=$name]" mode="mapper"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="d:with[not(d:getbyname(@name, $templates)/@context!=d:context(.)/@name)]" mode="normal" priority="1">
		<xsl:variable name="content" select="child::node()"/>
		<xsl:variable name="name"><xsl:value-of select="@name"/></xsl:variable>
		<xsl:apply-templates select="$templates[@name=$name]" mode="template">
			<xsl:with-param name="content" select="$content"/>
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template match="d:put[not(d:getbyname(@name, $fragments)/@context!=d:context(.)/@name)]" mode="normal" priority="1">
		<xsl:variable name="name"><xsl:value-of select="@name"/></xsl:variable>
		<xsl:apply-templates select="$fragments[@name=$name]" mode="mapper"/>
	</xsl:template>

	<xsl:template match="d:mapper | d:fragment" mode="mapper" priority="1">
		<xsl:apply-templates mode="xslpure"/>
	</xsl:template>

    <xsl:template match="ctx:use" mode="mapper" priority="1">
        <xsl:apply-templates mode="xslpure"/>
    </xsl:template>

    <xsl:template match="d:template" mode="template" priority="1">
		<xsl:param name="content"/>
		<xsl:apply-templates mode="xslcontent">
			<xsl:with-param name="content" select="$content"/>
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template match="d:join" mode="normal">
		<x:variable name="list">
			<xsl:apply-templates mode="normal"/>
		</x:variable>
		<x:value-of select="&#36;list/d:item[position()=1]"/>
		<x:for-each select="&#36;list/d:item[position()!=1]">
			<xsl:value-of select="@delimiter"/><x:value-of select="."/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="d:item" mode="normal">
		<xsl:element name="d:item">
			<xsl:apply-templates mode="normal"/>
		</xsl:element>
	</xsl:template>

	<xsl:template mode="section" match="/d:document/d:section" priority="1">
		<xsl:apply-templates mode="normal"/>
	</xsl:template>

	<xsl:template match="*[@name]" mode="#all" priority="0">
		<xsl:variable name="msg"><xsl:value-of select="name()"/> '<xsl:value-of select="@name"/>' is not allowed in `<xsl:value-of select="d:context(.)/@name"/>` scope </xsl:variable>
		<xsl:message terminate="yes"><xsl:value-of select="trace(., $msg)"/></xsl:message>
	</xsl:template>

	<xsl:function name="d:attr">
		<xsl:param name="node"/>
		<xsl:value-of select="d:context($node)/ctx:attribute[@name=$node/@name]"/>
	</xsl:function>

	<xsl:template match="d:get[d:attr(.)]" mode="normal" priority="1">
		<xsl:variable name="name" select="@name"/>
		<x:value-of select="{d:context(.)/ctx:attribute[@name=$name]/@path}"/>
	</xsl:template>

	<xsl:template match="d:if[d:attr(.)]" mode="normal" priority="1">
		<xsl:variable name="name" select="@name"/>
		<xsl:variable name="test" select="d:context(.)/ctx:attribute[@name=$name]/@path"/>
		<x:if test="{if(@not) then concat('not(',$test,')') else $test}">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="d:link[(not(@target) and d:call(.)[not(@target)]) or @target=d:call(.)/@target]" mode="normal" priority="1">
		<xsl:variable name="link" select="d:call(.)"/>
        <xsl:if test="$link/@link">
            <x:variable name="link"><xsl:value-of select="ancestor::d:link[last()]/@name"/></x:variable>
        </xsl:if>
        <x:if test="{$link/@test}">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="d:use[d:call(.)]" mode="normal" priority="1">
        <xsl:variable name="use" select="d:call(.)"/>
        <xsl:if test="$use/@link">
            <x:variable name="link"><xsl:value-of select="ancestor::d:link[last()]/@name"/></x:variable>
        </xsl:if>
		<xsl:apply-templates mode="mapper" select="$use"/>
	</xsl:template>

	<xsl:template match="d:list[d:call(.)]" mode="normal" priority="1">
        <xsl:variable name="list" select="d:call(.)"/>
        <xsl:if test="$list/@link">
            <x:variable name="link"><xsl:value-of select="ancestor::d:link[last()]/@name"/></x:variable>
        </xsl:if>
		<xsl:apply-templates mode="xslitem" select="$list/*"/>
	</xsl:template>

	<xsl:template match="d:for[d:selector(.)!='']" mode="normal" priority="1">
		<x:for-each select="{d:selector(.)}">
			<xsl:apply-templates mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="d:import[d:section(@name,.)]" mode="normal" priority="1">
		<xsl:apply-templates select="d:section(@name,.)" mode="section"/>
	</xsl:template>

	<xsl:template match="text()" mode="#all" priority="1">
		<xsl:value-of select="replace(., '^(.*?)&#10;[ &#9;]*$', '$1', 's')"/>
	</xsl:template>

	<xsl:template match="d:out" mode="normal" priority="1">
		<x:text><xsl:value-of select="@s|." xml:space="preserve"/></x:text>
	</xsl:template>

	<xsl:variable name="delimiter" select="/d:document/d:output/@delimiter"/>
	<xsl:variable name="br" select="if($delimiter) then $delimiter else '&#10;'"/>
	<xsl:template match="d:br" mode="normal" priority="1">
		<x:value-of select="'{$br}'" xml:space="preserve" disable-output-escaping="yes"/>
	</xsl:template>

</xsl:stylesheet>
