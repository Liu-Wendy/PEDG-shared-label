package MPC;

import Racos.Componet.Instance;
import Racos.Method.Continue;
import Racos.ObjectiveFunction.ObjectFunction;
import Racos.ObjectiveFunction.Task;
import Racos.Time.TimeAnalyst;
import Racos.Tools.ValueArc;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Combination {
    public Parser parser;
    public ComposedAutomata ca;
    public Automata product;
    public TimeAnalyst timeAnalyst;
    public String forbiddenConstraints;
    public ArrayList<Automata> automata;
    public int automata_num;
    File output;
    BufferedWriter bufferedWriter;
    public ArrayList<ArrayList<Integer>> PathMap;
//    public ArrayList<ArrayList<Integer>> combin;
    public String forbidden;
    public double delta ;
    public double cycle;
    public ValueArc minValueArc;
    public Map<String,Automata> autMap;
    public int feasibla_target;
    public int cbound;

    public Combination(ArrayList<Automata> list) {
        automata =list;
    }

    public Combination(String modelFile, String cfgFile,int bound){
        try {
            autMap=new HashMap<>();
            timeAnalyst=new TimeAnalyst();
            double before = System.currentTimeMillis();
            parser = new Parser(modelFile);
            ca = parser.generateComposedAutomaton(cfgFile);
            double after = System.currentTimeMillis();

            timeAnalyst.addParseTime((after-before)/1000);
            cbound=bound;
            before = System.currentTimeMillis();
            productAll();
            after = System.currentTimeMillis();
            timeAnalyst.addProductTime((after-before)/1000);

            automata_num=1;
            delta=ca.automataList.get(0).delta;

            for(Automata aut: ca.automataList){
                autMap.put(aut.name,aut);
            }

            before = System.currentTimeMillis();
            dfs(bound);
            after = System.currentTimeMillis();
            timeAnalyst.addDFSTime((after-before)/1000);
            System.out.println("totall paths:"+PathMap.size());
            System.out.println("encode time:"+timeAnalyst.getDfsTime());

        }catch (IOException e) {
            System.out.println("Open file fail!");
        }catch (ParserConfigurationException | SAXException ignored){
        }

    }

    private void dfs(int b) {
        if(product!=null){
            ArrayList<Integer> pathList=new ArrayList<>();
            PathMap=new ArrayList<>();
            if (product.initLoc != -1) {
                pathList.add(product.initLoc);
                DFS1(pathList,b);
            }

        }
    }

    void DFS1(ArrayList<Integer> arrayListPath,int maxPathSize) {
        int len = arrayListPath.size();
        int[] path = new int[len];
        ArrayList<Integer> tmp= (ArrayList<Integer>) arrayListPath.clone();
        for (int i = 0; i < len; ++i){
            path[i] = arrayListPath.get(i);
        }

        if(checkBound(tmp)){
            PathMap.add(tmp);
        }
//
//        if(tmp.size()!=1) {
//
//            PathMap.add(tmp);
////            System.out.println("The depth is " + len);
////            for (int i = 0; i < len - 1; ++i) {
////                System.out.print(path[i] + "->");
////            }
////            System.out.println(path[len - 1]);
//        }
        if (len == maxPathSize+(maxPathSize-1)*(automata_num-1)) return;
//        if (len == maxPathSize)  return;

        ArrayList<Integer> neibours = product.locations.get(path[len - 1]).getNeibours();
        for (int nextPos : neibours) {
            arrayListPath.add(nextPos);
            DFS1(arrayListPath, maxPathSize);
            arrayListPath.remove(arrayListPath.size() - 1);
        }
    }

    private boolean checkBound(ArrayList<Integer> path) {
        String[] last=new String[automata_num];
        int[] len=new int[automata_num];
        for(int j=0;j<automata_num;j++){
            last[j]="";
            len[j]=0;
        }

        for(int i=0;i<path.size();i++){
            String curLoc=product.locations.get(path.get(i)).name;
            String[] tmp=curLoc.split(",");
            for(int j=0;j<automata_num;j++){
                if(!tmp[j].equals(last[j])){
                    len[j]++;
                    last[j]=tmp[j];
                }
                if(len[j]>cbound) return false;
            }
        }

        return true;
    }

    public void print(String str) {
        try {
            bufferedWriter.write(str);
        } catch (IOException e) {
            System.out.println("write to file error!");
        }
    }
    public void println(String str) {
        try {
            bufferedWriter.write(str + "\n");
        } catch (IOException e) {
            System.out.println("write to file error!");
        }
    }
    double[] runRacos(Combination combination, int maxPathsize) {
        int samplesize = 20;       // parameter: the number of samples in each iteration
        int iteration = 1000;       // parameter: the number of iterations for batch racos
        int budget = 2000;         // parameter: the budget of sampling for sequential racos
        int positivenum = 2;       // parameter: the number of positive instances in each iteration
        double probability = 0.75; // parameter: the probability of sampling from the model
        int uncertainbit = 4;      // parameter: the number of sampled dimensions
        Instance ins = null;
        int repeat = 1;
        Task t = new ObjectFunction(combination,maxPathsize,timeAnalyst);
        double[] info = new double[3];
        for (int i = 0; i < repeat; i++) {
            double currentT = System.currentTimeMillis();
            Continue con = new Continue(t, combination);
            con.setMaxIteration(iteration);
            con.setSampleSize(samplesize);      // parameter: the number of samples in each iteration
            con.setBudget(budget);              // parameter: the budget of sampling
            con.setPositiveNum(positivenum);    // parameter: the number of positive instances in each iteration
            con.setRandProbability(probability);// parameter: the probability of sampling from the model
            con.setUncertainBits(uncertainbit); // parameter: the number of samplable dimensions
            con.setBound(maxPathsize);
            con.setPathMap(PathMap);
            ValueArc valueArc = con.run();                          // call sequential Racos              // call Racos
//            ValueArc valueArc = con.RRT();                          // call sequential Racos              // call Racos
//            ValueArc valueArc = con.monte();                          // call sequential Racos              // call Racos
//            ValueArc valueArc = con.run2();
            System.out.println("one component path choices: "+PathMap.size());


            double currentT2 = System.currentTimeMillis();
            ins = con.getOptimal();             // obtain optimal
//
//            if (minValueArc == null || minValueArc.value >= valueArc.value) {
//                minValueArc = valueArc;
//                int choice1=combin.get((int)ins.getFeature(0)).get(0);
//                int choice2=combin.get((int)ins.getFeature(0)).get(1);
//                ArrayList<Integer> path_tmp1 = PathMap.get(choice1);
//                ArrayList<Integer> path_tmp2 = PathMap.get(choice2);
//                minValueArc.path1=path_tmp1;
//                minValueArc.path2=path_tmp2;
//                System.out.print("best path1:");
//                for(int k=0;k<path_tmp1.size();k++){
//                    System.out.print(path_tmp1.get(k));
//                }
//                System.out.println(" ");
//                System.out.print("best path2:");
//                for(int k=0;k<path_tmp2.size();k++){
//                    System.out.print(path_tmp2.get(k));
//                }
//                System.out.println(" ");
//            }
//            info=new double[]{ins.getValue(),valueArc.cover,0,
//                    valueArc.time_cost[0],valueArc.time_cost[1],valueArc.time_cost[2],valueArc.time_cost[3],
//                    valueArc.time_cost[4],valueArc.time_cost[5],valueArc.time_cost[6],
//                    //valueArc.time_cost[7],valueArc.time_cost[8],
////                    valueArc.accuracy[0],valueArc.accuracy[1],valueArc.accuracy[2],valueArc.accuracy[3]
//                    };
            System.out.print("best function value:");
            System.out.println(ins.getValue() + "     ");
            info[0]=ins.getValue();


//            print("best function value:");
//            print(ins.getValue() + "     ");

            System.out.print("[");

            for (int j = 0; j < ins.getFeature().length; ++j) {
                System.out.print(Double.toString(ins.getFeature(j)) + ",");
            }
            System.out.println("]");

            info[1]=valueArc.iterativeNums;
        }
        return info;
    }

    private Automata CartesianProduct(Automata A, Automata B){
        Automata proAutomaton=new Automata(A.name+","+B.name);
        proAutomaton.rangeParameters.addAll(A.rangeParameters);
        proAutomaton.rangeParameters.addAll(B.rangeParameters);
        proAutomaton.initParameterValues.putAll(A.initParameterValues);
        proAutomaton.initParameterValues.putAll(B.initParameterValues);
        proAutomaton.parameters.addAll(A.parameters);
        proAutomaton.parameters.addAll(B.parameters);

        Map<String,Location> ID_Map=new HashMap<>();
        Map<Location,Location> decode_a=new HashMap<>();
        Map<Location,Location> decode_b=new HashMap<>();
        Stack<Location> to_search=new Stack<>();
        Set<Location> visited=new HashSet<>();

        int loc_id=0;
        Location iniLoc=productLoc(A.locations.get(A.initLoc),B.locations.get(B.initLoc),loc_id++,ID_Map,decode_a,decode_b,proAutomaton);
        proAutomaton.initLoc=iniLoc.getNo();
        proAutomaton.initLocName=iniLoc.name;

        to_search.push(iniLoc);


        while (!to_search.empty()){
            Location loc=to_search.pop();
            if(visited.contains(loc)) continue;

            visited.add(loc);

            Location locA=decode_a.get(loc);
            Location locB=decode_b.get(loc);

            if(locA.name.equals("loc5,out,recover,out,out")&&locB.name.equals("in")){
                System.out.println("here!");
            }

            ArrayList<Transition> next_trans_a=new ArrayList<>();
            ArrayList<Transition> next_trans_b=new ArrayList<>();
            Map<String,ArrayList<Transition>> next_sharedlabels=new HashMap<>();
            ArrayList<Transition> sharedTr_a=new ArrayList<>();
            ArrayList<Transition> sharedTr_b=new ArrayList<>();

            for(Transition tr:locA.next_trans){
                if(checkShareLabel(A,B,tr)){
                    ArrayList<Transition> tmp;
                    if(next_sharedlabels.containsKey(tr.label)){
                        tmp=next_sharedlabels.get(tr.label);
                    }else {
                        tmp = new ArrayList<>();
                        tmp.add(tr);
                        next_sharedlabels.put(tr.label, tmp);
                    }
                }else{
                    next_trans_a.add(tr);
                }
//                if(A.labels.contains(tr.label)&&B.labels.contains(tr.label)){
//                    ArrayList<Transition> tmp;
//                    if(next_sharedlabels.containsKey(tr.label)){
//                        tmp=next_sharedlabels.get(tr.label);
//                    }else {
//                        tmp = new ArrayList<>();
//                        tmp.add(tr);
//                        next_sharedlabels.put(tr.label, tmp);
//                    }
//                }
            }

            for(Transition tr:locB.next_trans){
                if(checkShareLabel(A,B,tr)){
//                if(A.labels.contains(tr.label)&&B.labels.contains(tr.label)){
                    if(next_sharedlabels.containsKey(tr.label)){
                        for(Transition tr_a:next_sharedlabels.get(tr.label)){
                            sharedTr_a.add(tr_a);
                            sharedTr_b.add(tr);
                        }
                    }
                }else{
                    next_trans_b.add(tr);
                }
            }

            for(Transition tr_a:next_trans_a){
                Location newloc=productLoc(A.locations.get(tr_a.target),locB,loc_id++,ID_Map,decode_a,decode_b,proAutomaton);
                Transition transition=new Transition(tr_a.label,loc.getNo(),newloc.getNo());
                copyTransition(tr_a,transition);
                proAutomaton.transitions.add(transition);
                loc.addNeibour(newloc.getNo());
                loc.next_trans.add(transition);
                to_search.push(newloc);
                if(!proAutomaton.labels.contains(transition.label))
                    proAutomaton.labels.add(transition.label);

                for(Transition tr_b:next_trans_b){
                    newloc=productLoc(A.locations.get(tr_a.target),B.locations.get(tr_b.target),loc_id++,ID_Map,decode_a,decode_b,proAutomaton);
                    transition=new Transition(tr_a.label+tr_b.label,loc.getNo(),newloc.getNo());
                    copyTransition(tr_b,transition);
                    copyTransition(tr_a,transition);
                    proAutomaton.transitions.add(transition);
                    loc.addNeibour(newloc.getNo());
                    loc.next_trans.add(transition);
                    to_search.push(newloc);
                    if(!proAutomaton.labels.contains(transition.label))
                        proAutomaton.labels.add(transition.label);
                }
            }

            for(Transition tr_b:next_trans_b){
                Location newloc=productLoc(locA,B.locations.get(tr_b.target),loc_id++,ID_Map,decode_a,decode_b,proAutomaton);
                Transition transition=new Transition(tr_b.label,loc.getNo(),newloc.getNo());
                copyTransition(tr_b,transition);
                proAutomaton.transitions.add(transition);
                loc.addNeibour(newloc.getNo());
                loc.next_trans.add(transition);
                to_search.push(newloc);
                if(!proAutomaton.labels.contains(transition.label))
                    proAutomaton.labels.add(transition.label);
            }

            for(int i=0;i<sharedTr_a.size();i++){
                Transition tr_a=sharedTr_a.get(i);
                Transition tr_b=sharedTr_b.get(i);

                Location newloc=productLoc(A.locations.get(tr_a.target),B.locations.get(tr_b.target),loc_id++,ID_Map,decode_a,decode_b,proAutomaton);
                Transition transition=new Transition(tr_b.label,loc.getNo(),newloc.getNo());
                copyTransition(tr_a,transition);
                copyTransition(tr_b,transition);
                proAutomaton.transitions.add(transition);
                loc.addNeibour(newloc.getNo());
                loc.next_trans.add(transition);
                to_search.push(newloc);
                if(!proAutomaton.labels.contains(transition.label))
                    proAutomaton.labels.add(transition.label);
            }

        }

        return proAutomaton;
    }

    private boolean checkShareLabel(Automata A, Automata B, Transition tr) {
        if(A.labels.contains(tr.label)&&B.labels.contains(tr.label)) return true;
        for(String strA:A.labels){
            if(tr.label.contains(strA)){
                for(String strB:B.labels){
                    if(tr.label.contains(strB)){
                        return true;
                    }
                }
            }
        }
        return false;
//        if(tr.label.contains(",")){
//
//        }else return A.labels.contains(tr.label)&&B.labels.contains(tr.label);
//        return true;
    }

    private void copyTransition(Transition src, Transition dst) {
        dst.assignments.putAll(src.assignments);
        dst.guards.addAll(src.guards);
    }

    private Location productLoc(Location locA, Location locB, int id, Map<String, Location> id_map,Map<Location,Location> decode_a,Map<Location,Location> decode_b,Automata proAutomaton) {
        String name=locA.name+","+locB.name;
        if(id_map.containsKey(name)) return id_map.get(name);

        Location location=new Location(id,name);

        location.invariants.addAll(locA.invariants);
        location.invariants.addAll(locB.invariants);

        location.flows.putAll(locA.flows);
        location.flows.putAll(locB.flows);

        id_map.put(name,location);
        decode_a.put(location,locA);
        decode_b.put(location,locB);

        proAutomaton.locations.put(id,location);
        return location;
    }

    private void productAll(){
        int num=ca.automataList.size();
        product=ca.automataList.get(0);
        product.obj_function= ca.objFun;
        if(num<=1) return;

        for(int i=1;i<num;i++){
            product=CartesianProduct(product,ca.automataList.get(i));
        }
        product.obj_function= ca.objFun;


    }



    public static void main(String[] args) {
        configUtil config = new configUtil();
        String prefix = config.get("system") + "_" + config.get("number");
        String modelFile = prefix + ".xml";
        String cfgFile = prefix + ".cfg";



        double value;
        double choices,total_choices;
        try {
            int bound = Integer.parseInt(config.get("bound"));
            File result = new File("result_" + prefix + "_bound_" + bound + ".txt");
            BufferedWriter buffer = new BufferedWriter(new FileWriter(result));

            int round = Integer.parseInt(config.get("round"));

            while(round>0){
                Combination combination=new Combination(modelFile,cfgFile,bound);
                Runtime r=Runtime.getRuntime();
                r.gc();
                combination.feasibla_target=Integer.parseInt(config.get("feasible_target"));
                long startMem=r.totalMemory()-r.freeMemory();
                System.out.println("round:"+Integer.toString(round));
                double currentTime = System.currentTimeMillis();
                System.out.println("PathMap size is: "+combination.PathMap.size());
                double[] ans=combination.runRacos(combination,bound);
                double endTime = System.currentTimeMillis();
                double t=(endTime - currentTime) / 1000;
                System.out.println("time cost: " + t);
                System.out.println("ODE time: "+ combination.timeAnalyst.getODETime());
                System.out.println("forbidden time: "+ combination.timeAnalyst.getForbiddenTime());
                System.out.println("dfs time: "+ combination.timeAnalyst.getDfsTime());

                int s=combination.timeAnalyst.getCnt();
                System.out.println("sample size: "+s);
                System.out.println("average ODE time: "+combination.timeAnalyst.getODETime()/s);
                System.out.println("average flow time: "+combination.timeAnalyst.getFlowTime()/s);
                System.out.println("average forbidden time: "+combination.timeAnalyst.getForbiddenTime()/s);

                long endMen=r.totalMemory()-r.freeMemory();

                String tmp=Double.toString(ans[0])
                        +" "+Double.toString(t)
                        +" "+Double.toString(combination.timeAnalyst.getDfsTime())
                        +" "+Double.toString(combination.timeAnalyst.getODETime())
                        +" "+Long.toString((endMen-startMem)/1024/1024)
                        + " " +Double.toString(ans[1])
//                        +" "+Double.toString(combination.timeAnalyst)
                        +"\n";

                try {
                    buffer.write(tmp);
                } catch (IOException e) {
                    System.out.println("write to file error!");
                }
                round--;
            }
            buffer.close();
        }catch (IOException e) {
            System.out.println("Open result.txt fail!");
        }
    }
}



