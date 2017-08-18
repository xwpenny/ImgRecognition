package com.example.imgrecognition.filters.ar;

import android.content.Context;

import com.example.imgrecognition.filters.Filter;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ImageDetectionFilter implements Filter {

    // The reference image (this detector's target).
    private final Mat mReferenceImage;
    // Features of the reference image.
    private final MatOfKeyPoint mReferenceKeypoints = new MatOfKeyPoint();
    // Descriptors of the reference image's features.
    private final Mat mReferenceDescriptors = new Mat();
    // The corner coordinates of the reference image, in pixels.
    // CvType defines the color depth, number of channels, and
    // channel layout in the image. Here, each point is represented
    // by two 32-bit floats.
    private final Mat mReferenceCorners = new Mat(4, 1, CvType.CV_32FC2);

    // Features of the scene (the current frame).
    private final MatOfKeyPoint mSceneKeypoints = new MatOfKeyPoint();
    // Descriptors of the scene's features.
    private final Mat mSceneDescriptors = new Mat();
    // Tentative corner coordinates detected in the scene, in
    // pixels.
    private final Mat mCandidateSceneCorners = new Mat(4, 1, CvType.CV_32FC2);
    // Good corner coordinates detected in the scene, in pixels.
    private final Mat mSceneCorners = new Mat(0, 0, CvType.CV_32FC2);
    // The good detected corner coordinates, in pixels, as integers.
    private final MatOfPoint mIntSceneCorners = new MatOfPoint();

    // A grayscale version of the scene.
    private final Mat mGraySrc = new Mat();
    // Tentative matches of scene features and reference features.
    private final MatOfDMatch mMatches = new MatOfDMatch();

    // A feature detector, which finds features in images.
    private final FeatureDetector mFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
    // A descriptor extractor, which creates descriptors of
    // features.
    private final DescriptorExtractor mDescriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    // A descriptor matcher, which matches features based on their
    // descriptors.
    private final DescriptorMatcher mDescriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);

    // The color of the outline drawn around the detected image.
    private final Scalar mLineColor = new Scalar(0, 255, 0);

    public ImageDetectionFilter(final Context context, final int referenceImageResourceID) throws IOException {

        // Load the reference image from the app's resources.
        // It is loaded in BGR (blue, green, red) format.
        mReferenceImage = Utils.loadResource(context, referenceImageResourceID, Imgcodecs.CV_LOAD_IMAGE_COLOR);

        // Create grayscale and RGBA versions of the reference image.
        final Mat referenceImageGray = new Mat();
        Imgproc.cvtColor(mReferenceImage, referenceImageGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(mReferenceImage, mReferenceImage, Imgproc.COLOR_BGR2RGBA);

        // Store the reference image's corner coordinates, in pixels.
        mReferenceCorners.put(0, 0, new double[]{0.0, 0.0});
        mReferenceCorners.put(1, 0, new double[]{referenceImageGray.cols(), 0.0});
        mReferenceCorners.put(2, 0, new double[]{referenceImageGray.cols(), referenceImageGray.rows()});
        mReferenceCorners.put(3, 0, new double[]{0.0, referenceImageGray.rows()});

        // Detect the reference features and compute their
        // descriptors.
        mFeatureDetector.detect(referenceImageGray, mReferenceKeypoints);
        mDescriptorExtractor.compute(referenceImageGray, mReferenceKeypoints, mReferenceDescriptors);
    }

    public ImageDetectionFilter(final String path) throws IOException {
        // Load the reference image from the app's resources.
        // It is loaded in BGR (blue, green, red) format.
        mReferenceImage = Utils.loadPath(path, Imgcodecs.CV_LOAD_IMAGE_COLOR);
//        mReferenceImage = Utils.loadResource(context, referenceImageResourceID, Imgcodecs.CV_LOAD_IMAGE_COLOR);

        // Create grayscale and RGBA versions of the reference image.
        final Mat referenceImageGray = new Mat();
        Imgproc.cvtColor(mReferenceImage, referenceImageGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(mReferenceImage, mReferenceImage, Imgproc.COLOR_BGR2RGBA);

        // Store the reference image's corner coordinates, in pixels.
        mReferenceCorners.put(0, 0, new double[]{0.0, 0.0});
        mReferenceCorners.put(1, 0, new double[]{referenceImageGray.cols(), 0.0});
        mReferenceCorners.put(2, 0, new double[]{referenceImageGray.cols(), referenceImageGray.rows()});
        mReferenceCorners.put(3, 0, new double[]{0.0, referenceImageGray.rows()});

        // Detect the reference features and compute their
        // descriptors.
        mFeatureDetector.detect(referenceImageGray, mReferenceKeypoints);
        mDescriptorExtractor.compute(referenceImageGray, mReferenceKeypoints, mReferenceDescriptors);
    }

    @Override
    public boolean match(final Mat src, final Mat dst) {

        // Convert the scene to grayscale.
        Imgproc.cvtColor(src, mGraySrc, Imgproc.COLOR_RGBA2GRAY);

        // Detect the scene features, compute their descriptors,
        // and match the scene descriptors to reference descriptors.
        mFeatureDetector.detect(mGraySrc, mSceneKeypoints);
        mDescriptorExtractor.compute(mGraySrc, mSceneKeypoints, mSceneDescriptors);
        mDescriptorMatcher.match(mSceneDescriptors, mReferenceDescriptors, mMatches);

        // Attempt to find the target image's corners in the scene.
        return findSceneCorners();
    }

    private boolean findSceneCorners() {

        final List<DMatch> matchesList = mMatches.toList();

        if (matchesList.size() < 4) {
            // There are too few matches to find the homography.
            return false;
        }

        final List<KeyPoint> referenceKeypointsList = mReferenceKeypoints.toList();
        final List<KeyPoint> sceneKeypointsList = mSceneKeypoints.toList();

        // Calculate the max and min distances between keypoints.
        double maxDist = 0.0;
        double minDist = Double.MAX_VALUE;
        for (final DMatch match : matchesList) {
            final double dist = match.distance;
            if (dist < minDist) {
                minDist = dist;
            }
            if (dist > maxDist) {
                maxDist = dist;
            }
        }

        // The thresholds for minDist are chosen subjectively
        // based on testing. The unit is not related to pixel
        // distances; it is related to the number of failed tests
        // for similarity between the matched descriptors.
        if (minDist > 50.0) {
            // The target is completely lost.
            // Discard any previously found corners.
            mSceneCorners.create(0, 0, mSceneCorners.type());
            return false;
        } else if (minDist > 25.0) {
            // The target is lost but maybe it is still close.
            // Keep any previously found corners.
            return false;
        }

        // Identify "good" keypoints based on match distance.
        final ArrayList<Point> goodReferencePointsList = new ArrayList<Point>();
        final ArrayList<Point> goodScenePointsList = new ArrayList<Point>();
        final double maxGoodMatchDist = 1.75 * minDist;
        for (final DMatch match : matchesList) {
            if (match.distance < maxGoodMatchDist) {
                goodReferencePointsList.add(referenceKeypointsList.get(match.trainIdx).pt);
                goodScenePointsList.add(sceneKeypointsList.get(match.queryIdx).pt);
            }
        }

        if (goodReferencePointsList.size() < 4 || goodScenePointsList.size() < 4) {
            // There are too few good points to find the homography.
            return false;
        }
        return true;
    }
}
