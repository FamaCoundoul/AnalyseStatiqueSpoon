package visiteurs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CombinedCategoryVisitorSpoon {
    public Set<String> analyze(List<String> topMethods, List<String> topAttributes) {
        Set<String> result = new HashSet<>(topMethods);
        result.retainAll(topAttributes);
        return result;
    }
}
