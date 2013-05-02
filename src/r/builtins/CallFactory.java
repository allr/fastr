package r.builtins;

import java.util.*;

import r.*;
import r.data.*;
import r.errors.*;
import r.nodes.*;
import r.nodes.truffle.*;

import java.lang.Integer; // needed because there is a class Integer in this package

/**
 * Parent of functions and operators. The create method is used to create the RNode for a particular call site.
 */
public abstract class CallFactory {

    // LICENSE: Some sub-classes include comments that are copy-pasted from GNU R online manual. GNU R is licensed under GPL.

    /** Declared name of function. */
    final RSymbol name;
    /** Names of the declared parameters. */
    private RSymbol[] parameters;
    /** Names of the required parameters. */
    private RSymbol[] required;
    /** Smallest legal number of parameters. */
    final int maxParameters;
    /** Largest legal number of parameters. */
    final int minParameters;

    public CallFactory(String name) {
        this.name = RSymbol.getSymbol(name);
        maxParameters = -1;
        minParameters = -1;
    }

    /**
     * @param name
     *            operation's name
     * @param parameters
     *            name of the parameters, can be empty but not null.
     * @param required
     *            array of argument names that are required. If null, same as parameters
     */
    CallFactory(String name, String[] parameters, String[] required) {
        this.name = RSymbol.getSymbol(name);
        this.parameters = RSymbol.getSymbols(parameters);
        this.required = required == null ? this.parameters : RSymbol.getSymbols(required);
        boolean dotdot = false;
        if (this.required != this.parameters) {
            for (RSymbol r : this.required) {
                boolean match = false;
                for (RSymbol p : this.parameters) {
                    match |= r == p;
                }
                if (!match) { throw Utils.nyi("Internal error in builtin definition for " + name + "required list has extra values"); }
            }
        }
        RSymbol[] minPs = new RSymbol[parameters.length];
        int pos = 0;
        for (RSymbol p : this.parameters) {
            dotdot |= p == RSymbol.THREE_DOTS_SYMBOL;
            if (!dotdot) minPs[pos++] = p;
        }
        int min = 0;
        for (int i = 0; i < pos; i++) {
            for (RSymbol r : this.required) {
                if (minPs[i] == r) {
                    min++;
                    break;
                }
            }
        }
        maxParameters = dotdot ? Integer.MAX_VALUE : parameters.length;
        minParameters = min;
    }

    /**
     * Create a RNode for a call to a function.
     *
     * @param call
     *            the abstract syntax tree node for this function call
     * @param names
     *            the names of the actual arguments (or null)
     * @param exprs
     *            the values of the actual arguments (not null)
     */
    public abstract RNode create(ASTNode call, RSymbol[] names, RNode[] exprs);

    /**
     * Create a RNode for a binary operation.
     *
     * @param call
     *            the abstract syntax tree node for this function call
     * @param left
     *            the left hand side expression
     * @param right
     *            the right hand side expression
     */
    public RNode create(ASTNode call, RNode left, RNode right) {
        return create(call, null, new RNode[]{left, right});
    }

    /** Description of the arguments passed at a call site. */
    static class ArgumentInfo {
        /** Parameter names in order. */
        RSymbol[] parameters;
        /**
         * For each formal parameter, in order, what is the index of the corresponding actual argument. The value -1
         * indicates that there was no actual to match that formal parameter.
         */
        int[] paramPositions;
        /** Arguments (indexes) that are not matched to one of the function's parameter. */
        ArrayList<Integer> unusedArgs;

        ArgumentInfo(RSymbol[] parameters) {
            this.parameters = parameters;
            int nParams = parameters.length;
            paramPositions = new int[nParams];
            for (int i = 0; i < paramPositions.length; i++) {
                paramPositions[i] = -1;
            }
        }

