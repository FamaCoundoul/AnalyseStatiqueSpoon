package visiteurs;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;
import java.util.stream.Collectors;

public class PlusXMethodCounterVisitorSpoon {
    public List<String> analyze(CtModel model, int x) {
        List<CtClass<?>> classes = model.getElements(new TypeFilter<>(CtClass.class));
        return classes.stream()
                .filter(c -> c.getMethods().size() > x)
                .map(CtClass::getQualifiedName)
                .collect(Collectors.toList());
    }

	
}
