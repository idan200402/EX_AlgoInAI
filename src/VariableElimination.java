import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

public class VariableElimination extends BayesianAlgorithm {

    @Override
    public void calculateProbability(Query query, List<CPT> cpts) {
        additionCount = 0;
        multiplicationCount = 0;
        Double extracted = tryExtractProbability(query, cpts);
        if (extracted != null) {
            this.probability = extracted;
            return;
        }
        // Step 1: Deep copy CPTs
        List<CPT> cptsCopy = deepCopyCPTs(cpts);

        // Step 2: Convert all CPTs into Factors
        Set<String> ancestors = getAncestorsFromCPTs(cptsCopy, query.getQuery().keySet(), query.getEvidence().keySet());
        System.out.println("Ancestors: " + ancestors);
        // Step 3: Convert only relevant CPTs into Factors
        List<Factor> factors = new ArrayList<>();
        for (CPT cpt : cptsCopy) {
            if (ancestors.contains(cpt.getVariable().getName())) {
                factors.add(convertCPTtoFactor(cpt));
            }
        }
        System.out.println("Factors: " + factors);
        restrictEvidence(factors, query.getEvidence());

        // Step 5: Identify hidden variables
        pruneFactors(factors, ancestors);
        Set<String> hiddenVariables = new HashSet<>(ancestors);
        hiddenVariables.removeAll(query.getQuery().keySet());
        hiddenVariables.removeAll(query.getEvidence().keySet());
        System.out.println("Hidden variables: " + hiddenVariables);
        // Step 6: Eliminate hidden variables one by one
        while (!hiddenVariables.isEmpty()) {
            List<String> hiddenList = new ArrayList<>(hiddenVariables);
            String hidden = chooseNextToEliminate(hiddenList, factors);
            System.out.println("eliminating hidden variable: " + hidden);
            hiddenVariables.remove(hidden);

            List<Factor> relatedFactors = getFactorsMentioning(hidden, factors);
            factors.removeAll(relatedFactors);

            if (relatedFactors.isEmpty()) continue;

            // Join all related factors
            Factor joined = joinFactors(relatedFactors);
            System.out.println("joined factor: " + joined);
            // Eliminate hidden variable
            Factor reduced = eliminate(joined, hidden);
            System.out.println("after eliminating: " + hidden + " the variable left are: " + reduced.getVariables());
            // Add reduced factor back
            factors.add(reduced);
        }

        // Step 7: Join remaining factors
        Factor finalFactor = joinFactors(factors);

        // Step 8: Normalize
        normalize(finalFactor);
        Map<String, String> fullAssignment = new HashMap<>(query.getQuery());
        fullAssignment.putAll(query.getEvidence());
        // Combine all known assignments
        Map<String, String> knownAssignment = new HashMap<>(query.getQuery());
        knownAssignment.putAll(query.getEvidence());

// Identify unassigned vars still in final factor
        Set<String> finalVars = finalFactor.getVariables().stream()
                .map(Variable::getName)
                .collect(Collectors.toSet());
        Set<String> unassigned = new HashSet<>(finalVars);
        unassigned.removeAll(knownAssignment.keySet());

// If there are no unassigned vars, just return result directly
        if (unassigned.isEmpty()) {
            this.probability =  finalFactor.getProbability(knownAssignment);
        }

// Otherwise, marginalize over them
        double result = 0.0;
        List<Map<String, String>> completions = generateAllAssignmentsForVars(finalFactor.getVariables(), unassigned);

        for (Map<String, String> completion : completions) {
            Map<String, String> full = new HashMap<>(knownAssignment);
            full.putAll(completion);
            result += finalFactor.getProbability(full);
            additionCount++; // each sum step
        }
        this.probability = result;
    }

    // ----- Helper methods -----

    // Deep copy CPTs to avoid modifying originals
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
    private List<Map<String, String>> generateAllAssignmentsForVars(List<Variable> allVars, Set<String> targetVarNames) {
        List<Variable> targetVars = allVars.stream()
                .filter(v -> targetVarNames.contains(v.getName()))
                .collect(Collectors.toList());

        List<Map<String, String>> result = new ArrayList<>();
        generateHelper(targetVars, 0, new HashMap<>(), result);
        return result;
    }


