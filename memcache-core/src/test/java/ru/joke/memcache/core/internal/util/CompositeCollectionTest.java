package ru.joke.memcache.core.internal.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class CompositeCollectionTest {

    @Test
    public void testSizeShouldReturnTotalSizeOfAllCollections() {
        final CompositeCollection<Integer> compositeCollection = new CompositeCollection<>(2);
        final List<Integer> collection1 = List.of(1, 2, 3);
        final List<Integer> collection2 = List.of(4, 5, 6, 7);
        compositeCollection.addCollection(collection1);
        compositeCollection.addCollection(collection2);

        assertEquals(collection1.size() + collection2.size(), compositeCollection.size(), "Size of composite collection must be equal to sum of nested collections size");
        assertEquals(collection1.size() + collection2.size(), compositeCollection.commonCount(), "Common count of composite collection must be equal to sum of nested collections size");
    }

    @Test
    public void testIsEmptyShouldReturnTrueIfNoElementsInAnyCollection() {
        final CompositeCollection<String> compositeCollection = new CompositeCollection<>(0);
        assertTrue(compositeCollection.isEmpty(), "Collection must be empty");
    }

    @Test
    public void testIsEmptyShouldReturnFalseIfAtLeastOneElementInAnyCollection() {
        final CompositeCollection<String> compositeCollection = new CompositeCollection<>(1);
        final List<String> collection1 = List.of("apple", "banana");
        compositeCollection.addCollection(collection1);
        assertFalse(compositeCollection.isEmpty(), "Collection must be not empty");
    }

    @Test
    public void testContainsShouldReturnTrueIfElementExistsInAnyCollection() {
        final CompositeCollection<Integer> compositeCollection = new CompositeCollection<>(2);
        final List<Integer> collection1 = List.of(1, 2, 3);
        final List<Integer> collection2 = List.of(4, 5, 6, 7);
        compositeCollection.addCollection(collection1);
        compositeCollection.addCollection(collection2);

        assertTrue(compositeCollection.contains(5), "Collection must contain element from one of nested collections");
        assertTrue(compositeCollection.containsAll(collection1), "Collection must contain all elements from one of nested collections");
    }

    @Test
    public void testContainsShouldReturnFalseIfElementDoesNotExistInAnyCollection() {
        final CompositeCollection<String> compositeCollection = new CompositeCollection<>(1);
        final List<String> collection1 = List.of("apple", "banana");
        compositeCollection.addCollection(collection1);
        assertFalse(compositeCollection.contains("orange"), "Collection must not contain element");
    }

    @Test
    public void testIteratorShouldReturnAllElementsInAllCollections() {
        final CompositeCollection<String> compositeCollection = new CompositeCollection<>(2);
        final List<String> collection1 = List.of("apple", "banana");
        final List<String> collection2 = List.of("orange");
        compositeCollection.addCollection(collection1);
        compositeCollection.addCollection(collection2);

        final List<String> actualElements = new ArrayList<>();
        for (String element : compositeCollection) {
            actualElements.add(element);
        }

        final List<String> expectedElements = List.of("apple", "banana", "orange");
        assertEquals(expectedElements, actualElements, "Elements collected by iteration must be equal to flat list of nested elements");
    }

    @Test
    public void testIteratorShouldThrowNoSuchElementExceptionWhenThereAreNoMoreElements() {
        final CompositeCollection<String> compositeCollection = new CompositeCollection<>(0);
        assertFalse(compositeCollection.iterator().hasNext(), "hasNext must return false when no elements available");
        assertThrows(NoSuchElementException.class, compositeCollection.iterator()::next, "next should throw exception when no elements available");
    }
}
