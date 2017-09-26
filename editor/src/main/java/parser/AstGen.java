package parser;

import exceptions.ParseException;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AstGen {

    /**
     * parse ANTLRInputStream containing thf and return ast
     * @param inputStream ANTLRInputStream object
     * @param rule start parsing at this rule
     * @return ast
     * @throws ParseException if there is no such rule
     */
    public static ParseContext parse(CharStream inputStream, String rule) throws ParseException {
        parser.TptpLexer lexer = new parser.TptpLexer(inputStream);
        lexer.removeErrorListeners(); // only for production
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CommonTokenStream comments = new CommonTokenStream(lexer,1);
        tokens.fill();

        parser.TptpParser parser = new parser.TptpParser(tokens);
        parser.removeErrorListeners(); // only for production
        ParseContext parseContext = new ParseContext();
        parser.addErrorListener(new DescriptiveCallbackErrorListener(parseContext));

        DefaultTreeListener treeListener = new DefaultTreeListener();
        treeListener.setParser(parser);

        //parser.addErrorListener(new DiagnosticErrorListener());
        parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
        parser.setBuildParseTree(true);
        parser.setTokenStream(tokens);

        // parsing starting from a rule requires invoking that rulename as parser method
        ParserRuleContext parserRuleContext = null;
        try {
            Class<?> parserClass = parser.getClass();
            Method method = parserClass.getMethod(rule, (Class<?>[]) null);
            parserRuleContext = (ParserRuleContext) method.invoke(parser, (Object[]) null);

        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException |
                InvocationTargetException e) {
            e.printStackTrace();
            throw new ParseException(e.getMessage());
        }        // the above or the below

        // if there is a fixed rule
        // ParserRuleContext context = parser.tPTP_file();

        // create ast
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(treeListener, parserRuleContext);

        // misc
        parseContext.setComments(comments.getTokens());
        parseContext.getComments().forEach(n-> System.out.println(n));

        // create and return ParseContext
        parseContext.setParserRuleContext(parserRuleContext);
        parseContext.setRoot(treeListener.getRootNode());
        return parseContext;
    }

}
