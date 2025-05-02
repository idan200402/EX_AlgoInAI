import java.util.*;
import java.util.stream.Collectors;
//a class that represents a factor in a Bayesian network , for VE algorithm.
public class Factor {
    private List<Variable> variables; // list of all variables
    private List<Double> probabilities; // probabilities list

    // constructor
    public Factor(List<Variable> variables, List<Double> probabilities) {
        //deep copying the variables list and sorting it alphabetically.
        List<Variable> copied = new ArrayList<>();
        for (Variable v : variables) {
            Variable copy = new Variable(v.getName());
            for (String outcome : v.getOutcomes()) {
                copy.addOutcome(outcome);
            }
            copied.add(copy);
        }
        //local comparator to sort the variables list.
        Collections.sort(copied, new Comparator<Variable>() {
            @Override
            public int compare(Variable v1, Variable v2) {
                return v1.getName().compareTo(v2.getName());
            }
        });
        this.variables = copied;


        this.probabilities = new ArrayList<>(probabilities);
    }


    // Getters
    public List<Variable> getVariables() {
        return variables;
    }

    public List<Double> getProbabilities() {
        return probabilities;
    }
    //setter
    public void setProbabilities(List<Double> probabilities) {
        this.probabilities = probabilities;
    }

    //main method to find the index of the probability in the list of probabilities and return it.
    //the same logic as the one in the CPT class.
    public double getProbability(Map<String, String> assignment) {
        int index = 0;
        int multiplier = 1;
        for (int i = variables.size() - 1; i >= 0; i--) {
            Variable var = variables.get(i);
            String value = assignment.get(var.getName());
            //debugging.
            if (value == null) {
                System.out.println("missing assaignment for Variable" + var.getName());
                return 0.0;
            }
            int outcomeIndex = var.getOutcomes().indexOf(value);
            if (outcomeIndex == -1) {
                System.out.println("invalid outcome for variable: " + var.getName() + ", value: " + value);
                return 0.0;
            }

            index += outcomeIndex * multiplier;
            multiplier *= var.getOutcomesCount();
        }

        if (index < 0 || index >= probabilities.size()) {
            System.out.println("Index out of bounds: " + index + " for factor of size " + probabilities.size());
            return 0.0;
        }

        return probabilities.get(index);
    }

    // a recursive method to generate all possible assignments of the variables in the factor.
    public List<Map<String, String>> generateAllAssignments() {
        List<Map<String, String>> assignments = new ArrayList<>();
        generateHelper(0, new HashMap<>(), assignments);
        return assignments;
    }
    //every call it creates a new assignment and adds it to the list of assignments.
    private void generateHelper(int idx, Map<String, String> current, List<Map<String, String>> result) {
        if (idx == variables.size()) {
            result.add(new HashMap<>(current));
            return;
        }

        Variable var = variables.get(idx);
        for (String outcome : var.getOutcomes()) {
            current.put(var.getName(), outcome);
            generateHelper(idx + 1, current, result);
            current.remove(var.getName());
        }
    }
    //toString method to print the factor , debugging purposes.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Factor with variables: ");
        for (Variable var : variables) {
            sb.append(var.getName()).append(" ");
        }
        return sb.toString();
    }
    public int getSize(){
        return probabilities.size();
    }

}
