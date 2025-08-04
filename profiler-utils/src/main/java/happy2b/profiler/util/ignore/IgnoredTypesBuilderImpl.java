/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package happy2b.profiler.util.ignore;



public class IgnoredTypesBuilderImpl implements IgnoredTypesBuilder {

  private Trie.Builder<IgnoreAllow> ignoredTypesTrieBuilder = Trie.builder();
  private Trie.Builder<IgnoreAllow> ignoredClassLoadersTrieBuilder = Trie.builder();

  private Trie<IgnoreAllow> ignoredTypesTrie;
  private Trie<IgnoreAllow> ignoredClassLoadersTrie;

  @Override
  public IgnoredTypesBuilder ignoreClass(String classNameOrPrefix) {
    ignoredTypesTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.IGNORE);
    return this;
  }

  @Override
  public IgnoredTypesBuilder allowClass(String classNameOrPrefix) {
    ignoredTypesTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.ALLOW);
    return this;
  }


  @Override
  public IgnoredTypesBuilder ignoreClassLoader(String classNameOrPrefix) {
    ignoredClassLoadersTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.IGNORE);
    return this;
  }

  @Override
  public IgnoredTypesBuilder allowClassLoader(String classNameOrPrefix) {
    ignoredClassLoadersTrieBuilder.put(classNameOrPrefix.replace('.', '/'), IgnoreAllow.ALLOW);
    return this;
  }

  @Override
  public Trie<IgnoreAllow> buildIgnoredClassloaderTrie() {
    if (ignoredClassLoadersTrie == null) {
      ignoredClassLoadersTrie = ignoredClassLoadersTrieBuilder.build();
    }
    return ignoredClassLoadersTrie;
  }

  @Override
  public Trie<IgnoreAllow> buildIgnoredTypesTrie() {
    if (ignoredTypesTrie == null) {
      ignoredTypesTrie = ignoredTypesTrieBuilder.build();
    }
    return ignoredTypesTrie;
  }


  @Override
  public IgnoredTypesPredicate buildTransformIgnoredPredicate() {
    return new IgnoredTypesPredicateImpl(buildIgnoredTypesTrie(), buildIgnoredClassloaderTrie());
  }

}
