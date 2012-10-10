/*
 * 
 * Zen Charts
 * 
 * Copyright © 2009-2012 United States Government as represented by 
 * the Chief Information Officer of the National Center for Telehealth 
 * and Technology. All Rights Reserved.
 * 
 * Copyright © 2009-2012 Contributors. All Rights Reserved. 
 * 
 * THIS OPEN SOURCE AGREEMENT ("AGREEMENT") DEFINES THE RIGHTS OF USE, 
 * REPRODUCTION, DISTRIBUTION, MODIFICATION AND REDISTRIBUTION OF CERTAIN 
 * COMPUTER SOFTWARE ORIGINALLY RELEASED BY THE UNITED STATES GOVERNMENT 
 * AS REPRESENTED BY THE GOVERNMENT AGENCY LISTED BELOW ("GOVERNMENT AGENCY"). 
 * THE UNITED STATES GOVERNMENT, AS REPRESENTED BY GOVERNMENT AGENCY, IS AN 
 * INTENDED THIRD-PARTY BENEFICIARY OF ALL SUBSEQUENT DISTRIBUTIONS OR 
 * REDISTRIBUTIONS OF THE SUBJECT SOFTWARE. ANYONE WHO USES, REPRODUCES, 
 * DISTRIBUTES, MODIFIES OR REDISTRIBUTES THE SUBJECT SOFTWARE, AS DEFINED 
 * HEREIN, OR ANY PART THEREOF, IS, BY THAT ACTION, ACCEPTING IN FULL THE 
 * RESPONSIBILITIES AND OBLIGATIONS CONTAINED IN THIS AGREEMENT.
 * 
 * Government Agency: The National Center for Telehealth and Technology
 * Government Agency Original Software Designation: ZenCharts001
 * Government Agency Original Software Title: Zen Charts
 * User Registration Requested. Please send email 
 * with your contact information to: robert.kayl2@us.army.mil
 * Government Agency Point of Contact for Original Software: robert.kayl2@us.army.mil
 * 
 */
package zencharts.data;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import zencharts.charts.DateChart;
import zencharts.engine.Symbol;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

/**
 * @author wes
 * 
 */
public class DateSeries extends ArrayList<DatePoint> implements Serializable {

	// Chart stuff
	public FloatBuffer vertexBuffer;
	public ByteBuffer vertexByteBuffer;
	public FloatBuffer borderBuffer;
	public ByteBuffer borderByteBuffer;

	public Symbol symbol;

	private Context ctx;
	public int symbolResID = -1;

	private ByteBuffer symbolByteBuffer;
	private FloatBuffer symbolBuffer;

	private float vertices[] = null;
	private float chartlines[] = null;

	public int lineColor = Color.MAGENTA;
	public String title = "";
	public Long id = 0L;
	public boolean visible = true;
	public float lineWidth = 1;
	public float[] dashEffect = null;
	public Bitmap markerBitmap = null;
	public int markerSize = 20;
	public boolean showMarkers = true;
	public boolean dateLabels = true;

	private long mMinDate = Long.MAX_VALUE;
	private long mMaxDate;

	private boolean mDrawSymbols = true;
	private boolean mDrawShade = true;
	private boolean mDrawLines = true;

	private float mShadeAlpha = 0.2f;
	private float mLineAlpha = 1f;

	public DateSeries(Context context, int resourceID)
	{
		ctx = context;
		symbolResID = resourceID;
		// symbol = new Symbol(0,0,10,10);
	}

	@Override
	public boolean add(DatePoint object) {
		super.add(object);
		mMinDate = Math.min(mMinDate, (object.timeStamp / 1000));
		mMaxDate = Math.max(mMaxDate, (object.timeStamp / 1000));
		calcVerticies();

		return true;
	};

	@Override
	public void add(int index, DatePoint object) {
		super.add(index, object);
		mMinDate = Math.min(mMinDate, (object.timeStamp / 1000));
		mMaxDate = Math.max(mMaxDate, (object.timeStamp / 1000));

		calcVerticies();

	};

