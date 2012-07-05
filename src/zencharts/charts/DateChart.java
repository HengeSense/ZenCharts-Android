package zencharts.charts;

//Uses GLText from - http://fractiousg.blogspot.com/2012/04/rendering-text-in-opengl-on-android.html

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.DurationFieldType;
import org.joda.time.format.DateTimeFormat;

import zencharts.data.DateSeries;
import zencharts.engine.GLText;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;

public class DateChart extends GLSurfaceView implements Renderer {
	private String fontName;
	private int fontSize;
	private int fontPadX;
	private int fontPadY;

	private static final int IDLE_FPS = 5;
	private static final int ACTIVE_FPS = 40;
	private static int FPS = IDLE_FPS;
	// public static final int FPS_MS = ;

	private static final int HORIZONTAL_GRID_SECTIONS = 5;
	private float mHorizontalGridSpacing;

	private DateTime mPeriodStartTime;
	private Duration mPeriod;
	private GridPeriod mGridPeriodType;
	private GridPeriod mPendingGridPeriodType;

	private boolean gridLines = true;

	private Bitmap lastScreenShot;
	private boolean screenShot = false;

	public float maxValue;
	public int maxDataPoints;

	public boolean xScaleLock;

	float ratio = 0;;
	Context ctx = null;
	private ArrayList<DateSeries> seriesCollection;

	private ScaleGestureDetector mScaleDetector;

	private float mScaleFactor;
	private float mScaleX = 0;

	private float mPosX;
	// private float mPosY;

	public static Rect mWindow;

	private float mLastTouchX;
	// private float mLastTouchY;

	private long mStartTime;
	private long mCurrentTime;
	private long mTimeDelta;

	private RectF mScaledScreenBounds;

	private GLText glText;

	private float mPeriodSeconds;

	private float mPeriodSpacing;
	private float mPeriodLines;

	private static final int INVALID_POINTER_ID = -1;

	// The ‘active pointer’ is the one currently moving our object.
	private int mActivePointerId = INVALID_POINTER_ID;

	public DateChart(Context context, AttributeSet attrs) {
		super(context, attrs);
		ctx = context;
		setFocusable(true);
		setFocusableInTouchMode(true);

		// Initiate the Open GL view and create an instance with this activity
		// glSurfaceView = new GLSurfaceView(ctx);

		this.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
		this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		// this.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR |
		// GLSurfaceView.DEBUG_LOG_GL_CALLS);

		// set our renderer to be the main renderer with the current activity
		// context
		this.setRenderer(this);

		// Create our ScaleGestureDetector
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

	}

	@Override
	public void onPause()
	{
		super.onPause();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (seriesCollection != null) {
			int iLoop = seriesCollection.size();
			for (int i = 0; i < iLoop; i++) {
				seriesCollection.get(i).symbol = null;
			}
		}
	}

	public void addSeries(DateSeries inSeries) {
		if (seriesCollection == null)
			seriesCollection = new ArrayList<DateSeries>();

		maxDataPoints = 0;
		maxValue = 0;

		inSeries.symbol = null;
		seriesCollection.add(inSeries);

		final int seriesCount = seriesCollection.size();
		for (int i = 0; i < seriesCount; i++) {
			DateSeries series = seriesCollection.get(i);
			maxDataPoints = Math.max(maxDataPoints, series.size() - 1);
			for (int j = 0; j < series.size(); j++) {
				maxValue = Math.max(maxValue, series.get(j).value);
			}
		}
		calculateGridlines(true);
	}

	/**
	 * @return the periodStartTime
	 */
	public DateTime getPeriodStartTime() {
		return mPeriodStartTime;
	}

	/**
	 * @param periodStartTime
	 *            the periodStartTime to set
	 */
	public void setPeriodStartTime(DateTime periodStartTime) {
		mPeriodStartTime = periodStartTime;
	}

	public Duration getPeriod() {
		return mPeriod;
	}

	public void setPeriod(Duration period) {
		mPeriod = period;
		mPeriodSeconds = period.toStandardSeconds().getSeconds();
	}

	public void clearChart()
	{
		if (seriesCollection != null)
		{
			seriesCollection.clear();
			seriesCollection = null;
		}
	}

	public void loadFont(String name, int size, int padx, int pady) {
		fontName = name;
		fontSize = size;
		fontPadX = padx;
		fontPadY = pady;
	}

	public void setSeriesVisibility(int series, boolean visible) {
		seriesCollection.get(series).visible = visible;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		super.surfaceDestroyed(holder);

	}

