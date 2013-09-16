package r.data;

import r.nodes.ASTNode;
import r.nodes.truffle.*;
import r.runtime.*;

public interface RFunction {
    RFunction enclosingFunction();

    RSymbol[] paramNames();
    RNode[] paramValues();
    RNode body();
    RClosure createClosure(Frame frame);
    RSymbol[] localWriteSet();
    FrameDescriptor frameDescriptor();
    Object call(Frame frame);
    ASTNode getSource();

    int nlocals();
    int nparams();
    int dotsIndex();

    public static final class EnclosingSlot {

        public EnclosingSlot(RSymbol sym, int hops, int slot) {
            symbol = sym;
            this.hops = hops;
            this.slot = slot;
        }

        public final RSymbol symbol;
        public final int hops;
        public final int slot;
    }

    int positionInLocalWriteSet(RSymbol sym);
    int positionInLocalReadSet(RSymbol sym);
    EnclosingSlot getLocalReadSetEntry(RSymbol sym);
    int localSlot(RSymbol sym);
    EnclosingSlot enclosingSlot(RSymbol sym);
    boolean isInWriteSet(RSymbol sym);
}
