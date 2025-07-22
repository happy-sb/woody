package happy_sb.profiling.agct.tool.ignore;

public interface IgnoredTypesPredicate {

    /**
     * 是否该被忽略
     *
     * @param loader
     * @param internalClassName
     * @return
     */
    boolean test(ClassLoader loader, String internalClassName);

}
