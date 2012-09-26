package r.data.internal;

import com.oracle.truffle.runtime.Frame;

import r.*;
import r.data.*;

public class ClosureImpl extends BaseObject implements RClosure {

    Frame environment;
    RFunction function;

    public ClosureImpl(RFunction function, Frame environment) {
        this.function = function;
        this.environment = environment;
    }

    @Override
    public String pretty() {
        Utils.check(function != null);
        StringBuilder str = new StringBuilder();
        str.append(function.getSource().toString());
        if (environment != null) {
            str.append(" <ENVIRONMENT " + environment + ">");
        }
        return str.toString();
    }

    @Override
    public RLogical asLogical() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RInt asInt() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RDouble asDouble() {
        return null;
    }

    @Override
    public Frame environment() {
        return environment;
    }

    @Override
    public RFunction function() {
        return function;
    }

}
