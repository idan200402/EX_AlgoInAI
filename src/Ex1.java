import java.util.List;
//main class to run the program.
public class Ex1 {
    public static void main(String[] args) {
        Extractor extractor = new Extractor();
        extractor.parseInput();
        List<CPT> CPTs = extractor.getFactors();
        List<Query> queries = extractor.getQueries();

        for(Query query : queries) {
            System.out.println("Query: " + query);
            BayesianAlgorithm algorithmFactory = AlgorithmFactory.createAlgorithm(query.getAlgorithmType());
            System.out.println("Probability: ");
            System.out.printf("%.5f%n", algorithmFactory.calculateProbability(query, CPTs));
            System.out.println("Addition count: " + algorithmFactory.getAdditionCount());
            System.out.println("Multiplication count: " + algorithmFactory.getMultiplicationCount());
            System.out.println();
        }


    }
}
