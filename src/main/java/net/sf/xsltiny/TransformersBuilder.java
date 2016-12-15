package net.sf.xsltiny;

import org.w3c.dom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused"})
public class TransformersBuilder {
    private static final String XSL_URI = "http://www.w3.org/1999/XSL/Transform";
    private static final String D_URI = "http://xsltiny.sf.net/document";
    private static final TransformerFactory transformerFactory;
    private static final SchemaFactory schemaFactory;
    private static final DocumentBuilder documentBuilder;
    private static final DocumentBuilder contextBuilder;
    private static final XPathFactory xPathfactory;
    private static final XPath xpath;
    private static final XPathExpression documentVariableXPath;
    private static final XPathExpression firstElementXPath;
    private static final XPathExpression propertyNameXPath;
    private static final XPathExpression propertyValueXPath;
    private static final XPathExpression elementsXPath;
    public static final DocumentBuilder builder;

    private final Transformer transformer;
    private static class SimpleErrorHandler implements ErrorHandler{
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            System.err.println(exception.getMessage());
        }
        @Override
        public void error(SAXParseException exception) throws SAXException {
            assert exception != null;
            throw exception;
        }
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            assert exception != null;
            throw exception;
        }
    }

    static {
        System.setProperty("javax.view.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        transformerFactory = TransformerFactory.newInstance();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Error("Unable to create a document builder for XML usage: "+e.getMessage());
        }
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            URL url = ClassLoader.getSystemResource("net/sf/xsltiny/schema/document.xsd");
            if(url == null) throw new IOException();
            documentBuilderFactory.setSchema(schemaFactory.newSchema(new Source[] {new StreamSource(url.openStream(), url.toExternalForm())}));
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            documentBuilder.setErrorHandler(new SimpleErrorHandler());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new Error("Unable to create a document builder for XML usage: "+e.getMessage());
        }
        DocumentBuilderFactory contextBuilderFactory = DocumentBuilderFactory.newInstance();
        contextBuilderFactory.setNamespaceAware(true);
        try {
            URL url = ClassLoader.getSystemResource("net/sf/xsltiny/schema/context.xsd");
            if(url == null) throw new IOException();
            contextBuilderFactory.setSchema(schemaFactory.newSchema(new Source[] {new StreamSource(url.openStream(), url.toExternalForm())}));
            contextBuilder = contextBuilderFactory.newDocumentBuilder();
            contextBuilder.setErrorHandler(new SimpleErrorHandler());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new Error("Unable to create a context builder for XML usage: "+e.getMessage());
        }
        try {
            final Map<String, String> namespaces = new HashMap<String, String>(){{
                put("xsl", XSL_URI);
                put("d", D_URI);
            }};
            NamespaceContext resolver = new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return namespaces.get(prefix);
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return namespaces.entrySet().stream().filter(a->namespaceURI.equals(a.getValue())).findAny().map(Map.Entry::getKey).orElse(null);
                }

                @Override
                public Iterator getPrefixes(String namespaceURI) {
                    return namespaces.keySet().iterator();
                }
            };
            xPathfactory = XPathFactory.newInstance(
                    XPathFactory.DEFAULT_OBJECT_MODEL_URI,
                    "net.sf.saxon.xpath.XPathFactoryImpl",
                    ClassLoader.getSystemClassLoader());
            xpath = xPathfactory.newXPath();
            xpath.setNamespaceContext(resolver);
            documentVariableXPath = xpath.compile("xsl:variable[@name='document']");
            firstElementXPath = xpath.compile("*[1]");
            elementsXPath = xpath.compile("*");
            propertyNameXPath = xpath.compile("@name");
            propertyValueXPath = xpath.compile("@value");
        } catch (XPathExpressionException | XPathFactoryConfigurationException e) {
            throw new Error("Unable to compile an xpath 1.0 expression");
        }
    }

    public static class DocumentData {
        private final Document document;
        private final Node propertiesNode;
        private final DocumentBuilder builder;

        private DocumentData(Document document, Node propertiesNode, DocumentBuilder builder) {
            this.document = document;
            this.propertiesNode = propertiesNode;
            this.builder = builder;
        }
        public Map<String, String> getProperties() throws XPathExpressionException {
            Map<String, String> properties = new HashMap<>();
            NodeList nodes = (NodeList) elementsXPath.evaluate(propertiesNode, XPathConstants.NODESET);
            for(int i=0, n=nodes.getLength(); i<n; i++) {
                Node node = nodes.item(i);
                if(node.getNodeType() == Node.ELEMENT_NODE) {
                    Boolean hasValue = (Boolean) propertyValueXPath.evaluate(node, XPathConstants.BOOLEAN);
                    properties.put(propertyNameXPath.evaluate(node), hasValue ? propertyValueXPath.evaluate(node) : null);
                }
            }
            return properties;
        }
        public void setProperties(Map<String, String> properties) throws XPathExpressionException {
            NodeList nodes = propertiesNode.getChildNodes();
            for(int i=0, n=nodes.getLength(); i<n; i++) {
                propertiesNode.removeChild(nodes.item(0));
            }
            properties.forEach((name, value) -> {
                Element element = document.createElementNS(D_URI, "d:property");
                element.setAttribute("name", name);
                if(value != null)
                    element.setAttribute("value", value);
                propertiesNode.appendChild(element);
            });
        }
    }

    public static DocumentData getContext(URL contextURL) throws IOException {
        return loadDocument(contextURL, contextBuilder);
    }

    public TransformersBuilder(URL contextURL) throws TransformerConfigurationException, IOException {
        this(getContext(contextURL));
    }

    public TransformersBuilder(DocumentData contextData) throws TransformerConfigurationException {
        if(contextData.builder!=contextBuilder)
            throw new TransformerConfigurationException("Context document must be created using context builder");
        URL transformerURL = ClassLoader.getSystemResource("net/sf/xsltiny/transform.xsl");
        Document transformerDocument;
        Node variableNode;
        try {
            transformerDocument = builder.parse(transformerURL.openStream(), transformerURL.toExternalForm());
            variableNode = (Node)documentVariableXPath.evaluate(transformerDocument.getDocumentElement(), XPathConstants.NODE);
        } catch (SAXException | IOException | XPathExpressionException e) {
            throw new Error("Unable de parse transformer document");
        }
        NodeList contextNodes;
        try {
            contextNodes = (NodeList) elementsXPath.evaluate(contextData.document.getDocumentElement(), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new AssertionError();
        }
        for(int i=0, n=contextNodes.getLength(); i<n; i++){
            variableNode.appendChild(transformerDocument.importNode(contextNodes.item(i), true));
        }
        DOMSource domSource = new DOMSource(transformerDocument, transformerURL.toExternalForm());
        transformer = transformerFactory.newTransformer(domSource);
    }

    public static DocumentData loadDocument(URL documentURL) throws IOException {
        return loadDocument(documentURL, documentBuilder);
    }

    private static DocumentData loadDocument(URL documentURL, DocumentBuilder documentBuilder) throws IOException {
        try {
            Document document = documentBuilder.parse(documentURL.openStream(), documentURL.toExternalForm());
            Node propertiesNode = (Node) firstElementXPath.evaluate(document.getDocumentElement(), XPathConstants.NODE);
            return new DocumentData(document, propertiesNode, documentBuilder);
        } catch (IOException | XPathExpressionException | SAXException e) {
            throw new IOException("Unable de load "+documentURL.toExternalForm()+": "+e.getMessage());
        }
    }

    public Map<String, Transformer> getTransformers(URL documentURL) throws TransformerException, IOException {
        return getTransformers(loadDocument(documentURL));
    }

    public Map<String, Transformer> getTransformers(DocumentData documentData) throws TransformerException {
        return getTransformersFromXsl(getTransformersDocument(documentData));
    }

    public static Map<String, Transformer> getTransformersFromXsl(Document xslDocument) throws TransformerException {
        NodeList nodes;
        try {
            nodes = (NodeList) elementsXPath.evaluate(xslDocument.getDocumentElement(), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new TransformerException(e);
        }
        return new HashMap<String, Transformer>(){{
            Attr attr = xslDocument.getDocumentElement().getAttributeNode("xmlns:d");
            if(attr==null) throw new AssertionError("xmlns:d is not an attr: "+xslDocument.getDocumentElement().getPrefix());
            for(int i = 0, n = nodes.getLength(); i < n ; i++) {
                Node node = nodes.item(i);
                String name = ((Element)node).getAttribute("name");
                Document document = builder.newDocument();
                try {
                    node = (Node) firstElementXPath.evaluate(node, XPathConstants.NODE);
                } catch (XPathExpressionException ignored) {
                    continue;
                }
                document.appendChild(document.importNode(node, true));
                document.getDocumentElement().setAttributeNode((Attr) document.importNode(attr, false));
                put(name, transformerFactory.newTransformer(new DOMSource(document)));
            }
        }};
    }

    private void transform(DocumentData documentData, Result result) throws TransformerException {
        if(documentData.builder!=documentBuilder)
            throw new TransformerConfigurationException("Document must be created using document builder");
        transformer.transform(new DOMSource(documentData.document), result);
    }

    public Document getTransformersDocument(DocumentData documentData) throws TransformerException {
        Document output = builder.newDocument();
        transform(documentData, new DOMResult(output));
        return output;
    }

    public String getTransformersData(DocumentData documentData) throws TransformerException {
        StringWriter output = new StringWriter();
        transform(documentData, new StreamResult(output));
        return output.toString();
    }

    public static String render(Transformer transformer, Document document) throws TransformerException {
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }
}
