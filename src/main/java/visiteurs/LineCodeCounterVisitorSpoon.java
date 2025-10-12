package visiteurs;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;

public class LineCodeCounterVisitorSpoon {
    public int analyze(CtModel model) {
        List<CtClass<?>> classes = model.getElements(new TypeFilter<>(CtClass.class));
        return classes.stream()
                .mapToInt(c -> c.toString().split("\n").length)
                .sum();
    }
}