    // Convert a CPT into a Factor
    private Factor convertCPTtoFactor(CPT cpt) {
        // Step 1: Create full variable list (parents + variable)
        List<Variable> originalOrder = new ArrayList<>(cpt.getParents());
        originalOrder.add(cpt.getVariable());

        // Step 2: Sort variable list alphabetically (to match getProbability)
        List<Variable> sortedVars = new ArrayList<>(originalOrder);
        sortedVars.sort(Comparator.comparing(Variable::getName));

        // Step 3: Generate all assignments in the sorted order
        List<Map<String, String>> assignments = generateAllAssignments(sortedVars);
        List<Double> newProbs = new ArrayList<>();

        // Step 4: For each assignment, build matching assignment in original CPT order and get the right probability
        for (Map<String, String> assignment : assignments) {
            Map<String, String> originalAssignment = new HashMap<>();
            for (Variable var : originalOrder) {
                originalAssignment.put(var.getName(), assignment.get(var.getName()));
            }
            double prob = cpt.getProbability(originalAssignment); // existing CPT method
            newProbs.add(prob);
        }

        // Step 5: Build factor with sorted variable list and reordered probability list
        return new Factor(sortedVars, newProbs);
    }

    // Prune factors unrelated to query/evidence
    private void pruneFactors(List<Factor> factors, Set<String> ancestors) {
        factors.removeIf(factor -> !containsAny(factor, ancestors));
    }
    // NEW method to compute ancestors from full CPTs instead of filtered factors
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





    private boolean containsAny(Factor factor, Set<String> vars) {
        for (Variable v : factor.getVariables()) {
            if (vars.contains(v.getName())) return true;
        }
        return false;
    }

    // Restrict evidence values
    private void restrictEvidence(List<Factor> factors, Map<String, String> evidence) {
        for (int i = 0; i < factors.size(); i++) {
            Factor restricted = restrictFactor(factors.get(i), evidence);
            factors.set(i, restricted);
        }
    }

    private Factor restrictFactor(Factor factor, Map<String, String> evidence) {
        // Create a new variable list without evidence variables
        List<Variable> newVars = new ArrayList<>();
        for (Variable v : factor.getVariables()) {
            if (!evidence.containsKey(v.getName())) {
                newVars.add(v);
            }
        }

        List<Map<String, String>> assignments = generateAllAssignments(newVars);
        List<Double> newProbs = new ArrayList<>();

        for (Map<String, String> assignment : assignments) {
            // Extend assignment with evidence values
            Map<String, String> fullAssignment = new HashMap<>(assignment);
            fullAssignment.putAll(evidence);

            newProbs.add(factor.getProbability(fullAssignment));
        }

        // Create a new factor with reduced variables
        return new Factor(newVars, newProbs);
    }


    // Get all variables from all factors
    private Set<String> getAllVariables(List<Factor> factors) {
        Set<String> vars = new HashSet<>();
        for (Factor f : factors) {
            for (Variable v : f.getVariables()) {
                vars.add(v.getName());
            }
        }
        return vars;
    }

    // Find all factors mentioning a variable
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

