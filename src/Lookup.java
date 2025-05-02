import java.util.HashMap;
import java.util.List;
import java.util.Map;
//the lookup algorithm is a simple algorithm that calculates the probability of a query given the CPTs
//it extracts the relevant assignments for each CPT and multiplies the probabilities.
public class Lookup extends BayesianAlgorithm {
    @Override
    public void calculateProbability(Query query , List<CPT> CPTS) {
        //since this is a joint probability all the variables are as query.
        Map<String,String> allAssignments = new HashMap<>(query.getQuery());
        //allAssignments.putAll(query.getEvidence());
        double probability = 1.0;
        //for each CPT we need to extract the relevant assignments and multiply the probabilities with the getProbability method of the CPT class.
        for(CPT CPT : CPTS) {
            Map<String,String> relevantAssignments = new HashMap<>();
            relevantAssignments.put(CPT.getVariable().getName() , allAssignments.get(CPT.getVariable().getName()));
            for(Variable parent : CPT.getParents()) {
                relevantAssignments.put(parent.getName() , allAssignments.get(parent.getName()));
            }
            probability *= CPT.getProbability(relevantAssignments);
        }
        //we haven't made any additions.
        this.additionCount = 0;
        //we have made n multiplications where n is the number of CPTs - 1.
        this.multiplicationCount += CPTS.size() - 1;
        this.probability = probability;
    }
}