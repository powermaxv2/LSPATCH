package de.robv.android.xposed;

/** Stub of the Xposed API 82 class. */
public abstract class XC_MethodReplacement extends XC_MethodHook {

    public XC_MethodReplacement() {
        super();
    }

    public XC_MethodReplacement(int priority) {
        super(priority);
    }

    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;

    public static final XC_MethodReplacement DO_NOTHING = new XC_MethodReplacement(PRIORITY_HIGHEST * 2) {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            throw new UnsupportedOperationException("stub");
        }
    };

    public static XC_MethodReplacement returnConstant(final Object result) {
        throw new UnsupportedOperationException("stub");
    }

    public static XC_MethodReplacement returnConstant(int priority, final Object result) {
        throw new UnsupportedOperationException("stub");
    }
}