    // Join multiple factors into one
    private Factor joinFactors(List<Factor> factors) {
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
                int minAsciiValue = Integer.MAX_VALUE;
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
    private int estimateJoinSize(Factor f1, Factor f2) {
        Map<String, Variable> varMap = new HashMap<>();

        for (Variable v : f1.getVariables()) {
            varMap.put(v.getName(), v);
        }
        for (Variable v : f2.getVariables()) {
            varMap.put(v.getName(), v);
        }

        int total = 1;
        for (Variable var : varMap.values()) {
            total *= var.getOutcomes().size(); // Use actual number of outcomes
        }

        return total;
    }




    // Join two factors
    private Factor joinTwoFactors(Factor f1, Factor f2) {
        // Merge variables with deep copies to avoid leftover references
        Map<String, Variable> varMap = new HashMap<>();

        for (Variable v : f1.getVariables()) {
            varMap.putIfAbsent(v.getName(), deepCopyVariable(v));
        }
        for (Variable v : f2.getVariables()) {
            varMap.putIfAbsent(v.getName(), deepCopyVariable(v));
        }

        List<String> sortedNames = new ArrayList<>(varMap.keySet());
        Collections.sort(sortedNames);

        List<Variable> sortedVars = new ArrayList<>();
        for (String name : sortedNames) sortedVars.add(varMap.get(name));

        List<Map<String, String>> allAssignments = generateAllAssignments(sortedVars);
        List<Double> newProbs = new ArrayList<>();

        for (Map<String, String> assignment : allAssignments) {
            double p1 = f1.getProbability(assignment);
            double p2 = f2.getProbability(assignment);
            newProbs.add(p1 * p2);
            multiplicationCount++;
        }

        return new Factor(sortedVars, newProbs);
    }

    // Utility method to deep copy a Variable
    private Variable deepCopyVariable(Variable v) {
        Variable copy = new Variable(v.getName());
        for (String outcome : v.getOutcomes()) {
            copy.addOutcome(outcome);
        }
        return copy;
    }


    // Eliminate a variable by summing it out
    private Factor eliminate(Factor factor, String varToEliminate) {
        List<Variable> remainingVars = factor.getVariables().stream()
                .filter(v -> !v.getName().equals(varToEliminate))
                .map(v -> {
                    Variable copy = new Variable(v.getName());
                    for (String outcome : v.getOutcomes()) copy.addOutcome(outcome);
                    return copy;
                })
                .sorted(Comparator.comparing(Variable::getName))
                .toList();

        List<Map<String, String>> assignments = generateAllAssignments(remainingVars);
        List<Double> newProbs = new ArrayList<>();

        Variable toRemove = findVariable(factor, varToEliminate);

        for (Map<String, String> assignment : assignments) {
            double sum = 0;
            boolean firstOutcome = true;
            for (String outcome : toRemove.getOutcomes()) {
                Map<String, String> extendedAssignment = new HashMap<>(assignment);
                extendedAssignment.put(varToEliminate, outcome);
               double prob = factor.getProbability(extendedAssignment);
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

        List<Variable> cleanVars = new ArrayList<>();
        for (Variable v : remainingVars) {
            Variable copy = new Variable(v.getName());
            for (String outcome : v.getOutcomes()) copy.addOutcome(outcome);
            cleanVars.add(copy);
        }
        System.out.println("after eliminating: " + varToEliminate + " the variable left are: " + cleanVars);
        return new Factor(cleanVars, newProbs);
    }



    private void normalize(Factor factor) {
        double total = 0;
        for (double p : factor.getProbabilities()) total += p;

        List<Double> normalized = new ArrayList<>();
        for (double p : factor.getProbabilities()) {
            normalized.add(p / total);
        }
        factor.setProbabilities(normalized);
    }

    private Variable findVariable(Factor factor, String name) {
        for (Variable v : factor.getVariables()) {
            if (v.getName().equals(name)) return v;
        }
        throw new RuntimeException("Variable not found: " + name);
    }

    private List<Map<String, String>> generateAllAssignments(List<Variable> vars) {
        List<Map<String, String>> result = new ArrayList<>();
        generateHelper(vars, 0, new HashMap<>(), result);
        return result;
    }

    private void generateHelper(List<Variable> vars, int idx, Map<String, String> current, List<Map<String, String>> result) {
        if (idx == vars.size()) {
            result.add(new HashMap<>(current));
            return;
        }

        Variable var = vars.get(idx);
        for (String outcome : var.getOutcomes()) {
            current.put(var.getName(), outcome);
            generateHelper(vars, idx + 1, current, result);
            current.remove(var.getName());
        }
    }
    protected String chooseNextToEliminate(List<String> hiddenVariables, List<Factor> factors) {
        List<String> sortedHidden = new ArrayList<>(hiddenVariables);
        Collections.sort(sortedHidden);
        return sortedHidden.get(0);
    }
    //method to calculate the ascii value of the factor to use it as a secondary key in the sorting.
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
