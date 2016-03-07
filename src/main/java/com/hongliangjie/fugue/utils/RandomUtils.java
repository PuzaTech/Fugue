package com.hongliangjie.fugue.utils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by liangjie on 3/3/16.
 */
public class RandomUtils {

    protected abstract class RandomGNR{
        public abstract double nextDouble();
        public abstract int nextInt(int upper);
    }

    protected class NativeRandom extends RandomGNR{
        @Override
        public double nextDouble() {
            return ThreadLocalRandom.current().nextDouble();
        }

        @Override
        public int nextInt(int upper) { return ThreadLocalRandom.current().nextInt(0, upper); }
    }

    protected class DeterministicRandom extends RandomGNR{
        private Random r;

        public DeterministicRandom(){
            r = new Random(22);
        }

        @Override
        public double nextDouble(){
            return r.nextDouble();
        }

        @Override
        public int nextInt(int upper) { return r.nextInt(upper); }

    }

    RandomGNR r;

    public RandomUtils(){
        this(0);
    }

    public RandomUtils(int deterministic){
        if(deterministic == 0){
            r = new NativeRandom();
        }
        else{
            r = new DeterministicRandom();
        }

    }

    public double nextDouble(){
       return r.nextDouble();
   }
    public int nextInt(int upper) {return r.nextInt(upper);}



}
