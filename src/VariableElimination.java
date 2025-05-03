import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;
//this class represents the variable elimination algorithm with heuristic of sorting the hidden variables lexicographically.
public class VariableElimination extends BayesianAlgorithm {

    @Override
    public void calculateProbability(Query query, List<CPT> cpts) {
        additionCount = 0;
        multiplicationCount = 0;
        // Check if the probability can be extracted directly from the CPTs
        // If so, extract it and return
        Double extracted = tryExtractProbability(query, cpts);
        if (extracted != null) {
            this.probability = extracted;
            return;
        }
        //deep copy the CPTs to avoid modifying the originals  , thanks to this the next query will not be affected by the previous one.
        List<CPT> cptsCopy = deepCopyCPTs(cpts);
        //getting the ancestors of the query and evidence variables , in my implementation Va can be a parent of Va.
        Set<String> ancestors = getAncestorsFromCPTs(cptsCopy, query.getQuery().keySet(), query.getEvidence().keySet());
        //now making only the factors that are related to the query and evidence variables , preprocessing.
        List<Factor> factors = new ArrayList<>();
        for (CPT cpt : cptsCopy) {
            if (ancestors.contains(cpt.getVariable().getName())) {
                factors.add(convertCPTtoFactor(cpt));
            }
        }
        //now we are going to set the evidence values to the factors , and by that we are going to restrict the factors.
        restrictEvidence(factors, query.getEvidence());
        //extra preprocess to be sure that all the factors are query  , evidence or hidden that are ancestor of the query or evidence.
        pruneFactors(factors, ancestors);
        //getting the hidden variables.
        Set<String> hiddenVariables = new HashSet<>(ancestors);
        hiddenVariables.removeAll(query.getQuery().keySet());
        hiddenVariables.removeAll(query.getEvidence().keySet());
        //hidden variables elimination process.
        while (!hiddenVariables.isEmpty()) {
            //every iteration we are going to sort the hidden variables , in classic VE sorting lexicographically .
            List<String> hiddenList = new ArrayList<>(hiddenVariables);
            String hidden = chooseNextToEliminate(hiddenList, factors);
            hiddenVariables.remove(hidden);
            //getting the factors that has the hidden variable in them.
            List<Factor> relatedFactors = getFactorsMentioning(hidden, factors);
            factors.removeAll(relatedFactors);
            if (relatedFactors.isEmpty()) continue;
            // join all related factors
            Factor joined = joinFactors(relatedFactors);
            //eliminating the hidden variable from the joined factor.
            Factor last = eliminate(joined, hidden);
            //add adding the last factor to the list of factors.
            factors.add(last);
        }
        //another join all the last factors that are left.
        Factor finalFactor = joinFactors(factors);
        // normalizing the final factor to get the probability.
        normalize(finalFactor);
        //mapping the query.
        Map<String, String> knownAssignment = new HashMap<>(query.getQuery());
        knownAssignment.putAll(query.getEvidence());
        //checking if there are any unassigned variables in the final factor to marginalize them.
        Set<String> finalVars = new HashSet<>();
        for (Variable variable : finalFactor.getVariables()) {
            finalVars.add(variable.getName());
        }
        Set<String> unassigned = new HashSet<>(finalVars);
        unassigned.removeAll(knownAssignment.keySet());

        //if all the values are assigned we can just get the probability of the query.
        if (unassigned.isEmpty()) {
            List<Double> probs = finalFactor.getProbabilities();
            //adding the addition count because of the summation of the probabilities.
            if (probs.size() > 1) {
                additionCount += probs.size() - 1;
            }
            this.probability = finalFactor.getProbability(knownAssignment);
            return;
        }

        // otherwise, marginalize over them:
        double result = 0.0;
        List<Map<String, String>> assignmentsOverFactor = generateAllAssignmentsForVars(finalFactor.getVariables(), unassigned);
        boolean isFirst = true;
        for (Map<String, String> assignment : assignmentsOverFactor) {
            Map<String, String> full = new HashMap<>(knownAssignment);
            full.putAll(assignment);
            double prob = finalFactor.getProbability(full);
            if(isFirst) {
                result = prob;
                isFirst = false;
            } else {
                result += prob;
                additionCount++;
            }
        }
        this.probability = result;
    }

    // ----- Helper methods -----

