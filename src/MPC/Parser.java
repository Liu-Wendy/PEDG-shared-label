package MPC;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import MPC.*;
import MPC.tools.BindInfo;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Parser {
    public Map<String, Automata> automataProtoMap;
    public Document doc;
    public char separator='_';
    public CFGDriver cfgdriver;
    public ComposedAutomata ca;

    public Parser(String xmlfile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        doc = dbf.newDocumentBuilder().parse(new File("models/"+xmlfile));
        automataProtoMap=new HashMap<>();
        NodeList nodes=doc.getElementsByTagName("component");

        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            parseComponent(element);
        }
    }

    private void parseCFGFile() {
        String initialStr=cfgdriver.getCfgPropertiesValue("initially");

        parseIniSpec(initialStr);
        String forbiddenStr=cfgdriver.getCfgPropertiesValue("forbidden").trim();
        ca.forbidden=forbiddenStr;
        ca.objFun= cfgdriver.getCfgPropertiesValue("obj_function").trim();
        ca.objFun=mathExp(ca.objFun);
        ca.feasible_fun= cfgdriver.getCfgPropertiesValue("feasible_fun").trim();
        ca.feasible_fun=mathExp(ca.feasible_fun);
        String flag=cfgdriver.getCfgPropertiesValue("staytime4loc");
        if(Objects.equals(flag, "false")){
            ca.staytime4loc=false;
        }
        String tm=cfgdriver.getCfgPropertiesValue("addInit").trim();
        ca.addInit=Boolean.parseBoolean(tm);

    }

    private String mathExp(String string) {
        string = string.replace("pow","$(Math).pow");
        string = string.replace("sin","$(Math).sin");
        string = string.replace("cos","$(Math).cos");
        string = string.replace("tan","$(Math).tan");
        string = string.replace("abs","$(Math).abs");
        return string;
    }

    private void parseIniSpec(String initialStr) {
        String[] strings = initialStr.split("&");
        for (String string : strings) {
            String[] temp = string.trim().split("==");
            if (temp[0].trim().contains("loc(")) {
                int start = temp[0].trim().indexOf("(")+1;
                int end = temp[0].trim().indexOf(")");
                String aut_name=temp[0].substring(start, end);
                Automata aut = ca.automataMap.get(aut_name);
                aut.initLocName = temp[1].trim();
                for (Map.Entry<Integer, Location> entry : aut.locations.entrySet()) {
                    //System.out.println(allParametersValues.size());
                    if (entry.getValue().name.equals(aut.initLocName)) {
                        aut.initLoc = entry.getKey();
                        break;
                    }
                }
            } else if (temp[0].trim().contains(".")) {
                String[] str = temp[0].trim().split("\\.");
                Automata aut = ca.automataMap.get(str[0]);
                if (temp[1].indexOf('[') != -1) {
                    int firstIndex = temp[1].indexOf("[");
                    int lastIndex = temp[1].indexOf("]");
                    String[] bounds = temp[1].substring(firstIndex + 1, lastIndex).trim().split(",");
                    double lowerbound = Double.parseDouble(bounds[0].trim());
                    double upperbound = Double.parseDouble(bounds[1].trim());
                    if (aut.rangeParameters == null) aut.rangeParameters = new ArrayList<>();
                    aut.rangeParameters.add(new RangeParameter(aut.name+"_"+str[1].trim(), lowerbound, upperbound));
                } else aut.initParameterValues.put(aut.name+"_"+str[1].trim(), Double.parseDouble(temp[1].trim()));
            }
        }
    }

    public ComposedAutomata generateComposedAutomaton(String cfgfile){
        cfgdriver=new CFGDriver(cfgfile);
        String system=cfgdriver.getCfgPropertiesValue("system");

        if(automataProtoMap.containsKey(system)){
            Automata autProtoType=automataProtoMap.get(system);
            ca=new ComposedAutomata();
            for(BindInfo b:autProtoType.bindInfos){
                ca.setAutomaton(generateAutomatonVariables(b,b.name+separator));
            }
            for(BindInfo b:autProtoType.bindInfos){
                generateAutomatonStatesAndTransitions(b,ca,ca.automataMap.get(b.name),b.name+separator);
            }
            parseCFGFile();
            return ca;
        }
        return null;

    }

    private Automata generateAutomatonVariables(BindInfo b, String prefix) {
        Automata rnt=new Automata(b.name);
        Automata aprototype=automataProtoMap.get(b.component);

        for(String param:aprototype.parameters){
            if(!b.map.containsKey(param)){
                rnt.parameters.add(prefix+param);
            }
        }
        for(Map.Entry<String,Double> entry:aprototype.initParameterValues.entrySet()){
            rnt.initParameterValues.put(prefix+entry.getKey(),entry.getValue());
        }
        for(RangeParameter range:aprototype.rangeParameters){
            rnt.rangeParameters.add(new RangeParameter(prefix+range.name,range.lowerBound,range.upperBound));
        }


        //todo generatelabels
        return rnt;
    }

    private void generateAutomatonStatesAndTransitions(BindInfo b, ComposedAutomata ca, Automata automata, String prefix) {
        Automata prototype=automataProtoMap.get(b.component);
        Map<String,Integer> label_counts=new HashMap<>();

        for(Map.Entry<Integer,Location> entry:prototype.locations.entrySet()){
            Location loc=new Location(entry.getKey(),entry.getValue().name);
            for(Map.Entry<String,String> flow:entry.getValue().flows.entrySet()){
                loc.flows.put(prefix+flow.getKey(),checkexp(flow.getValue(),b,prefix,prototype.parameters));
            }

            for (String invariant:entry.getValue().invariants){
                loc.invariants.add(checkexp(invariant,b,prefix,prototype.parameters));
            }

            automata.locations.put(entry.getKey(),loc);
        }

        for(Transition tran:prototype.transitions){
            String label=tran.label;
            boolean shared=false;
            if(b.map.containsKey(label)){
                label=b.map.get(label);
            }else label=prefix+label;

            if(label_counts.containsKey(label)){
                //shared label
                shared=true;
                label_counts.put(label,label_counts.get(label)+1);
                automata.shared_labels.add(label);
                if(ca.label_automata_map.get(label)!=null){
                    ca.label_automata_map.get(label).add(automata);
                }
            }else{
                label_counts.put(label,1);
            }

            Transition t=new Transition(label,tran.source,tran.target);
            automata.locations.get(tran.source).addNeibour(tran.target);
            automata.locations.get(tran.source).next_trans.add(t);
            t.setShared(shared);
            for(String guard:tran.guards){
                t.guards.add(checkexp(guard,b,prefix,prototype.parameters));
            }

            for(Map.Entry<String, String>assignment:tran.assignments.entrySet()){
                t.assignments.put(prefix+assignment.getKey(),checkexp(assignment.getValue(),b,prefix, prototype.parameters));
            }
            if(!automata.labels.contains(t.label)) automata.labels.add(t.label);
            automata.transitions.add(t);
        }
    }

    private String checkexp(String exp, BindInfo b, String prefix, ArrayList<String> parameters) {
        String ans=exp;
        for(String s:b.map.keySet()){
            if(ans.contains(s)) {
                ans=ans.replace(s,b.map.get(s));
            }
        }
        for(String s:parameters){
            if(ans.contains(s)){
                ans=ans.replace(s,prefix+s);
            }
        }
        return ans;
    }

    private String checkexpPro(String exp, BindInfo b, String prefix, ArrayList<String> parameters) {

        String ans=exp;
        for(String s:b.map.keySet()){
            if(ans.contains(s)) {
                ans=ans.replace(s,b.map.get(s));
            }
        }
        for(String s:parameters){
            if(ans.contains(s)){
                ans=ans.replace(s,prefix+s);
            }
        }
        return ans;
    }

    private String checkexp(String exp, BindInfo b) {
        String ans=exp;
        for(String s:b.map.keySet()){
            if(ans.contains(s)) {
                ans=ans.replace(s,b.map.get(s));
            }
        }
        return ans;
    }

    private void parseComponent(Element element) {
        String aut_name = element.getAttribute("id");
        Automata automata=new Automata(aut_name);

        NodeList params=element.getElementsByTagName("param");
        for (int i = 0; i < params.getLength(); i++) {
            Element pNode = (Element) params.item(i);
            String pname=pNode.getAttribute("name");

            String ptype=pNode.getAttribute("type");
            if(Objects.equals(ptype, "real")){
                automata.parameters.add(pname);
            }else if(Objects.equals(ptype, "label")){
                automata.labels.add(pname);
            }
        }

        NodeList locs=element.getElementsByTagName("location");
        for (int i = 0; i < locs.getLength(); i++) {
            Element pNode = (Element) locs.item(i);
            parseLocation(automata,pNode);
        }

        NodeList trans=element.getElementsByTagName("transition");
        for (int i = 0; i < trans.getLength(); i++) {
            Element pNode = (Element) trans.item(i);
            parseTransition(automata,pNode);
        }

        NodeList bindInfos=element.getElementsByTagName("bind");
        for (int i = 0; i < bindInfos.getLength(); i++) {
            Element pNode = (Element) bindInfos.item(i);
            parseBindInfo(automata,pNode);
        }
        automataProtoMap.put(aut_name,automata);

    }

    private void parseBindInfo(Automata automata, Element pNode) {
        String com=pNode.getAttribute("component");
        String name=pNode.getAttribute("as");
        BindInfo bindInfo=new BindInfo(com,name);
        NodeList mapList=pNode.getElementsByTagName("map");
        for (int i = 0; i < mapList.getLength(); i++) {
            Element mapinfo = (Element) mapList.item(i);
            String k =mapinfo.getAttribute("key");
            String v=mapinfo.getTextContent().trim();
            bindInfo.map.put(k,v);
        }
        automata.bindInfos.add(bindInfo);

    }

    private void parseTransition(Automata automata, Element pNode) {
        NodeList labelList=pNode.getElementsByTagName("label");
        String label="";
        if(labelList.getLength()>0)     label=labelList.item(0).getTextContent();
        int source = Integer.parseInt(pNode.getAttribute("source"));
        int target = Integer.parseInt(pNode.getAttribute("target"));
        Transition transition = new Transition(label,source, target);
        automata.locations.get(source).addNeibour(target);

        NodeList guardList=pNode.getElementsByTagName("guard");
        for (int i = 0; i < guardList.getLength(); i++) {
            Element invariant = (Element) guardList.item(i);
            String guardStr=invariant.getTextContent().trim();
            transition.setGuard(guardStr, automata.parameters);
        }

        NodeList assignmentList=pNode.getElementsByTagName("assignment");
        for (int i = 0; i < assignmentList.getLength(); i++) {
            Element assignment = (Element) assignmentList.item(i);
            String assignmentStr=assignment.getTextContent().trim();
            transition.setAssignment(assignmentStr, automata.parameters);
        }
        automata.locations.get(source).next_trans.add(transition);
        automata.transitions.add(transition);
    }

    private void parseLocation(Automata automata, Element pNode) {
        String lname=pNode.getAttribute("name");
        int id=Integer.parseInt(pNode.getAttribute("id"));
        Location location=new Location(id,lname);

        NodeList invarList=pNode.getElementsByTagName("invariant");
        for (int i = 0; i < invarList.getLength(); i++) {
            Element invariant = (Element) invarList.item(i);
            String invariantStr=invariant.getTextContent().trim();
            location.setVariant(invariantStr, automata.parameters);
        }

        NodeList flowList=pNode.getElementsByTagName("flow");
        for (int i = 0; i < flowList.getLength(); i++) {
            Element flow = (Element) flowList.item(i);
            String flowStr=flow.getTextContent().trim();
            location.setFlow(flowStr, automata.parameters);
        }

        automata.locations.put(location.getNo(), location);
    }


}


