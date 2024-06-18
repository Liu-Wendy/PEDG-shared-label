package Racos.Time;

public class TimeAnalyst {
    public double ODETime;
    public double forbiddenTime;
    public double productTime;
    public double parseTime;
    public double flowTime;
    public double dfsTime;
    public int cnt;

    public TimeAnalyst() {
        ODETime = 0;
        forbiddenTime = 0;
        productTime = 0;
        parseTime = 0;
        flowTime = 0;
        cnt = 0;
        dfsTime = 0;
    }

    public double getFlowTime() {
        return flowTime;
    }

    public int getCnt() {
        return cnt;
    }

    public double getForbiddenTime() {
        return forbiddenTime;
    }

    public double getODETime() {
        return ODETime;
    }

    public double getParseTime() {
        return parseTime;
    }

    public double getProductTime() {
        return productTime;
    }

    public double getDfsTime(){return dfsTime;}

    public void addParseTime(double t) {
        parseTime += t;
    }

    public void addODETime(double t) {
        ODETime += t;
        cnt++;
    }

    public void addForbiddenTime(double t) {
        forbiddenTime += t;
    }

    public void addProductTime(double t) {
        productTime += t;
    }

    public void addFlowTime(double t) {
        flowTime += t;
    }

    public void addDFSTime(double t) {
        dfsTime += t;
    }
}