    // deep copy CPTs to avoid modifying originals
    //this method deep copies its Variable  , its parents and its probabilities.
    private List<CPT> deepCopyCPTs(List<CPT> cpts) {
        List<CPT> copied = new ArrayList<>();
        for (CPT cpt : cpts) {
            Variable newVar = new Variable(cpt.getVariable().getName());
            for (String outcome : cpt.getVariable().getOutcomes()) newVar.addOutcome(outcome);

            CPT newCPT = new CPT(newVar);
            for (Variable parent : cpt.getParents()) {
                Variable newParent = new Variable(parent.getName());
                for (String outcome : parent.getOutcomes()) newParent.addOutcome(outcome);
                newCPT.addParent(newParent);
            }
            newCPT.setProbabilities(new ArrayList<>(cpt.getProbabilities()));
            copied.add(newCPT);
        }
        return copied;
    }
    //this method generates all the assignments for the variables in the list.
    //for example if the list has 2 variables with 2 outcomes each it will generate 4 assignments. {T ,T} , {T ,F} , {F ,T} , {F ,F}
    private List<Map<String, String>> generateAllAssignmentsForVars(List<Variable> allVars, Set<String> targetVarNames) {
        List<Variable> targetVars = allVars.stream()
                .filter(v -> targetVarNames.contains(v.getName()))
                .collect(Collectors.toList());

        List<Map<String, String>> result = new ArrayList<>();
        generateHelper(targetVars, 0, new HashMap<>(), result);
        return result;
    }


    // method to convert a CPT to a Factor
    private Factor convertCPTtoFactor(CPT cpt) {
        // adding the variable and its parents to the list of variables
        List<Variable> originalOrder = new ArrayList<>(cpt.getParents());
        originalOrder.add(cpt.getVariable());
        // sort variable list alphabetically (to match getProbability)
        List<Variable> sortedVars = new ArrayList<>(originalOrder);
        sortedVars.sort(Comparator.comparing(Variable::getName));
        //generate all assignments in the sorted order
        List<Map<String, String>> assignments = generateAllAssignments(sortedVars);
        List<Double> newProbs = new ArrayList<>();
        //for each assignment we are going to get the probability of the original order of the variables.
        for (Map<String, String> assignment : assignments) {
            Map<String, String> originalAssignment = new HashMap<>();
            for (Variable var : originalOrder) {
                originalAssignment.put(var.getName(), assignment.get(var.getName()));
            }
            double prob = cpt.getProbability(originalAssignment);
            newProbs.add(prob);
        }
        return new Factor(sortedVars, newProbs);
    }

    // safe method to remove factors that does not contribute to the query.
    private void pruneFactors(List<Factor> factors, Set<String> ancestors) {
        factors.removeIf(factor -> !containsAny(factor, ancestors));
    }
    //getting the ancestors of the query and evidence variables , I mentioned explicitly from CPTs because
    // we cannot extract in from factors.
    private Set<String> getAncestorsFromCPTs(List<CPT> cpts, Set<String> queryVars, Set<String> evidenceVars) {
        Set<String> target = new HashSet<>(queryVars);
        target.addAll(evidenceVars);
        Set<String> ancestors = new HashSet<>(target);

        boolean changed = true;
        while (changed) {
            changed = false;
            for (CPT cpt : cpts) {
                if (ancestors.contains(cpt.getVariable().getName())) {
                    for (Variable parent : cpt.getParents()) {
                        if (ancestors.add(parent.getName())) {
                            changed = true;
                        }
                    }
                }
            }
        }
        return ancestors;
    }
    // Check if a factor contains any of the variables in the set
    private boolean containsAny(Factor factor, Set<String> vars) {
        for (Variable v : factor.getVariables()) {
            if (vars.contains(v.getName())) return true;
        }
        return false;
    }

    // restrict the factors by the evidence outcomes.
    private void restrictEvidence(List<Factor> factors, Map<String, String> evidence) {
        for (int i = 0; i < factors.size(); i++) {
            Factor restricted = restrictFactor(factors.get(i), evidence);
            factors.set(i, restricted);
        }
    }
    //for each factor obtaining only the assignments that contains the evidence given outcomes.
    private Factor restrictFactor(Factor factor, Map<String, String> evidence) {
        //create a new variable list without evidence variables
        List<Variable> newVars = new ArrayList<>();
        for (Variable v : factor.getVariables()) {
            if (!evidence.containsKey(v.getName())) {
                newVars.add(v);
            }
        }

        List<Map<String, String>> assignments = generateAllAssignments(newVars);
        List<Double> newProbs = new ArrayList<>();

        for (Map<String, String> assignment : assignments) {
            //adding to each partial assignment the evidence.
            Map<String, String> fullAssignment = new HashMap<>(assignment);
            fullAssignment.putAll(evidence);
            //get the probability of the full assignment
            newProbs.add(factor.getProbability(fullAssignment));
        }

        //returning the new factor with the new variables and probabilities.
        return new Factor(newVars, newProbs);
    }


