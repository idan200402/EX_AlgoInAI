import java.util.*;
import java.util.stream.Collectors;

public class VariableElimination extends BayesianAlgorithm {

    @Override
    public double calculateProbability(Query query, List<CPT> cpts) {
        additionCount = 1;
        multiplicationCount = 0;

        // Step 1: Deep copy CPTs
        List<CPT> cptsCopy = deepCopyCPTs(cpts);

        // Step 2: Convert all CPTs into Factors
        List<Factor> factors = new ArrayList<>();
        for (CPT cpt : cptsCopy) {
            factors.add(convertCPTtoFactor(cpt));
        }

        // Step 3: Prune factors that are not relevant
        pruneFactors(factors, query.getQuery().keySet(), query.getEvidence().keySet());


        // Step 4: Restrict evidence
        restrictEvidence(factors, query.getEvidence());

        // Step 5: Identify hidden variables
        Set<String> allVariables = getAllVariables(factors);
        Set<String> ancestors = getAncestors(factors, query.getQuery().keySet(), query.getEvidence().keySet());

        Set<String> hiddenVariables = new HashSet<>(ancestors);
        hiddenVariables.removeAll(query.getQuery().keySet());
        hiddenVariables.removeAll(query.getEvidence().keySet());
        System.out.println("Hidden variables: " + hiddenVariables);
        // Step 6: Eliminate hidden variables one by one
        while (!hiddenVariables.isEmpty()) {
            List<String> hiddenList = new ArrayList<>(hiddenVariables);
            Collections.sort(hiddenList);
            String hidden = hiddenList.get(0);
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

        Set<String> factorVars  = finalFactor.getVariables().stream()
                .map(Variable::getName)
                .collect(Collectors.toSet());
        fullAssignment.entrySet()
                .removeIf(entry -> !factorVars.contains(entry.getKey()));
        System.out.println("Final Factor Vars: " + finalFactor.getVariables().stream().map(Variable::getName).toList());
        System.out.println("Final Assignment: " + fullAssignment);

        // Step 9: Return probability for query assignment
        return finalFactor.getProbability(fullAssignment);
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
    private void pruneFactors(List<Factor> factors, Set<String> queryVars, Set<String> evidenceVars) {
        Set<String> ancestors = getAncestors(factors, queryVars, evidenceVars);
        factors.removeIf(factor -> !containsAny(factor, ancestors));
    }


    private boolean containsAny(CPT cpt, Set<String> vars) {
        if (vars.contains(cpt.getVariable().getName())) return true;
        for (Variable parent : cpt.getParents()) {
            if (vars.contains(parent.getName())) return true;
        }
        return false;
    }

    // Find ancestors of query and evidence
    private Set<String> getAncestors(List<Factor> factors, Set<String> queryVars, Set<String> evidenceVars) {
        Set<String> targets = new HashSet<>(queryVars);
        targets.addAll(evidenceVars);
        Set<String> ancestors = new HashSet<>(targets);

        boolean changed;
        do {
            changed = false;
            for (Factor factor : factors) {
                for (Variable v : factor.getVariables()) {
                    if (targets.contains(v.getName())) {
                        for (Variable u : factor.getVariables()) {
                            if (ancestors.add(u.getName())) {
                                changed = true;
                            }
                        }
                    }
                }
            }
            targets = new HashSet<>(ancestors);
        } while (changed);

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
    private List<Factor> getFactorsMentioning(String var, List<Factor> factors) {
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
        List<Factor> working = new ArrayList<>(factors);

        while (working.size() > 1) {
            // Find the two smallest factors
            Factor f1 = null, f2 = null;
            int minSize = Integer.MAX_VALUE;

            for (int i = 0; i < working.size(); i++) {
                for (int j = i + 1; j < working.size(); j++) {
                    int size = estimateJoinSize(working.get(i), working.get(j));
                    if (size < minSize) {
                        minSize = size;
                        f1 = working.get(i);
                        f2 = working.get(j);
                    }
                }
            }

            // Join the smallest pair
            Factor joined = joinTwoFactors(f1, f2);

            // Replace them with the joined factor
            working.remove(f1);
            working.remove(f2);
            working.add(joined);
        }

        return working.get(0);
    }
    private int estimateJoinSize(Factor f1, Factor f2) {
        Set<String> vars = new HashSet<>();
        for (Variable v : f1.getVariables()) vars.add(v.getName());
        for (Variable v : f2.getVariables()) vars.add(v.getName());

        int total = 1;
        for (String varName : vars) {
            total *= 2; // assuming binary outcomes (T, F)
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
}
