package de.robv.android.xposed.callbacks;

/** Stub of the Xposed API 82 class. */
public abstract class XC_LoadPackage {

    public static final class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        /** android.content.pm.ApplicationInfo in the real API (typed Object here to avoid an android.jar dependency). */
        public Object appInfo;
        public boolean isFirstApplication;
    }
}
