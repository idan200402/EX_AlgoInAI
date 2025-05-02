import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
//main class to run the program.
public class Ex1 {
    public static void main(String[] args) {
        Extractor extractor = new Extractor();
        extractor.parseInput();
        List<CPT> CPTs = extractor.getFactors();
        List<Query> queries = extractor.getQueries();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("src/test.txt"));
            for (Query query : queries) {
                System.out.println("Query: " + query);
                BayesianAlgorithm algorithmFactory = AlgorithmFactory.createAlgorithm(query.getAlgorithmType());
                algorithmFactory.calculateProbability(query, CPTs);
                System.out.println("Probability: " + String.format("%.5f", algorithmFactory.getProbability()));
                System.out.println("Addition count: " + algorithmFactory.getAdditionCount());
                System.out.println("Multiplication count: " + algorithmFactory.getMultiplicationCount());
                System.out.println();
                writer.write(String.format("%.5f,%d,%d\n", algorithmFactory.getProbability(), algorithmFactory.getAdditionCount(), algorithmFactory.getMultiplicationCount()));
            }
            writer.close();
        }catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
