package happy2b.woody.util.ignore;



public class IgnoredTypesPredicateImpl implements IgnoredTypesPredicate {

    private Trie<IgnoreAllow> ignoredTypesTrie;
    private Trie<IgnoreAllow> ignoredClassLoadersTrie;

    public IgnoredTypesPredicateImpl(Trie<IgnoreAllow> ignoredTypesTrie, Trie<IgnoreAllow> ignoredClassLoadersTrie) {
        this.ignoredTypesTrie = ignoredTypesTrie;
        this.ignoredClassLoadersTrie = ignoredClassLoadersTrie;
    }

    @Override
    public boolean test(ClassLoader loader, String internalClassName) {
        if (loader == null) {
            return false;
        }
        return ignoredTypesTrie.getOrNull(internalClassName) == IgnoreAllow.ALLOW;
    }

}
