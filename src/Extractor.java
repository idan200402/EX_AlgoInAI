import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Extractor {
    private final Map<String, Variable> variablesLookup;
    private final List<CPT> CPTS;
    private final List<Query> queries;
    private String netPath;

    //constructor
    public Extractor() {
        this.variablesLookup = new HashMap<>();
        this.CPTS = new ArrayList<>();
        this.queries = new ArrayList<>();
    }
    public void parseInput() {
        this.netPath = getNetPath();
        parseXMLNetwork();
        parseQueries();
    }
    private void parseQueries() {
        String inputPath = "src/input.txt";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputPath));
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    Query query = parseQuery(line);
                    queries.add(query);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error: couldnt parse the queries from the input file " + e.getMessage());
        }
    }
    private Query parseQuery(String line) {
        int closingParenthesisIndex = line.indexOf(")")+1;
        String queryPart = line.substring(0, closingParenthesisIndex);
        String algorithmPart = line.length() > closingParenthesisIndex ? line.substring(closingParenthesisIndex) : "";
        int algorithmType = 0;
        if (algorithmPart.startsWith(",")){
            algorithmPart = algorithmPart.substring(1).trim();
            try{
                algorithmType = Integer.parseInt(algorithmPart);
            }catch (NumberFormatException e){
                throw new RuntimeException("Error: the algorithm type is not a number");
            }

        }
        Pattern pattern = Pattern.compile("P\\(([^|]*)(?:\\|([^)]*))?\\)");
        Matcher matcher = pattern.matcher(queryPart);
        if (matcher.find()) {
            String queryVariables = matcher.group(1).trim();
            String evidenceVariables = matcher.group(2);
            Map<String, String> varAssignment = parseAssignment(queryVariables);
            Map<String, String> evidenceAssignment = new HashMap<>();
            boolean isJointQuery = true;
            if (evidenceVariables != null&&!evidenceVariables.isEmpty()){
                evidenceAssignment = parseAssignment(evidenceVariables);
                isJointQuery = false;
            }
            return new Query(varAssignment, evidenceAssignment, algorithmType , isJointQuery);

        }
        else{
            throw new RuntimeException("Error: the query is not in the right format");

        }

    }
    private Map<String, String> parseAssignment(String assignment) {
        Map<String, String> res = new LinkedHashMap<>();
        String[] varsAssigns = assignment.split(",");
        for (String varAssign : varsAssigns) {
            String[] varAssignPair = varAssign.trim().split("=");
            if (varAssignPair.length == 2) {
                String var = varAssignPair[0].trim();
                String assign = varAssignPair[1].trim();
                res.put(var, assign);
            } else {
                throw new RuntimeException("Error: the variable has no assignment or otherwise the format is wrong");
            }
        }
        return res;
    }
        private void parseXMLNetwork() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            File xmlFile = new File(netPath);
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
        for (int i = 0; i < cptList.getLength(); i++) {
            Element cptElement = (Element) cptList.item(i);
            String variableName = extractText(cptElement, "FOR");
            Variable variable = variablesLookup.get(variableName);
            if (variable != null) {
                CPT CPT = new CPT(variable);
                NodeList parentsList = cptElement.getElementsByTagName("GIVEN");
                for (int j = 0; j < parentsList.getLength(); j++) {
                    Element parentElement = (Element) parentsList.item(j);
                    String parentName = parentElement.getTextContent();
                    Variable parentVariable = variablesLookup.get(parentName);
                    if (parentVariable != null) {
                        CPT.addParent(parentVariable);
                    }
                }
                String probabilityTable = extractText(cptElement, "TABLE");
                List<Double> probabilities = parseTable(probabilityTable);
                CPT.setProbabilities(probabilities);
                CPTS.add(CPT);
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
    public List<CPT> getFactors() {
        return CPTS;
    }

    public void printFactors() {
        for (CPT CPT : CPTS) {
            System.out.println(CPT);
        }
    }

    private String getNetPath() {
        String inputPath = "src/input.txt";
        String res = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputPath));
            res = reader.readLine();
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return "src/" + res;
    }
    public List<Query> getQueries() {
        return queries;
    }
    public void printQueries() {
        for (Query query : queries) {
            System.out.println(query);
        }
    }

}
