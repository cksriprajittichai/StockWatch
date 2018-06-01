package c.chasesriprajittichai.stockwatch;

import com.robinhood.spark.SparkAdapter;

public class SparkViewAdapter extends SparkAdapter {

    private float[] yData = {68, 22, 31, 57, 35, 79, 86, 47, 34, 55, 80, 72, 99, 66, 47, 42, 56, 64, 66, 80, 97, 10, 43, 12, 25, 71, 47, 73, 49, 36};


//    public SparkViewAdapter(float[] yData) {
//        this.yData = yData;
//    }


    @Override
    public int getCount() {
        return yData.length;
    }


    @Override
    public Object getItem(int index) {
        return yData[index];
    }


    @Override
    public float getY(int index) {
        return yData[index];
    }
}
