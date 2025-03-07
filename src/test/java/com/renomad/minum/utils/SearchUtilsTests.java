package com.renomad.minum.utils;

import com.renomad.minum.auth.User;
import org.junit.Test;

import java.util.List;

import static com.renomad.minum.testing.TestFramework.*;
import static com.renomad.minum.utils.SearchUtils.findExactlyOne;

public class SearchUtilsTests {

    /**
     * It's a common situation to search a collection and expect to either
     * find one or none, and anything else represents a bug.  For example,
     * if I am searching a list of users by id, I should either find the user
     * or not - but finding two or three would indicate a bug.  This helper
     * is to cut down on needing to write that code over and over.
     */
    @Test
    public void test_SearchUtils_OneOrNone() {
        List<String> items = List.of("a", "b", "c");

        // if we find it, return it
        String b = findExactlyOne(items.stream(), x -> x.equals("b"));
        assertEquals(b, "b");

        // if we don't find anything, return null
        String d = findExactlyOne(items.stream(), x -> x.equals("d"));
        assertTrue(d == null);
    }

    /**
     * If there are duplicates, throw an exception
     */
    @Test
    public void test_SearchUtils_OneOrNone_Duplicates() {
        // a list with two identical elements
        List<String> items = List.of("a", "b", "b");

        // when finding "b", our method throws an exception because there are 2.
        var ex = assertThrows(InvariantException.class, () -> findExactlyOne(items.stream(), x -> x.equals("b")));
        assertEquals(ex.getMessage(), "Must be zero or one of this thing, or it's a bug.  We found a size of 2");
    }

    /**
     * By default the findExactlyOne method returns null if none are found, but
     * if you are using a null object pattern, you can specify something else to
     * be returned.  In fact, you specify a {@link java.util.concurrent.Callable},
     * which is run at the point in code where it would have returned null.  You have
     * some nice options.
     */
    @Test
    public void test_SearchUtils_OneOrNone_SpecifyReturnValue() {
        // an empty list.
        List<User> users = List.of();

        // when we search the empty list and find nothing, we can specify an empty object to return.
        User result = findExactlyOne(
                users.stream(),
                user -> user.getId() == 1,
                () -> User.EMPTY);

        assertEquals(result, User.EMPTY);
    }
}
