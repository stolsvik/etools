package net.stolsvik.etools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A helper to do trampoline-based tail recursion. Dead slow compared to do things "directly", but sometimes it is
 * neater to code up something using a recursive logic.
 * <p/>
 * An example - here the reducer is just the return from the tail call:
 * <pre>
 * static TailTrampoline<Boolean> isPalindrome(String s) {
 *     if (s.length() <= 1) {
 *         return TailTrampoline.stop(true);
 *     }
 *     if (s.charAt(0) != s.charAt(s.length()-1)) {
 *         return TailTrampoline.stop(false);
 *     }
 *     return TailTrampoline.recurse(s, (in, parent) -> parent, (in) -> isPalindrome(in.substring(1, in.length() - 1)));
 * }
 *
 * Assert.assertEquals(true, isPalindrome("").compute());
 * Assert.assertEquals(true, isPalindrome("x").compute());
 * Assert.assertEquals(true, isPalindrome("xx").compute());
 * Assert.assertEquals(true, isPalindrome("xax").compute());
 * Assert.assertEquals(true, isPalindrome("amanaplanacanalpanama").compute());
 * Assert.assertEquals(false, isPalindrome("amanaplanacxnalpanama").compute());
 * </pre>
 * Another example - here the reducer uses the return from the tail call:
 * <pre>
 * static TailTrampoline<Double> computeFactorial(long n) {
 *     if (n == 0) {
 *         return TailTrampoline.stop(1d);
 *     }
 *     return TailTrampoline.recurse(n, (in, parent) -> in * parent, (in) -> computeFactorial(in - 1));
 * }
 *
 * Assert.assertEquals(1d, computeFactorial(1).compute(), 0);
 * Assert.assertEquals(40320d, computeFactorial(8).compute(), 0);
 * Assert.assertEquals(9.33262154439441e157, computeFactorial(100).compute(), 0);
 * </pre>
 * And an example where the stack would definitely have exploded wasn't it for the trampoline (but the implementation is
 * obviously mindblowingly useless!):
 * <pre>
 * static TailTrampoline<Long> computeTriangular(long n) {
 *     if (n == 0) {
 *         return TailTrampoline.stop(0L);
 *     }
 *     return TailTrampoline.recurse(n, (in, parent) -> in + parent, (in) -> computeTriangular(in - 1));
 * }
 *
 * Assert.assertEquals(triangular(4), computeTriangular(4).compute(), 0);
 * Assert.assertEquals(triangular(500), computeTriangular(500).compute(), 0);
 * Assert.assertEquals(triangular(10000), computeTriangular(10000).compute(), 0);
 * Assert.assertEquals(triangular(2539071), computeTriangular(2539071).compute(), 0);
 *
 * private static long triangular(long n) {
 *     return n*(n+1)/2;
 * }
 * </pre>
 *
 * @param <R> the Return value from the tail recursive trampolined function.
 *
 * @author Endre Stølsvik, 2017-09-08 22:00 - http://endre.stolsvik.com/
 */
public interface TailTrampoline<R> {
    R compute();

    static <I, R> TailTrampolineActive<I, R> recurse(I in, BiFunction<I, R, R> reducer, Function<I, TailTrampoline<R>> recurser) {
        return new TailTrampolineActive<>(in, reducer, recurser);
    }

    static <R> TailTrampolineStop<R> stop(R identity) {
        return new TailTrampolineStop<>(identity);
    }

}

class TailTrampolineStop<R> implements TailTrampoline<R> {

    private final R _identity;

    TailTrampolineStop(R identity) {
        _identity = identity;
    }

    public R compute() {
        return _identity;
    }
}

class TailTrampolineActive<I, R> implements TailTrampoline<R> {
    private I _in;

    private BiFunction<I, R, R> _reducer;

    private Function<I, TailTrampoline<R>> _recurser;

    TailTrampolineActive(I in, BiFunction<I, R, R> reducer, Function<I, TailTrampoline<R>> recurser) {
        _in = in;
        _reducer = reducer;
        _recurser = recurser;
    }

    public R compute() {
        // ?: Shortcut because parent is TailTrampolineStop?
        TailTrampoline<R> parent = _recurser.apply(_in);  // Immediate parent. (we are RecursiveActive, so we must have parent)
        if (parent instanceof TailTrampolineStop) {
            // -> Yes, parent is stop.
            return _reducer.apply(_in, parent.compute());
        }

        List<TailTrampolineActive<I, R>> recursionList = new ArrayList<>(128);
        recursionList.add(this);  // Above, we already got our parent (which wasn't stop) ...
        recursionList.add((TailTrampolineActive<I, R>) parent);  // ... so we'll add both ourselves and parent, ...
        TailTrampolineActive<I, R> trampolineActive = (TailTrampolineActive<I, R>) parent; // ... and then start recursion from parent

        TailTrampoline<R> trampoline;
        while (true) {
            // This next line is the "recursion point" ...
            trampoline = trampolineActive._recurser.apply(trampolineActive._in);
            // ?: Is this grand-parent the exit, i.e. where we should stop recursing.
            if (trampoline instanceof TailTrampolineStop) {
                // -> Yes stop, so break out.
                break;
            }
            trampolineActive = (TailTrampolineActive<I, R>) trampoline;

            // ... and this next line is where it ends up in a list, for subsequent iterative evaluation.
            recursionList.add(trampolineActive);
        }

        // :: Iterate backwards, feeding the result from a parent up to the evaluation in the child.
        R currentValue = trampoline.compute();  // This is the RecursiveStop (identity) instance
        for (int i = recursionList.size() - 1; i >= 0; i--) {
            trampolineActive = recursionList.get(i);
            currentValue = trampolineActive._reducer.apply(trampolineActive._in, currentValue);
        }

        return currentValue;
    }
}
