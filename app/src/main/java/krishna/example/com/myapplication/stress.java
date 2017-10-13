package krishna.example.com.myapplication;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class stress{
    private InputStream is;
    private ArrayList<Integer> list;
    private int mean;
    private Integer[] arr;

    public void readfile(InputStream res){
        list = new ArrayList<>();
        is = res;
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        try{
            String line;
            while((line = br.readLine())!=null){
                list.add(Integer.parseInt(line));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        this.arr = new Integer[list.size()];
        this.arr = list.toArray(arr);
    }

    public void meanremoval(){
        int sum=0;

        //FINDING MEAN
        for(int i=0 ; i<this.arr.length ; i++){
            sum = sum + this.arr[i];
        }
        mean = sum/this.arr.length;

        //MEAN REMOVAL
        for(int i=0;i<this.arr.length;i++){
            this.arr[i] = this.arr[i] - mean;
        }
    }

    public double stresspoints(){
        int slope[] = new int[this.arr.length-1];
        for(int i=0;i<(this.arr.length-1);i++){
            slope[i] = this.arr[i+1]-this.arr[i];
        }
        int pos=0,neg=0;
        for(int i=0;i<slope.length;i++){
            if(slope[i]>=0){
                pos = pos + slope[i];
            }else{
                neg = neg + slope[i];
            }
        }
        double perc = (double)Math.abs(neg)/(Math.abs(neg)+pos);
        return perc;
    }
}
