package com.ai.library.ai;

import java.util.Arrays;
import java.util.Comparator;

public class EmbeddingUtil {
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null) return -1.0;
        if (a.length != b.length) return -1.0;
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return -1.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
