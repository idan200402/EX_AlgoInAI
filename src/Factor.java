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
            sb.append(", parents=");
            for (Variable parent : parents) {
                sb.append(parent.getName()).append(" ");
            }
        }
        sb.append(", probabilities=").append(probabilities);
        sb.append("}\n");

        return sb.toString();
    }


}

