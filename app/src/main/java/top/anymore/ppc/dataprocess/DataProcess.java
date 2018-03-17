package top.anymore.ppc.dataprocess;

import org.opencv.core.Mat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import top.anymore.ppc.logutil.LogUtil;

/**
 * Created by anymore on 17-7-22.
 */

public class DataProcess {
    private int count;
    private boolean[][] matrix;
    private int rows,cols;
    private List<Integer> pixels;
//    private int max;
    //test
    private BufferedWriter bw;
    public void setMatrix(Mat mat){

        rows = mat.rows();
        cols = mat.cols();
        matrix = new boolean[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double[] p = mat.get(i,j);
                if (p[0] != 255.0){
                    matrix[i][j] = true;
                }
            }
        }
        pixels = new ArrayList<>();
        //test
        File log = new File("/storage/emulated/0/Android/data/top.anymore.ppc/cache/log.txt");
        try {
            bw = new BufferedWriter(new FileWriter(log));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public int solve(boolean mode){
//        int result = 0;
        LogUtil.v("tag","rows:"+rows+"cols"+cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (matrix[i][j] == true){
                    count = 1;
                    matrix[i][j] = false;
                    dfs(i-1,j-1);
                    dfs(i-1,j);
                    dfs(i-1,j+1);
                    dfs(i,j-1);
                    dfs(i,j+1);
                    dfs(i+1,j-1);
                    dfs(i+1,j);
                    dfs(i+1,j+1);
//                    result++;
                    try {
                        bw.write("["+count+"]");
                        bw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //up test
                    if (count > 10) {
                        Integer pixel = new Integer(count);
                        pixels.add(pixel);
                    LogUtil.v("count:",count+"");
                    }
                }
            }
        }
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mode){
            dataAnalysis2();
        }else {
            dataAnalysis();
        }
        return pixels.size();
//        return result;
    }

    private void dataAnalysis2() {
        LogUtil.v("DataProcess","处理前："+pixels.size());
        Collections.sort(pixels);
        int middleIndex = pixels.size()/2;
        int middlePixels = pixels.get(middleIndex);
        int min = (int) (middlePixels*0.5),max = middlePixels*5;
        for (int i = 0; i < pixels.size(); i++) {
            Integer pixel = pixels.get(i);
            if (pixel < CacheValue.threshold){
                pixels.remove(i);
                --i;
            }
        }
        LogUtil.v("DataProcess","处理后："+pixels.size());
    }

    private void dataAnalysis() {
//        int min = (int) (max*0.1);
        LogUtil.v("DataProcess","处理前："+pixels.size());
        Collections.sort(pixels);
        int middleIndex = pixels.size()/2;
        int middlePixels = pixels.get(middleIndex);
        int min = (int) (middlePixels*0.5),max = middlePixels*5;
        for (int i = 0; i < pixels.size(); i++) {
            Integer pixel = pixels.get(i);
            if (pixel < 10 || pixel > max){
                pixels.remove(i);
                --i;
            }
        }
        LogUtil.v("DataProcess","处理后："+pixels.size());
    }

    private void dfs(int i, int j) {
        if (i < 0 || i >= rows || j < 0 || j >= cols){
            return;
        }
        if (matrix[i][j] == true){
            count++;
            matrix[i][j] = false;
            dfs(i-1,j-1);
            dfs(i-1,j);
            dfs(i-1,j+1);
            dfs(i,j-1);
            dfs(i,j+1);
            dfs(i+1,j-1);
            dfs(i+1,j);
            dfs(i+1,j+1);
        }
    }
}
