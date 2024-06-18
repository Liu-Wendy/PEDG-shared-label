package MPC.tools;

import java.util.HashMap;
import java.util.Map;

public class BindInfo {
    public String component;
    public String name;
    public Map<String, String> map;

    public BindInfo(String comp,String name){
        this.component=comp;
        this.name=name;
        map=new HashMap<>();
    }

}
