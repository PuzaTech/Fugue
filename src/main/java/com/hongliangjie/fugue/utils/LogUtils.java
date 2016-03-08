package com.hongliangjie.fugue.utils;

/**
 * Created by hongliangjie on 3/4/2016.
 */
public class LogUtils {
    public static double logSumTwo(double x1, double x2){

        if (x1 > x2){
            return x1 + Math.log(1 + Math.exp(x2 - x1));
        }
        else{
            return x2 + Math.log(1 + Math.exp(x1 - x2));
        }
    }

    public static double logSumAll(double[] x){
        double maximum = 0;
        for (int i = 0; i < x.length; i++){
            if (i == 0) {
                maximum = x[i];
            }
            else{
                if (x[i] > maximum)
                    maximum = x[i];
            }
        }

        double result = 0.0;

        for (int i =  0; i < x.length; i++){
            result += Math.exp(x[i] - maximum);
        }
        return maximum + Math.log(result);
    }
}
