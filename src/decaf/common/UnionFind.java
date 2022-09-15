package decaf.common;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class UnionFind<T> {
    Map<T, T> parents = new HashMap<>();
    Map<T, Integer> weights = new HashMap<>();

    /**
     * MAKE-SET()
     * we don't need to store the elements, instead we can hash them, and since each element is unique, the
     * hashes won't collide. I will use union by size instead of union by rank
     * using union by rank needs more careful handling in the union of multiple item
     *
     * @param items a list of items to add to the UnionFind structure
     */
    public UnionFind(Collection<T> items) {
        for (T item : items) {
            parents.put(item, item);
            weights.put(item, 1);
        }
    }


    public T find(T item) {
        if (!parents.containsKey(item)) {
            parents.put(item, item);
            weights.put(item, 1);
            return item;
        } else {
            var path = new ArrayList<T>();
            var representative = parents.get(item);
            while (!representative.equals(item)) {
                path.add(item);
                item = representative;
                representative = parents.get(item);
            }

            for (var ancestor : path) {
                parents.put(ancestor, representative);
            }
            return representative;
        }
    }

    @SafeVarargs
    public final void union(T... items) {
        var roots = Arrays.stream(items)
                .map(this::find)
                .sorted(Comparator.comparingInt(weights::get).reversed()
                ).iterator();

        try {
            var heaviest = roots.next();
            while (roots.hasNext()) {
                var r = roots.next();
                weights.put(heaviest, weights.get(r) + weights.get(heaviest));
                parents.put(r, heaviest);
            }
        } catch (NoSuchElementException ignored) {
        }
    }

    public Collection<Set<T>> toSets() {
        for (var parent : parents.keySet()) {
            find(parent);
        }
        Map<T, Set<T>> results = new HashMap<>();
        for (var entry : parents.entrySet()) {
            results.computeIfAbsent(entry.getValue(), v -> new HashSet<>())
                    .add(entry.getKey());
        }
        return results.values();
    }
}
