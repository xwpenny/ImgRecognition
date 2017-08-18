package com.example.imgrecognition.filters;

import org.opencv.core.Mat;

public interface Filter {
    public abstract boolean match(final Mat src, final Mat dst);
}
