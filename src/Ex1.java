import java.util.ArrayList;
import java.util.List;

public class Ex1 {
    public static void main(String[] args) {
        FactorExtractorFromXML extractorFromXML = new FactorExtractorFromXML();
        extractorFromXML.parseXML("src/alarm_net.xml");
        System.out.println("number of factors: " + extractorFromXML.getFactors().size());
        extractorFromXML.printFactors();
    }
}