        /** Return the index in the formal parameter list of one particular formal parameter. */
        private int ix(RSymbol p) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] == p) { return i; }
            }
            throw new Error(p + " not found in " + this);
        }

        /** Return the index in the formal parameter list of one particular formal parameter. */
        private int ix(String p) {
            return ix(RSymbol.getSymbol(p));
        }

        /** Returns true if an actual was passed for the formal. */
        boolean provided(String name) {
            return paramPositions[ix(name)] != -1;
        }

        /** Returns the position in the actuals of the formal name or -1. */
        int position(String name) {
            return paramPositions[ix(name)];
        }

        /** For debugging. */
        @Override public String toString() {
            String res = "[";
            for (int i = 0; i < parameters.length; i++) {
                res += parameters[i] + "=" + paramPositions[i] + ((i == parameters.length - 1) ? "" : ",");
            }
            return res + "]";
        }
    }

    /**
     * Match the formal (parameters) to the actuals (arguments) at a call site. This is done in three passes, first
     * gather all the argument passed by exact name, then get the arguments passed by partial name as long as they are
     * not ambiguous. Last get the positional arguments.
     *
     * @param names
     *            array of the names of arguments (or null)
     * @param exprs
     *            array of the expressions passed as arguments (not null)
     */
    ArgumentInfo resolveArguments(RSymbol[] names, RNode[] exprs) {
        ArgumentInfo a = new ArgumentInfo(parameters);
        boolean[] used = new boolean[exprs.length];
        // Match by name, remember which args are unused.
        if (names != null) {
            outer: for (int i = 0; i < exprs.length; i++) {
                if (names[i] == null) continue;
                for (int j = 0; j < parameters.length; j++) {
                    if (names[i] == parameters[j]) {
                        a.paramPositions[j] = i;
                        used[i] = true;
                        continue outer;
                    }
                }
            }
        }
        // Match by partial name, ignore arguments already matched and with no name.
        if (names != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (a.paramPositions[i] != -1) continue;
                boolean match = false;
                for (int j = 0; j < exprs.length; j++) {
                    if (used[j]) continue;
                    if (names[j] == null) continue;
                    if (parameters[i].startsWith(names[j])) {
                        if (match) { throw RError.getGenericError(null, "Argument " + names[j] + " matches multiple formal arguments."); }
                        a.paramPositions[i] = j;
                        used[j] = true;
                        match = true;
                    }
                }
            }
        }
        // Match the remaining arguments by position, taking care of the three dots and of extra arguments.
        int nextP = 0;
        for (int i = 0; i < exprs.length; i++) {
            if (used[i]) continue;
            while (nextP < parameters.length && a.paramPositions[nextP] != -1 && parameters[nextP] != RSymbol.THREE_DOTS_SYMBOL) {
                nextP++; // skip params that have been matched already, but if the param is ..., don't advance
            }
            if (nextP == parameters.length) { // Garbage params...
                if (a.unusedArgs == null) {
                    a.unusedArgs = new ArrayList<>();
                }
                a.unusedArgs.add(i);
            } else {
                if (parameters[nextP] == RSymbol.THREE_DOTS_SYMBOL) { // Record the last argument that was taken by ...
                    a.paramPositions[nextP] = i;
                    continue;
                } else if (exprs[i] != null) { // positional match  (can the expr be null?????)
                    if (names != null && names[i] != null) { throw RError.getGenericError(null, "Unknown parameter " + names[i].pretty() + " passed to " + name()); } // FIXME: better error message
                    a.paramPositions[nextP] = i;
                } else { // FIXME: JAN asks if this point can ever be reached?
                    nextP++;
                    throw new Error("Aha, so it does ...");
                }
            }
        }
        return a;
    }

    /** Return the index in the formal parameter list of one particular formal parameter. */
    int ix(RSymbol p) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == p) { return i; }
        }
        throw Utils.nyi(p + " not found  in  " + this);
    }

    /** Return the index in the formal parameter list of one particular formal parameter. */
    int ix(String p) {
        return ix(RSymbol.getSymbol(p));
    }

    /** Return the name of the function or operation. */
    public RSymbol name() {
        return name;
    }

    ArgumentInfo check(ASTNode call, RSymbol[] names, RNode[] exprs) {
        ArgumentInfo ai = resolveArguments(names, exprs);
        int provided = 0;
        for (int i = 0; i < ai.paramPositions.length; i++) {
            if (ai.paramPositions[i] != -1) {
                provided++;
            }
        }
        if (provided < minParameters) {
            for (int i = 0; i < required.length; i++) {
                if (ai.paramPositions[ix(required[i])] == -1) { throw RError.getGenericError(call, String.format(RError.ARGUMENT_MISSING, required[i].name())); }
            }
        }
        if (exprs.length < minParameters) { throw RError.getWrongArity(call, name().name(), minParameters, exprs.length); }
        if (ai.unusedArgs != null) { throw RError.getWrongArity(call, name().name(), maxParameters, exprs.length); }
        return ai;
    }

    /** Check that the argument provided has the right name else throw an error. */
    static void ensureArgName(ASTNode ast, String expectedName, RSymbol actualName) {
        if (actualName == null) { return; }
        RSymbol expected = RSymbol.getSymbol(expectedName);
        if (actualName != expected) { throw RError.getGenericError(ast, String.format(RError.ARGUMENT_NOT_MATCH, actualName.pretty(), expectedName)); }
    }

    @Override public String toString() {
        String res = "CallFactory[" + name + "(";
        if (parameters != null)
            for (RSymbol r : parameters)
                res += r + " ";
        return res + ")]";
    }

    // TODO: convert this to something smarter, e.g. an automaton
    static class ArgumentMatch {
        final String[] allowed; // must not include NA and must be unique

        public ArgumentMatch(String[] allowed) {
            this.allowed = allowed;
        }

        public int match(RAny arg, ASTNode ast, String argName) {
            if (arg instanceof RNull) {
                return 0; // default value
            }
            if (!(arg instanceof RString)) {
                throw RError.getMustNullOrString(ast, argName);
            }
            RString m = (RString) arg;
            if (m.size() != 1) {
                throw RError.getMustBeScalar(ast, argName); // in GNU-R, this will appear part of match.arg
            }
            String s = m.getString(0);
            if (s == RString.NA) {
                throw RError.getArgOneOf(ast, argName, allowed);
            }
            int match = -1;
            int nmatches = 0;
            for (int i = 0; i < allowed.length; i++) {
                String a = allowed[i];
                if (a.startsWith(s)) {
                    if (a.length() == s.length()) {  // FIXME: does this check pay off?
                        return i;
                    }
                    nmatches++;
                    match = i;
                }
            }
            if (nmatches == 1) {
                return match;
            }
            throw RError.getArgOneOf(ast, argName, allowed);
        }
    }

    // parses a logical argument to a builtin that is not checked for validity and is used in the GNU-R code
    // of a builtin written in R; this can never exactly match the GNU-R semantics
    // perhaps error messages should be made explicit in a spec, instead
    public static boolean parseUncheckedLogical(RAny arg, ASTNode ast) {
        RLogical l = arg.asLogical();
        int size = l.size();
        if (size >= 1) {
            int v = l.getLogical(0);
            if (v == RLogical.NA) {
                throw RError.getUnexpectedNA(ast);
            }
            if (size > 1) {
                RContext.warning(ast, RError.LENGTH_GT_1);
            }
            return (v == RLogical.TRUE);
        }
        throw RError.getUnexpectedNA(ast);
    }

}
