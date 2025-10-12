package visiteurs;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;

public class MaxParametersCounterVisitorSpoon {
    public int analyze(CtModel model) {
        List<CtMethod<?>> methods = model.getElements(new TypeFilter<>(CtMethod.class));
        return methods.stream()
                .mapToInt(m -> m.getParameters().size())
                .max()
                .orElse(0);
    }
}
