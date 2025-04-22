import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FactorExtractorFromXML {
    private Map<String, Variable> variablesLookup;
    private List<Factor> factors;

    //constructor
    public FactorExtractorFromXML() {
        this.variablesLookup = new HashMap<>();
        this.factors = new ArrayList<>();
    }

    public void parseXML(String xmlFilePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            File xmlFile = new File(xmlFilePath);
            Document doc = (Document) builder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            parseFirstHalve(doc);
            parseSecondHalve(doc);

        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    //this method parses the first half of the xml file which contains the variables and their outcomes.
    private void parseFirstHalve(Document doc) {
        NodeList variablesList = doc.getElementsByTagName("VARIABLE");
        for (int i = 0; i < variablesList.getLength(); i++) {
            Element varElement = (Element) variablesList.item(i);
            String name = extractText(varElement, "NAME");
            Variable variable = new Variable(name);
            NodeList outcomesList = varElement.getElementsByTagName("OUTCOME");
            for (int j = 0; j < outcomesList.getLength(); j++) {
                Element outcomeElement = (Element) outcomesList.item(j);
                String outcome = outcomeElement.getTextContent();
                variable.addOutcome(outcome);
            }
            variablesLookup.put(name, variable);
        }
    }

    //this method parses the second half of the xml file which contains the variable's parents and their conditional probabilities.
    private void parseSecondHalve(Document doc) {
        NodeList cptList = doc.getElementsByTagName("DEFINITION");
        System.out.println("number of cpts: " + cptList.getLength());
        for (int i = 0; i < cptList.getLength(); i++) {
            Element cptElement = (Element) cptList.item(i);
            String variableName = extractText(cptElement, "FOR");
            System.out.println("variable name: " + variableName);
            Variable variable = variablesLookup.get(variableName);
            if(variable != null) {
                Factor factor = new Factor(variable);
                NodeList parentsList = cptElement.getElementsByTagName("GIVEN");
                for (int j = 0; j < parentsList.getLength(); j++) {
                    Element parentElement = (Element) parentsList.item(j);
                    String parentName = parentElement.getTextContent();
                    Variable parentVariable = variablesLookup.get(parentName);
                    if (parentVariable != null) {
                        factor.addParent(parentVariable);
                    }
                }
                String probabilityTable = extractText(cptElement, "TABLE");
                System.out.println("probability table: " + probabilityTable);
                List<Double> probabilities = parseTable(probabilityTable);
                factor.setProbabilities(probabilities);
                factors.add(factor);
            }
        }
    }

    private String extractText(Element superElement, String nameTag) {
        NodeList nodeList = superElement.getElementsByTagName(nameTag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        } else {
            return "";
        }
    }
    private List<Double> parseTable(String table) {
        String[] values = table.split("\\s+");
        List<Double> res = new ArrayList<>();
        for (String value : values) {
            res.add(Double.parseDouble(value));
        }
        return res;
    }
    //getters
    public List<Factor> getFactors() {
        return factors;
    }
    public void printFactors() {
        for (Factor factor : factors) {
            System.out.println(factor);
        }
    }
}
