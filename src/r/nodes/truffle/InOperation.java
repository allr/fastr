package r.nodes.truffle;

import java.util.*;

import r.*;
import r.data.*;
import r.nodes.*;

import com.oracle.truffle.runtime.*;

// FIXME: this is a very unoptimized version
// FIXME: could optimize "%in% names" using the hash-map stored in Names
public class InOperation extends BaseR {
    @Stable RNode left;
    @Stable RNode right;

    public InOperation(ASTNode ast, RNode left, RNode right) {
        super(ast);
        this.left = updateParent(left);
        this.right = updateParent(right);
    }

    @Override
    public final  Object execute(RContext context, Frame frame) {
        RAny leftValue = (RAny) left.execute(context, frame);
        RAny rightValue = (RAny) right.execute(context, frame);
        return execute(context, leftValue, rightValue);
    }

    // TODO: this could be customized to elide cast view creation, boxing when creating a hash map
    // FIXME: could use some primitive collections library, e.g. Trove or PCJ?
    public Object execute(RContext context, RAny left, RAny right) {

        RArray typedX;
        RArray typedTable;

        // note: R also converts raw to string, but using int seems functionally equivalent
        if (left instanceof RString || right instanceof RString || left instanceof RList || right instanceof RList) {
            typedX = left.asString();
            typedTable = right.asString();
        } else if (left instanceof RComplex || right instanceof RComplex) {
            typedX = left.asComplex();
            typedTable = right.asComplex();
        } else if (left instanceof RDouble || right instanceof RDouble) {
            typedX = left.asDouble();
            typedTable = right.asDouble();
        } else if (left instanceof RInt || right instanceof RInt || left instanceof RRaw || right instanceof RRaw) {
            typedX = left.asInt();
            typedTable = right.asInt();
        } else if (left instanceof RLogical || right instanceof RLogical) {
            typedX = left.asLogical();
            typedTable = right.asLogical();
        } else {
            Utils.nyi("unsupported type");
            return null;
        }

        int xsize = typedX.size();
        int tableSize = typedTable.size();

        if (xsize == 1) {
            Object x = typedX.get(0);
            for (int i = 0; i < tableSize; i++) {
                Object v = typedTable.get(i);
                if (x.equals(v)) {
                    return RLogical.BOXED_TRUE;
                }
            }
            return RLogical.BOXED_FALSE;
        }
        int[] content = new int[xsize];
        HashSet set = new HashSet<>(tableSize);
        for (int i = 0; i < tableSize; i++) {
            Object v = typedTable.get(i);
            set.add(v);
        }
        for (int i = 0; i < xsize; i++) {
            Object x = typedX.get(i);
            content[i] = set.contains(x) ? RLogical.TRUE : RLogical.FALSE;
        }
        return RLogical.RLogicalFactory.getFor(content);
    }

}