package Racos.ObjectiveFunction;

import MPC.*;
import Racos.Componet.Dimension;
import Racos.Componet.Instance;
import Racos.Time.TimeAnalyst;
import Racos.Tools.ValueArc;
import com.greenpineyu.fel.*;
import com.greenpineyu.fel.context.FelContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class ObjectFunction implements Task {
    private Dimension dim;
    private Combination combination;
    private Automata product;
    private int automata_num;
    public TimeAnalyst timeAnalyst;
    //    private int []path1;
//    private int []path2;
    private FelEngine fel;
    private FelContext ctx;
    private ArrayList<ArrayList<HashMap<String, Double>>> allParametersValuesList;
    public double delta;
    private double penalty = 0;
    private double globalPenalty = 0;
    private boolean sat = true;
    private double cerr = 0.01;
    public ArrayList<ArrayList<Boolean>> infeasiblelist;
    public ArrayList<ArrayList<Integer>> path;
    int rangeParaSize;
    int maxpathSize;
    int locsize;
    ArrayList<ArrayList<Integer>> PathMap;
//    ArrayList<Boolean> infeasible_loc1;
//    ArrayList<Boolean> infeasible_loc2;

    public ValueArc valueArc;

    double[] time = new double[4];

    public ObjectFunction(Combination combination, int maxPathSize, TimeAnalyst timeAnalyst) {

        this.combination = combination;
        product = combination.product;
        automata_num = 1;
        dim = new Dimension();
        delta = combination.delta;
        maxpathSize = maxPathSize;
        this.PathMap = combination.PathMap;
        this.timeAnalyst = timeAnalyst;
//        this.combinPath=combination.combin;

        rangeParaSize = (product.rangeParameters == null) ? 0 : product.rangeParameters.size();
        dim.setSize(1 + rangeParaSize + (combination.ca.staytime4loc ? maxpathSize : 0));

        dim.setDimension(0, 0, PathMap.size() - 1, false);
        for (int i = 0; i < rangeParaSize; ++i)
            dim.setDimension(i + 1, product.rangeParameters.get(i).lowerBound, product.rangeParameters.get(i).upperBound, true);
        if (combination.ca.staytime4loc) {
            for (int i = 0; i < maxPathSize; ++i) {
                if(combination.ca.addInit&&i==0){
                    dim.setDimension(i + rangeParaSize + 1, 1, 1, false);
                    continue;
                }
                dim.setDimension(i + rangeParaSize + 1, 1, 300, false);
            }
        }
        fel = new FelEngineImpl();
        ctx = fel.getContext();
        valueArc = new ValueArc();
    }


    public int getPathLength() {
        return this.maxpathSize;
    }

    public boolean checkConstraints(String forbidden, HashMap<String, Double> parametersValues) {
        for (Map.Entry<String, Double> entry : parametersValues.entrySet()) {
            ctx.set(entry.getKey(), entry.getValue());
        }
        if (forbidden == null)
            return true;
        boolean result = (boolean) fel.eval(forbidden);
        if (!result) return true;
        sat = false;
        globalPenalty += computeConstraintValue(forbidden.trim());
        return false;
    }

    public boolean checkConstraints(Automata automata, HashMap<String, Double> parametersValues) {
        for (Map.Entry<String, Double> entry : parametersValues.entrySet()) {
            ctx.set(entry.getKey(), entry.getValue());
        }
        if (automata.forbiddenConstraints == null)
            return true;
        boolean result = (boolean) fel.eval(automata.forbiddenConstraints);
        if (!result) return true;
        sat = false;
        globalPenalty += computeConstraintValue(automata.forbiddenConstraints.trim());
        return false;
    }

    public double computeConstraintValue(String constraint) {
        int firstRightBracket = constraint.trim().indexOf(")");
        if (firstRightBracket != -1 && constraint.indexOf('&') == -1 && constraint.indexOf('|') == -1)
            return computePenalty(constraint.substring(constraint.indexOf('(') + 1, constraint.lastIndexOf(")")), false);
        if (firstRightBracket != -1 && firstRightBracket != constraint.length() - 1) {
            for (int i = firstRightBracket; i < constraint.length(); ++i) {
                if (constraint.charAt(i) == '&') {
                    int index = 0;
                    int numOfBrackets = 0;
                    int partBegin = 0;
                    double pen = 0;
                    while (index < constraint.length()) {
                        if (constraint.charAt(index) == '(')
                            ++numOfBrackets;
                        else if (constraint.charAt(index) == ')')
                            --numOfBrackets;
                        else if (constraint.charAt(index) == '&' && numOfBrackets == 0) {
                            String temp = constraint.substring(partBegin, index);
                            boolean result = (boolean) fel.eval(temp);
                            if (!result) return 0;
                            else pen += computeConstraintValue(temp);
                            index = index + 2;
                            partBegin = index;
                            constraint = constraint.substring(index);
                            continue;
                        }
                        ++index;
                    }
                    return pen;
                } else if (constraint.charAt(i) == '|') {
                    int index = 0;
                    int numOfBrackets = 0;
                    int partBegin = 0;
                    double minPen = Double.MAX_VALUE;
                    while (index < constraint.length()) {
                        if (constraint.charAt(index) == '(')
                            ++numOfBrackets;
                        else if (constraint.charAt(index) == ')')
                            --numOfBrackets;
                        else if (constraint.charAt(index) == '|' && numOfBrackets == 0) {
                            String temp = constraint.substring(partBegin, index);
                            boolean result = (boolean) fel.eval(temp);
                            if (result) {
                                temp = temp.trim();
                                minPen = (computeConstraintValue(temp) < minPen) ? computeConstraintValue(temp) : minPen;
                            }
                            index = index + 3;
                            partBegin = index;

                            continue;
                        }
                        ++index;
                    }
                    return minPen;
                }
            }
        } else {
            if (firstRightBracket != -1) {
                constraint = constraint.substring(constraint.indexOf('(') + 1, firstRightBracket);
            }
            if (constraint.indexOf('&') != -1) {
                String[] strings = constraint.split("&");
                double pen = 0;
                for (int i = 0; i < strings.length; ++i) {
                    if (strings[i].equals("")) continue;
                    boolean result = (boolean) fel.eval(strings[i]);
                    if (!result) return 0;
                    else pen += computeConstraintValue(strings[i]);
                }
                return pen;
            } else if (constraint.indexOf('|') != -1) {
                String[] strings = constraint.split("\\|");
                double minPen = Double.MAX_VALUE;
                for (int i = 0; i < strings.length; ++i) {
                    if (strings[i].equals("")) continue;
                    boolean result = (boolean) fel.eval(strings[i]);
                    if (!result) continue;
                    else
                        minPen = (computeConstraintValue(strings[i]) < minPen) ? computeConstraintValue(strings[i]) : minPen;
                }
                return minPen;
            } else return computePenalty(constraint, false);
        }
        return 0;
    }

    public HashMap<String, Double> computeValuesByFlow(HashMap<String, Double> parametersValues, Location location, double arg) {
        HashMap<String, Double> tempMap = new HashMap<>();
        for (HashMap.Entry<String, Double> entry : parametersValues.entrySet()) {
            ctx.set(entry.getKey(), entry.getValue());
        }
        for (HashMap.Entry<String, Double> entry : parametersValues.entrySet()) {
            if (location.flows.containsKey(entry.getKey())) {
                String expression = location.flows.get(entry.getKey());
                expression = expression.replaceAll("phi\\(([^)]+)\\)", "($1 <= 2 ? 1 : $1 <= 5 ? 2 : 3)");

//                if (expression.contains("phi(drone_vx)")) {
//                    if (parametersValues.get("drone_vx") <= 2)
//                        expression = expression.replace("phi(drone_vx)", "1");
//                    else if (parametersValues.get("drone_vx") <= 5)
//                        expression = expression.replace("phi(drone_vx)", "2");
//                    else
//                        expression = expression.replace("phi(drone_vx)", "3");
//                }
//                if (expression.contains("phi(drone_vy)")) {
//                    if (parametersValues.get("drone_vy") <= 2)
//                        expression = expression.replace("phi(drone_vy)", "1");
//                    else if (parametersValues.get("drone_vy") <= 5)
//                        expression = expression.replace("phi(drone_vy)", "2");
//                    else
//                        expression = expression.replace("phi(drone_vy)", "3");
//                }
                double currentTime = System.currentTimeMillis();
                Object obj = fel.eval(expression);
                double endTime = System.currentTimeMillis();
                double temptime = (endTime - currentTime) / 1000;
                time[3] += temptime;
                double result;
                if (obj instanceof Double)
                    result = (double) obj;
                else if (obj instanceof Integer) {
                    result = (int) obj;
                } else if (obj instanceof Long) {
                    result = ((Long) obj).doubleValue();
                } else {
                    result = 0;
                    System.out.println("Not Double and Not Integer!");
                    System.out.println(obj.getClass().getName());
                    System.out.println(obj);
                    System.out.println(location.flows.get(entry.getKey()));
                    System.exit(0);
                }
                double delta = result * arg;
                tempMap.put(entry.getKey(), entry.getValue() + delta);
            } else {
                tempMap.put(entry.getKey(), entry.getValue());
            }
        }
        return tempMap;

    }

    public boolean checkGuards(Automata automata, int index) {
        ArrayList<Integer> path_tmp = path.get(index);

        int[] path = new int[path_tmp.size()];
        for (int i = 0; i < path_tmp.size(); i++) {
            path[i] = path_tmp.get(i);
        }

        ArrayList<HashMap<String, Double>> allParametersValues = allParametersValuesList.get(index);

        for (int i = 0; i < path.length; ++i) {
            Location location = automata.locations.get(path[i]);
            if (i >= allParametersValues.size()) {
                int sad = 1;
                sad++;
            }
            HashMap<String, Double> parameterValues = allParametersValues.get(i);
            int target;
            if (i + 1 < path.length) {
                target = path[i + 1];
                int source = path[i];
                for (int k = 0; k < automata.transitions.size(); ++k) {
                    Transition transition = automata.transitions.get(k);
                    if (transition.source == source && transition.target == target) {
                        for (Map.Entry<String, Double> entry : parameterValues.entrySet()) {
                            ctx.set(entry.getKey(), entry.getValue());
                        }
                        for (int guardIndex = 0; guardIndex < transition.guards.size(); ++guardIndex) {
                            boolean result = (boolean) fel.eval(transition.guards.get(guardIndex));
                            if (!result) {
                                String guard = transition.guards.get(guardIndex);
                                if (Double.isNaN(computePenalty(guard, false))) {
                                    sat = false;
                                    penalty += 100000;
                                } else if (computePenalty(guard, false) > cerr) {
                                    sat = false;
                                    penalty += computePenalty(guard, false);
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    public double checkCombine(ArrayList<HashMap<String, Double>> newMap) {
        double res = 0;
//        for(int i=0;i< newMap.size();i++){
//            for(int j=i+1;j< newMap.size();j++){
//                HashMap<String,Double> newMap1=newMap.get(i);
//                HashMap<String,Double> newMap2=newMap.get(j);
//                res=Math.pow(newMap1.get("x")-newMap2.get("x"),2)+Math.pow(newMap1.get("y")-newMap2.get("y"),2);
//                if(res<0.01) {
//                    sat=false;
//                    globalPenalty+=res;
//                }
//            }
//        }
        return res;
    }

    public boolean checkInvarientsByODE(ArrayList<ArrayList<Double>> argsList) {
        double step = 0;
        ArrayList<HashMap<String, Double>> allstepMap = new ArrayList<>();
        ArrayList<Integer> locIndexList = new ArrayList<>();
        double max_total = 0;
        for (int n = 0; n < automata_num; n++) {
            HashMap<String, Double> newMap = product.duplicateInitParametersValues();
            allstepMap.add(newMap);
            for (ArrayList<Double> doubles : argsList) {
                double temp = 0;
                for (int j = 0; j < doubles.size(); j++) {
                    temp += doubles.get(j);
                }
                max_total = Math.max(max_total, temp);
            }
            double sum = 0;
            for (int i = 0; i < argsList.get(n).size(); i++) {
                sum += argsList.get(n).get(i);
                argsList.get(n).set(i, sum);
            }
            locIndexList.add(0);
            combination.automata = new ArrayList<>();
            combination.automata.add(product);
        }
        while (step < max_total) {
            for (int n = 0; n < automata_num; n++) {
                int locIndex = locIndexList.get(n);
                HashMap<String, Double> newMap = allstepMap.get(n);
                Automata automata = combination.automata.get(n);
                ArrayList<Double> args = argsList.get(n);
                if (step < args.get(locIndex)) {
                    double before = System.currentTimeMillis();
                    newMap = computeValuesByFlow(newMap, automata.locations.get(path.get(n).get(locIndex)), delta);
                    double after = System.currentTimeMillis();
                    timeAnalyst.addFlowTime((after - before) / 1000);
//                    newMap = computeValuesByFlow(newMap, automata.locations.get(path.get(n).get(locIndex)), delta);

                    before = System.currentTimeMillis();
                    checkConstraints(combination.ca.forbidden, newMap);
                    after = System.currentTimeMillis();

                    timeAnalyst.addForbiddenTime((after - before) / 1000);


                    for (HashMap.Entry<String, Double> entry : newMap.entrySet()) {
                        ctx.set(entry.getKey(), entry.getValue());
                    }
                    //check invariants
                    for (int i = 0; i < automata.locations.get(path.get(n).get(locIndex)).invariants.size(); ++i) {
                        boolean result = (boolean) fel.eval(automata.locations.get(path.get(n).get(locIndex)).invariants.get((i)));
                        if (!result) {
                            String invariant = automata.locations.get(path.get(n).get(locIndex)).invariants.get(i);
                            if (computePenalty(invariant, false) < cerr)
                                continue;
                            if (Double.isNaN(computePenalty(invariant, false))) {
                                sat = false;
                                penalty += 100000;
                                infeasiblelist.get(n).set(locIndex, false);
                            } else {
                                sat = false;
                                //System.out.println(invariant);
                                penalty += computePenalty(invariant, false);
//                                infeasiblelist.get(n).set(locIndex, false);
                            }

                        }
                    }
                    if (step == args.get(locIndex) - 1) {
                        allParametersValuesList.get(n).add((HashMap<String, Double>) newMap.clone());
                        if (locIndex != path.get(n).size() - 1) {
                            locIndex++;

                            Transition transition = automata.getTransitionBySourceAndTarget(path.get(n).get(locIndex - 1), path.get(n).get(locIndex));
                            if (transition == null) {
                                System.out.println("Found no transition");
                                System.exit(-1);
                            }
                            for (HashMap.Entry<String, String> entry : transition.assignments.entrySet()) {
                                Object obj = fel.eval(entry.getValue());
                                double result = 0;
                                if (obj instanceof Integer) result = (int) obj;
                                else if (obj instanceof Double) result = (double) obj;
                                else {
                                    System.out.println("Not Double and Not Integer!");
                                    System.out.println(entry.getValue());
                                    System.exit(0);
                                }
                                newMap.put(entry.getKey(), result);
                            }
                        }
                    }
                    locIndexList.set(n, locIndex);
                    allstepMap.set(n, newMap);
                }
            }
            checkCombine(allstepMap);
            step++;
        }

        return true;
    }

    public boolean checkInvarientsByODEOnebyOne(ArrayList<ArrayList<Double>> argsList) {
        double step = 0;
        ArrayList<HashMap<String, Double>> allstepMap = new ArrayList<>();
        ArrayList<Integer> locIndexList = new ArrayList<>();
        double max_total = 0;
        for (int n = 0; n < automata_num; n++) {
            HashMap<String, Double> newMap = combination.ca.automataList.get(n).duplicateInitParametersValues();
            allstepMap.add(newMap);
            for (ArrayList<Double> doubles : argsList) {
                double temp = 0;
                for (int j = 0; j < doubles.size(); j++) {
                    temp = doubles.get(j);
                }
                max_total = Math.max(max_total, temp);
            }
//            double sum = 0;
//            for (int i = 0; i < argsList.get(n).size(); i++) {
//                sum += argsList.get(n).get(i);
//                argsList.get(n).set(i, sum);
//            }
            locIndexList.add(0);

        }
        while (step < max_total) {
            for (int n = 0; n < automata_num; n++) {
                int locIndex = locIndexList.get(n);
                HashMap<String, Double> newMap = allstepMap.get(n);
                Automata automata = combination.ca.automataList.get(n);
                ArrayList<Double> args = argsList.get(n);
                if (step < args.get(locIndex)) {
                    double before = System.currentTimeMillis();
                    newMap = computeValuesByFlow(newMap, automata.locations.get(path.get(n).get(locIndex)), delta);
                    double after = System.currentTimeMillis();
                    timeAnalyst.addFlowTime((after - before) / 1000);
//                    newMap = computeValuesByFlow(newMap, automata.locations.get(path.get(n).get(locIndex)), delta);

                    before = System.currentTimeMillis();
                    checkConstraints(combination.ca.forbidden, newMap);
                    after = System.currentTimeMillis();

                    timeAnalyst.addForbiddenTime((after - before) / 1000);


                    for (HashMap.Entry<String, Double> entry : newMap.entrySet()) {
                        ctx.set(entry.getKey(), entry.getValue());
                    }
                    //check invariants
                    for (int i = 0; i < automata.locations.get(path.get(n).get(locIndex)).invariants.size(); ++i) {
                        boolean result = (boolean) fel.eval(automata.locations.get(path.get(n).get(locIndex)).invariants.get((i)));
                        if (!result) {
                            String invariant = automata.locations.get(path.get(n).get(locIndex)).invariants.get(i);
                            if (computePenalty(invariant, false) < cerr)
                                continue;
                            if (Double.isNaN(computePenalty(invariant, false))) {
                                sat = false;
                                penalty += 100000;
                                infeasiblelist.get(n).set(locIndex, false);
                            } else {
                                sat = false;
                                //System.out.println(invariant);
                                penalty += computePenalty(invariant, false);
//                                infeasiblelist.get(n).set(locIndex, false);
                            }

                        }
                    }
                    if (step == args.get(locIndex) - 1) {
                        allParametersValuesList.get(n).add((HashMap<String, Double>) newMap.clone());
                        if (locIndex != path.get(n).size() - 1) {
                            locIndex++;

                            Transition transition = automata.getTransitionBySourceAndTarget(path.get(n).get(locIndex - 1), path.get(n).get(locIndex));
                            if (transition == null) {
                                System.out.println("Found no transition");
                                System.exit(-1);
                            }
                            for (HashMap.Entry<String, String> entry : transition.assignments.entrySet()) {
                                Object obj = fel.eval(entry.getValue());
                                double result = 0;
                                if (obj instanceof Integer) result = (int) obj;
                                else if (obj instanceof Double) result = (double) obj;
                                else {
                                    System.out.println("Not Double and Not Integer!");
                                    System.out.println(entry.getValue());
                                    System.exit(0);
                                }
                                newMap.put(entry.getKey(), result);
                            }
                        }
                    }
                    locIndexList.set(n, locIndex);
                    allstepMap.set(n, newMap);
                }
            }
            checkCombine(allstepMap);
            step++;
        }

        return true;
    }


    private double computePenaltyOfConstraint(String expression) {//just one level
        String[] expressions = expression.split("\\|");
        double result = Double.MAX_VALUE;
        for (String string : expressions) {
            if (string.length() <= 0) continue;
            double temp = computePenalty(string, false);
            result = (temp < result) ? temp : result;
        }
        return result;
    }

    private double computePenalty(String expression, boolean isConstraint) {
        if (isConstraint && expression.indexOf("|") != -1)
            return computePenaltyOfConstraint(expression);

        String[] strings;
        String bigPart = "", smallPart = "";
        strings = expression.split("<=|<|>=|>|==");
        Object obj1 = fel.eval(strings[0].trim());
        Object obj2 = fel.eval(strings[1].trim());
        double big = 0, small = 0;
        if (obj1 instanceof Double)
            big = (double) obj1;
        else if (obj1 instanceof Integer) {
            big = (int) obj1;
            //System.out.println(entry.getKey() + " " + entry.getValue());
        } else {
            System.out.println("Not Double and Not Integer!");
            System.out.println(expression);
            System.out.println(obj1);
            System.out.println(obj1.getClass().getName());
            System.out.println("here");
            System.exit(0);
        }
        if (obj2 instanceof Double)
            small = (double) obj2;
        else if (obj2 instanceof Integer) {
            small = (int) obj2;
        } else if (obj2 instanceof Long) {
            small = ((Long) obj2).doubleValue();
        } else {
            small = 0;
            System.out.println("Not Double and Not Integer!");
            System.exit(0);
        }
        return Math.abs(big - small);
    }


    @Override
//    public double getValue(Instance ins) {
//        penalty = 0;
//        globalPenalty = 0;
//        sat = true;
//        allParametersValuesList = new ArrayList<>();
//        infeasiblelist = new ArrayList<>();
//        ArrayList<Integer> pat = PathMap.get((int) ins.getFeature(0));
//        path = new ArrayList<>();
//        path.add(pat);
//
//        for (int i = 0; i < product.rangeParameters.size(); ++i) {
//            product.initParameterValues.put(product.rangeParameters.get(i).name, ins.getFeature(i + 1));
//        }
//        if (combination.ca.staytime4loc) {
//            ArrayList<ArrayList<Double>> args = new ArrayList<>();
//            ArrayList<Double> arg = new ArrayList<>();
//            for (int i = 0; i < pat.size(); ++i)
//                arg.add(ins.getFeature(i + 1 + rangeParaSize));
//            args.add(arg);
//            for (int n = 0; n < automata_num; n++) {
//                ArrayList<HashMap<String, Double>> allParametersValues = new ArrayList<>();
//                allParametersValuesList.add(allParametersValues);
//            }
//
//            double before = System.currentTimeMillis();
//            checkInvarientsByODE(args);
//            double after = System.currentTimeMillis();
//
//            timeAnalyst.addODETime((after - before) / 1000);
//
//        }
//        for (int n = 0; n < automata_num; n++)
//            checkGuards(combination.automata.get(n), n);
//        if (!sat) {
////            System.out.println("guard infesasible!");
//            if (penalty + globalPenalty == 0) {
//                //todo cfg file should have brackets
//                System.out.println("penalty = 0 when unsat");
//                return Double.MAX_VALUE;
//            }
//            double penAll = penalty + globalPenalty;
//            if (penAll < valueArc.penAll) {
//                valueArc.penalty = penalty;
//                valueArc.globalPenalty = globalPenalty;
//                valueArc.penAll = penAll;
//            }
////            System.out.println("path:"+ins.getFeature(0)+"  value:"+penAll);
//            return penAll*100;
//        }
//        double value = 0;
//        for (int n = 0; n < automata_num; n++) {
//            double t = computeValue(n);
//            value += t;
//        }
////        if(value1<0||value2<0){
////            System.out.println("YES");
////        }
////        if(value1*value2>0)
////            return value1+value2;
////        else return value1+value2+10000;
////        System.out.println("path:"+ins.getFeature(0)+"  value:"+value);
//        return value;
//    }

    public double getValue(Instance ins) {
//        double[] t1=new double[]{7,0.45562139167673843,-0.2185886359922441,0.16357142786912993,-0.02512133158748242,133.0,84.0,153.0};
//        ins.setFeature(t1);
        penalty = 0;
        globalPenalty = 0;
        sat = true;
        allParametersValuesList = new ArrayList<>();
        infeasiblelist = new ArrayList<>();
        ArrayList<Integer> pat = PathMap.get((int) ins.getFeature(0));
        path = new ArrayList<>();
        path.add(pat);

        for (int i = 0; i < product.rangeParameters.size(); ++i) {
            String param=product.rangeParameters.get(i).name;
            product.initParameterValues.put(param, ins.getFeature(i + 1));
            String autName=product.rangeParameters.get(i).name.trim().split("_")[0];
            Automata aut=combination.autMap.get(autName);
            aut.initParameterValues.put(param,ins.getFeature(i + 1));
        }
        if (combination.ca.staytime4loc) {
            ArrayList<ArrayList<Double>> args = new ArrayList<>();
            ArrayList<Double> arg = new ArrayList<>();
            for (int i = 0; i < pat.size(); ++i)
                arg.add(ins.getFeature(i + 1 + rangeParaSize));
            args.add(arg);
            for (int n = 0; n < automata_num; n++) {
                ArrayList<HashMap<String, Double>> allParametersValues = new ArrayList<>();
                allParametersValuesList.add(allParametersValues);
            }

            double before = System.currentTimeMillis();
            checkInvarientsByODE(args);
            double after = System.currentTimeMillis();

            timeAnalyst.addODETime((after - before) / 1000);

        }
        for (int n = 0; n < automata_num; n++)
            checkGuards(combination.automata.get(n), n);
        if (!sat) {
//            System.out.println("guard infesasible!");
            if (penalty + globalPenalty == 0) {
                //todo cfg file should have brackets
                System.out.println("penalty = 0 when unsat");
                return Double.MAX_VALUE;
            }
            double penAll = penalty + globalPenalty;
            if (penAll < valueArc.penAll) {
                valueArc.penalty = penalty;
                valueArc.globalPenalty = globalPenalty;
                valueArc.penAll = penAll;
            }
//            System.out.println("path:"+ins.getFeature(0)+"  value:"+penAll);
            return penAll*100;
        }
        double value = 0;
        for (int n = 0; n < automata_num; n++) {
            double t = computeValue(n);
            value += t;
        }
//        if(value1<0||value2<0){
//            System.out.println("YES");
//        }
//        if(value1*value2>0)
//            return value1+value2;
//        else return value1+value2+10000;
//        System.out.println("path:"+ins.getFeature(0)+"  value:"+value);
        return value;
    }


    public double computeValue(int index) {
        Automata automata = product;
        ArrayList<HashMap<String, Double>> allParametersValues = allParametersValuesList.get(index);
        if (allParametersValues.size() < 1) {
            sat = false;
            valueArc.penalty = 100;
            return valueArc.penalty;
        }
        HashMap<String, Double> map = allParametersValues.get(allParametersValues.size() - 1);
//        if(Math.pow(automata.target_x - map.get("x"), 2) >1){
//            sat=false;
//            valueArc.penalty=Math.pow(automata.target_x - map.get("x"), 2)/10000;
//            return valueArc.penalty;
//        }
        for (HashMap.Entry<String, Double> entry : map.entrySet()) {
            ctx.set(entry.getKey(), entry.getValue());
        }

        Object obj = fel.eval(combination.ca.feasible_fun);
        double tmp=10000;
        if (obj instanceof Double)
            tmp = (double) obj ;
        else if (obj instanceof Integer) {
            tmp = (int) obj ;
        }

//        if(tmp > combination.feasibla_target){
////            System.out.println("infeasible: "+tmp);
//            sat=false;
//            valueArc.penalty=tmp/100;
//            return valueArc.penalty;
//        }
        obj = fel.eval(automata.obj_function);
        double value = 0;
        if (obj instanceof Double)
            value = (double) obj - 10000;
        else if (obj instanceof Integer) {
            value = (int) obj - 10000;
        } else {
            System.err.println("error: result not of double!");
            System.out.println(obj);
            return Double.MAX_VALUE;
        }
        if (value + 10000 < 0) {
            System.err.println("error: objective function result is negtive!");
            return Double.MAX_VALUE;
        }
        if (value < valueArc.value) {
            valueArc.value = value;
            valueArc.allParametersValues = allParametersValues.get(allParametersValues.size() - 1);
        }
        return value;
    }

    @Override
    public Dimension getDim() {
        return dim;
    }

    public double[] getTime() {
        return time;
    }

    @Override
    public ArrayList<Boolean> getInfeasibleLoc(int index) {
        return infeasiblelist.get(index);
    }
}
