import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
//main class to run the program.
public class Ex1 {
    public static void main(String[] args) {
        //calling the extractor class to extract the data from the xml file , default is "src/input.xml".
        Extractor extractor = new Extractor();
        extractor.parseInput();
        List<CPT> CPTs = extractor.getFactors();
        List<Query> queries = extractor.getQueries();
        try {
            //creating the output file and writing the results to it in the format specified in the ex1 file.
            BufferedWriter writer = new BufferedWriter(new FileWriter("src/output.txt"));
            //for each Query (contains the query  , evidence and algorithm type) we create the algorithm object and call the calculateProbability method.
            for(int i = 0; i < queries.size(); i++) {
                Query query = queries.get(i);
                //using factory design pattern to create the algorithm object based on the algorithm type.
                BayesianAlgorithm algorithmFactory = AlgorithmFactory.createAlgorithm(query.getAlgorithmType());
                //this method is assigning values to the probability , addition and multiplication attributes.
                algorithmFactory.calculateProbability(query, CPTs);
                writer.write(String.format("%.5f,%d,%d", algorithmFactory.getProbability(), algorithmFactory.getAdditionCount(), algorithmFactory.getMultiplicationCount()));
                //for the last query we don't need to add a new line.
                if(i<queries.size()-1){
                    writer.write("\n");
                }
            }
            writer.close();
        }catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
