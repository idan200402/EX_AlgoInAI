import java.util.List;

public class Ex1 {
    public static void main(String[] args) {
        Extractor extractor = new Extractor();
        extractor.parseInput();
        List<Factor> factors = extractor.getFactors();
        List<Query> queries = extractor.getQueries();
        for(Query query : queries) {
            System.out.println("Query: " + query);
            BayesianAlgorithm algorithmFactory = AlgorithmFactory.createAlgorithm(query.getAlgorithmType());
            System.out.println(algorithmFactory.calculateProbability(query, factors));
            System.out.println();
        }


    }
}
