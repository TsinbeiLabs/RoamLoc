package com.tsinbei.roamloc.xposed.compat;

import java.lang.reflect.Executable;
import java.util.List;

public class XC_MethodHook {
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    public final void dispatchBefore(MethodHookParam param) throws Throwable {
        beforeHookedMethod(param);
    }

    public final void dispatchAfter(MethodHookParam param) throws Throwable {
        afterHookedMethod(param);
    }

    public static final class MethodHookParam {
        public final Executable method;
        public final Object thisObject;
        public final Object[] args;

        private Object result;
        private Throwable throwable;
        private boolean returnEarly;
        private boolean before = true;

        public MethodHookParam(Executable method, Object thisObject, List<Object> args) {
            this.method = method;
            this.thisObject = thisObject;
            this.args = args.toArray();
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
            throwable = null;
            if (before) returnEarly = true;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
            result = null;
            if (before) returnEarly = true;
        }

        public boolean hasThrowable() {
            return throwable != null;
        }

        public boolean isReturnEarly() {
            return returnEarly;
        }

        public void setAfterResult(Object result) {
            before = false;
            this.result = result;
            throwable = null;
        }

        public void setAfterThrowable(Throwable throwable) {
            before = false;
            result = null;
            this.throwable = throwable;
        }

        public void resetAfterBeforeFailure() {
            result = null;
            throwable = null;
            returnEarly = false;
        }

        public void restoreAfterFailure(Object result, Throwable throwable) {
            before = false;
            this.result = result;
            this.throwable = throwable;
        }
    }

    public static final class Unhook {
        private final Runnable action;

        public Unhook(Runnable action) {
            this.action = action;
        }

        public void unhook() {
            action.run();
        }
    }
}
