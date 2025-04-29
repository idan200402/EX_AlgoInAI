//a factory class to create different types of Bayesian algorithms by the type of algorithm.
public class AlgorithmFactory {

    public static BayesianAlgorithm createAlgorithm(int type){
        switch (type) {
               case 0:
                   return new Lookup();
               case 1:
                   return new SimpleInference();
               case 2:
                   return new VariableElimination();
               case 3:
                   return new SomethingElse();
               default:
                   throw new IllegalArgumentException("Invalid algorithm type: " + type);
           }
    }
}