	public void onDrawFrame(GL10 gl) {
		final int fpsMs = (int) ((1.0 / FPS) * 1000);
		mCurrentTime = System.currentTimeMillis();
		mTimeDelta = mCurrentTime - mStartTime;
		if (mTimeDelta < fpsMs) {
			try {
				Thread.sleep(fpsMs - mTimeDelta);
			} catch (Exception e) {
			}
		}
		mStartTime = System.currentTimeMillis();

		if ((seriesCollection != null) && (seriesCollection.size() > 0)) {
			updateChart(gl);
			renderChart(gl);
		}

		if (screenShot) {
			int screenshotSize = mWindow.width() * mWindow.height();
			ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
			bb.order(ByteOrder.nativeOrder());
			gl.glReadPixels(0, 0, mWindow.width(), mWindow.height(), GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
			int pixelsBuffer[] = new int[screenshotSize];
			bb.asIntBuffer().get(pixelsBuffer);
			bb = null;
			Bitmap bitmap = Bitmap.createBitmap(mWindow.width(), mWindow.height(), Bitmap.Config.RGB_565);
			bitmap.setPixels(pixelsBuffer, screenshotSize - mWindow.width(), -mWindow.width(), 0, 0, mWindow.width(), mWindow.height());
			pixelsBuffer = null;

			short sBuffer[] = new short[screenshotSize];
			ShortBuffer sb = ShortBuffer.wrap(sBuffer);
			bitmap.copyPixelsToBuffer(sb);

			// Making created bitmap (from OpenGL points) compatible with
			// Android bitmap
			for (int i = 0; i < screenshotSize; ++i) {
				short v = sBuffer[i];
				sBuffer[i] = (short) (((v & 0x1f) << 11) | (v & 0x7e0) | ((v & 0xf800) >> 11));
			}
			sb.rewind();
			bitmap.copyPixelsFromBuffer(sb);
			lastScreenShot = bitmap.copy(Config.RGB_565, true);

			screenShot = false;
		}
	}

	private void updateChart(GL10 gl) {

		// final float scaledDataWidth = (1 / mScaleFactor) * (1 / mScaleX) *
		// ((float) mPeriodSeconds);
		final float scaledWidth = mScaleFactor * ((float) mWindow.width() * mScaleX);
		final float scaledHeight = mScaleFactor * (float) mWindow.height();

		final float currentWidth = (mPeriodSeconds / mScaleFactor);
		if ((maxDataPoints <= 2) || currentWidth < (mWindow.width() * .9f)) {
			xScaleLock = true;
			mScaleX = currentWidth / (mWindow.width() * .9f);

		}

		final float left = (.5f * -mWindow.width());
		final float right = (.5f * mWindow.width());
		final float top = (.5f * mWindow.height());
		final float bottom = (.5f * -mWindow.height());

		// mPosX = Math.min(mPosX, -(scaledWidth * 0.9f));

		final float minX = (-scaledWidth * 0.9f) / 2.0f;
		final float maxX = -(mPeriodSeconds) + ((scaledWidth / 2.0f) * 0.9f);
		mPosX = Math.max(Math.min(minX, mPosX), maxX);

		final float leftTrans = mPosX;// / 2.0f;

		final float topTrans = (0 - ((mWindow.height() * mScaleFactor) / 2.0f)) * 0.9f;

		mScaledScreenBounds = new RectF(0, 0, (mWindow.width() * mScaleX) * mScaleFactor,
				mWindow.height() * mScaleFactor);
		mScaledScreenBounds.offset(-(leftTrans) - (scaledWidth / 2.0f), -topTrans
				- (scaledHeight / 2.0f));
		mScaledScreenBounds.inset(-15, -15);
		// Log.v("sdafads", "" + mBounds);

		if (verticalGridlines == null) {
			calculateGridlines(true);
		}
		// calculateVerticalGridlines();

		if (glText.collisionRects == null)
			glText.collisionRects = new ArrayList<RectF>();
		glText.collisionRects.clear();

		// Reset the Modelview Matrix
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();

		gl.glOrthof(left * mScaleX, right * mScaleX, bottom, top, -1f, 1f);

		gl.glScalef(1 / mScaleFactor, 1 / mScaleFactor, 1 / mScaleFactor);

		gl.glTranslatef(leftTrans, topTrans, 0);

	}

	private void renderChart(GL10 gl) {

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		// clear Screen and Depth Buffer
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		gl.glMatrixMode(GL10.GL_MODELVIEW);

		try
		{
			if (gridLines)
				drawGridlines(gl);
		} catch (Exception ex) {
		}

		boolean drawnLabels = false;

		// gl.glTranslatef(5000, 0, 0);
		if (seriesCollection != null) {
			int iLoop = seriesCollection.size();
			for (int i = 0; i < iLoop; i++) {
				if (seriesCollection.get(i).visible)
				{
					float x = seriesCollection.get(i).getMinDate() - (mPeriodStartTime.getMillis() / 1000);
					gl.glTranslatef(x, 0, 0);
					if (drawnLabels)
						seriesCollection.get(i).dateLabels = false;
					else
						seriesCollection.get(i).dateLabels = true;

					seriesCollection.get(i).draw(gl, mPeriodStartTime.getMillis() / 1000, mScaleFactor,
							mScaleX, mScaledScreenBounds);

					drawnLabels = true;
					gl.glTranslatef(-x, 0, 0);
				}
			}
		}

		// gl.glTranslatef(-5000, 0, 0);

		drawText(gl, glText, mScaleFactor, mScaleX, mScaledScreenBounds);
	}

	private float[] verticalGridlines;
	private float[] horizontalGridlines;
	private FloatBuffer horizontalGridlineBuffer;
	private FloatBuffer verticalGridlineBuffer;

	private float[] verticalGridlineSwap;
	private float[] horizontalGridlineSwap;
	private FloatBuffer horizontalGridlineSwapBuffer;
	private ByteBuffer horizontalGridlineSwapByteBuffer;
	private FloatBuffer verticalGridlineSwapBuffer;
	private ByteBuffer verticalGridlineSwapByteBuffer;

	private RectF mPrevGridBounds = new RectF();
	private boolean calculating = false;

	public void calculateGridlines(boolean force) {
		new AsyncTask<Boolean, Void, Void>() {
			@Override
			protected Void doInBackground(Boolean... params) {
				calculating = true;
				calculateHorizontalGridlines();
				calculateVerticalGridlines(params[0]);

				return null;
			}

			protected void onPostExecute(Void result) {
				queueEvent(new Runnable() {
					public synchronized void run() {
						verticalGridlines = verticalGridlineSwap;
						horizontalGridlines = horizontalGridlineSwap;
						verticalGridlineBuffer = verticalGridlineSwapBuffer;
						horizontalGridlineBuffer = horizontalGridlineSwapBuffer;
						mGridPeriodType = mPendingGridPeriodType;
						calculating = false;
					}
				});
			};
		}.execute(force);
	}

	private void calculateHorizontalGridlines() {

		final float horizontalGridSpacingSize = HORIZONTAL_GRID_SECTIONS / maxValue;

		horizontalGridlineSwap = new float[(HORIZONTAL_GRID_SECTIONS + 1) * 6];
		int hPos = 0;
		for (int i = 0; i < HORIZONTAL_GRID_SECTIONS + 1; i++) {

			horizontalGridlineSwap[hPos] = 0;
			hPos++;
			horizontalGridlineSwap[hPos] = i * horizontalGridSpacingSize;
			hPos++;
			horizontalGridlineSwap[hPos] = 0;// ;
			hPos++;

			horizontalGridlineSwap[hPos] = (float) (mPeriodSpacing * (Math.floor(mPeriodLines)));
			hPos++;
			horizontalGridlineSwap[hPos] = i * horizontalGridSpacingSize;
			hPos++;
			horizontalGridlineSwap[hPos] = 0;// ;
			hPos++;
		}

		// Gridlines
		horizontalGridlineSwapByteBuffer = ByteBuffer
				.allocateDirect((horizontalGridlineSwap.length) * 4);
		horizontalGridlineSwapByteBuffer.order(ByteOrder.nativeOrder());
		horizontalGridlineSwapBuffer = horizontalGridlineSwapByteBuffer
				.asFloatBuffer();
		horizontalGridlineSwapBuffer.put(horizontalGridlineSwap);
		horizontalGridlineSwapBuffer.position(0);
	}

	private synchronized void calculateVerticalGridlines(boolean force) {
		if (mScaledScreenBounds == null) {
			return;
		} else if (!force && mPrevGridBounds.contains(mScaledScreenBounds)) {
			return;
		}

		RectF bounds = new RectF(mScaledScreenBounds);
		bounds.inset(-mScaledScreenBounds.width() * 0.3f, 0);

		final float horizontalGridSpacingSize = HORIZONTAL_GRID_SECTIONS / maxValue;

		int vPos = 0;
		int firstLine = (int) Math.max(0, bounds.left / mPeriodSpacing);
		int lastLine = (int) Math.min(Math.floor(mPeriodLines), Math.ceil(bounds.right / mPeriodSpacing));
		verticalGridlineSwap = new float[(lastLine - firstLine + 1) * 6];
		for (int i = firstLine; i <= lastLine; i++) {
			// Log.v("bla", "" + i * mPeriodSpacing);
			verticalGridlineSwap[vPos] = i * mPeriodSpacing;
			vPos++;
			verticalGridlineSwap[vPos] = 0;
			vPos++;
			verticalGridlineSwap[vPos] = 0;// ;
			vPos++;

			verticalGridlineSwap[vPos] = i * mPeriodSpacing;
			vPos++;
			verticalGridlineSwap[vPos] = HORIZONTAL_GRID_SECTIONS * horizontalGridSpacingSize;
			vPos++;
			verticalGridlineSwap[vPos] = 0;// ;
			vPos++;
		}

		// Gridlines
		verticalGridlineSwapByteBuffer = ByteBuffer
				.allocateDirect((verticalGridlineSwap.length + 1) * 4);
		verticalGridlineSwapByteBuffer.order(ByteOrder.nativeOrder());
		verticalGridlineSwapBuffer = verticalGridlineSwapByteBuffer.asFloatBuffer();
		verticalGridlineSwapBuffer.put(verticalGridlineSwap);
		verticalGridlineSwapBuffer.position(0);

		mPrevGridBounds = bounds;
		// Log.v("Lines", "" + (lastLine - firstLine + 1));
	}

	public void drawGridlines(GL10 gl) {
		gl.glPushMatrix();
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, verticalGridlineBuffer);
		gl.glLineWidth(2);
		gl.glColor4f(255, 255, 255, 0.25f);
		gl.glDrawArrays(GL10.GL_LINES, 0, verticalGridlines.length / 3);

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, horizontalGridlineBuffer);
		gl.glDrawArrays(GL10.GL_LINES, 0, horizontalGridlines.length / 3);
		gl.glDisable(GL10.GL_BLEND);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glPopMatrix();
	}

	public void calculatePeriod() {
		// Duration duration = mPeriod.toStandardDuration();
		mPeriodLines = mPendingGridPeriodType.getNumberInPeriod(mPeriod);
		mPeriodSpacing = mPeriodSeconds / mPeriodLines;
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if (height == 0) { // Prevent A Divide By Zero By
			height = 1; // Making Height Equal One
		}

		DateChart.mWindow = new Rect(0, 0, width, height);

		float maxScaleFactor = maxValue / ((float) height - (height * 0.1f));
		if (mScaleFactor == 0) {
			mScaleFactor = maxScaleFactor;
			mScaleX = (float) mPeriodSeconds / 10;

			mScaleX = Math.min(mScaleX, 1.0f / ((mWindow.width() * .9f) / mPeriodSeconds / (1 / mScaleFactor)));
		}

		glText.setScale(mScaleFactor * mScaleX, mScaleFactor);

		if (mPendingGridPeriodType == null) {
			mGridPeriodType = GridPeriod.MONTHS;
			mPendingGridPeriodType = GridPeriod.MONTHS;
			calculateGridPeriod();
		}
		calculatePeriod();
		calculateGridlines(true);

		ratio = (float) width / (float) height;
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		mPosX = Integer.MAX_VALUE;
	}

	public Bitmap getScreenShot()
	{
		screenShot = true;
		while (lastScreenShot == null)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return lastScreenShot;
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		glText = new GLText(gl, ctx.getAssets());
		glText.load(fontName, fontSize, fontPadX, fontPadY);
		mStartTime = System.currentTimeMillis();
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// Let the ScaleGestureDetector inspect all events.
		mScaleDetector.onTouchEvent(ev);
		// mFlingDetector.onTouchEvent(ev);

		final int action = ev.getAction();
		float x; // , y;
		int pointerIndex;

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			x = ev.getX();
			// y = ev.getY();

			mLastTouchX = x;
			// mLastTouchY = y;
			mActivePointerId = ev.getPointerId(0);

			FPS = ACTIVE_FPS;
			// Log.v("fps", "" + FPS);
			break;

		case MotionEvent.ACTION_MOVE:
			pointerIndex = ev.findPointerIndex(mActivePointerId);
			x = ev.getX(pointerIndex);
			// y = ev.getY(pointerIndex);
			FPS = ACTIVE_FPS;
			// Only move if the ScaleGestureDetector isn't processing a gesture.
			if (!mScaleDetector.isInProgress()) {
				final float dx = (x - mLastTouchX) * 1.3f;
				// final float dy = (y - mLastTouchY) * 3f;
				// dx = dx * 0.5f;

				mPosX += dx * mScaleX * mScaleFactor;

				// if(mPosX>(0-mScaleFactor * ((float) width *
				// mScaleX)))mPosX=(0-mScaleFactor * ((float) width * mScaleX));
				// mPosY += dy * mScaleFactor;

				// Log.v("left", "" + mPosX);

				if (!calculating) {
					calculateGridlines(false);
				}

				invalidate();
			}

			mLastTouchX = x;
			// mLastTouchY = y;

			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			FPS = IDLE_FPS;
			Log.v("fps", "" + FPS);
			mActivePointerId = INVALID_POINTER_ID;
			break;

		case MotionEvent.ACTION_POINTER_UP:
			pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			final int pointerId = ev.getPointerId(pointerIndex);
			if (pointerId == mActivePointerId) {
				// This was our active pointer going up. Choose a new
				// active pointer and adjust accordingly.
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				mLastTouchX = ev.getX(newPointerIndex);
				// mLastTouchY = ev.getY(newPointerIndex);
				mActivePointerId = ev.getPointerId(newPointerIndex);
			}
			break;

		}

		return true;
	}

	private boolean calculateGridPeriod() {

		GridPeriod type = mPendingGridPeriodType;
		GridPeriod[] periods = GridPeriod.values();

		final int length = periods.length;
		final float textWidth = glText.getLength("        ");
		// final Duration duration = mPeriod.toStandardDuration();

		float lines;
		float spacing;

		for (int i = length - 1; i >= 0; i--) {
			GridPeriod gridPeriod = periods[i];

			lines = gridPeriod.getNumberInPeriod(mPeriod);
			spacing = mPeriod.getStandardSeconds() / (float) lines;

			if (textWidth > spacing) {
				break;
			}
			mPendingGridPeriodType = gridPeriod;
		}

		calculatePeriod();

		return type != mPendingGridPeriodType;
	}

	private class ScaleListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {

		@Override
		public boolean onScale(ScaleGestureDetector detector) {

			if (!xScaleLock) {
				// mScaleFactor *= 1 / detector.getScaleFactor();
				mScaleX *= 1 / detector.getScaleFactor();
				mScaleX = Math.min(mScaleX, 1.0f / ((mWindow.width() * .9f) / mPeriodSeconds / (1 / mScaleFactor)));
				invalidate();
			}

			boolean changed = calculateGridPeriod();
			calculateGridlines(changed);

			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			FPS = ACTIVE_FPS;
			// Log.v("fps", "" + FPS);
			return super.onScaleBegin(detector);
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			super.onScaleEnd(detector);
			FPS = IDLE_FPS;
		}
	}

	public void drawText(GL10 gl, GLText glText, float zoomLevel, float xZoomLevel, RectF bounds) {
		gl.glPushMatrix();

		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_BLEND);

		glText.setScale(zoomLevel * xZoomLevel, zoomLevel);

		glText.begin(1.0f, 1.0f, 1.0f, 1.0f);
		// final int pointCount = this.size();

		float x = 0;

		// Duration duration = mPeriod.toStandardDuration();
		float verticalLines = mGridPeriodType.getNumberInPeriod(mPeriod);
		float verticalGridSpacingSize = mPeriod.getStandardSeconds() / (float) verticalLines;
		String date;

		RectF cullBound = new RectF(bounds);
		cullBound.inset(-(cullBound.width() * 0.15f), 0);

		int currentYear = -1;

		final float lines = mGridPeriodType.getNumberInPeriod(mPeriod);
		final float spacing = mPeriodSeconds / lines;

		final int firstLine = (int) Math.max(0, bounds.left / spacing);
		final int lastLine = (int) Math.min(Math.floor(lines), Math.ceil(bounds.right / spacing));

		for (int i = firstLine; i <= lastLine; i++) {
			x = i * verticalGridSpacingSize;

			DateTime instant = mGridPeriodType.increment(mPeriodStartTime, i);

			if (!cullBound.contains(x, -(DateChart.mWindow.height() * zoomLevel * 0.025f))) {
				currentYear = instant.getYear();
				continue;
			}

			DateMidnight midnight = new DateMidnight(instant);
			Duration midDur = new Duration(midnight, instant);

			if (midDur.toStandardSeconds().getSeconds() == 0) {
				if (instant.getYear() != currentYear || mGridPeriodType.ordinal() > GridPeriod.YEARS.ordinal()) {
					date = instant.toString(DateTimeFormat.forPattern("MMM/yyyy"));
				} else {
					date = instant.toString(DateTimeFormat.forPattern("MMM/dd"));
				}
			} else {
				if (mGridPeriodType == GridPeriod.HALF_DAYS) {
					date = "12:00";
				} else if (mGridPeriodType == GridPeriod.HALF_HOURS && instant.getMinuteOfHour() > 15) {
					date = instant.toString(DateTimeFormat.forPattern("k:mm"));
				} else {
					date = instant.toString(DateTimeFormat.forPattern("k':00'"));
				}
			}

			if (i == 0) {
				// TRLOLOLOLOL
				glText.drawC("      " + date, x,
						-(DateChart.mWindow.height() * zoomLevel * 0.025f));
			} else if (i == verticalLines) {
				glText.drawC(date + "      ", x,
						-(DateChart.mWindow.height() * zoomLevel * 0.025f));
			} else {
				glText.drawC(date, x, -(DateChart.mWindow.height()
						* zoomLevel * 0.025f));
			}

			currentYear = instant.getYear();
		}
		glText.end();
		gl.glDisable(GL10.GL_BLEND);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glPopMatrix();
	}

	private static enum GridPeriod {
		HALF_HOURS,
		HOURS,
		// THREE_HOURS,
		SIX_HOURS,
		HALF_DAYS,
		DAYS,
		// THREE_DAYS,
		WEEKS,
		TWO_WEEKS,
		MONTHS,
		TWO_MONTHS,
		SIX_MONTHS,
		YEARS;

		public DateTime increment(DateTime instant, int amount) {
			switch (this) {
			case DAYS:
				return instant.plusDays(amount);
			case HALF_DAYS:
				return instant.withFieldAdded(DurationFieldType.halfdays(), amount);
			case HOURS:
				return instant.plusHours(amount);
				// case THREE_HOURS:
				// return instant.plusHours(3 * amount);
			case SIX_HOURS:
				return instant.plusHours(6 * amount);
			case HALF_HOURS:
				return instant.plusMinutes(30 * amount);
			case MONTHS:
				return instant.plusMonths(amount);
				// case THREE_DAYS:
				// return instant.plusDays(3 * amount);
			case TWO_WEEKS:
				return instant.plusWeeks(2 * amount);
			case WEEKS:
				return instant.plusWeeks(amount);
			case SIX_MONTHS:
				return instant.plusMonths(6 * amount);
			case TWO_MONTHS:
				return instant.plusMonths(2 * amount);
			case YEARS:
				return instant.plusYears(amount);

			}
			return instant;
		}

		public float getNumberInPeriod(Duration period) {
			float count = 0;
			switch (this) {
			case DAYS:
				count = period.toStandardDays().getDays();
				break;
			case HALF_DAYS:
				count = period.toStandardDays().getDays() * 2;
				break;
			case HOURS:
				count = period.toStandardHours().getHours();
				break;
			// case THREE_HOURS:
			// count = period.toStandardHours().getHours() / 3.0f;
			// break;
			case SIX_HOURS:
				count = period.toStandardHours().getHours() / 6.0f;
				break;
			case HALF_HOURS:
				count = period.toStandardHours().getHours() * 2;
				break;
			case MONTHS:
				count = period.toStandardDays().getDays() / 30.0f;
				break;
			// case THREE_DAYS:
			// count = period.toStandardDays().getDays() / 3.0f;
			// break;
			case TWO_WEEKS:
				count = period.toStandardDays().getDays() / 14.0f;
				break;
			case WEEKS:
				count = period.toStandardDays().getDays() / 7.0f;
				break;
			case SIX_MONTHS:
				count = period.toStandardDays().getDays() / 180.0f;
				break;
			case TWO_MONTHS:
				count = period.toStandardDays().getDays() / 60.0f;
				break;
			case YEARS:
				count = period.toStandardDays().getDays() / 365.0f;
				break;

			}

			return count;
		}
	}
}