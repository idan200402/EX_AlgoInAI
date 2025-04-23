public class AlgorithmFactory {
    public static BayesianAlgorithm createAlgorithm(int type){
        return switch(type){
            case 0 -> new Lookup();
            case 1 -> new SimpleInference();
            case 2 -> new VariableElimination();
            case 3 -> new SomethingElse();
            default -> throw new IllegalArgumentException("Invalid algorithm type: " + type);
        };
    }
}
