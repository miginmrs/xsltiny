package net.sf.xsltiny;

import org.apache.commons.cli.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.function.Function;

public class Main {
    private static class Argument {
        final URL context, transformer, data;
        final PrintStream out;

        private Argument(URL context, URL transformer, URL data, PrintStream out) {
            this.context = context;
            this.transformer = transformer;
            this.data = data;
            this.out = out;
        }
    }

    private static Function<Action<Argument, Object>, Object> getArgument(String[] args){
        CommandLineParser parser = new BasicParser();
        HelpFormatter formatter = new HelpFormatter(){
            @Override
            public void printHelp(String cmdLineSyntax, Options options) {
                PrintWriter pw = new PrintWriter(System.err);
                this.printHelp(pw, 80, cmdLineSyntax, null, options, 1, 3, null, false);
                pw.flush();
            }
        };
        Options options = new Options();
        options.addOption(new Option("c", "context", true, "context file path"){{setRequired(true);}});
        options.addOption(new Option("t", "transformer ", true, "transformer file path"){{setRequired(true);}});
        options.addOption(new Option("d", "data", true, "data file path"){{setRequired(true);}});
        options.addOption(new Option("o", "output", true, "output file path"));
        options.addOption(new Option("h", "help", false, "show this help"));
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("xsltiny", options);
            System.exit(1);
            throw new AssertionError();
        }

        if(cmd.hasOption('h')) {
            formatter.printHelp("xsltiny", options, false);
            System.exit(0);
        }

        class Handler {
            private <In, Out> Function<Action<In, Out>, Out> apply(In param) {
                return action -> { try {
                    return action.doAction(param);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    formatter.printHelp("xsltiny", options);
                    System.exit(1);
                    return null;
                }};
            }
        }

        Handler handler = new Handler();

        return handler.apply(handler.<Object, Argument>apply(null).apply(arg -> {
            URL context = new URL(cmd.getOptionValue('c'));
            URL transformer = new URL(cmd.getOptionValue('t'));
            URL data = new URL(cmd.getOptionValue('d'));
            PrintStream out = cmd.hasOption('o') ? new PrintStream(new FileOutputStream(cmd.getOptionValue('o'))) : System.out;
            return new Argument(context, transformer, data, out);
        }));
    }

    @FunctionalInterface
    interface Action<In, Out> {
        Out doAction(In argument) throws Exception;
    }

    public static void main(String[] args) throws IOException, XPathExpressionException, SAXException, TransformerConfigurationException {
        getArgument(args).apply(argument -> {
            assert argument != null;
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            Document document = builderFactory.newDocumentBuilder().parse(argument.data.openStream());
            TransformersBuilder transformersBuilder = new TransformersBuilder(argument.context);
            transformersBuilder.getTransformers(argument.transformer).forEach((name, transformer) -> {
                try {
                    TransformersBuilder.render(transformer, document);
                } catch (TransformerException e) {
                    throw new RuntimeException("The data document is not well formed: "+e.getMessage());
                }
            });
            return null;
        });
    }
}
