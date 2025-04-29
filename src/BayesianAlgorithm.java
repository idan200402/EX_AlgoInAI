import java.util.List;

public abstract class  BayesianAlgorithm {
    protected int additionCount = 0;
    protected int multiplicationCount = 0;
    public abstract double calculateProbability(Query query , List<CPT> CPTS);
    // Add any other methods that are needed for the Bayesian algorithm
    public int getAdditionCount() {
        return additionCount;
    }
    public int getMultiplicationCount() {
        return multiplicationCount;
    }
}
