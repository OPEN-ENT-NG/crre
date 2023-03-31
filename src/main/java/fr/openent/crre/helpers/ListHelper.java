package fr.openent.crre.helpers;

import java.util.List;
import java.util.stream.Collectors;

public class ListHelper {

    private ListHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> List<T> distinct(List<T> list) {
        return list.stream()
                .distinct()
                .collect(Collectors.toList());
    }
}