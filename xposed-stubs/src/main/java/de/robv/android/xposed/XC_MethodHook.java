package de.robv.android.xposed;

import java.lang.reflect.Member;

/** Stub of the Xposed API 82 class. */
public abstract class XC_MethodHook {

    public static final int PRIORITY_DEFAULT = 50;
    public static final int PRIORITY_LOWEST = -10000;
    public static final int PRIORITY_HIGHEST = 10000;

    public final int priority;

    public XC_MethodHook() {
        this.priority = 50;
    }

    public XC_MethodHook(int priority) {
        this.priority = priority;
    }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        throw new UnsupportedOperationException("stub");
    }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        throw new UnsupportedOperationException("stub");
    }

    public static class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;

        public Object getResult() {
            throw new UnsupportedOperationException("stub");
        }

        public void setResult(Object result) {
            throw new UnsupportedOperationException("stub");
        }

        public Throwable getThrowable() {
            throw new UnsupportedOperationException("stub");
        }

        public boolean hasThrowable() {
            throw new UnsupportedOperationException("stub");
        }

        public void setThrowable(Throwable throwable) {
            throw new UnsupportedOperationException("stub");
        }

        public Object getResultOrThrowable() throws Throwable {
            throw new UnsupportedOperationException("stub");
        }
    }

    public class Unhook {
        public Member getHookedMethod() {
            throw new UnsupportedOperationException("stub");
        }

        public XC_MethodHook getCallback() {
            throw new UnsupportedOperationException("stub");
        }

        public void unhook() {
            throw new UnsupportedOperationException("stub");
        }
    }
}
