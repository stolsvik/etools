package net.stolsvik.etools;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link TailTrampoline} class.
 *
 * @author Endre Stølsvik, 2017-09-08 23:00 - http://endre.stolsvik.com/
 */
public class TailTrampolineTest {
    @Test
    public void testTriangular() {
        Assert.assertEquals(triangular(0), computeTriangular(0).compute(), 0);
        Assert.assertEquals(triangular(1), computeTriangular(1).compute(), 0);
        Assert.assertEquals(triangular(2), computeTriangular(2).compute(), 0);
        Assert.assertEquals(triangular(3), computeTriangular(3).compute(), 0);
        Assert.assertEquals(triangular(4), computeTriangular(4).compute(), 0);
        Assert.assertEquals(triangular(500), computeTriangular(500).compute(), 0);
        Assert.assertEquals(triangular(10000), computeTriangular(10000).compute(), 0);
        Assert.assertEquals(triangular(2539071), computeTriangular(2539071).compute(), 0);
    }

    private static long triangular(long n) {
        return n*(n+1)/2;
    }

    @Test
    public void testFactorial() {
        Assert.assertEquals(1d, computeFactorial(0).compute(), 0);
        Assert.assertEquals(1d, computeFactorial(1).compute(), 0);
        Assert.assertEquals(40320d, computeFactorial(8).compute(), 0);
        Assert.assertEquals(9.33262154439441e157, computeFactorial(100).compute(), 0);
    }

    @Test
    public void testFibonacci() {
        Assert.assertEquals(0, computeFibonacci(0).compute().a);
        Assert.assertEquals(1, computeFibonacci(1).compute().a);
        Assert.assertEquals(1, computeFibonacci(2).compute().a);
        Assert.assertEquals(2, computeFibonacci(3).compute().a);
        Assert.assertEquals(8665637060948656192L, computeFibonacci(1200).compute().a);
    }

    @Test
    public void testPalindrome() {
        Assert.assertEquals(true, isPalindrome("").compute());
        Assert.assertEquals(true, isPalindrome("x").compute());
        Assert.assertEquals(true, isPalindrome("xx").compute());
        Assert.assertEquals(true, isPalindrome("xax").compute());
        Assert.assertEquals(true, isPalindrome("testset").compute());
        Assert.assertEquals(true, isPalindrome("amanaplanacanalpanama").compute());

        Assert.assertEquals(false, isPalindrome("xaf").compute());
        Assert.assertEquals(false, isPalindrome("amanaplanacanalpanamx").compute());
        Assert.assertEquals(false, isPalindrome("amanaplanacxnalpanama").compute());
        Assert.assertEquals(false, isPalindrome("amanaplanacxanalpanama").compute());
    }

    static TailTrampoline<Long> computeTriangular(long n) {
        if (n == 0) {
            return TailTrampoline.stop(0L);
        }
        return TailTrampoline.recurse(n, (in, parent) -> in + parent, (in) -> computeTriangular(in - 1));
    }

    static TailTrampoline<Double> computeFactorial(long n) {
        if (n == 0) {
            return TailTrampoline.stop(1d);
        }
        return TailTrampoline.recurse(n, (in, parent) -> in * parent, (in) -> computeFactorial(in - 1));
    }

    static TailTrampoline<FibState> computeFibonacci(int n) {
        if (n == 0) {
            return TailTrampoline.stop(new FibState(0L, 1L));
        }
        return TailTrampoline.recurse(n,
                (in, parentFib) -> new FibState(parentFib.b, parentFib.a + parentFib.b),
                (in) -> computeFibonacci(in - 1));
    }

    static TailTrampoline<Boolean> isPalindrome(String s) {
        if (s.length() <= 1) {
            return TailTrampoline.stop(true);
        }
        if (s.charAt(0) != s.charAt(s.length()-1)) {
            return TailTrampoline.stop(false);
        }
        return TailTrampoline.recurse(s, (in, parent) -> parent, (in) -> isPalindrome(in.substring(1, in.length() - 1)));
    }

    private static class FibState {
        long a;
        long b;

        public FibState(long a, long b) {
            this.a = a;
            this.b = b;
        }
    }
}
