package visiteurs;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;
import java.util.stream.Collectors;

public class TopMethodClassVisitorSpoon {
    public List<String> analyze(CtModel model) {
        List<CtClass<?>> classes = model.getElements(new TypeFilter<>(CtClass.class));
        int topCount = Math.max(1, (int) Math.ceil(classes.size() * 0.1));
        return classes.stream()
                .sorted((a, b) -> Integer.compare(b.getMethods().size(), a.getMethods().size()))
                .limit(topCount)
                .map(CtClass::getQualifiedName)
                .collect(Collectors.toList());
    }
}
