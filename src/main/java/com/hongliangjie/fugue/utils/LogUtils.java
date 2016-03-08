package com.hongliangjie.fugue.utils;

/**
 * Created by hongliangjie on 3/4/2016.
 */
public class LogUtils {

    private MathLog mathLog;
    private MathExp mathExp;

    public LogUtils(MathLog m, MathExp e){
        mathLog = m;
        mathExp = e;
    }

    public double logSumTwo(double x1, double x2) {
        if (x1 > x2) {
            return x1 + mathLog.compute(1 + mathExp.compute(x2 - x1));
        } else {
            return x2 + mathLog.compute(1 + mathExp.compute(x1 - x2));
        }
    }

    public double logSumAll(double[] x) {
        double maximum = 0;
        for (int i = 0; i < x.length; i++) {
            if (i == 0) {
                maximum = x[i];
            } else {
                if (x[i] > maximum)
                    maximum = x[i];
            }
        }

        double result = 0.0;

        for (int i = 0; i < x.length; i++) {
            result += mathExp.compute(x[i] - maximum);
        }
        return maximum + mathLog.compute(result);
    }


}

