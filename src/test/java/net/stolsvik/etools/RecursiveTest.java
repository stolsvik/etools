package net.stolsvik.etools;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link Recursive} class.
 *
 * @author Endre Stølsvik, 2017-09-08 20:00 - http://endre.stolsvik.com/
 */
public class RecursiveTest {

    @Test
    public void testFromFirst() {
        List<Node> nodes = getFreshNodesWithParentPointer();
        testFromFirst_(nodes);

        // Do rest too
        testFromLast_(nodes);
        testFromMiddleToFirst_(nodes);
        testFromMiddleToLast_(nodes);
    }

    @Test
    public void testFromLast() {
        List<Node> nodes = getFreshNodesWithParentPointer();
        testFromLast_(nodes);

        // Do rest too
        testFromFirst_(nodes);
        testFromMiddleToFirst_(nodes);
        testFromMiddleToLast_(nodes);
    }

    @Test
    public void testFromMiddleToLast() {
        List<Node> nodes = getFreshNodesWithParentPointer();
        testFromMiddleToLast_(nodes);

        // Do rest too
        testFromMiddleToFirst_(nodes);
        testFromLast_(nodes);
        testFromFirst_(nodes);
    }

    @Test
    public void testFromMiddleToFirst() {
        List<Node> nodes = getFreshNodesWithParentPointer();
        testFromMiddleToFirst_(nodes);

        // Do rest too
        testFromMiddleToLast_(nodes);
        testFromLast_(nodes);
        testFromFirst_(nodes);
    }

    private void testFromFirst_(List<Node> nodes) {
        for (Node n : nodes) {
            Assert.assertEquals(n._iterativeValue, n.getRecursiveSum());
        }
    }

    private void testFromLast_(List<Node> nodes) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node n = nodes.get(i);
            Assert.assertEquals(n._iterativeValue, n.getRecursiveSum());
        }
    }

    private void testFromMiddleToLast_(List<Node> nodes) {
        for (int i = nodes.size() / 2; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            Assert.assertEquals(n._iterativeValue, n.getRecursiveSum());
        }
    }

    private void testFromMiddleToFirst_(List<Node> nodes) {
        for (int i = nodes.size() / 2; i >= 0; i--) {
            Node n = nodes.get(i);
            Assert.assertEquals(n._iterativeValue, n.getRecursiveSum());
        }
    }

    private List<Node> getFreshNodesWithParentPointer() {
        int size = 1_000_000;
        List<Node> nodes = new ArrayList<>(size);
        Node parent = null;
        for (int i = 0; i < size; i++) {
            Node n = new Node();
            nodes.add(n);
            n._parentNode = parent;

            n._iterativeValue = (parent != null ? parent._iterativeValue + n._value : n._value);

            parent = n;
        }
        return nodes;
    }

    private static class Node {
        private Long _value = (long) (Math.random() * 1_000_000);

        private long _iterativeValue;

        Node _parentNode;

        private Recursive<Long> _recursive = Recursive.recurse(
                parentSum -> _value + parentSum,
                () -> _parentNode == null ? Recursive.stop(0L) : _parentNode._recursive);

        long getRecursiveSum() {
            return _recursive.get();
        }
    }
}
