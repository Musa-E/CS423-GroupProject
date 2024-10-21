// This code was downloaded as C# source code from https://depts.washington.edu/acelab/proj/dollar/pdollar.html
// Then converted to Java using https://products.codeporting.app/convert/csharp-to-java-project/
/**
 * The $P Point-Cloud Recognizer (.NET Framework 4.0 C# version)
 *
 * 	    Radu-Daniel Vatavu, Ph.D.
 *	    University Stefan cel Mare of Suceava
 *	    Suceava 720229, Romania
 *	    vatavu@eed.usv.ro
 *
 *	    Lisa Anthony, Ph.D.
 *      UMBC
 *      Information Systems Department
 *      1000 Hilltop Circle
 *      Baltimore, MD 21250
 *      lanthony@umbc.edu
 *
 *	    Jacob O. Wobbrock, Ph.D.
 * 	    The Information School
 *	    University of Washington
 *	    Seattle, WA 98195-2840
 *	    wobbrock@uw.edu
 *
 * The academic publication for the $P recognizer, and what should be 
 * used to cite it, is:
 *
 *	Vatavu, R.-D., Anthony, L. and Wobbrock, J.O. (2012).  
 *	  Gestures as point clouds: A $P recognizer for user interface 
 *	  prototypes. Proceedings of the ACM Int'l Conference on  
 *	  Multimodal Interfaces (ICMI '12). Santa Monica, California  
 *	  (October 22-26, 2012). New York: ACM Press, pp. 273-280.
 *
 * This software is distributed under the "New BSD License" agreement:
 *
 * Copyright (c) 2012, Radu-Daniel Vatavu, Lisa Anthony, and 
 * Jacob O. Wobbrock. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the names of the University Stefan cel Mare of Suceava, 
 *	    University of Washington, nor UMBC, nor the names of its contributors 
 *	    may be used to endorse or promote products derived from this software 
 *	    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Radu-Daniel Vatavu OR Lisa Anthony
 * OR Jacob O. Wobbrock BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT 
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
**/
package com.example.pdollarrecognizer;

// import com.aspose.ms.System.SingleExtensions;
// import com.aspose.ms.System.msMath;


import static java.lang.Float.isNaN;
import static java.lang.Float.MAX_VALUE;
import static java.lang.Float.MIN_VALUE;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * Implements a gesture as a cloud of points (i.e., an unordered set of points).
 * Gestures are normalized with respect to scale, translated to origin, and resampled into a fixed number of 32 points.
 * </p>
 */
public class Gesture
{
    public List<Point> Points = null;            // gesture points (normalized)
    public String Name = "";                 // gesture class
    private static final int SAMPLING_RESOLUTION = 32;

    /**
     * <p>
     * Constructs a gesture from an array of points
     * </p>
     * @param points 
     */
    public Gesture(List<Point> points, String gestureName)
    {
        this.Name = gestureName;
        
        // normalizes the array of points with respect to scale, origin, and number of points
        this.Points = scale(points);
        this.Points = translateTo(Points, centroid(Points));
        this.Points = resample(Points, SAMPLING_RESOLUTION);
    }

    public String toString() {
        String result = "Gesture name: " + this.Name + " - ";
        for (Point point : this.Points) {
            result = result + "(" + point.X + ", " + point.Y + ", " + point.StrokeID + "), ";
        }
        return result;
    }

    //>>>>>>>> #region  gesture pre-processing steps: scale normalization, translation to origin, and resampling

    /**
     * <p>
     * Performs scale normalization with shape preservation into [0..1]x[0..1]
     * </p>
     * @return 
     * @param points 
     */
    private List<Point> scale(List<Point> points)
    {
        float minx = MAX_VALUE, miny = MAX_VALUE, maxx = MIN_VALUE, maxy = MIN_VALUE;
        for (int i = 0; i < points.size(); i++)
        {
            if (minx > points.get(i).X) minx = points.get(i).X;
            if (miny > points.get(i).Y) miny = points.get(i).Y;
            if (maxx < points.get(i).X) maxx = points.get(i).X;
            if (maxy < points.get(i).Y) maxy = points.get(i).Y;
        }

        List<Point> newPoints = new ArrayList<Point>(points.size());
        float scale = max(maxx - minx, maxy - miny);
        for (int i = 0; i < points.size(); i++)
            newPoints.add(new Point((points.get(i).X - minx) / scale, (points.get(i).Y - miny) / scale, points.get(i).StrokeID));
        return newPoints;
    }

    /**
     * <p>
     * Translates the array of points by p
     * </p>
     * @return 
     * @param points 
     * @param p 
     */
    private List<Point> translateTo(List<Point> points, Point p)
    {
        List<Point> newPoints = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++)
            newPoints.add(new Point(points.get(i).X - p.X, points.get(i).Y - p.Y, points.get(i).StrokeID));
        return newPoints;
    }

    /**
     * <p>
     * Computes the centroid for an array of points
     * </p>
     * @return 
     * @param points 
     */
    private Point centroid(List<Point> points)
    {
        int pointsLength = points.size();
        float cx = 0, cy = 0;
        for (int i = 0; i < pointsLength; i++)
        {
            cx += points.get(i).X;
            cy += points.get(i).Y;
        }
        return new Point(cx / pointsLength, cy / pointsLength, 0);
    }

    /**
     * <p>
     * Resamples the array of points into n equally-distanced points
     * </p>
     * @return 
     * @param points 
     * @param n 
     */
    public final List<Point> resample(List<Point> points, int n)
    {
        List<Point> newPoints = new ArrayList<Point>(n);
        newPoints.add(new Point(points.get(0).X, points.get(0).Y, points.get(0).StrokeID));
        int numPoints = 1;

        float I = pathLength(points) / (n - 1); // computes interval length
        float D = 0;
        for (int i = 1; i < points.size(); i++)
        {
            if (points.get(i).StrokeID == points.get(i - 1).StrokeID)
            {
                float d = Geometry.euclideanDistance(points.get(i - 1), points.get(i));
                if (D + d >= I)
                {
                    Point firstPoint = points.get(i - 1);
                    while (D + d >= I)
                    {
                        // add interpolated point
                        float t = min(max((I - D) / d, 0.0f), 1.0f);
                        if (isNaN(t)) t = 0.5f;
                        newPoints.add(new Point(
                                (1.0f - t) * firstPoint.X + t * points.get(i).X,
                                (1.0f - t) * firstPoint.Y + t * points.get(i).Y,
                                points.get(i).StrokeID
                        ));
                        numPoints++;

                        // update partial length
                        d = D + d - I;
                        D = 0;
                        firstPoint = newPoints.get(numPoints - 1);
                    }
                    D = d;
                }
                else D += d;
            }
        }

        if (numPoints == n - 1) // sometimes we fall a rounding-error short of adding the last point, so add it if so
            newPoints.add(new Point(points.get(points.size() - 1).X, points.get(points.size() - 1).Y, points.get(points.size() - 1).StrokeID));
        return newPoints;
    }

    /**
     * <p>
     * Computes the path length for an array of points
     * </p>
     * @return 
     * @param points 
     */
    private float pathLength(List<Point> points)
    {
        float length = 0;
        for (int i = 1; i < points.size(); i++)
            if (points.get(i).StrokeID == points.get(i - 1).StrokeID)
                length += Geometry.euclideanDistance(points.get(i - 1), points.get(i));
        return length;
    }

    //<<<<<<<< #endregion 
}
