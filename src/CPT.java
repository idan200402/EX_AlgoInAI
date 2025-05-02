import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//this class represents the conditional probability table (CPT) of a variable in a Bayesian network.
public class CPT {
    //it has one variable name it parents if has and the probabilities table that given.
    private final Variable variable;
    private List<Double> probabilities;
    private List<Variable> parents;

    //constructor
    public CPT(Variable variable) {
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
    //since not all the cpts have parents we need to check if the list is null or not and then initialize it and add the parent.
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
    //the integral part of the class , it calculates the index of the probability table based on the assignment of the variables.
    //The main variable changes the fastest and the parents changing rate is in reverse order.
    //this method is mainly used in the algorithm lookup and simple inference algorithms.
    public double getProbability(Map<String, String> assignment) {
        //first putting all parents by order and then adding the variable to the list.
        List<Variable> allVars = new ArrayList<>(parents);
        allVars.add(variable);
        //initializing the index and multiplier as the neutral to addition and multiplication.
        int index = 0;
        int multiplier = 1;
        //hovering the list of variables in reverse order (as the logic of the CPT).
        for (int i = allVars.size() - 1; i >= 0; i--) {
            Variable var = allVars.get(i);
            String outcome = assignment.get(var.getName());
            //adding debugging statements.
            if (outcome == null) {
                System.out.println("Missing assignment for variable: " + var.getName());
                return 0;
            }
            //getting the position of the outcome in the list of outcomes of this variable.
            int currIndex = var.getOutcomes().indexOf(outcome);
            //if the assignment is not valid we return 0.
            if (currIndex == -1) {
                System.out.println("Invalid outcome for variable: " + var.getName());
                return 0;
            }
            //updating the index and only then the multiplier by the number of outcomes.
            index += currIndex * multiplier;
            multiplier *= var.getOutcomesCount();
        }
        //for debug purposes.
        if (index < 0 || index >= probabilities.size()) {
            System.out.println("Index out of bounds for factor lookup: " + index + " for CPT of size " + probabilities.size());
            return 0;
        }
        return probabilities.get(index);
    }


}

