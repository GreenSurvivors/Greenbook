/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Almost everything here is heavily modified, this does NOT reassemble the original sourcecode anymore.
 * In fact only the core principle of how everything relies on an array, that is accessed by hash indexes,
 * everything was at least touched by me.
 */
package de.greensurvivors.greenbook.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.function.IntFunction;

/**
 * A {@code Set} implementation that allows to pair two int values together.
 * <p>
 * You can either directly input two int values or combine them together to a {@link DoubleInt}.
 * Every methode taking a plain object will expect a {@link DoubleInt}, and may act unexpectedly or even throw a
 * {@code ClassCastException} if used with other types.
 * {@code null}-Values are also NOT allowed and may throw a {@code NullPointerException}.
 * </p>
 * <p>
 * Note: There is NO map backing this set up.
 * This works internally with an array being addressed by hashes.
 * <p/>
 * <p>
 * <strong>Note that LinkedDoubleIntHashSet is not synchronized and is not thread-safe.</strong>
 * If you wish to use this set from multiple threads concurrently, you must use
 * appropriate synchronization. This class may throw exceptions when accessed
 * by concurrent threads without synchronization.
 * </p>
 */
public class LinkedDoubleIntHashSet extends AbstractSet<LinkedDoubleIntHashSet.DoubleInt> implements SequencedSet<LinkedDoubleIntHashSet.DoubleInt>, Cloneable {
    protected static final @NotNull String NO_NEXT_ENTRY = "No next() entry in the iteration";
    protected static final @NotNull String NO_PREVIOUS_ENTRY = "No previous() entry in the iteration";
    protected static final @NotNull String REMOVE_INVALID = "remove() can only be called once after next()";
    /// The default capacity to use
    protected static final int DEFAULT_CAPACITY = 16;
    /// The default threshold to use
    protected static final int DEFAULT_THRESHOLD = 12;
    /// The default load factor to use
    protected static final float DEFAULT_LOAD_FACTOR = 0.75f;
    /// The maximum capacity allowed
    protected static final int MAXIMUM_CAPACITY = 1 << 30;

    /// Load factor, normally 0.75
    private final transient float loadFactor;
    /// Header in the linked list
    protected transient @Nullable DoubleInt header;
    /// The size of the set
    private transient int size;
    /// set entries
    private @Nullable DoubleInt @NotNull [] data;
    /// Size at which to rehash
    private int threshold;
    /// Modification count for iterators
    private int modCount;

