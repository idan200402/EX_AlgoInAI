import java.util.HashMap;
import java.util.Map;

public class Query {
    private final Map<String,String> query;
    private final Map<String,String> evidence;
    private final int algorithmType;
    private final boolean isJointQuery;
    public Query(Map<String,String> query, Map<String,String> evidence, int algorithmType , boolean isJointQuery) {
        this.query = query;
        this.evidence = evidence;
        this.algorithmType = algorithmType;
        this.isJointQuery = isJointQuery;
    }

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
    @Override
    public String toString() {
        Map<Integer, String> algorithmTypeMap = new HashMap<>();
                algorithmTypeMap.put(0, "Lookup");
                algorithmTypeMap.put(1, "Simple inference");
                algorithmTypeMap.put(2, "Variable elimination");
                algorithmTypeMap.put(3, "Something else");
        if(isJointQuery){
            return "P(" + assaignmentToString(query) + ") , " + algorithmTypeMap.get(algorithmType) + "\n";
        }
        else{
            return "P(" + assaignmentToString(query) + " | " + assaignmentToString(evidence ) + ") , " + algorithmTypeMap.get(algorithmType) + "\n";
        }

    }
    private String assaignmentToString(Map<String, String> assignments) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> entry : assignments.entrySet()) {
            if(!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}
