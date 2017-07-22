package top.anymore.ppc.dataprocess;

import org.opencv.core.Mat;

/**
 * Created by anymore on 17-7-22.
 */

public class DataProcess {
    private boolean[][] matrix;
    private int rows,cols;
    public void setMatrix(Mat mat){
        rows = mat.rows();
        cols = mat.cols();
        matrix = new boolean[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double[] p = mat.get(i,j);
                if (p[0] == 255){
                    matrix[i][j] = true;
                }
            }
        }
    }
    public int solve(){
        int result = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (matrix[i][j] == true){
                    matrix[i][j] = false;
                    dfs(i-1,j-1);
                    dfs(i-1,j);
                    dfs(i-1,j+1);
                    dfs(i,j-1);
                    dfs(i,j+1);
                    dfs(i+1,j-1);
                    dfs(i+1,j);
                    dfs(i+1,j+1);
                    result++;
                }
            }
        }
        return result;
    }

    private void dfs(int i, int j) {
        if (i < 0 || i >= rows || j < 0 || j >= cols){
            return;
        }
        if (matrix[i][j] == true){
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
