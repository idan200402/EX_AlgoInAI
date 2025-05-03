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

//this class is responsible for parsing the input file and the xml file , then the attributes of it its the
//map from the name of a var to its object , the CPTs , the queries and the path to the xml file.
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

    //encapsulates method that contains the main methods of first get the file path to the XML and parse the network,
    //and then parse the queries.
    public void parseInput() {
        this.netPath = getNetPath();
        parseXMLNetwork();
        parseQueries();
    }

    //wraps the parsing of the queries.
    private void parseQueries() {
        //_for_big_net
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

    //main method that parses the query from the input file.
    private Query parseQuery(String line) {
        //splitting the txt line to 2 parts the query and the algorithm type  , not all the queries have an algorithm type.
        int closingParenthesisIndex = line.indexOf(")") + 1;
        String queryPart = line.substring(0, closingParenthesisIndex);
        int algorithmType = getAlgorithmType(line, closingParenthesisIndex);
        //using regular expression to extract the variables and their assignments from the query part.
        Pattern pattern = Pattern.compile("P\\(([^|]*)(?:\\|([^)]*))?\\)");
        Matcher matcher = pattern.matcher(queryPart);
        //if the query is in the expected format:
        if (matcher.find()) {
            //spitting the query part to get query variable and the evidence variable.
            //if the query is a joint Probability query then the evidence variable is null , and all the variables are in the query variable.
            String queryVariables = matcher.group(1).trim();
            String evidenceVariables = matcher.group(2);
            //for each variable there is a mapping to its assignment.
            Map<String, String> varAssignment = parseAssignment(queryVariables);
            Map<String, String> evidenceAssignment = new HashMap<>();
            boolean isJointQuery = true;
            if (evidenceVariables != null && !evidenceVariables.isEmpty()) {
                evidenceAssignment = parseAssignment(evidenceVariables);
                isJointQuery = false;
            }
            return new Query(varAssignment, evidenceAssignment, algorithmType, isJointQuery);

        } else {
            throw new RuntimeException("Error: the query is not in the right format");

        }

    }

    //method that checks if the algorithm type is in the query or not , if not it returns 0 as default. (for AlgorithmFactory initialization)
    private static int getAlgorithmType(String line, int closingParenthesisIndex) {
        String algorithmPart = line.length() > closingParenthesisIndex ? line.substring(closingParenthesisIndex) : "";
        int algorithmType = 0;
        if (algorithmPart.startsWith(",")) {
            algorithmPart = algorithmPart.substring(1).trim();
            //in case the txt is corrupted and the algorithm type is not a number.
            try {
                algorithmType = Integer.parseInt(algorithmPart);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Error: the algorithm type is not a number");
            }

        }
        return algorithmType;
    }

    //method maps the variables to their assignments.
    private Map<String, String> parseAssignment(String subQuery) {
        Map<String, String> res = new LinkedHashMap<>();
        //array of the variables and their assignments. eg ['X=True', 'Y=FALSE']
        String[] varsAssigns = subQuery.split(",");
        for (String varAssign : varsAssigns) {
            //for all X = True , we split it throw the '=' sign and trim the spaces.
            String[] varAssignPair = varAssign.trim().split("=");
            if (varAssignPair.length == 2) {
                String var = varAssignPair[0].trim();
                String assign = varAssignPair[1].trim();
                res.put(var, assign);
            } else {
                //exception in case the format is wrong.
                throw new RuntimeException("Error: the variable has no assignment or otherwise the format is wrong");
            }
        }
        return res;
    }

    //this method parses the xml file and creates the Variable objects , the CPT objects and Map.
    //serves as a wrapper for the parsing methods.
    private void parseXMLNetwork() {
        try {
            //using this Library to parse the xml file since there are a lot of Libraries that uses DOM (JS).
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            File xmlFile = new File(netPath);
            Document doc = (Document) builder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            //assuming the network is first declaring the variables and their outcomes and then their parents and their conditional probabilities.
            parseFirstHalve(doc);
            parseSecondHalve(doc);

        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    //this method parses the first half of the xml file which contains the variables and their outcomes.
    private void parseFirstHalve(Document doc) {
        //tree structure of the xml file.
        NodeList variablesList = doc.getElementsByTagName("VARIABLE");
        //iterating over each element with the tag "Name" and creating a Variable object , then foe it it iterating over the outcomes and adding them to the variable.
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
            //before starting new iteration we adding the string name to the map with the variable object we just created.
            variablesLookup.put(name, variable);
        }
    }

    //this method parses the second half of the xml file which contains the variable's parents and their conditional probabilities.
    private void parseSecondHalve(Document doc) {
        //a list of potential CPTs.
        //for each FOR (variable) we iterate over its Given (parents) adding it to the CPT object.
        //and after that it has one TABLE and we adding it to the CPT object.
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
    //getting the text content of the element with the given name tag.
    private String extractText(Element superElement, String nameTag) {
        NodeList nodeList = superElement.getElementsByTagName(nameTag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        } else {
            return "";
        }
    }
    //creating an array of string with regex (spaces) and then parsing it to double arrayList.
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
    //method to read the first line of the input file to get the path to the xml file assuming the format is correct.
    private String getNetPath() {
        //_for_big_net
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
    //debugging method to print the queries and to we successfully parsed the queries.
    public void printQueries() {
        for (Query query : queries) {
            System.out.println(query);
        }
    }

}