    //get all variables from all factors
    private Set<String> getAllVariables(List<Factor> factors) {
        Set<String> vars = new HashSet<>();
        for (Factor f : factors) {
            for (Variable v : f.getVariables()) {
                vars.add(v.getName());
            }
        }
        return vars;
    }

    //it helps us find all the factors that one of their variables is equal to the given variable.
    protected List<Factor> getFactorsMentioning(String var, List<Factor> factors) {
        List<Factor> result = new ArrayList<>();
        for (Factor f : factors) {
            for (Variable v : f.getVariables()) {
                if (v.getName().equals(var)) {
                    result.add(f);
                    break;
                }
            }
        }
        return result;
    }

    //this method gets all the factors that has the hidden variable in them and joins them.
    private Factor joinFactors(List<Factor> factors) {
        //copying the factors to avoid modifying the original list.
        List<Factor> factorsContainsHidden = new ArrayList<>(factors);
        while (factorsContainsHidden.size() > 1) {
            //first sort the factors by their size aka the number of entries in the probabilities list.
            factorsContainsHidden.sort(Comparator.comparingInt(Factor::getSize));
            int smallestSize = factorsContainsHidden.get(0).getSize();
            List<Factor> smallestFactors = new ArrayList<>();
            //adding the least size factors to the list of smallest factors.
            for (Factor f : factorsContainsHidden) {
                if (f.getSize() == smallestSize) {
                    smallestFactors.add(f);
                } else {
                    break;
                }
            }
            //in case there are more than 2 factors with the same size we sort them by their ascii value , in case there are for most
            // 2 than thats the values.
            Factor f1 = factorsContainsHidden.get(0);
            Factor f2 = factorsContainsHidden.get(1);
            if (smallestFactors.size() > 1) {
                //bubble sort the factors by their ascii value.
                for (int i = 0; i < smallestFactors.size(); i++) {
                    for (int j = 0; j < smallestFactors.size() - 1; j++) {
                        if (asciiValue(smallestFactors.get(j)) > asciiValue(smallestFactors.get(j + 1))) {
                            Factor temp = smallestFactors.get(j);
                            smallestFactors.set(j, smallestFactors.get(j + 1));
                            smallestFactors.set(j + 1, temp);
                        }
                    }
                }
                //getting the first two factors with the least ascii value.
                f1 = smallestFactors.get(0);
                f2 = smallestFactors.get(1);
            }
            Factor joined = joinTwoFactors(f1, f2);
            factorsContainsHidden.remove(f1);
            factorsContainsHidden.remove(f2);
            factorsContainsHidden.add(joined);
        }
        //the last factor left is the joined factor.
        return factorsContainsHidden.get(0);

    }
    //the helper method for joinFactors that joins two factors.
    private Factor joinTwoFactors(Factor f1, Factor f2) {
        //setting a map of variable's names to variables.
        Map<String, Variable> combineVariablesMap = new HashMap<>();

        for (Variable v : f1.getVariables()) {
            combineVariablesMap.putIfAbsent(v.getName(), deepCopyVariable(v));
        }
        for (Variable v : f2.getVariables()) {
            combineVariablesMap.putIfAbsent(v.getName(), deepCopyVariable(v));
        }
        //sorting the variables by their names. for example A1 will be before A2.
        List<String> sortedNames = new ArrayList<>(combineVariablesMap.keySet());
        Collections.sort(sortedNames);
        //based on the sorting we are going to create a new list of variables.
        List<Variable> sortedVars = new ArrayList<>();
        for (String name : sortedNames) sortedVars.add(combineVariablesMap.get(name));
        //generate all assignments for all combined variables
        List<Map<String, String>> allAssignments = generateAllAssignments(sortedVars);
        List<Double> newProbs = new ArrayList<>();
        Set<String> factor1Variables = f1.getVariables().stream().map(Variable::getName).collect(Collectors.toSet());
        Set<String> factor2Variables = f2.getVariables().stream().map(Variable::getName).collect(Collectors.toSet());
        for (Map<String, String> assignment : allAssignments) {
            double p1 = f1.getProbability(assignment);
            double p2 = f2.getProbability(assignment);
            newProbs.add(p1 * p2);

            // only count multiplications if the assignment depends on both
            //  both factors contribute different variables to the assignment.
            Set<String> keys = assignment.keySet();
            // check if the assignment uses variables from both factors.
            boolean usesF1 = keys.stream().anyMatch(factor1Variables::contains);
            boolean usesF2 = keys.stream().anyMatch(factor2Variables::contains);
            //if it does then we are going to add the multiplication count.
            if (usesF1 && usesF2) {
                multiplicationCount++;
            }
        }
        return new Factor(sortedVars, newProbs);
    }