    /**
     * Constructs a new empty set with default size and load factor.
     */
    public LinkedDoubleIntHashSet() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
    }

    /**
     * Constructs a new, empty set with the specified initial capacity and
     * default load factor.
     *
     * @param initialCapacity the initial capacity
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public LinkedDoubleIntHashSet(final int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new, empty set with the specified initial capacity and
     * load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     * @throws IllegalArgumentException if the load factor is less than or equal to zero
     */
    public LinkedDoubleIntHashSet(int initialCapacity, final float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity must be a non negative number");
        }
        if (loadFactor <= 0.0f || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Load factor must be greater than 0");
        }
        this.loadFactor = loadFactor;
        initialCapacity = calculateNewCapacity(initialCapacity);
        this.threshold = calculateThreshold(initialCapacity, loadFactor);
        this.data = new DoubleInt[initialCapacity];
        init();
    }

    /**
     * Constructor which performs no validation on the passed in parameters.
     *
     * @param initialCapacity the initial capacity, must be a power of two
     * @param loadFactor      the load factor, must be &gt; 0.0f and generally &lt; 1.0f
     * @param threshold       the threshold, must be sensible
     */
    public LinkedDoubleIntHashSet(final int initialCapacity, final @Range(from = 0, to = 1) float loadFactor, final int threshold) {
        this.loadFactor = loadFactor;
        this.data = new DoubleInt[initialCapacity];
        this.threshold = threshold;
        init();
    }

    /**
     * Constructor copying elements from another Collection.
     *
     * @param coll the collection to copy
     * @throws NullPointerException if the collection is null
     */
    public LinkedDoubleIntHashSet(final @NotNull Collection<? extends DoubleInt> coll) {
        this(Math.max(2 * coll.size(), DEFAULT_CAPACITY), DEFAULT_LOAD_FACTOR);
        addAll(coll);
    }

    /**
     * Checks whether the set contains the specified value.
     *
     * @param value1 the first value
     * @param value2 the second value
     * @return true if the set contains the values
     */
    @Contract(pure = true)
    public boolean contains(final int value1, final int value2) {
        final int hashCode = DoubleInt.hash(value1, value2);
        DoubleInt entry = data[hashCode & data.length - 1];
        while (entry != null) {
            if (entry.hashCode == hashCode && isEntryEqual(entry, value1, value2)) {
                return true;
            }
            entry = entry.next;
        }
        return false;
    }

    /**
     * Returns {@code true} if this collection contains the specified element.
     * More formally, returns {@code true} if and only if this collection
     * contains at least one element {@code e} such that
     * {@code Objects.equals(o, e)}.
     *
     * @param o element whose presence in this collection is to be tested
     * @return {@code true} if {@code Object o} is {@link DoubleInt},
     * and an object with the same hash was added into this set.
     */
    @Contract(pure = true)
    @Override
    public boolean contains(@Nullable Object o) {
        if (o instanceof DoubleInt entry) {
            final int hashCode = entry.hashCode();
            DoubleInt other = data[hashIndex(hashCode, data.length)]; // no local for hash index
            while (other != null) {
                if (other.hashCode == hashCode && entry.equals(other)) {
                    return true;
                }
                other = other.next;
            }
        }
        return false;
    }

    /**
     * Stores the value against the specified value pair.
     *
     * @param value1 the first value
     * @param value2 the second value
     * @return true, if the values got added to the set, else false.
     * A Value pair will not get added into the set, if it already inside.
     */
    @Contract(mutates = "this")
    public boolean add(final int value1, final int value2) {
        final int hashCode = DoubleInt.hash(value1, value2);
        final int index = hashCode & data.length - 1;
        DoubleInt entry = data[index];
        while (entry != null) {
            if (entry.hashCode == hashCode && isEntryEqual(entry, value1, value2)) {
                return false;
            }
            entry = entry.next;
        }
        addMappingEnd(index, new DoubleInt(value1, value2, hashCode));
        return true;
    }

    /**
     * Puts a value into this set.
     *
     * @param values the values to add
     * @return true if the new values where added, false otherwise.
     * In most cases, when the result is false, that means this set already contains the values.
     */
    @Contract(mutates = "this")
    public boolean add(final @NotNull DoubleInt values) {
        final int hashCode = values.hashCode();
        final int index = hashIndex(hashCode, data.length);
        DoubleInt entry = data[index];
        while (entry != null) {
            if (entry.hashCode == hashCode && values.equals(entry)) {
                return false;
            }
            entry = entry.next;
        }

        addMappingEnd(index, values);
        return true;
    }

    /**
     * Adds all the elements of the specified collection to this set.
     * The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in progress.
     * (This implies that the behavior of this call is undefined if the
     * specified collection is this collection, and this collection is
     * nonempty.) If the specified collection has a defined
     * <a href="SequencedCollection.html#encounter">encounter order</a>,
     * processing of its elements generally occurs in that order.
     *
     * @param collection collection containing elements to be added to this collection
     * @return {@code true} if this collection changed as a result of the call
     * @throws UnsupportedOperationException if the {@code addAll} operation
     *                                       is not supported by this collection
     * @throws ClassCastException            if the class of an element of the specified
     *                                       collection prevents it from being added to this collection
     * @throws NullPointerException          if the specified collection contains a
     *                                       null element and this collection does not permit null elements,
     *                                       or if the specified collection is null
     * @throws IllegalArgumentException      if some property of an element of the
     *                                       specified collection prevents it from being added to this
     *                                       collection
     * @throws IllegalStateException         if not all the elements can be added at
     *                                       this time due to insertion restrictions
     * @see #add(DoubleInt)
     * <p>
     * Copies all the values from the specified set to this set.
     * All values must be non-null and a DoubleInt object.
     * <p>
     * This implementation iterates around the specified set and
     * uses {@link #add(DoubleInt)}.
     */
    @Contract(mutates = "this")
    public boolean addAll(final @NotNull Collection<? extends @NotNull DoubleInt> collection) {
        for (final DoubleInt valuePair : collection) {
            // Check to ensure that input values are valid DoubleInt objects.
            Objects.requireNonNull(valuePair, "valuePair");
        }

        final int mapSize = collection.size();
        if (mapSize == 0) {
            return false;
        }

        boolean changed = false;

        final int newSize = (int) ((size + mapSize) / loadFactor + 1);
        ensureCapacity(calculateNewCapacity(newSize));
        for (final DoubleInt entry : collection) {
            changed = changed || add(entry);
        }

        return changed;
    }

    /**
     * Adds all the elements of the specified collection to this set,
     * but clones all values, so they don't share the same reference.
     * <p>
     * The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in progress.
     * (This implies that the behavior of this call is undefined if the
     * specified collection is this collection, and this collection is
     * nonempty.) If the specified collection has a defined
     * <a href="SequencedCollection.html#encounter">encounter order</a>,
     * processing of its elements generally occurs in that order.
     * <p/>
     *
     * @param collection collection containing elements to be added to this collection
     * @return {@code true} if this collection changed as a result of the call
     * @throws UnsupportedOperationException if the {@code addAll} operation
     *                                       is not supported by this collection
     * @throws ClassCastException            if the class of an element of the specified
     *                                       collection prevents it from being added to this collection
     * @throws NullPointerException          if the specified collection contains a
     *                                       null element and this collection does not permit null elements,
     *                                       or if the specified collection is null
     * @throws IllegalArgumentException      if some property of an element of the
     *                                       specified collection prevents it from being added to this
     *                                       collection
     * @throws IllegalStateException         if not all the elements can be added at
     *                                       this time due to insertion restrictions
     * @see #add(DoubleInt)
     * <p>
     * Copies all the values from the specified set to this set.
     * All values must be non-null and a DoubleInt object.
     * <p>
     * This implementation iterates around the specified set and
     * uses {@link #add(DoubleInt)}.
     */
    public boolean addAllCloned(final @NotNull Collection<? extends @NotNull DoubleInt> collection) {
        for (final DoubleInt valuePair : collection) {
            // Check to ensure that input values are valid DoubleInt objects.
            Objects.requireNonNull(valuePair, "valuePair");
        }

        final int mapSize = collection.size();
        if (mapSize == 0) {
            return false;
        }

        boolean changed = false;

        final int newSize = (int) ((size + mapSize) / loadFactor + 1);
        ensureCapacity(calculateNewCapacity(newSize));
        for (final DoubleInt entry : collection) {
            changed = changed || add(entry.clone());
        }

        return changed;
    }

    /**
     * Adds an element as the first element of this collection (optional operation).
     * After this operation completes normally, the given element will be a member of
     * this collection, and it will be the first element in encounter order.
     * If this element already is in the set, this will do nothing.
     *
     * @param newEntry the element to be added
     * @throws NullPointerException if the specified element is null and this
     *                              collection does not permit null elements
     */
    @Override
    @Contract(mutates = "this")
    public void addFirst(@NotNull DoubleInt newEntry) {
        final int hashCode = newEntry.hashCode();
        final int index = hashIndex(hashCode, data.length);
        DoubleInt entry = data[index];
        while (entry != null) {
            if (entry.hashCode == hashCode && newEntry.equals(entry)) {
                return;
            }
            entry = entry.next;
        }

        addMappingBegin(index, newEntry);
    }

    /**
     * Adds an element as the last element of this collection (optional operation).
     * After this operation completes normally, the given element will be a member of
     * this collection, and it will be the last element in encounter order.
     * If this element already is in the set, this will do nothing.
     *
     * @param newEntry the element to be added.
     * @throws NullPointerException          if the specified element is null and this
     *                                       collection does not permit null elements
     * @throws UnsupportedOperationException if this collection implementation
     *                                       does not support this operation
     */
    @Override
    @Contract(mutates = "this")
    public void addLast(@NotNull DoubleInt newEntry) {
        final int hashCode = newEntry.hashCode();
        final int index = hashIndex(hashCode, data.length);
        DoubleInt entry = data[index];
        while (entry != null) {
            if (entry.hashCode == hashCode && newEntry.equals(entry)) {
                return;
            }
            entry = entry.next;
        }

        addMappingEnd(index, newEntry);
    }

    /**
     * Gets the entry mapped to the values specified.
     * This will probably only be needed in very specific situations...
     *
     * @param value1 the first value
     * @param value2 the second value
     * @return the entry, null if no match
     */
    public @Nullable DoubleInt getEntry(int value1, int value2) {
        final int hashCode = DoubleInt.hash(value1, value2);
        DoubleInt entry = data[hashIndex(hashCode, data.length)]; // no local for hash index
        while (entry != null) {
            if (entry.hashCode == hashCode && isEntryEqual(entry, value1, value2)) {
                return entry;
            }
            entry = entry.next;
        }

        return null;
    }

    /**
     * Gets the first element of this collection.
     *
     * @return the retrieved element
     * @throws NoSuchElementException if this collection is empty
     */
    @Override
    @Contract(pure = true)
    public @NotNull DoubleInt getFirst() throws NoSuchElementException {
        if (size == 0 || header == null) {
            throw new NoSuchElementException("Set is empty");
        }
        return header.after;
    }

    /**
     * Gets the last element of this collection, which is the most recently inserted.
     *
     * @return the retrieved element
     * @throws NoSuchElementException if this collection is empty
     */
    @Override
    @Contract(pure = true)
    public @NotNull DoubleInt getLast() throws NoSuchElementException {
        if (size == 0 || header == null) {
            throw new NoSuchElementException("set is empty");
        }
        return header.before;
    }

    /**
     * Removes the specified {@code DoubleInt} from this set.
     *
     * @param value1 the first value
     * @param value2 the second value
     * @return if anything was removed and the set has changed or not
     */
    @Contract(mutates = "this")
    public boolean remove(final int value1, final int value2) {
        final int hashCode = DoubleInt.hash(value1, value2);
        final int index = hashCode & data.length - 1;
        DoubleInt entry = data[index];
        DoubleInt previous = null;
        while (entry != null) {
            if (entry.hashCode == hashCode && isEntryEqual(entry, value1, value2)) {
                removeMapping(entry, index, previous);
                return true;
            }
            previous = entry;
            entry = entry.next;
        }
        return false;
    }

    /**
     * Removes a single instance of the specified element from this
     * collection, if it is present (optional operation).  More formally,
     * removes an element {@code e} such that
     * {@code Objects.equals(o, e)}, if
     * this collection contains one or more such elements.  Returns
     * {@code true} if this collection contained the specified element (or
     * equivalently, if this collection changed as a result of the call).
     *
     * @param element element to be removed from this collection, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException   if the type of the specified element
     *                              is incompatible with this collection
     *                              ({@linkplain Collection##optional-restrictions optional})
     * @throws NullPointerException if the specified element is null and this
     *                              collection does not permit null elements
     *                              ({@linkplain Collection##optional-restrictions optional})
     */
    @Override
    @Contract(mutates = "this")
    public boolean remove(Object element) {
        if (element instanceof DoubleInt entry) {
            final int hashCode = entry.hashCode();
            final int index = hashIndex(hashCode, data.length);
            DoubleInt other = data[index];
            DoubleInt previous = null;
            while (other != null) {
                if (other.hashCode == hashCode && entry.equals(other)) {
                    removeMapping(other, index, previous);
                    return false;
                }
                previous = other;
                other = other.next;
            }
        }
        return true;
    }

    /**
     * Removes and returns the first element of this set.
     *
     * @return the removed element
     * @throws NoSuchElementException if this collection is empty
     */
    @Override
    public @NotNull DoubleInt removeFirst() {
        if (size() > 0 && header != null) {
            DoubleInt value = header;
            remove(header);
            return value;
        } else {
            throw new NoSuchElementException("empty");
        }
    }

    /**
     * Removes and returns the last element of this set.
     *
     * @return the removed element
     * @throws NoSuchElementException if this collection is empty
     */
    @Override
    public DoubleInt removeLast() {
        if (size() > 0 && header != null) {
            DoubleInt value = header.before;
            remove(header.before);
            return value;
        } else {
            throw new NoSuchElementException("empty");
        }
    }

    /**
     * Removes all {@code DoubleInt} where the first value matches the one specified.
     *
     * @param value the first value
     * @return true if any elements were removed
     */
    @Contract(mutates = "this")
    public boolean removeAllFirstVal(final int value) {
        boolean modified = false;
        if (size > 0) {
            final SequencedIterator<DoubleInt> it = iterator();
            while (it.hasNext()) {
                final DoubleInt next = it.next();
                if (value == next.getValue1()) {
                    it.remove();
                    modified = true;
                }
            }
        }
        return modified;
    }

    /**
     * Clears the set, resetting the size to zero and nullifying references
     * to avoid garbage collection issues.
     */
    @Override
    @Contract(mutates = "this")
    public void clear() {
        modCount++;
        final DoubleInt[] data = this.data;
        Arrays.fill(data, null);
        size = 0;
        header = null;
    }

    /**
     * Gets the size of the set.
     *
     * @return the size
     */
    @Override
    @Contract(pure = true)
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     * Checks whether the set is currently empty.
     *
     * @return true if the set is currently size zero
     */
    @Override
    @Contract(pure = true)
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return a reverse-ordered view of this collection, as a {@code SequencedSet}
     */
    public @NotNull SequencedSet<@NotNull DoubleInt> reversed() {
        class ReverseLinkedDoubleHashSetView extends AbstractSet<DoubleInt> implements SequencedSet<DoubleInt> {
            public int size() {
                return LinkedDoubleIntHashSet.this.size();
            }

            public @NotNull Iterator<DoubleInt> iterator() {
                if (isEmpty()) {
                    return EmptySequenceIterator.INSTANCE;
                }
                return new LinkIterator(LinkedDoubleIntHashSet.this, true);
            }

            public boolean add(DoubleInt e) {
                return LinkedDoubleIntHashSet.this.add(e);
            }

            public void addFirst(DoubleInt e) {
                LinkedDoubleIntHashSet.this.addLast(e);
            }

            public void addLast(DoubleInt e) {
                LinkedDoubleIntHashSet.this.addFirst(e);
            }

            public DoubleInt getFirst() {
                return LinkedDoubleIntHashSet.this.getLast();
            }

            public DoubleInt getLast() {
                return LinkedDoubleIntHashSet.this.getFirst();
            }

            public DoubleInt removeFirst() {
                return LinkedDoubleIntHashSet.this.removeLast();
            }

            public DoubleInt removeLast() {
                return LinkedDoubleIntHashSet.this.removeFirst();
            }

            public @NotNull SequencedSet<DoubleInt> reversed() {
                return LinkedDoubleIntHashSet.this;
            }

            public Object @NotNull [] toArray() {
                return toArrayInternal(new DoubleInt[LinkedDoubleIntHashSet.this.size()], true);
            }

            public <T> T @NotNull [] toArray(T @NotNull [] a) {
                return toArrayInternal(prepareArray(a), true);
            }
        }

        return new ReverseLinkedDoubleHashSetView();
    }

