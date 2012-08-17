package r.interpreter;

import java.io.*;

import org.antlr.runtime.*;

import r.*;
import r.nodes.*;
import r.nodes.tools.*;
import r.parser.*;

public class Console {

    public static final boolean DEBUG_GUI = Boolean.parseBoolean(Utils.getProperty("RConsole.debug.gui", "true"));
    public static final boolean QUIET = Boolean.parseBoolean(Utils.getProperty("RConsole.quiet", "false"));

    public static String prompt = Utils.getProperty("RConsole.prompt", "> ");
    public static String promptMore = Utils.getProperty("RConsole.promptmore", "+ ");

    static RLexer lexer;
    static RParser parser;
    static Node tree;
    static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    static StringBuilder incomplete = new StringBuilder();

    public static void main(String[] args) {
        boolean errorStmt = false;
        try {
            if (args.length > 0) {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
            }

            lexer = new RLexer();
            parser = new RParser(null);
            do {
                print(incomplete.length() == 0 ? prompt : promptMore);
                errorStmt = !parse_statement();
                if (!errorStmt) {
                    parser.reset();
                    if (DEBUG_GUI) {
                        TreeViewer.showTree(tree);
                    }
                    if (!QUIET) {
                        new PrettyPrinter(System.out).print(tree);
                    }
                    // Evaluate
                }
            } while (true);
        } catch (IOException e) {
        }
    }

    static boolean parse_statement() throws IOException {
        String line = in.readLine();
        if (line == null) {
            throw new EOFException();
        }
        incomplete.append(line);
        lexer.setCharStream(new ANTLRStringStream(incomplete.toString()));
        parser.setTokenStream(new CommonTokenStream(lexer));
        Node result;
        try {
            result = parser.interactive();
        } catch (RecognitionException e) {
            if (e.getUnexpectedType() != -1) {
                Token tok = e.token;
                String[] tokNames = parser.getTokenNames();
                System.err.print("Parse error on '" + tok.getText() + "' at " + tok.getLine() + ":" + tok.getCharPositionInLine() + " (" + tokNames[tok.getType()] + ") expected:");
                parser.display_next_tokens();
                System.err.println("");
                incomplete.setLength(0); // Kind of reset
            } else {
                incomplete.append('\n');
            }
            return false;
        }
        incomplete.setLength(0); // Kind of reset
        tree = result;
        return true;
    }

    static void print(String text) {
        print(text, System.out);
    }

    static void print(String text, PrintStream out) {
        out.print(text);
        out.flush();
    }

}