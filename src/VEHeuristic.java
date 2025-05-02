import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VEHeuristic extends VariableElimination {
    @Override
    protected String chooseNextToEliminate(List<String> hiddenVariables, List<Factor> factors) {
        String bestVar = null;
        int minEstimatedSize = Integer.MAX_VALUE;

        for (String var : hiddenVariables) {
            Set<String> involvedVars = new HashSet<>();
            for (Factor factor : getFactorsMentioning(var, factors)) {
                for (Variable v : factor.getVariables()) {
                    involvedVars.add(v.getName());
                }
            }

            int estimatedSize = 1;
            for (String v : involvedVars) {
                estimatedSize *= 2; // assume binary outcomes
            }

            if (estimatedSize < minEstimatedSize) {
                minEstimatedSize = estimatedSize;
                bestVar = var;
            }
        }

        return bestVar;
    }


}