// Theoretically you could work with indices on this set, like you could on a list.
// Practically this is always slow, since we have to iterate through the set to get it to work
// And it could be quite confusing since many methods get addressed by int (values)
//    /**
//     * Gets the index of the specified entry.
//     *
//     * @param value1  the fist value to find the index of
//     * @param value2  the second value to find the index of
//     * @return the index, or -1 if not found
//     */
//    public int indexOf(int value1, int value2) {
//        if (size != 0 && header != null) {
//        int i = 0;
//            for (DoubleInt entry = header.after; entry != header; entry = entry.after, i++) {
//                if (isEqual(entry, value1, value2)) {
//                    return i;
//                }
//            }
//        }
//        return CollectionUtils.INDEX_NOT_FOUND;
//    }
//
//    /**
//     * Gets the index of the specified entry.
//     *
//     * @param entry  the entry to find the index of
//     * @return the index, or -1 if not found
//     */
//    public int indexOf(@NotNull DoubleInt entry) {
//        if (size != 0 && header != null) {
//            int i = 0;
//            for (DoubleInt other = header.after; entry != header; entry = entry.after, i++) {
//                if (entry.equals(other)) {
//                    return i;
//                }
//            }
//        }
//        return CollectionUtils.INDEX_NOT_FOUND;
//    }
//    /**
//     * Gets the entry at the specified index.
//     *
//     * @param index  the index to retrieve
//     * @return the entry at the specified index
//     * @throws IndexOutOfBoundsException if the index is invalid
//     */
//    public DoubleInt get(final int index) {
//        return getEntry(index);
//    }
//
//    /**
//     * Removes the element at the specified index.
//     *
//     * @param index  the index of the object to remove
//     * @return the previous value corresponding the {@code entry},
//     *  or {@code false} if none existed
//     * @throws IndexOutOfBoundsException if the index is invalid
//     */
//    public boolean remove(final int index) {
//        return remove(get(index));
//    }
//
//    /**
//     * Gets the entry at the specified index.
//     *
//     * @param index  the index to retrieve
//     * @return the entry at the specified index
//     * @throws IndexOutOfBoundsException if the index is invalid
//     */
//    protected DoubleInt getEntry(final int index) throws IndexOutOfBoundsException { // todo handle empty set
//        if (index < 0) {
//            throw new IndexOutOfBoundsException("Index " + index + " is less than zero");
//        }
//        if (index >= size) {
//            throw new IndexOutOfBoundsException("Index " + index + " is invalid for size " + size);
//        }
//        DoubleInt entry;
//        if (index < size / 2) {
//            // Search forwards
//            entry = header.after;
//            for (int currentIndex = 0; currentIndex < index; currentIndex++) {
//                entry = entry.after;
//            }
//        } else {
//            // Search backwards
//            entry = header;
//            for (int currentIndex = size; currentIndex > index; currentIndex--) {
//                entry = entry.before;
//            }
//        }
//        return entry;
//    }

    /**
     * Returns an iterator over the elements in this collection.  There are no
     * guarantees concerning the order in which the elements are returned
     * (unless this collection is an instance of some class that provides a
     * guarantee).
     *
     * @return an {@code Iterator} over the elements in this collection
     */
    @Override
    public @NotNull SequencedIterator<@NotNull DoubleInt> iterator() {
        if (isEmpty()) {
            return EmptySequenceIterator.INSTANCE;
        }
        return new LinkIterator(this, false);
    }

    /**
     * Returns an array containing all the elements of this collection.
     * If this collection makes any guarantees as to what order its elements
     * are returned by its iterator, this method must return the elements in
     * the same order. The returned array's {@linkplain Class#getComponentType
     * runtime component type} is {@code Object}.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this collection.  (In other words, this method must
     * allocate a new array even if this collection is backed by an array).
     * The caller is thus free to modify the returned array.
     *
     * @return an array, whose {@linkplain Class#getComponentType runtime component
     * type} is {@code Object}, containing all of the elements in this collection
     * @apiNote This method acts as a bridge between array-based and collection-based APIs.
     * It returns an array whose runtime type is {@code Object[]}.
     * Use {@link #toArray(Object[]) toArray(T[])} to reuse an existing
     * array, or use {@link #toArray(IntFunction)} to control the runtime type
     * of the array.
     */
    @Override
    public @NotNull DoubleInt @NotNull [] toArray() {
        return toArrayInternal(new DoubleInt[size()], false);
    }

    @Override
    public <T> @NotNull T @NotNull [] toArray(@NotNull T @NotNull [] a) {
        return toArrayInternal(prepareArray(a), false);
    }

    /**
     * Compares this set with another.
     *
     * @param obj the object to compare to
     * @return true if equal
     */
    @Override
    public boolean equals(final @Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof LinkedDoubleIntHashSet other) {
            if (other.size() == size()) {
                final Iterator<DoubleInt> it = iterator();

                try {
                    while (it.hasNext()) {
                        final DoubleInt next = it.next();

                        if (next == null || !other.contains(next)) {
                            return false;
                        }
                    }
                } catch (final ClassCastException | NullPointerException ignored) {
                    return false;
                }

            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int total = size();
        for (DoubleInt entry : this) {
            total = 31 * total + entry.hashCode();
        }
        return total;
    }

    /**
     * Returns a string representation of this set.  The string representation
     * consists of a list of values in the order returned by the
     * iterator, enclosed in braces ({@code "{}"}).
     * Adjacent values are separated by the characters
     * {@code ", "} (comma and space). Values are converted to strings
     * by their {@code .toString()} methode
     *
     * @return a string representation of this set
     */
    public @NotNull String toString() {
        Iterator<DoubleInt> i = iterator();
        if (!i.hasNext()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (; ; ) {
            DoubleInt e = i.next();
            sb.append(e);
            if (!i.hasNext()) {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }

    //==================================================================
    // Behind the curtain

    /**
     * Initialize this subclass during construction.
     */
    protected void init() {
        header = null;
    }

    /**
     * Gets the index into the data storage for the hashCode specified.
     * This implementation uses the least significant bits of the hashCode.
     *
     * @param hashCode the hash code to use
     * @param dataSize the size of the data to pick a bucket from
     * @return the bucket index
     */
    protected int hashIndex(final int hashCode, final int dataSize) {
        return hashCode & dataSize - 1;
    }

    /**
     * Is the entry equal to the combined values.
     *
     * @param entry  the entry to compare to
     * @param value1 the first value
     * @param value2 the second value
     * @return true if the entry matches
     */
    protected boolean isEntryEqual(final DoubleInt entry, final int value1, final int value2) {
        return value1 == entry.getValue1() && value2 == entry.getValue2();
    }

    protected <T> T @NotNull [] toArrayInternal(@NotNull T[] a, boolean reversed) {
        int idx = 0;
        if (reversed) {
            if (header != null) {
                for (DoubleInt e = header.before; e != null; e = e.before) {
                    ((Object[]) a)[idx++] = e;
                }
            }
        } else {
            for (DoubleInt e = header; e != null; e = e.after) {
                ((Object[]) a)[idx++] = e;
            }
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    protected final <T> T[] prepareArray(T[] a) {
        int size = this.size;
        if (a.length < size) {
            return (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    /**
     * Adds a new value into this set, maintaining insertion order.
     * <p>
     * This implementation calls {@code checkCapacity()}.
     * It also handles changes to {@code modCount} and {@code size}.
     * This implementation adds the entry to the data storage table and
     * to the end of the linked set.
     *
     * @param hashIndex the index into the data array to store at
     * @param entry     the entry to add
     */
    protected void addMappingEnd(final int hashIndex, final @NotNull DoubleInt entry) {
        modCount++;
        if (size == 0 || header == null) {
            entry.after = entry;
            entry.before = entry;
        } else {
            entry.after = header;
            entry.before = header.before;
            header.before.after = entry;
            header.before = entry;
        }
        entry.next = data[hashIndex];
        data[hashIndex] = entry;
        size++;
        checkCapacity();
    }

    /**
     * Adds a new value into this set, maintaining insertion order.
     * <p>
     * This implementation calls {@code checkCapacity()}.
     * It also handles changes to {@code modCount} and {@code size}.
     * This implementation adds the entry to the data storage table and
     * to the beginning of the linked list.
     *
     * @param hashIndex the index into the data array to store at
     * @param entry     the entry to add
     */
    protected void addMappingBegin(final int hashIndex, final @NotNull DoubleInt entry) {
        modCount++;
        if (size == 0 || header == null) {
            entry.before = entry;
            entry.after = entry;
        } else {
            entry.before = header;
            entry.after = header.after;
            header.before = entry;
            header.after.before = entry;
        }
        entry.next = data[hashIndex];
        data[hashIndex] = entry;
        size++;
        checkCapacity();
    }

    /**
     * Calculates the new capacity of the set.
     * This implementation normalizes the capacity to a power of two.
     *
     * @param proposedCapacity the proposed capacity
     * @return the normalized new capacity
     */
    protected int calculateNewCapacity(final int proposedCapacity) {
        int newCapacity = 1;
        if (proposedCapacity >= MAXIMUM_CAPACITY) {
            newCapacity = MAXIMUM_CAPACITY;
        } else {
            while (newCapacity < proposedCapacity) {
                newCapacity <<= 1;  // multiply by two
            }
            if (newCapacity > MAXIMUM_CAPACITY) {
                newCapacity = MAXIMUM_CAPACITY;
            }
        }
        return newCapacity;
    }

    /**
     * Calculates the new threshold of the set, where it will be resized.
     * This implementation uses the load factor.
     *
     * @param newCapacity the new capacity
     * @param factor      the load factor
     * @return the new resize threshold
     */
    protected int calculateThreshold(final int newCapacity, final float factor) {
        return (int) (newCapacity * factor);
    }

    /**
     * Checks the capacity of the set and enlarges it if necessary.
     * <p>
     * This implementation uses the threshold to check if the set needs enlarging
     */
    protected void checkCapacity() {
        if (size >= threshold) {
            final int newCapacity = data.length * 2;
            if (newCapacity <= MAXIMUM_CAPACITY) {
                ensureCapacity(newCapacity);
            }
        }
    }

    /**
     * Changes the size of the data structure to the capacity proposed.
     *
     * @param newCapacity the new capacity of the array (a power of two, less or equal to max)
     */
    protected void ensureCapacity(final int newCapacity) {
        final int oldCapacity = data.length;
        if (newCapacity <= oldCapacity) {
            return;
        }
        if (size == 0) {
            threshold = calculateThreshold(newCapacity, loadFactor);
            data = new DoubleInt[newCapacity];
        } else {
            final DoubleInt[] oldEntries = data;
            final DoubleInt[] newEntries = new DoubleInt[newCapacity];

            modCount++;
            for (int i = oldCapacity - 1; i >= 0; i--) {
                DoubleInt entry = oldEntries[i];
                if (entry != null) {
                    oldEntries[i] = null;  // gc
                    do {
                        final DoubleInt next = entry.next;
                        final int index = hashIndex(entry.hashCode, newCapacity);
                        entry.next = newEntries[index];
                        newEntries[index] = entry;
                        entry = next;
                    } while (entry != null);
                }
            }
            threshold = calculateThreshold(newCapacity, loadFactor);
            data = newEntries;
        }
    }

    /**
     * Removes an entry from the set and the linked list.
     * <p>
     * This implementation removes the entry from the linked list chain, then
     * removes the entry from the data storage table.
     * The size is not updated.
     *
     * @param entry     the entry to remove
     * @param hashIndex the index into the data structure
     * @param previous  the previous entry in the chain
     */
    protected void removeEntry(final DoubleInt entry, final int hashIndex, final DoubleInt previous) {
        entry.before.after = entry.after;
        entry.after.before = entry.before;
        entry.after = null;
        entry.before = null;

        if (previous == null) {
            data[hashIndex] = entry.next;
        } else {
            previous.next = entry.next;
        }
    }

    /**
     * Kills an entry ready for the garbage collector.
     * <p>
     * This implementation prepares the DoubleInt for garbage collection.
     *
     * @param entry the entry to destroy
     */
    protected void destroyEntry(final @NotNull DoubleInt entry) {
        entry.next = null;
        entry.after = null;
        entry.before = null;
    }

    /**
     * Removes a value from the set.
     * <p>
     * This implementation calls {@code removeEntry()} and {@code destroyEntry()}.
     * It also handles changes to {@code modCount} and {@code size}.
     * Subclasses could override to fully control removals from the set.
     *
     * @param entry     the entry to remove
     * @param hashIndex the index into the data structure
     * @param previous  the previous entry in the chain
     */
    protected void removeMapping(final DoubleInt entry, final int hashIndex, final DoubleInt previous) {
        modCount++;
        removeEntry(entry, hashIndex, previous);
        size--;
        destroyEntry(entry);
    }

    /**
     * Clones the set as well as all the entries.
     * <p>
     * To implement {@code clone()}, a subclass must implement the
     * {@code Cloneable} interface and make this method public.
     *
     * @return a shallow clone
     */
    @Override
    protected LinkedDoubleIntHashSet clone() throws CloneNotSupportedException {
        try {
            final LinkedDoubleIntHashSet cloned = (LinkedDoubleIntHashSet) super.clone();
            cloned.data = new DoubleInt[data.length];
            cloned.modCount = 0;
            cloned.size = 0;
            cloned.init();
            cloned.addAllCloned(this);
            return cloned;
        } catch (final CloneNotSupportedException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    /**
     * Base Iterator that iterates in link order.
     */
    protected static class LinkIterator implements SequencedIterator<DoubleInt> {
        /// The parent set
        protected final @NotNull LinkedDoubleIntHashSet parent;
        protected final boolean reversed;
        /// The current (last returned) entry
        protected DoubleInt last;
        /// The next entry
        protected DoubleInt next;
        /// The modification count expected
        protected int expectedModCount;

        protected LinkIterator(final @NotNull LinkedDoubleIntHashSet parent, boolean reversed) {
            this.parent = parent;
            if (parent.header != null) {
                if (reversed) {
                    this.next = parent.header.before;
                } else {
                    this.next = parent.header.after;
                }
            } else {
                this.next = null;
            }
            this.expectedModCount = parent.modCount;

            this.reversed = reversed;
        }

        @Override
        public DoubleInt next() {
            return nextEntry();
        }

        @Override
        public DoubleInt previous() {
            return previousEntry();
        }

        protected DoubleInt currentEntry() {
            return last;
        }

        public boolean hasNext() {
            return next != parent.header;
        }

        public boolean hasPrevious() {
            if (reversed) {
                return next.before != parent.header;
            } else {
                return next.after != parent.header;
            }
        }

        /**
         * Don't forget to check {@link #hasNext()},
         * as this will throw a {@link NoSuchElementException} if it loops around
         */
        protected @NotNull DoubleInt nextEntry() {
            if (parent.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (next == parent.header) {
                throw new NoSuchElementException(NO_NEXT_ENTRY);
            }
            last = next;

            if (reversed) {
                next = next.before;
            } else {
                next = next.after;
            }

            return last;
        }

        /**
         * Don't forget to check {@link #hasNext()},
         * as this will throw a {@link NoSuchElementException} if it loops around
         */
        protected @NotNull DoubleInt previousEntry() {
            if (parent.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            final DoubleInt previous;
            if (reversed) {
                previous = next.after;
            } else {
                previous = next.before;
            }

            if (previous == parent.header) {
                throw new NoSuchElementException(NO_PREVIOUS_ENTRY);
            }
            next = previous;
            last = previous;
            return last;
        }

        public void remove() {
            if (last == null) {
                throw new IllegalStateException(REMOVE_INVALID);
            }
            if (parent.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            parent.remove(last);
            last = null;
            expectedModCount = parent.modCount;
        }

        public void reset() {
            last = null;
            if (parent.header != null) {
                if (reversed) {
                    next = parent.header.before;
                } else {
                    next = parent.header.after;
                }
            } else {
                this.next = null;
            }
        }

        @Override
        public String toString() {
            if (last != null) {
                return "Iterator[" + last + "]";
            }
            return "Iterator[]";
        }
    }

    /**
     * A {@code DoubleInt} allows two integers to act as a union.
     * <p>
     * This way we don't need a map of sets or unnecessary big matrix to
     * store and address the same 2d coordinates efficient.
     * </p>
     */
    public static class DoubleInt implements Cloneable {
        /// The individual values
        private final int value1;
        private final int value2;
        /// The entry before this one in the order
        protected DoubleInt before;
        /// The entry after this one in the order
        protected DoubleInt after;
        /// The next entry in the hash chain
        // next is an entry with the same hashIndex in the array
        protected @Nullable DoubleInt next;
        /// The hash code of the value
        protected final int hashCode;

        public DoubleInt(final int value1, final int value2) {
            this.value1 = value1;
            this.value2 = value2;
            this.hashCode = hash(value1, value2);
        }

        /**
         * internal used constructor to calculate the hashCode only once
         */
        protected DoubleInt(final int value1, final int value2, final int hashCode) {
            this.value1 = value1;
            this.value2 = value2;
            this.hashCode = hashCode;
        }

        /**
         * Gets the hash code for the specified double-value.
         *
         * @param value1 the first value
         * @param value2 the second value
         * @return the hash code
         */
        protected static int hash(final int value1, final int value2) {
            int h = value1 ^ value2;

            return h ^ (h >>> 16);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof DoubleInt other) {
                return value1 == other.value1 && value2 == other.value2;
            }
            return false;
        }

        public int getValue1() {
            return value1;
        }

        public int getValue2() {
            return value2;
        }

        /**
         * Gets the hashcode that was computed and cached combined of both ints.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public @NotNull String toString() {
            return "DoubleInt[" + value1 + ", " + value2 + "]";
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public DoubleInt clone() {
            return new DoubleInt(value1, value2, hashCode);
        }
    }

    public interface SequencedIterator<T> extends Iterator<T> {

        /**
         * Checks to see if there is a previous element that can be iterated to.
         *
         * @return {@code true} if the iterator has a previous element
         */
        boolean hasPrevious();

        /**
         * Gets the previous element of the sequence.
         *
         * @return the previous element in the iteration
         * @throws java.util.NoSuchElementException if the iteration is finished
         */
        T previous();

        /**
         * Resets the iterator back to the position at which the iterator
         * was created.
         */
        void reset();
    }

    protected static class EmptySequenceIterator<E> implements SequencedIterator<E> {
        protected static SequencedIterator INSTANCE = new EmptySequenceIterator();

        protected EmptySequenceIterator() {
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public E next() {
            throw new NoSuchElementException("Iterator contains no elements");
        }

//        public int nextIndex() {
//            return 0;
//        }

        @Override
        public E previous() {
            throw new NoSuchElementException("Iterator contains no elements");
        }

//        public int previousIndex() {
//            return -1;
//        }

        @Override
        public void remove() {
            throw new IllegalStateException("Iterator contains no elements");
        }

        @Override
        public void reset() {
            // do nothing
        }

//        public void set(final E obj) {
//            throw new IllegalStateException("Iterator contains no elements");
//        }
    }
}
