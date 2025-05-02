import java.util.*;

//this class implements the simple inference algorithm , aka algorithm 1.
public class SimpleInference extends BayesianAlgorithm {
    //we are applying bayesian rule on the query , and then calculating the marginal probability for each query for
    //the denominator and the nominator while not calculating again the nominator.
    @Override
    public void calculateProbability(Query query, List<CPT> CPTS) {
        additionCount = 0;
        multiplicationCount = 0;
        Double extracted = tryExtractProbability(query, CPTS);
        if (extracted != null) {
            this.probability = extracted;
            return;
        }
        //grouping all the variables and their outcomes in a map.
        Set<String> allVariables = new HashSet<>();
        Map<String, List<String>> variableAndOutcomePairs = new HashMap<>();
        //for each CPT we get their variable and outcomes and add them to the map.
        for (CPT CPT : CPTS) {
            String variableName = CPT.getVariable().getName();
            allVariables.add(variableName);
            variableAndOutcomePairs.put(variableName, CPT.getVariable().getOutcomes());
        }
        //creating the hidden variables list since the algorithm is based on their assignments
        Map<String, String> queryVariables = query.getQuery();
        Map<String, String> evidenceVariables = query.getEvidence();
        Set<String> hiddenVariables = new HashSet<>(allVariables);
        hiddenVariables.removeAll(queryVariables.keySet());
        hiddenVariables.removeAll(evidenceVariables.keySet());
        List<String> hiddenVariableList = new ArrayList<>(hiddenVariables);
        //calculating the nominator and denominator of the bayesian rule.
        double nominator = calculateMarginalProbability(queryVariables, evidenceVariables, hiddenVariableList, variableAndOutcomePairs, CPTS);
        List<String> queryVariableList = new ArrayList<>(queryVariables.keySet());
        double denominator = calculateDenominator(nominator, queryVariableList, queryVariables, hiddenVariableList, variableAndOutcomePairs, evidenceVariables, CPTS);
        //returning the final result of the bayesian rule.
        this.probability = nominator / denominator;


    }
    //as we discussed we turning the nominator (for example) into a marginal probabilities
    //so this method calculates the marginal probability.
    private double calculateMarginalProbability(Map<String, String> queryVariables, Map<String, String> evidenceVariables, List<String> hiddenVarToSum, Map<String, List<String>> variableAndOutcomePairs, List<CPT> CPTS) {
        Map<String, String> currentAssignment = new HashMap<>();
        currentAssignment.putAll(queryVariables);
        currentAssignment.putAll(evidenceVariables);
        return sumPossibilities(hiddenVarToSum, 0, currentAssignment, variableAndOutcomePairs, CPTS);
    }
    //recursive method to calculate the sum of the probabilities of the hidden variables.
    //every time added to the current sun the new calculation of the joint probability
    private double sumPossibilities(List<String> hiddenVarToSum, int index, Map<String, String> currentAssignment, Map<String, List<String>> variableAndOutcomePairs, List<CPT> CPTS) {
        if (index == hiddenVarToSum.size()) {
            return calculateJointProbability(currentAssignment, CPTS);
        }

        String currVariable = hiddenVarToSum.get(index);
        List<String> outcomes = variableAndOutcomePairs.get(currVariable);
        double sum = 0.0;
        boolean firstOutcome = true;
        for (String outcome : outcomes) {
            currentAssignment.put(currVariable, outcome);
            double currentValue = sumPossibilities(hiddenVarToSum, index + 1, currentAssignment, variableAndOutcomePairs, CPTS);
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
    //to simply we are using the lookup algorithm .
    private double calculateJointProbability(Map<String, String> assignment, List<CPT> CPTS) {
        Query query = new Query(assignment, new HashMap<>(), 0, true);
        BayesianAlgorithm lookup = AlgorithmFactory.createAlgorithm(0);
        lookup.calculateProbability(query, CPTS);
        double jointProbability = lookup.getProbability();
        multiplicationCount += lookup.getMultiplicationCount();
        return jointProbability;
    }
    //the same logic to the denominator but we are not calculating the nominator again , but adding it.
    private double calculateDenominator ( double nominator, List<
    String > queryVariableList, Map < String, String > queryVariables, List < String > hiddenVariableList, Map < String, List < String >> variableAndOutcomePairs, Map < String, String > evidenceVariables, List <CPT> CPTS)
    {
        double res = nominator;
        if (queryVariableList.isEmpty()) {
            return res;
        }
        List<Map<String, String>> allQueryAssignments = generateAllAssignments(variableAndOutcomePairs, queryVariableList);
        allQueryAssignments.removeIf(assignment -> assignment.equals(queryVariables));
        for (Map<String, String> assignment : allQueryAssignments) {
            double partialProbability = calculateMarginalProbability(assignment, evidenceVariables, hiddenVariableList, variableAndOutcomePairs, CPTS);
            res += partialProbability;
            additionCount++;
        }
        return res;
    }
    //generating all the possible assignments of the variables. for example if we have A,B,C with outcomes {0,1} the possible assignments are:
    // {A=0,B=0,C=0} , {A=0,B=0,C=1} , {A=0,B=1,C=0} , {A=0,B=1,C=1} , {A=1,B=0,C=0} , {A=1,B=0,C=1} , {A=1,B=1,C=0} , {A=1,B=1,C=1}.
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
        String variableName = variableList.get(index);
        List<String> outcomesForVariable = variableAndOutcomePairs.get(variableName);
        //null check
        if(outcomesForVariable == null){
            System.out.println("Error: outcomes for variable " + variableName + "is null");
        } else if(outcomesForVariable.isEmpty()) {
            System.out.println("Error: outcomes for variable " + variableName + "is empty");
        }
    }


}
