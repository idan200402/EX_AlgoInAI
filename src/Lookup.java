import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * To be implemented
 */
public class Lookup extends BayesianAlgorithm {
    @Override
    public double calculateProbability(Query query , List<Factor> factors) {
        Map<String,String> allAssignments = new HashMap<>(query.getQuery());
        allAssignments.putAll(query.getEvidence());
        double probability = 1.0;
        for(Factor factor : factors) {
            Map<String,String> relevantAssignments = new HashMap<>();
            relevantAssignments.put(factor.getVariable().getName() , allAssignments.get(factor.getVariable().getName()));
            for(Variable parent : factor.getParents()) {
                relevantAssignments.put(parent.getName() , allAssignments.get(parent.getName()));
            }
            probability *= factor.getProbability(relevantAssignments);
        }
        return probability;
    }
}