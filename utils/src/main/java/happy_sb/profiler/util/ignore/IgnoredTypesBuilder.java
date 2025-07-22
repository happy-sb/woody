/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package happy_sb.profiler.util.ignore;


public interface IgnoredTypesBuilder {

    IgnoredTypesBuilder ignoreClass(String classNameOrPrefix);

    IgnoredTypesBuilder allowClass(String classNameOrPrefix);

    IgnoredTypesBuilder ignoreClassLoader(String classNameOrPrefix);

    IgnoredTypesBuilder allowClassLoader(String classNameOrPrefix);

    Trie<IgnoreAllow> buildIgnoredTypesTrie();

    Trie<IgnoreAllow> buildIgnoredClassloaderTrie();

    IgnoredTypesPredicate buildTransformIgnoredPredicate();


}
