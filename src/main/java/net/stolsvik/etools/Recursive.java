package net.stolsvik.etools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A tool to prevent "stack explosions" when doing recursive calls, typically by calculating something like
 * <code>double sum() { return this.getValue() + <b>parent.sum()</b>; }</code>. If there is a massive parent chain,
 * the java stack will explode. This tool lets you define the recursion and the calculation in two separate
 * closures, so that the tool then can invoke them in an iterative fashion.
 * <p/>
 * It will by default provide automatic caching/memoization when employing {@link #recurse(Function, Supplier)}.
 * Note that to get memoization, you need to put an instance of this class as state on each of your instances in the
 * parent-chain, i.e. define it as a private field of the "node" class, and then provide a getter for the value,
 * i.e. <code>double sum() { return this.recurser.get(); }</code>.
 * Here's a "Node" that utilizes this class (employed in the JUnit test):
 * <pre>
 * private static class Node {
 *     private Long _value = (long) (Math.random() * 1_000_000);
 *
 *     Node _parentNode;
 *
 *     private Recursive<Long> _recursive = Recursive.recurse(
 *                    parentSum -> _value + parentSum
 *                    () -> _parentNode == null ? Recursive.stop(0L) : _parentNode._recursive);
 *
 *     long getRecursiveSum() {
 *         return _recursive.get();
 *     }
 * }
 * </pre>
 * When researching the problem and solutions for Java, I assumed that this was an instance of "tail recursion".
 * However, it probably isn't, since you change object on which invoke "the same" method on (i.e. 'this' changes), even
 * though the call is at the very end.
 *
 * @param <T> the type of the value that will be calculated and returned by {@link #get()}.
 *
 * @author Endre Stølsvik, 2017-09-08 18:15 - http://endre.stolsvik.com/
 */
public interface Recursive<T> {

    /**
     * @return gets the value, either by returning the cached value, or by firing up the recursive->iterative magic.
     */
    T get();

    /**
     * Returns a Recursive with caching.
     *
     * @param reducer   The calculation that needs to be performed, as a Function&lt;T&gt;. The value returned from the
     *                  "parent-call" is provided, and you need to do the calculation with that value and this node'
     *                  value.
     * @param recursion A Supplier that should return the same {@link Recursive}-instance as this, only for the parent.
     * @param <T> The type that should be returned by {@link #get()}.
     * @return A Recursive instance, on which you may invoke {@link #get()} to start the recursive calculation, only
     * it will happen iteratively, not exploding the stack.
     */
    static <T> RecursiveActive<T> recurse(Function<T, T> reducer, Supplier<Recursive<T>> recursion) {
        return new RecursiveActive<>(reducer, recursion, true);
    }

    /**
     * A variant of {@link #recurse(Function, Supplier)} that doesn't cache results. This is only useful if the values
     * in the chain can change from invocation to invocation. If the parent chain is long, and you'll end up invoking
     * all elements of it, then this will be incredibly slow. With the Node-example in the class JavaDoc, a parent
     * list of 5000, and invocation of all nodes, the cached runs in &lt;5 ms, and the non-cached in ~750 ms. With
     * 10,000 nodes, you get &lt;10ms vs. ~2,500ms.
     */
    static <T> RecursiveActive<T> recurseNoCache(Function<T, T> reducer, Supplier<Recursive<T>> recursion) {
        return new RecursiveActive<>(reducer, recursion, false);
    }

    static <T> RecursiveStop<T> stop(T identity) {
        return new RecursiveStop<>(identity);
    }
}

interface RecursiveInternal<T> extends Recursive<T> {
    boolean hasValue();
}

class RecursiveActive<T> implements RecursiveInternal<T> {
    private Function<T, T> _reducer;

    private Supplier<Recursive<T>> _recurser;

    private boolean _caching;

    private T _cachedValue;

    RecursiveActive(Function<T, T> reducer, Supplier<Recursive<T>> recurser, boolean caching) {
        _reducer = reducer;
        _recurser = recurser;
        _caching = caching;
    }

    public boolean hasValue() {
        return _cachedValue != null;
    }

    public T get() {
        // ?: Shortcut because of Cached value?
        if (_cachedValue != null) {
            // -> Yes
            return _cachedValue;
        }

        // :: If the underlying parent-chain is actually traversed from start to last, then we should shortcut.

        // ?: Shortcut because parent is RecursiveStop or has cached value already?
        RecursiveInternal<T> parent = (RecursiveInternal<T>) _recurser.get();  // Immediate parent. (we are RecursiveActive, so we must have parent)
        if (parent.hasValue()) {
            // -> Yes, parent is stop or cached.
            T value = _reducer.apply(parent.get());
            if (_caching) {
                _cachedValue = value;
            }
            return value;
        }

        // ----- Parent is NOT RecursiveStop and does not have cached value, need to do recursion -> iteration magic.

        List<RecursiveActive<T>> recursionList = new ArrayList<>(1024);
        recursionList.add(this);  // Above, we already got our parent (which was not Stop nor cached) ...
        recursionList.add((RecursiveActive<T>) parent);  // ... so we'll add both ourselves and parent, ...
        RecursiveInternal<T> recursive = parent;  // ... and then start recursion from parent
        while (true) {
            // This next line is the "recursion point" ...
            recursive = (RecursiveInternal<T>) ((RecursiveActive<T>) recursive)._recurser.get();
            // ?: Is this grand-parent the start of the chain, i.e. where we should stop recursing - or has cached value
            if (recursive.hasValue()) {
                // -> Yes stop or cached value, so break out.
                break;
            }

            // ... and this next line is where it ends up in a list, for subsequent iterative evaluation.
            recursionList.add((RecursiveActive<T>) recursive);
        }

        // :: Iterate backwards, feeding the result from a parent up to the evaluation in the child.
        T currentValue = recursive.get();  // This is the RecursiveStop (identity) instance, OR cached entry.
        for (int i = recursionList.size() - 1; i >= 0; i--) {
            RecursiveActive<T> recursiveActive = recursionList.get(i);
            currentValue = recursiveActive._reducer.apply(currentValue);
            if (_caching) {
                recursiveActive._cachedValue = currentValue;
            }
        }

        return currentValue;
    }
}

class RecursiveStop<T> implements RecursiveInternal<T> {

    private final T _identity;

    RecursiveStop(T identity) {
        _identity = identity;
    }

    public T get() {
        return _identity;
    }

    public boolean hasValue() {
        return true;
    }
}