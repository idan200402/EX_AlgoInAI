import java.util.*;

/**
 * To be implemented
 */
public class SimpleInference extends BayesianAlgorithm {
    @Override
    public double calculateProbability(Query query, List<Factor> factors) {
        Set<String> allVariables = new HashSet<>();
        Map<String, List<String>> variableAndOutcomePairs = new HashMap<>();
        for (Factor factor : factors) {
            String variableName = factor.getVariable().getName();
            allVariables.add(variableName);
            variableAndOutcomePairs.put(variableName, factor.getVariable().getOutcomes());
        }
        Map<String, String> queryVariables = query.getQuery();
        Map<String, String> evidenceVariables = query.getEvidence();
        Set<String> hiddenVariables = new HashSet<>(allVariables);
        hiddenVariables.removeAll(queryVariables.keySet());
        hiddenVariables.removeAll(evidenceVariables.keySet());
        List<String> hiddenVariableList = new ArrayList<>(hiddenVariables);
        double nominator = calculateMarginalProbability(queryVariables, evidenceVariables, hiddenVariableList, variableAndOutcomePairs, factors);
        List<String> queryVariableList = new ArrayList<>(queryVariables.keySet());
        double denominator = calculateDenominator(nominator, queryVariableList, queryVariables, hiddenVariableList, variableAndOutcomePairs, evidenceVariables, factors);
        return nominator / denominator;


    }

    private double calculateMarginalProbability(Map<String, String> queryVariables, Map<String, String> evidenceVariables, List<String> hiddenVarToSum, Map<String, List<String>> variableAndOutcomePairs, List<Factor> factors) {
        Map<String, String> currentAssignment = new HashMap<>();
        currentAssignment.putAll(queryVariables);
        currentAssignment.putAll(evidenceVariables);
        return sumPossibilities(hiddenVarToSum, 0, currentAssignment, variableAndOutcomePairs, factors);
    }

    private double sumPossibilities(List<String> hiddenVarToSum, int index, Map<String, String> currentAssignment, Map<String, List<String>> variableAndOutcomePairs, List<Factor> factors) {
        if (index == hiddenVarToSum.size()) {
            return calculateJointProbability(currentAssignment, factors);
        }

        String currVariable = hiddenVarToSum.get(index);
        List<String> outcomes = variableAndOutcomePairs.get(currVariable);
        double sum = 0.0;
        boolean firstOutcome = true;
        for (String outcome : outcomes) {
            currentAssignment.put(currVariable, outcome);
            double currentValue = sumPossibilities(hiddenVarToSum, index + 1, currentAssignment, variableAndOutcomePairs, factors);
            if (firstOutcome) {
                sum = currentValue;
                firstOutcome = false;
            } else {
                sum += currentValue;
                additionCount++;
            }
        }
        currentAssignment.remove(currVariable);
        return sum;
    }
    private double calculateJointProbability(Map<String, String> assignment, List<Factor> factors) {
        Query query = new Query(assignment, new HashMap<>(), 0, true);
        BayesianAlgorithm lookup = AlgorithmFactory.createAlgorithm(0);
        double jointProbability = lookup.calculateProbability(query, factors);
        multiplicationCount += lookup.getMultiplicationCount();
        return jointProbability;
    }

    private double calculateDenominator ( double nominator, List<
    String > queryVariableList, Map < String, String > queryVariables, List < String > hiddenVariableList, Map < String, List < String >> variableAndOutcomePairs, Map < String, String > evidenceVariables, List < Factor > factors)
    {
        double res = nominator;
        if (queryVariableList.isEmpty()) {
            return res;
        }
        List<Map<String, String>> allQueryAssignments = generateAllAssignments(variableAndOutcomePairs, queryVariableList);
        allQueryAssignments.removeIf(assignment -> assignment.equals(queryVariables));
        for (Map<String, String> assignment : allQueryAssignments) {
            double partialProbability = calculateMarginalProbability(assignment, evidenceVariables, hiddenVariableList, variableAndOutcomePairs, factors);
            res += partialProbability;
            additionCount++;
        }
        return res;
    }
    private List<Map<String, String>> generateAllAssignments
    (Map < String, List < String >> variableAndOutcomePairs, List < String > variableList){
        List<Map<String, String>> res = new ArrayList<>();
        helperGenerator(variableList, 0, new HashMap<>(), variableAndOutcomePairs, res);
        return res;
    }
    private void helperGenerator (List < String > variableList,int index, Map<
    String, String > currentAssignment, Map < String, List < String >> variableAndOutcomePairs, List < Map < String, String >> res)
    {
        if (index == variableList.size()) {
            res.add(new HashMap<>(currentAssignment));
            return;
        }
        String currVariable = variableList.get(index);
        List<String> outcomes = variableAndOutcomePairs.get(currVariable);
        for (String outcome : outcomes) {
            currentAssignment.put(currVariable, outcome);
            helperGenerator(variableList, index + 1, currentAssignment, variableAndOutcomePairs, res);
        }
        currentAssignment.remove(currVariable);
    }


}
