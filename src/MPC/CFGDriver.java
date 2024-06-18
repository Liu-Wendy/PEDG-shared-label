package MPC;
import java.io.IOException;

import java.io.InputStream;

import java.util.Properties;

public class CFGDriver {
    Properties pro ;
    InputStream is;

    public CFGDriver(String cfgFile){
        pro = new Properties();
        is = null;
        try {
            is = CFGDriver.class.getClassLoader().getResourceAsStream(
                    "models/"+cfgFile);
            pro.load(is);
        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public String getCfgPropertiesValue(String key) {
        String value=pro.getProperty(key);
        if(value==null) return null;
        value=value.substring(1,value.length()-1);
        return value;
    }
}

