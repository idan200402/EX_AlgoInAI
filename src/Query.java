import java.util.HashMap;
import java.util.Map;
//this class represents a query in the Bayesian network.
//has a map from name of the variable to its outcome for query and evidence.
//it also has the algorithm type as it written in the input file and a boolean to check if the query is a joint query or not.
public class Query {
    private final Map<String,String> query;
    private final Map<String,String> evidence;
    private final int algorithmType;
    private final boolean isJointQuery;
    //constructor
    public Query(Map<String,String> query, Map<String,String> evidence, int algorithmType , boolean isJointQuery) {
        this.query = query;
        this.evidence = evidence;
        this.algorithmType = algorithmType;
        this.isJointQuery = isJointQuery;
    }
    //getters
    public Map<String, String> getQuery() {
        return query;
    }

    public Map<String, String> getEvidence() {
        return evidence;
    }

    public int getAlgorithmType() {
        return algorithmType;
    }
    public boolean isJointQuery() {
        return isJointQuery;
    }
    //toString method to print the query in a readable format.
    @Override
    public String toString() {
        Map<Integer, String> algorithmTypeMap = new HashMap<>();
                algorithmTypeMap.put(0, "Lookup");
                algorithmTypeMap.put(1, "Simple inference");
                algorithmTypeMap.put(2, "Variable elimination");
                algorithmTypeMap.put(3, "VE Heuristic");
        if(isJointQuery){
            return "P(" + assaignmentToString(query) + ") , " + algorithmTypeMap.get(algorithmType) + "\n";
        }
        else{
            return "P(" + assaignmentToString(query) + " | " + assaignmentToString(evidence ) + ") , " + algorithmTypeMap.get(algorithmType) + "\n";
        }

    }
    //returns the string representation of the assignments in the format of P(var1=outcome1, var2=outcome2, ...) or
    //P(var1=outcome1 | var2=outcome2, ...)
    private String assaignmentToString(Map<String, String> assignments) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> entry : assignments.entrySet()) {
            if(sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}
