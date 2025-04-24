import java.util.ArrayList;
import java.util.List;

//this class represents a variable in bayesian network.
public class Variable {
    //it has two fields his name for example "A" or "B" and a list of outcomes for example {True, False} or {0, 1}.
    private final String name;
    private List<String> outcomes;

    //constructor
    //first assigning the name a value because it appears first in the xml file and also initializing the outcomes list as arraylist.
    public Variable(String name) {
        this.name = name;
        this.outcomes = new ArrayList<>();
    }
    //adding the outcomes to the list one by one since that's how they appear in the xml file.
    public void addOutcome(String outcome) {
        outcomes.add(outcome);
    }
    //getters and setters
    public String getName() {
        return name;
    }

    public List<String> getOutcomes() {
        return outcomes;
    }
    //for future use when dealing with factor operations.
    public int getOutcomesCount() {
        return outcomes.size();
    }
    //for debugging purposes.
    @Override
    public String toString() {
        return "name='" + name + '\'' + ", outcomes=" + outcomes + '}';
    }

}