	/*
	 * public void surfaceChanged(GL10 gl, Rect window) { mWindow = window;
	 * //final float size = window.height() * 0.075f; //symbol = new Symbol(0,
	 * 0, size, size); //symbol.loadGLTexture(gl, ctx, symbolResID, lineColor);
	 * // symbol.loadGLTexture(gl, ctx, symbolResID, lineColor); }
	 */

	public void calcVerticies()
	{
		chartlines = new float[this.size() * 6];
		vertices = new float[this.size() * 18];
		int vPos = 0;
		int cPos = 0;
		float x = 0, nextX = 0;
		final int pointCount = this.size();
		DatePoint point, nextPoint;
		for (int i = 0; i < pointCount; i++)
		{
			point = get(i);
			nextPoint = (i + 1 < pointCount) ? get(i + 1) : point;

			x = (point.timeStamp / 1000) - mMinDate;
			nextX = (nextPoint.timeStamp /1000) - mMinDate;

			if (i + 1 < size()) {
				// Vert 1
				// X
				vertices[vPos] = x;
				vPos++;
				// Y
				vertices[vPos] = point.value;
				vPos++;
				// Z
				vertices[vPos] = 0;
				vPos++;

				// Vert 2
				vertices[vPos] = x;
				vPos++;
				vertices[vPos] = 0;
				vPos++;
				vertices[vPos] = 0;// ;
				vPos++;

				// Vert 3
				vertices[vPos] = nextX;
				vPos++;
				vertices[vPos] = 0;
				vPos++;
				vertices[vPos] = 0;// this.get(v).value;
				vPos++;

				vertices[vPos] = x;
				vPos++;
				vertices[vPos] = point.value;
				vPos++;
				vertices[vPos] = 0;
				vPos++;

				vertices[vPos] = nextX;
				vPos++;
				vertices[vPos] = 0;
				vPos++;
				vertices[vPos] = 0;// this.get(v).value;
				vPos++;

				vertices[vPos] = nextX;
				vPos++;
				vertices[vPos] = nextPoint.value;
				vPos++;
				vertices[vPos] = 0;// ;
				vPos++;

				// Border
				chartlines[cPos] = x;
				cPos++;
				chartlines[cPos] = point.value;
				cPos++;
				chartlines[cPos] = 0;// ;
				cPos++;

				chartlines[cPos] = nextX;
				cPos++;
				chartlines[cPos] = nextPoint.value;
				cPos++;
				chartlines[cPos] = 0;// ;
				cPos++;
			}
		}

		// a float has 4 bytes so we allocate for each coordinate 4 bytes
		vertexByteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
		vertexByteBuffer.order(ByteOrder.nativeOrder());

		// allocates the memory from the byte buffer
		vertexBuffer = vertexByteBuffer.asFloatBuffer();

		// fill the vertexBuffer with the vertices
		vertexBuffer.put(vertices);

		// set the cursor position to the beginning of the buffer
		vertexBuffer.position(0);

		// Gridlines
		borderByteBuffer = ByteBuffer.allocateDirect(chartlines.length * 4);
		borderByteBuffer.order(ByteOrder.nativeOrder());
		borderBuffer = borderByteBuffer.asFloatBuffer();
		borderBuffer.put(chartlines);
		borderBuffer.position(0);

		// Symbols
		symbolByteBuffer = ByteBuffer.allocateDirect(12 * 4);
		symbolByteBuffer.order(ByteOrder.nativeOrder());
		symbolBuffer = symbolByteBuffer.asFloatBuffer();

	}

