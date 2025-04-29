import java.util.*;
import java.util.stream.Collectors;

public class Factor {
    private List<Variable> variables; // List of all variables
    private List<Double> probabilities; // Probabilities list

    // Constructor
    public Factor(List<Variable> variables, List<Double> probabilities) {
        // Always deep-copy and sort the variable list
        this.variables = variables.stream()
                .map(v -> {
                    Variable copy = new Variable(v.getName());
                    for (String outcome : v.getOutcomes()) {
                        copy.addOutcome(outcome);
                    }
                    return copy;
                })
                .sorted(Comparator.comparing(Variable::getName))
                .collect(Collectors.toList());

        this.probabilities = new ArrayList<>(probabilities);
    }


    // Getter
    public List<Variable> getVariables() {
        return variables;
    }

    public List<Double> getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(List<Double> probabilities) {
        this.probabilities = probabilities;
    }

    // Main method: get probability for a given assignment
    public double getProbability(Map<String, String> assignment) {
        int index = 0;
        int multiplier = 1;

        // Walk from last to first to calculate correct index
        for (int i = variables.size() - 1; i >= 0; i--) {
            Variable var = variables.get(i);
            String value = assignment.get(var.getName());

            if (value == null) {
                System.out.println("⚠️ Missing assignment for variable: " + var.getName());
                return 0.0;
            }

            int outcomeIndex = var.getOutcomes().indexOf(value);
            if (outcomeIndex == -1) {
                System.out.println("⚠️ Invalid outcome for variable: " + var.getName() + ", value: " + value);
                return 0.0;
            }

            index += outcomeIndex * multiplier;
            multiplier *= var.getOutcomesCount();
        }

        if (index < 0 || index >= probabilities.size()) {
            System.out.println("❌ Index out of bounds: " + index + " for factor of size " + probabilities.size());
            return 0.0;
        }

        return probabilities.get(index);
    }

    // Generate all possible assignments for this factor
    public List<Map<String, String>> generateAllAssignments() {
        List<Map<String, String>> assignments = new ArrayList<>();
        generateHelper(0, new HashMap<>(), assignments);
        return assignments;
    }

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
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Factor with variables: ");
        for (Variable var : variables) {
            sb.append(var.getName()).append(" ");
        }
        return sb.toString();
    }

}
