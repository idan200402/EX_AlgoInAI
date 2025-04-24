import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

//this class represents a factor in bayesian network. The initial factors after the parse are the CPTs.
public class Factor {
    //it has the variable name the probabilities and the parents of the variable , they are appearing as Given in the xml file.
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
    //since not all the factors have parents we need to check if the list is null or not and then initialize it and add the parent.

    public void addParent(Variable parent) {
        if (parents == null) {
            parents = new ArrayList<>();
        }
        parents.add(parent);
    }
    //for debugging purposes.
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
//integral
public double getProbability(Map<String, String> assignment) {
    int index = 0;
    int multiplier = 1;
    List<Variable> parentsReverse = new ArrayList<>(parents);
    Collections.reverse(parentsReverse);
    for (Variable parent : parentsReverse) {
        String parentAssignment = assignment.get(parent.getName());
        int outcomeIndex = parent.getOutcomes().indexOf(parentAssignment);
        index += outcomeIndex * multiplier;
        multiplier *= parent.getOutcomesCount();
    }
    String varAssignment = assignment.get(variable.getName());
    int varOutcomeIndex = variable.getOutcomes().indexOf(varAssignment);
    index = index * variable.getOutcomesCount() + varOutcomeIndex;

    return probabilities.get(index);
}


}

