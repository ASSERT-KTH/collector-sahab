package foo;

import java.util.*;

public class PrintingCollections {
    private static List<Integer> list = List.of(1, 2, 3, 4, 5);
    private static Set<String> set = Set.of("aman", "sharma", "sahab");

    public static boolean returnTruthy() {
        Queue<String> q = new ArrayDeque<>();
        q.add("Added at runtime");
        return true;
    }
}
