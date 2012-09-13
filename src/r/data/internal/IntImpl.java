package r.data.internal;

import r.*;
import r.data.*;
import r.nodes.*;
import r.nodes.truffle.*;

public class IntImpl extends ArrayImpl implements RInt {

    int[] content;

    public IntImpl(int[] values, boolean doCopy) {
        if (doCopy) {
            content = new int[values.length];
            System.arraycopy(values, 0, content, 0, values.length);
        } else {
            content = values;
        }
    }

    public IntImpl(int [] values) {
        this(values, true);
    }

    public IntImpl(int size) {
        content = new int[size];
    }

    public IntImpl(RInt v) {
        content = new int[v.size()];
        for (int i = 0; i < content.length; i++) {
            content[i] = v.getInt(i);
        }
    }

    @Override
    public int size() {
        return content.length;
    }

    public Object get(int i) {
        return content[i];
    }

    public int getInt(int i) {
        return content[i];
    }

    @Override
    public RArray set(int i, Object val) {
        return set(i, ((Integer) val).intValue()); // FIXME better conversion
    }

    @Override
    public RArray set(int i, int val) {
        content[i] = val;
        return this;
    }

    @Override
    public RInt asInt() {
        return this;
    }

    public String pretty() {
        if (content.length == 0) {
            return RInt.TYPE_STRING + "(0)";
        }
        String fst = Convert.int2string(content[0]);
        if (content.length == 1) {
            return fst;
        }
        StringBuilder str = new StringBuilder();
        str.append(fst);
        for (int i = 1; i < content.length; i++) {
            str.append(", ");
            str.append(Convert.int2string(content[i]));
        }
        return str.toString();
    }

    @Override
    public RLogical asLogical() {
        Utils.nyi();
        return null;
    }

    @Override
    public RDouble asDouble() {
        return new DoubleView();
    }

    @Override
    public <T extends RNode> T callNodeFactory(OperationFactory<T> factory) {
        return factory.fromInt();
    }

    class DoubleView extends View implements RDouble {

        @Override
        public Object get(int i) {
            return getDouble(i);
        }

        public int size() {
            return IntImpl.this.size();
        }

        @Override
        public RInt asInt() {
            return IntImpl.this;
        }

        @Override
        public RDouble asDouble() {
            return this;
        }

        @Override
        public RArray materialize() {
            return RDouble.RDoubleFactory.copy(this);
        }

        @Override
        public RAttributes getAttributes() {
            return IntImpl.this.getAttributes();
        }

        @Override
        public RLogical asLogical() {
            return IntImpl.this.asLogical();
        }

        @Override
        public RArray set(int i, double val) {
            return materialize().set(i, val);
        }

        @Override
        public double getDouble(int i) {
            int v = IntImpl.this.getInt(i);
            return Convert.int2double(v);
        }
    }
}