	public void draw(GL10 gl, float graphStartTime, float zoomLevel, float xZoomLevel, RectF bounds) {
		try {

			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

			gl.glEnable(GL10.GL_BLEND);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

			if (mDrawSymbols) {
				drawSymbols(gl, bounds, graphStartTime, zoomLevel, xZoomLevel);
			}

			// Draw chart
			if (mDrawShade) {
				gl.glColor4f(Color.red(lineColor)/255f, Color.green(lineColor)/255f,
						Color.blue(lineColor)/255f, mShadeAlpha);
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
				gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertices.length / 3);
			}

			// Borderlines
			if (mDrawLines) {
				// gl.glEnable(GL10.GL_LINE_SMOOTH);
				gl.glLineWidth(this.lineWidth);
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, borderBuffer);
				gl.glColor4f(Color.red(lineColor)/255f, Color.green(lineColor)/255f,
						Color.blue(lineColor)/255f, mLineAlpha);
				gl.glDrawArrays(GL10.GL_LINES, 0, chartlines.length / 3);
				gl.glLineWidth(1f);
				// gl.glDisable(GL10.GL_LINE_SMOOTH);
			}

			// drawText(gl, glText, zoomLevel, xZoomLevel, bounds);

			// Disable the client state before leaving
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		} catch (Exception ex) {
		}
	}

	public void drawSymbols(GL10 gl, RectF bounds, float graphStartTime, float zoomLevel, float xZoomLevel)
	{
		if (symbolResID == 0) {
			return;
		}

		long x = 0;
		float y = 0;

		if (symbol == null) {
			float size = DateChart.mWindow.height() * 0.035f;
			symbol = new Symbol(0, 0, size, size);
			symbol.loadGLTexture(gl, ctx, symbolResID, lineColor);
		}

		final int pointCount = this.size();
		DatePoint point;
		for (int i = 0; i < pointCount; i++) {
			point = get(i);
			x = (point.timeStamp / 1000) - mMinDate;
			y = point.value;

			if (bounds.contains((point.timeStamp / 1000) - graphStartTime, y)) {

				gl.glPushMatrix();
				gl.glTranslatef(x, y, 0);
				gl.glScalef(zoomLevel * xZoomLevel, zoomLevel, 1.0f);

				symbol.draw(gl, zoomLevel, xZoomLevel);

				gl.glPopMatrix();

			}
		}
	}

	/**
	 * @return the drawSymbols
	 */
	public boolean isDrawSymbols() {
		return mDrawSymbols;
	}

	/**
	 * @param drawSymbols
	 *            the drawSymbols to set
	 */
	public void setDrawSymbols(boolean drawSymbols) {
		mDrawSymbols = drawSymbols;
	}

	/**
	 * @return the drawShade
	 */
	public boolean isDrawShade() {
		return mDrawShade;
	}

	/**
	 * @param drawShade
	 *            the drawShade to set
	 */
	public void setDrawShade(boolean drawShade) {
		mDrawShade = drawShade;
	}

	/**
	 * @return the drawLines
	 */
	public boolean isDrawLines() {
		return mDrawLines;
	}

	/**
	 * @param drawLines
	 *            the drawLines to set
	 */
	public void setDrawLines(boolean drawLines) {
		mDrawLines = drawLines;
	}

	/**
	 * @return the shadeAlpha
	 */
	public float getShadeAlpha() {
		return mShadeAlpha;
	}

	/**
	 * @param shadeAlpha
	 *            the shadeAlpha to set
	 */
	public void setShadeAlpha(float shadeAlpha) {
		mShadeAlpha = shadeAlpha;
	}

	/**
	 * @return the lineAlpha
	 */
	public float getLineAlpha() {
		return mLineAlpha;
	}

	/**
	 * @param lineAlpha
	 *            the lineAlpha to set
	 */
	public void setLineAlpha(float lineAlpha) {
		mLineAlpha = lineAlpha;
	}

	/**
	 * @return the minDate
	 */
	public long getMinDate() {
		return mMinDate;
	}

	/**
	 * @return the maxDate
	 */
	public long getMaxDate() {
		return mMaxDate;
	}

}