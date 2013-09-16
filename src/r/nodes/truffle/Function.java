package r.nodes.truffle;


import r.data.*;
import r.nodes.*;
import r.runtime.*;


public class Function extends RNode {
    final RFunction function;

    public Function(RFunction function) {
        this.function = function;
    }

    @Override
    public final Object execute(Frame frame) {
        return function.createClosure(frame);
    }

    @Override
    public final ASTNode getAST() {
     return function.getSource();
    }
}
