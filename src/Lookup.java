import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * To be implemented
 */
public class Lookup extends BayesianAlgorithm {
    @Override
    public double calculateProbability(Query query , List<CPT> CPTS) {
        Map<String,String> allAssignments = new HashMap<>(query.getQuery());
        allAssignments.putAll(query.getEvidence());
        double probability = 1.0;
        for(CPT CPT : CPTS) {
            Map<String,String> relevantAssignments = new HashMap<>();
            relevantAssignments.put(CPT.getVariable().getName() , allAssignments.get(CPT.getVariable().getName()));
            for(Variable parent : CPT.getParents()) {
                relevantAssignments.put(parent.getName() , allAssignments.get(parent.getName()));
            }
            probability *= CPT.getProbability(relevantAssignments);
        }
        this.additionCount = 0;
        this.multiplicationCount += CPTS.size() - 1;
        return probability;
    }
}