package com.hongliangjie.fugue.utils;


import net.jafama.FastMath;
import net.jafama.StrictFastMath;

/**
 * Created by liangjie on 3/7/16.
 */
public class MathLog {
    private Log mathLog;
    protected abstract class Log{
        public abstract double compute(double x);
    }

    protected class NativeLog extends Log{
        @Override
        public double compute(double x){
            return Math.log(x);
        }
    }

    protected class FastLog extends Log{
        @Override
        public double compute(double x){
            return StrictFastMath.log(x);
        }
    }

    protected class QuickLog extends Log{
        @Override
        public double compute(double x){
            return FastMath.logQuick(x);
        }
    }

    public MathLog(){
        this(0);
    }

    public MathLog(int type){
        if (type == 0){
            mathLog = new NativeLog();
        }
        else if (type == 1){
            mathLog = new FastLog();
        }
        else{
            mathLog = new QuickLog();
        }
    }

    public double compute(double x){
        return mathLog.compute(x);
    }

}
