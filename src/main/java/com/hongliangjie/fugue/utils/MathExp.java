package com.hongliangjie.fugue.utils;


import net.jafama.FastMath;

/**
 * Created by liangjie on 3/7/16.
 */

public class MathExp {
    private Exp mathExp;
    protected abstract class Exp{
        public abstract double compute(double x);
    }

    protected class NativeExp extends Exp{
        @Override
        public double compute(double x){
            return Math.exp(x);
        }
    }

    protected class FastExp extends Exp{
        @Override
        public double compute(double x){
            return StrictMath.exp(x);
        }
    }

    protected class QuickExp extends Exp{
        @Override
        public double compute(double x){
            return FastMath.expQuick(x);
        }
    }

    public MathExp(int type){
        if (type == 0){
            mathExp = new NativeExp();
        }
        else if (type == 1){
            mathExp = new FastExp();
        }
        else{
            mathExp = new QuickExp();
        }
    }

    public double compute(double x){
        return mathExp.compute(x);
    }

}
