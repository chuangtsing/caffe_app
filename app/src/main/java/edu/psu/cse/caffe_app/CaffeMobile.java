package edu.psu.cse.caffe_app;

/**
 * Created by shiro on 3/26/15.
 */
public class CaffeMobile {
    public native void enableLog(boolean enabled);
    public native int loadModel(String modelPath, String weightsPath);
    public native int[] predictImage(String imgPath, int topk);
    public native int[] predictImageMat(long img, int topk);
    public native int[] predictImageMatArray(long[] imgArray, int topk);
}
