package visiteurs;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;

public class LinePerMethodAVGVisitorSpoon {
    public double analyze(CtModel model) {
        List<CtMethod<?>> methods = model.getElements(new TypeFilter<>(CtMethod.class));
        if (methods.isEmpty()) return 0;
        return methods.stream()
                .mapToInt(m -> m.toString().split("\n").length)
                .average()
                .orElse(0);
    }
}
