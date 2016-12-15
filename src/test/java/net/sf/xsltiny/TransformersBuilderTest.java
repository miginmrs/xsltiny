package net.sf.xsltiny;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.Assert.*;
import static java.lang.ClassLoader.getSystemResource;

public class TransformersBuilderTest {
    private static final String CONTEXT_PATH = "net/sf/xsltiny/context.xml";
    private static final String DIALECT_PATH = "net/sf/xsltiny/dialect.xml";
    private static final String DATA_PATH = "net/sf/xsltiny/data.xml";
    private static final String OUT_PATH = "net/sf/xsltiny/out.txt";
    @Test
    public void read() throws IOException, TransformerException, SAXException {
        TransformersBuilder transformersBuilder = new TransformersBuilder(getSystemResource(CONTEXT_PATH));
        Map<String, Transformer> transformerMap = transformersBuilder.getTransformers(TransformersBuilder.loadDocument(getSystemResource(DIALECT_PATH)));
        assertTrue(transformerMap.size()==1);
        Transformer transformer = transformerMap.get("section1");
        assertTrue(transformer != null);
//        System.out.println(transformersBuilder.getTransformersData(TransformersBuilder.loadDocument(getSystemResource(DIALECT_PATH))));
        String out = TransformersBuilder.render(transformer, TransformersBuilder.builder.parse(getSystemResourceAsStream(DATA_PATH)));
        assertEquals(new Scanner(getSystemResourceAsStream(OUT_PATH)).useDelimiter("$^").next(), out);
    }
}
