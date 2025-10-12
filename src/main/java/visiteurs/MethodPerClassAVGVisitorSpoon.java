package visiteurs;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;

public class MethodPerClassAVGVisitorSpoon {
    public double analyze(CtModel model) {
        List<CtClass<?>> classes = model.getElements(new TypeFilter<>(CtClass.class));
        if (classes.isEmpty()) return 0;
        int totalMethods = classes.stream().mapToInt(c -> c.getMethods().size()).sum();
        return (double) totalMethods / classes.size();
    }
}
