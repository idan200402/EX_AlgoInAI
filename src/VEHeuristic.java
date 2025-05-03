import java.util.HashSet;
import java.util.List;
import java.util.Set;
//this class implements the variable elimination algorithm with a heuristic approach.
//it inherits from the variable elimination class and overrides the chooseNextToEliminate method since we're using a heuristic approach.
//how it works:
//instead of choosing the next variable to eliminate based on their ascii names.
//we're choosing the variable to eliminate based on the estimated size of the all the factors that contain it.
//this approach will choose the hidden variable that will result in the smallest estimated factor size.
public class VEHeuristic extends VariableElimination {
    @Override
    protected String chooseNextToEliminate(List<String> hiddenVariables, List<Factor> factors) {
        String res = null;
        // initialize the minimum estimated size to a large value.
        int minEstimatedSize = Integer.MAX_VALUE;
        //for each hidden variable we're getting the factors that mention it and calculating the estimated size of the last factor.
        for (String var : hiddenVariables) {
            // get the variables that also appear in the factors that mention this variable.
            Set<Variable> involvedVars = new HashSet<>();
            for (Factor factor : getFactorsMentioning(var, factors)) {
                involvedVars.addAll(factor.getVariables());
            }
            // calculate the estimated size of the factor that would be created by eliminating this variable.
            //for example if we have 3 variables A,B,C and the outcomes of each one are {True, False} then the estimated size is 2*2*2 = 8.
            int estimatedSize = 1;
            for (Variable v : involvedVars) {
                estimatedSize *= v.getOutcomes().size();
            }
            //if the current estimated size is less than the minimum estimated size this is the variable to eliminate for now.
            //so we're lowering the minimum estimated size to the current one and assigning the variable to the result.
            if (estimatedSize < minEstimatedSize) {
                minEstimatedSize = estimatedSize;
                res = var;
            }
        }

        return res;
    }



}
