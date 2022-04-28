package foo.collections;

import java.util.*;

public class NestedCollection {
    public static boolean canWeRepresentNestedCollection() {
        Set<Set<String>> nestedSet = new HashSet<>();
        nestedSet.add(new HashSet<>() {{
            add("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        }});
        return true;
    }
}
