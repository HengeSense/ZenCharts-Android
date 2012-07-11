package zencharts.charts;

//Uses GLText from - http://fractiousg.blogspot.com/2012/04/rendering-text-in-opengl-on-android.html

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import zencharts.data.LineSeries;
import zencharts.engine.GLText;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;

public class LineChart extends GLSurfaceView implements Renderer {

	private String fontName;
	private int fontSize;
	private int fontPadX;
	private int fontPadY;

	public static final int IDLE_FPS = 5;
	public static final int ACTIVE_FPS = 40;
	public static int FPS = IDLE_FPS;
	// public static final int FPS_MS = ;
	public static boolean gridLines = true;
	public static boolean drawShade = true;
	public static boolean drawLines = true;
	public static boolean drawSymbols = true;
	public static int gridSize = 10;
	public static float shadeAlpha = 0.30f;
	public static float lineAlpha = 1f;

	private Bitmap lastScreenShot;
	private boolean screenShot = false;
	
	public float maxValue;
	public int maxDataPoints;

	public boolean xScaleLock;

	float ratio = 0;;
	Context ctx = null;
	private ArrayList<LineSeries> seriesCollection;

	private ScaleGestureDetector mScaleDetector;
	private GestureDetector mFlingDetector;

	private float mMaxScaleFactor;
	private float mScaleFactor;
	private float mScaleX = 0;

	private float mScaleFocalX, mScaleFocalY;

	private float mPosX;
	//private float mPosY;

	public static Rect mWindow;

	private float mLastTouchX;
	//private float mLastTouchY;

	private long mStartTime;
	private long mCurrentTime;
	private long mTimeDelta;

	private RectF mBounds;

	private GLText glText;
	private int maxSeriesSize = 0;

	private static final int INVALID_POINTER_ID = -1;

	// The ‘active pointer’ is the one currently moving our object.
	private int mActivePointerId = INVALID_POINTER_ID;

