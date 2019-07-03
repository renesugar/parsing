package at.searles.parsing.utils.test;

import at.searles.parsing.Fold;
import at.searles.parsing.utils.list.ConsFold;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class UtilsTest {
    @Test
    public void testConsMayBeEmpty() {
        Fold<List<Integer>, Integer, List<Integer>> cons = new ConsFold<>(0);

        List<Integer> l = Arrays.asList(1, 2);

        Assert.assertEquals((Integer) 2, cons.rightInverse(null, l));
        l = cons.leftInverse(null, l);
        Assert.assertEquals((Integer) 1, cons.rightInverse(null, l));
        l = cons.leftInverse(null, l);

        Assert.assertNotNull(l);
        Assert.assertTrue(l.isEmpty());

        Assert.assertNull(cons.leftInverse(null, l));
        Assert.assertNull(cons.rightInverse(null, l));
    }

    @Test
    public void testConsMayNotBeEmpty() {
        Fold<List<Integer>, Integer, List<Integer>> cons = new ConsFold<>(1);

        List<Integer> l = Arrays.asList(1, 2);

        Assert.assertEquals((Integer) 2, cons.rightInverse(null, l));
        l = cons.leftInverse(null, l);

        Assert.assertEquals(1, l.size());

        Assert.assertNull(cons.leftInverse(null, l));
        Assert.assertNull(cons.rightInverse(null, l));
    }
}
