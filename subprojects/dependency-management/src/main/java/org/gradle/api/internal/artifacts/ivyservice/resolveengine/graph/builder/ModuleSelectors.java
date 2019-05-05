/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.builder;

import org.gradle.api.internal.artifacts.ResolvedVersionConstraint;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.DefaultVersionComparator;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.ExactVersionSelector;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.LatestVersionSelector;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.Version;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelector;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.selectors.ResolvableSelectorState;
import org.gradle.internal.Cast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class ModuleSelectors<T extends ResolvableSelectorState> implements Iterable<T> {
    private static final VersionParser VERSION_PARSER = new VersionParser();
    private static final Version EMPTY_VERSION = VERSION_PARSER.transform("");
    private static final Comparator<Version> VERSION_COMPARATOR = (new DefaultVersionComparator().asVersionComparator()).reversed();

    private static <T, U extends Comparable<? super U>> Comparator<T> reverse(
        Function<? super T, ? extends U> keyExtractor) {
        return Cast.uncheckedCast(Comparator.comparing(keyExtractor).reversed());
    }

    final static Comparator<ResolvableSelectorState> SELECTOR_COMPARATOR =
        reverse(ResolvableSelectorState::isProject)
            .thenComparing(reverse(ResolvableSelectorState::isFromLock))
            .thenComparing(reverse(ModuleSelectors::hasLatestSelector))
            .thenComparing(ModuleSelectors::isDynamicSelector)
            .thenComparing(ModuleSelectors::requiredVersion, VERSION_COMPARATOR)
            .thenComparing(ModuleSelectors::preferredVersion, VERSION_COMPARATOR);

    private final List<T> selectors = new SortedSelectors<T>();
    private boolean deferSelection;

    public boolean checkDeferSelection() {
        if (deferSelection) {
            deferSelection = false;
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<T> iterator() {
        return selectors.iterator();
    }

    public void add(T selector, boolean deferSelection) {
        this.deferSelection = deferSelection;
        selectors.add(selector);
    }

    public boolean remove(T selector) {
        return selectors.remove(selector);
    }

    private static boolean isDynamicSelector(ResolvableSelectorState selector) {
        return selector.getVersionConstraint() != null && selector.getVersionConstraint().isDynamic();
    }

    private static boolean hasLatestSelector(ResolvableSelectorState selector) {
        return selector.getVersionConstraint() != null
            && hasLatestSelector(selector.getVersionConstraint());
    }

    private static boolean hasLatestSelector(ResolvedVersionConstraint vc) {
        return hasLatestSelector(vc.getPreferredSelector()) || hasLatestSelector(vc.getRequiredSelector());
    }

    private static boolean hasLatestSelector(VersionSelector versionSelector) {
        return versionSelector instanceof LatestVersionSelector;
    }

    private static Version requiredVersion(ResolvableSelectorState selector) {
        ResolvedVersionConstraint versionConstraint = selector.getVersionConstraint();
        if (versionConstraint == null) {
            return EMPTY_VERSION;
        }
        return versionOf(versionConstraint.getRequiredSelector());
    }

    private static Version preferredVersion(ResolvableSelectorState selector) {
        ResolvedVersionConstraint versionConstraint = selector.getVersionConstraint();
        if (versionConstraint == null) {
            return EMPTY_VERSION;
        }
        return versionOf(versionConstraint.getPreferredSelector());
    }

    private static Version versionOf(VersionSelector selector) {
        if (!(selector instanceof ExactVersionSelector)) {
            return EMPTY_VERSION;
        }
        return VERSION_PARSER.transform(selector.getSelector());
    }

    public int size() {
        return selectors.size();
    }

    public T first() {
        if (selectors.isEmpty()) {
            return null;
        }
        if (selectors.size() == 1) {
            return selectors.get(0);
        }
        return selectors.get(0);
    }

    private static class SortedSelectors<T extends ResolvableSelectorState> extends ArrayList<T> {
        @Override
        public boolean add(T elem) {
            if (isEmpty()) {
                return super.add(elem);
            }
            super.add(insertionPointOf(elem), elem);
            return true;
        }

        private int insertionPointOf(T elem) {
            int index = Collections.binarySearch(this, elem, SELECTOR_COMPARATOR);
            if (index < 0) {
                return ~index;
            }
            // preserve insertion order
            int size = size();
            while (index >= 0 && index < size && SELECTOR_COMPARATOR.compare(get(index), elem) == 0) {
                index++;
            }
            return index;
        }

        @Override
        public void add(int index, T element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            boolean added = false;
            for (T e : c) {
                added |= add(e);
            }
            return added;
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }
    }
}