	public LineChart(Context context, AttributeSet attrs) {
		super(context, attrs);
		ctx = context;
		setFocusable(true);
		setFocusableInTouchMode(true);

		// Initiate the Open GL view and create an instance with this activity
		// glSurfaceView = new GLSurfaceView(ctx);

		this.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
		this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		//this.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
		
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
	
	public void addSeries(LineSeries inSeries) {
		if (seriesCollection == null)
			seriesCollection = new ArrayList<LineSeries>();
		
		inSeries.symbol = null;
		seriesCollection.add(inSeries);

		if (inSeries.size() > maxSeriesSize)
			maxSeriesSize = inSeries.size();

		calculateGridlines();
		

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

	public void SetMaxYValue(int maxY) {
		maxValue = maxY;
	}

	public void SetSeriesVisibility(int series, boolean visible) {
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

		if((seriesCollection != null) && (seriesCollection.size()>0))
		{
			
			updateChart(gl);
			renderChart(gl);
		}
		
		if(screenShot){                     
            int screenshotSize = mWindow.width() * mWindow.height();
            ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
            bb.order(ByteOrder.nativeOrder());
            gl.glReadPixels(0, 0, mWindow.width(), mWindow.height(), GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
            int pixelsBuffer[] = new int[screenshotSize];
            bb.asIntBuffer().get(pixelsBuffer);
            bb = null;
            Bitmap bitmap = Bitmap.createBitmap(mWindow.width(), mWindow.height(), Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixelsBuffer, screenshotSize-mWindow.width(), -mWindow.width(), 0, 0, mWindow.width(), mWindow.height());
            pixelsBuffer = null;

            short sBuffer[] = new short[screenshotSize];
            ShortBuffer sb = ShortBuffer.wrap(sBuffer);
            bitmap.copyPixelsToBuffer(sb);

            //Making created bitmap (from OpenGL points) compatible with Android bitmap
            for (int i = 0; i < screenshotSize; ++i) {                  
                short v = sBuffer[i];
                sBuffer[i] = (short) (((v&0x1f) << 11) | (v&0x7e0) | ((v&0xf800) >> 11));
            }
            sb.rewind();
            bitmap.copyPixelsFromBuffer(sb);
            lastScreenShot = bitmap.copy(Config.ARGB_8888, true);

            screenShot = false;
        }
	}

	private void updateChart(GL10 gl) {

		final float scaledWidth = mScaleFactor * ((float) mWindow.width() * mScaleX);
		final float scaledHeight = mScaleFactor * (float) mWindow.height();

		final float currentWidth = ((maxDataPoints * 10) / mScaleFactor);
		if ( (maxDataPoints <= 2) || currentWidth < (mWindow.width() * .9f)) {
			xScaleLock = true;
			mScaleX = currentWidth / (mWindow.width() * .9f);

		}

		final float left = (.5f * -mWindow.width());
		final float right = (.5f * mWindow.width());
		final float top = (.5f * mWindow.height());
		final float bottom = (.5f * -mWindow.height());

		mScaleFactor = mMaxScaleFactor;

		mPosX = Math.min(mPosX, -(scaledWidth * 0.9f));

		mPosX = Math.max(mPosX, -(maxDataPoints * (gridSize * 2))
				+ (scaledWidth * 0.9f));

		// mPosX = Math.max(mPosX, scaledGraphWidth);
		// Log.v("sdafads", "" + mPosX + "| " + graphWidth);
		// mPosX = Math.min(mPosX, xMin);

		final float leftTrans = mPosX / 2.0f;

		final float topTrans = (0 - ((mWindow.height() * mScaleFactor) / 2.0f)) * 0.9f; 

		mBounds = new RectF(0, 0, (mWindow.width() * mScaleX) * mScaleFactor,
				mWindow.height() * mScaleFactor);
		mBounds.offset(-(leftTrans) - (scaledWidth / 2.0f), -topTrans
				- (scaledHeight / 2.0f));
		mBounds.inset(-15, -15);
		// Log.v("sdafads", "" + mBounds);

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
		}catch(Exception ex){}
		
		boolean drawnLabels = false;
		
		if (seriesCollection != null) {
			int iLoop = seriesCollection.size();
			for (int i = 0; i < iLoop; i++) {
				if (seriesCollection.get(i).visible)
				{
					if(drawnLabels)
						seriesCollection.get(i).xLabels = false;
					else
						seriesCollection.get(i).xLabels = true;
					
					seriesCollection.get(i).draw(gl, glText, mScaleFactor,
							mScaleX, mBounds, gridLines);
					
					drawnLabels = true;
				}
			}
		}
	}

	private float[] verticalGridlines;
	private float[] horizontalGridlines;
	public FloatBuffer horizontalGridlineBuffer;
	public ByteBuffer horizontalGridlineByteBuffer;
	public FloatBuffer verticalGridlineBuffer;
	public ByteBuffer verticalGridlineByteBuffer;

	public void calculateGridlines() {
		maxDataPoints = 0;
		maxValue = 0;

		final int seriesCount = seriesCollection.size();
		for (int i = 0; i < seriesCount; i++) {
			LineSeries series = seriesCollection.get(i);
			maxDataPoints = Math.max(maxDataPoints, series.size() - 1);
			for (int j = 0; j < series.size(); j++) {
				maxValue = Math.max(maxValue, series.get(j).value);
			}
		}

		final int maxHorizontalLines = (int) (Math.ceil((maxValue + 20)
				/ gridSize));

		verticalGridlines = new float[(maxDataPoints + 1) * 6];
		int vPos = 0;
		for (int i = 0; i < maxDataPoints + 1; i++) {

			verticalGridlines[vPos] = i * gridSize;
			vPos++;
			verticalGridlines[vPos] = 0;
			vPos++;
			verticalGridlines[vPos] = 0;// ;
			vPos++;

			verticalGridlines[vPos] = i * gridSize;
			vPos++;
			verticalGridlines[vPos] = (maxHorizontalLines - 1) * gridSize;
			vPos++;
			verticalGridlines[vPos] = 0;// ;
			vPos++;

			// Gridlines
			verticalGridlineByteBuffer = ByteBuffer
					.allocateDirect((verticalGridlines.length + 1) * 4);
			verticalGridlineByteBuffer.order(ByteOrder.nativeOrder());
			verticalGridlineBuffer = verticalGridlineByteBuffer.asFloatBuffer();
			verticalGridlineBuffer.put(verticalGridlines);
			verticalGridlineBuffer.position(0);
		}

		horizontalGridlines = new float[(maxHorizontalLines) * 6];
		int hPos = 0;
		for (int i = 0; i < maxHorizontalLines; i++) {

			horizontalGridlines[hPos] = 0;
			hPos++;
			horizontalGridlines[hPos] = i * gridSize;
			hPos++;
			horizontalGridlines[hPos] = 0;// ;
			hPos++;

			horizontalGridlines[hPos] = maxDataPoints * gridSize;
			hPos++;
			horizontalGridlines[hPos] = i * gridSize;
			hPos++;
			horizontalGridlines[hPos] = 0;// ;
			hPos++;

			// Gridlines
			horizontalGridlineByteBuffer = ByteBuffer
					.allocateDirect((horizontalGridlines.length) * 4);
			horizontalGridlineByteBuffer.order(ByteOrder.nativeOrder());
			horizontalGridlineBuffer = horizontalGridlineByteBuffer
					.asFloatBuffer();
			horizontalGridlineBuffer.put(horizontalGridlines);
			horizontalGridlineBuffer.position(0);
		}
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

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if (height == 0) { // Prevent A Divide By Zero By
			height = 1; // Making Height Equal One
		}

		LineChart.mWindow = new Rect(0, 0, width, height);

		//calculateGridlines();
		int verticalRows = (int) (Math.ceil((maxValue + 20) / gridSize));
		mMaxScaleFactor = (float) ((verticalRows - 1) * gridSize)
				/ ((float) height - (height * 0.1f));
		if (mScaleFactor == 0) {
			mScaleFactor = mMaxScaleFactor;
			mScaleX = 1.0f;
		}

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
		while(lastScreenShot == null)
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
		//mFlingDetector.onTouchEvent(ev);

		final int action = ev.getAction();
		float x; //, y;
		int pointerIndex;

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			x = ev.getX();
			//y = ev.getY();

			mLastTouchX = x;
			//mLastTouchY = y;
			mActivePointerId = ev.getPointerId(0);

			FPS = ACTIVE_FPS;
			//Log.v("fps", "" + FPS);
			break;

		case MotionEvent.ACTION_MOVE:
			pointerIndex = ev.findPointerIndex(mActivePointerId);
			x = ev.getX(pointerIndex);
			//y = ev.getY(pointerIndex);
			FPS = ACTIVE_FPS;
			// Only move if the ScaleGestureDetector isn't processing a gesture.
			if (!mScaleDetector.isInProgress()) {
				final float dx = (x - mLastTouchX) * 3f;
				//final float dy = (y - mLastTouchY) * 3f;
				// dx = dx * 0.5f;

				mPosX += dx * mScaleX * mScaleFactor;
				// if(mPosX>(0-mScaleFactor * ((float) width *
				// mScaleX)))mPosX=(0-mScaleFactor * ((float) width * mScaleX));
				// mPosY += dy * mScaleFactor;

				// Log.v("left", "" + mPosX);

				invalidate();
			}

			mLastTouchX = x;
			//mLastTouchY = y;

			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			FPS = IDLE_FPS;
			//Log.v("fps", "" + FPS);
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
				//mLastTouchY = ev.getY(newPointerIndex);
				mActivePointerId = ev.getPointerId(newPointerIndex);
			}
			break;

		}

		return true;
	}

	private class ScaleListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFocalX = detector.getFocusX() - mScaleFocalX;
			mScaleFocalY = detector.getFocusY() - mScaleFocalY;

			if (!xScaleLock) {
				mScaleFactor *= 1 / detector.getScaleFactor();
				mScaleX += 1 - detector.getScaleFactor();
				mScaleX = Math.min(3, Math.max(.4f, mScaleX));
				invalidate();
			}
			// Log.v("scale", "" + mScaleX);

			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			FPS = ACTIVE_FPS;
			//Log.v("fps", "" + FPS);
			return super.onScaleBegin(detector);
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			super.onScaleEnd(detector);
			FPS = IDLE_FPS;
			//Log.v("fps", "" + FPS);
			// mScaleFocalX = width / 2.0f;
			// mScaleFocalY = height / 2.0f;
		}
	}

}