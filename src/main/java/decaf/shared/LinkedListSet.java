package decaf.shared;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * This is just linked list which does not allow duplicate elements.
 *
 * @param <T>
 *       the type of elements held in this collection
 */
public class LinkedListSet<T> extends LinkedList<T> {
    /**
     * This is just
     */
    private final HashSet<T> inListItems;


    public LinkedListSet() {
        super();
        inListItems = new HashSet<T>();
    }

    @Override
    public boolean add(@NotNull T e) {
        if (inListItems.contains(e)) {
            return false;
        }
        inListItems.add(e);
        return super.add(e);
    }

    @Override
    public void add(int index, @NotNull T element) {
        if (inListItems.contains(element)) {
            return;
        }
        inListItems.add(element);
        super.add(index, element);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        boolean changed = false;
        for (T e : c) {
            if (inListItems.contains(e)) {
                continue;
            }
            inListItems.add(e);
            changed |= super.add(e);
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        boolean changed = false;
        for (T e : c) {
            if (inListItems.contains(e)) {
                continue;
            }
            inListItems.add(e);
            changed |= super.add(e);
        }
        return changed;
    }

    @Override
    public void addFirst(@NotNull T e) {
        if (inListItems.contains(e)) {
            return;
        }
        inListItems.add(e);
        super.addFirst(e);
    }

    @Override
    public void addLast(@NotNull T e) {
        if (inListItems.contains(e)) {
            return;
        }
        inListItems.add(e);
        super.addLast(e);
    }

    @Override
    public void clear() {
        inListItems.clear();
        super.clear();
    }

    @Override
    public boolean contains(Object o) {
        return inListItems.contains(o);
    }
}
