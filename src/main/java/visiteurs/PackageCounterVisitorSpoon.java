package visiteurs;

import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;

import java.util.Set;
import java.util.stream.Collectors;

public class PackageCounterVisitorSpoon {
    public int analyze(CtModel model) {
        Set<String> packages = model.getAllPackages()
                .stream()
                .map(CtPackage::getQualifiedName)
                .collect(Collectors.toSet());
        return packages.size();
    }
}
