package ru.joke.memcache.core.internal.util;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

@NotThreadSafe
public final class CompositeCollection<E> extends AbstractCollection<E> {

    private final List<Collection<E>> collections;

    public CompositeCollection(int capacity) {
        this.collections = new ArrayList<>(capacity);
    }

    public void addCollection(@Nonnull final Collection<E> collection) {
        this.collections.add(collection);
    }

    public long commonCount() {
        long size = 0;
        for (Collection<E> collection : this.collections) {
            size += collection.size();
        }
        return size;
    }

    @Override
    public int size() {
        return (int) commonCount();
    }

    @Override
    public boolean isEmpty() {
        for (Collection<E> collection : this.collections) {
            if (!collection.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(Object o) {
        for (Collection<E> collection : this.collections) {
            if (collection.contains(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nonnull
    public Iterator<E> iterator() {
        return new CompositeIterator();
    }

    private class CompositeIterator implements Iterator<E> {

        private final Iterator<Collection<E>> collectionIterator;
        private Iterator<E> elementIterator;

        public CompositeIterator() {
            this.collectionIterator = CompositeCollection.this.collections.iterator();
            this.elementIterator = null;
        }

        @Override
        public boolean hasNext() {
            while ((this.elementIterator == null || !this.elementIterator.hasNext()) && this.collectionIterator.hasNext()) {
                this.elementIterator = this.collectionIterator.next().iterator();
            }
            return this.elementIterator != null && this.elementIterator.hasNext();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            return this.elementIterator.next();
        }
    }
}
