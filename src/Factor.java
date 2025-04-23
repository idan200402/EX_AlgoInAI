import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//this class represents a factor in bayesian network. The initial factors after the parse are the CPTs.
public class Factor {
    private final Variable variable;
    private List<Double> probabilities;
    private List<Variable> parents;

    //constructor
    public Factor(Variable variable) {
        this.variable = variable;
        this.probabilities = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    //getters and setters
    public Variable getVariable() {
        return variable;
    }

    public List<Double> getProbabilities() {
        return probabilities;
    }

    public List<Variable> getParents() {
        return parents;
    }

    public void setProbabilities(List<Double> probabilities) {
        this.probabilities = probabilities;
    }

    public void addParent(Variable parent) {
        if (parents == null) {
            parents = new ArrayList<>();
        }
        parents.add(parent);
    }

    public int calcIndex(Map<Variable, Integer> assignment) {
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Factor{");
        sb.append("variable=").append(variable.getName()).append(" ");
        if(parents.isEmpty()){
            sb.append("variable has no parents");
        }else{
            sb.append(", parents={");
            for(int i = 0; i < parents.size(); i++){
                sb.append(parents.get(i).getName());
                if(i < parents.size() - 1){
                    sb.append(",");
                }
            }
            sb.append("}");
        }
        sb.append(" , probabilities=").append(probabilities);
        sb.append("}\n");

        return sb.toString();
    }
    public double getProbability(Map<String, String> assignment) {
        String assignmentString = assignment.get(variable.getName());
        int outcomeIndex = variable.getOutcomes().indexOf(assignmentString);
        int index = 0;
        int multiplier = this.variable.getOutcomesCount();
        for(int i = 0; i < parents.size(); i++){
            Variable parent = parents.get(i);
            String parentAssignmentString = assignment.get(parent.getName());
            int parentOutcomeIndex = parent.getOutcomes().indexOf(parentAssignmentString);
            index += parentOutcomeIndex * multiplier;
            multiplier *= parent.getOutcomesCount();
        }
        index += outcomeIndex;
        return probabilities.get(index);
    }


}

