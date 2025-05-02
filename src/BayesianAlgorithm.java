import java.util.HashMap;
import java.util.List;
import java.util.Map;

//an abstract class that represents the Bayesian algorithm , it has main method to calculate the probability of a query given the CPTs.
//as well as it should update the addition and multiplication counts.
public abstract class BayesianAlgorithm {
    //initializing the addition and multiplication counts to 0.
    protected int additionCount = 0;
    protected int multiplicationCount = 0;
    protected double probability = 0.0;

    //this method should be implemented in the subclasses to calculate the probability of a query given the CPTs.
    public abstract void calculateProbability(Query query, List<CPT> CPTS);

    //getters , no setters since we cannot set the counts.
    public int getAdditionCount() {
        return additionCount;
    }

    public int getMultiplicationCount() {
        return multiplicationCount;
    }

    public double getProbability() {
        return probability;
    }

    //shared method among 1 2 3 algorithms to extract the probability of a query from the CPTs without operations.
    //we're checking if the query variable is as CPT main variable and if the parents are as the condition variables.
    protected Double tryExtractProbability(Query query, List<CPT> CPTs) {
        String queryVariable = query.getQuery().keySet().iterator().next();
        String queryValue = query.getQuery().get(queryVariable);
        //iterating over the CPTs until we find equalization between the query variable and the CPT variable.
        for (CPT CPT : CPTs) {
            if (CPT.getVariable().getName().equals(queryVariable)) {
                //building the assignments for the getProbability method.
                Map<String, String> assignmentBuilder = new HashMap<>();
                assignmentBuilder.put(queryVariable, queryValue);
                boolean parentsFound = true;
                //iterating over the parents of the CPT and checking if they are in the query.
                for (Variable parent : CPT.getParents()) {
                    //if one of the parents is not equal to the evidence breaking and starting over with another CPT.
                    if (!query.getEvidence().containsKey(parent.getName())) {
                        parentsFound = false;
                        break;
                    }
                    assignmentBuilder.put(parent.getName(), query.getEvidence().get(parent.getName()));
                }
                //checking if the assignment is equal to the CPT
                if (parentsFound && query.getEvidence().size() == CPT.getParents().size()) {
                    //if the assignment is equal to the CPT we return the probability , explicitly initializing the addition and multiplication counts to 0 for clearance.
                    additionCount = 0;
                    multiplicationCount = 0;
                    return CPT.getProbability(assignmentBuilder);
                }

            }
        }
        return null; // if no match is found, return null
    }
}