    // deep copy a variable
    private Variable deepCopyVariable(Variable v) {
        Variable copy = new Variable(v.getName());
        for (String outcome : v.getOutcomes()) {
            copy.addOutcome(outcome);
        }
        return copy;
    }


    //eliminate the variable from the factor , by summing over its outcomes.
    private Factor eliminate(Factor factor, String varToEliminate) {
        //creating a list of the variables without the variable to eliminate.
        List<Variable> remainingVars  = new ArrayList<>();
        for (Variable v : factor.getVariables()) {
            if (!v.getName().equals(varToEliminate)) {
                remainingVars.add(deepCopyVariable(v));
            }
        }
        Collections.sort(remainingVars, new Comparator<Variable>() {
                    @Override
                    public int compare(Variable o1, Variable o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
        //generate all assignments for the remaining variables
        List < Map < String, String >> assignments = generateAllAssignments(remainingVars);
        List<Double> newProbs = new ArrayList<>();
        //getting the variable to eliminate from the factor.
        Variable variableToRemove = findVariable(factor, varToEliminate);
        //for each assignment (without the variable to eliminate) we are going to sum the probabilities of the assignment including the variable outcomes.
        for (Map<String, String> currAssignment : assignments) {
            double sum = 0;
            boolean firstOutcome = true;
            for (String outcome : variableToRemove.getOutcomes()) {
                Map<String, String> currAssignmentWithOutcome = new HashMap<>(currAssignment);
                currAssignmentWithOutcome.put(varToEliminate, outcome);
                // get the probability of the extended assignment
               double prob = factor.getProbability(currAssignmentWithOutcome);
                if (firstOutcome) {
                    sum = prob;
                    firstOutcome = false;
                } else {
                    sum += prob;
                    additionCount++;

                }
            }
            newProbs.add(sum);
        }
        //creating a new factor with the remaining variables and the new probabilities.
        List<Variable> cleanVars = new ArrayList<>();
        for (Variable v : remainingVars) {
            cleanVars.add(deepCopyVariable(v));
        }
        return new Factor(cleanVars, newProbs);
    }


    // normalize the factor probabilities so all sum to 1
    private void normalize(Factor factor) {
        double total = 0;
        // summing all the probabilities
        for (double p : factor.getProbabilities()) total += p;
        //then dividing each probability by the total so get the normalized (real) probabilities.
        List<Double> normalized = new ArrayList<>();
        for (double p : factor.getProbabilities()) {
            normalized.add(p / total);
        }
        factor.setProbabilities(normalized);
    }
    //this method is used to find the variable in the factor by its name.
    private Variable findVariable(Factor factor, String name) {
        for (Variable v : factor.getVariables()) {
            if (v.getName().equals(name)) return v;
        }
        throw new RuntimeException("Variable not found: " + name);
    }
    //this method generates all the assignments for the variables in the list , for example if the list has 2 variables with 2 outcomes each it will generate 4 assignments.
    private List<Map<String, String>> generateAllAssignments(List<Variable> vars) {
        List<Map<String, String>> result = new ArrayList<>();
        generateHelper(vars, 0, new HashMap<>(), result);
        return result;
    }
    //helper method to generate all assignments for the variables in the list by choosing one outcome at a time recursively.
    private void generateHelper(List<Variable> vars, int idx, Map<String, String> current, List<Map<String, String>> result) {
        // base case: if we have assigned all variables, add the assignment to the result and return
        if (idx == vars.size()) {
            result.add(new HashMap<>(current));
            return;
        }
        // recursive case: choose the current variable and iterate over its outcomes
        Variable var = vars.get(idx);
        for (String outcome : var.getOutcomes()) {
            current.put(var.getName(), outcome);
            generateHelper(vars, idx + 1, current, result);
            // backtrack: remove the current variable from the assignment
            current.remove(var.getName());
        }
    }
    //this method is used to choose the next variable to eliminate from the hidden variables list.
    //in the classic VE we are going to choose the one with the lowest ascii value  , or lexicographically.
    protected String chooseNextToEliminate(List<String> hiddenVariables, List<Factor> factors) {
        List<String> sortedHidden = new ArrayList<>(hiddenVariables);
        Collections.sort(sortedHidden);
        return sortedHidden.get(0);
    }
    //method to calculate the ascii value of the factor to use it as a secondary key in the sorting.
    //this helps us compare B3 and B2 for example.
    private int asciiValue(Factor factor) {
        int sum = 0;
        for (Variable v : factor.getVariables()) {
            for (char c : v.getName().toCharArray()) {
                sum += c;
            }
        }
        return sum;
    }
}
