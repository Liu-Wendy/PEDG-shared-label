package MPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ComposedAutomata {
    public ArrayList<Automata> automataList;
    public Map<String, Automata> automataMap;
    public ArrayList<String> sharedlabels;
    public Map<String ,ArrayList<Automata>> label_automata_map;
    public String objFun;
    public boolean staytime4loc;
    public String forbidden;
    public String feasible_fun;
    public boolean addInit;

    // --todo-- shared variables

    public ComposedAutomata(){
        automataList=new ArrayList<>();
        automataMap=new HashMap<>();
        sharedlabels=new ArrayList<>();
        label_automata_map=new HashMap<>();
        staytime4loc=true;
        addInit=false;
    }

    public void setAutomaton(Automata aut){
        aut.ID=automataList.size();
        automataMap.put(aut.name,aut);
        automataList.add(aut);
    }
}
