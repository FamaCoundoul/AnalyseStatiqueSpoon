package visiteurs;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;
import java.util.stream.Collectors;

public class LongestMethodsPerClassVisitorSpoon {
    public Map<String, List<CtMethod<?>>> analyze(CtModel model) {
        Map<String, List<CtMethod<?>>> result = new HashMap<>();
        List<CtClass<?>> classes = model.getElements(new TypeFilter<>(CtClass.class));
        for (CtClass<?> cls : classes) {
            List<CtMethod<?>> methods = new ArrayList<>(cls.getMethods());
            if (!methods.isEmpty()) {
                int limit = Math.max(1, (int) Math.ceil(methods.size() * 0.1));
                List<CtMethod<?>> top = methods.stream()
                        .sorted((m1, m2) -> Integer.compare(
                                m2.toString().split("\n").length,
                                m1.toString().split("\n").length))
                        .limit(limit)
                        .collect(Collectors.toList());
                result.put(cls.getQualifiedName(), top);
            }
        }
        return result;
    }
}
