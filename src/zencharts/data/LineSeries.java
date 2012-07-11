package zencharts.data;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import zencharts.charts.LineChart;
import zencharts.engine.GLText;
import zencharts.engine.Symbol;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

public class LineSeries extends ArrayList<LinePoint> implements Serializable {

	// Chart stuff
	public FloatBuffer vertexBuffer;
	public ByteBuffer vertexByteBuffer;
	public FloatBuffer borderBuffer;
	public ByteBuffer borderByteBuffer;

	public Symbol symbol;

	//public Rect mWindow;

	private Context ctx;
	public int symbolResID = -1;
	// private Symbol symbol;

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
	public boolean xLabels = true;

	public LineSeries(Context context, int resourceID)
	{
		ctx = context;
		symbolResID = resourceID;
		// symbol = new Symbol(0,0,10,10);
	}

	@Override
	public boolean add(LinePoint object) {
		super.add(object);
		calcVerticies();

		return true;
	};

	@Override
	public void add(int index, LinePoint object) {
		calcVerticies();
		super.add(index, object);
	};

	/*public void surfaceChanged(GL10 gl, Rect window)
	{
		mWindow = window;
		//final float size = window.height() * 0.075f;
		//symbol = new Symbol(0, 0, size, size);
		//symbol.loadGLTexture(gl, ctx, symbolResID, lineColor);
		// symbol.loadGLTexture(gl, ctx, symbolResID, lineColor);
	}*/

	public void calcVerticies()
	{
		chartlines = new float[this.size() * 6];
		vertices = new float[this.size() * 18];
		int x = 0;
		float y = 0;
		int vPos = 0;
		int cPos = 0;
		int vLoop = this.size();
		for (int v = 0; v < vLoop; v++)
		{
			x = v * LineChart.gridSize;
			y = get(v).value;

			if (v + 1 < size()) {
				// Draw "primary" triangle point
				vertices[vPos] = x;
				vPos++;
				vertices[vPos] = this.get(v).value;
				vPos++;
				vertices[vPos] = 0;
				vPos++;

				vertices[vPos] = x;
				vPos++;
				vertices[vPos] = 0;
				vPos++;
				vertices[vPos] = 0;// ;
				vPos++;

				vertices[vPos] = x + LineChart.gridSize;
				vPos++;
				vertices[vPos] = 0;
				vPos++;
				vertices[vPos] = 0;// this.get(v).value;
				vPos++;

				vertices[vPos] = x;
				vPos++;
				vertices[vPos] = this.get(v).value;
				vPos++;
				vertices[vPos] = 0;
				vPos++;

				vertices[vPos] = x + LineChart.gridSize;
				vPos++;
				vertices[vPos] = 0;
				vPos++;
				vertices[vPos] = 0;// this.get(v).value;
				vPos++;

				vertices[vPos] = x + LineChart.gridSize;
				vPos++;
				vertices[vPos] = this.get(v + 1).value;
				vPos++;
				vertices[vPos] = 0;// ;
				vPos++;

				// Border
				chartlines[cPos] = x;
				cPos++;
				chartlines[cPos] = y;
				cPos++;
				chartlines[cPos] = 0;// ;
				cPos++;

				chartlines[cPos] = x + LineChart.gridSize;
				cPos++;
				chartlines[cPos] = this.get(v + 1).value;
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

	public void draw(GL10 gl, GLText glText, float zoomLevel, float xZoomLevel, RectF bounds, boolean gridLines) {
		try
		{

			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			//gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

			gl.glEnable(GL10.GL_BLEND);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

			if (LineChart.drawSymbols)
				drawSymbols(gl, bounds, zoomLevel, xZoomLevel);

			// Draw chart
			if (LineChart.drawShade)
			{
				gl.glColor4f(Color.red(lineColor), Color.green(lineColor),
						Color.blue(lineColor), LineChart.shadeAlpha);
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
				gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertices.length / 3);
			}

			// Borderlines
			if (LineChart.drawLines)
			{
				//gl.glEnable(GL10.GL_LINE_SMOOTH);
				gl.glLineWidth(this.lineWidth);
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, borderBuffer);
				gl.glColor4f(Color.red(lineColor), Color.green(lineColor),
						Color.blue(lineColor), LineChart.lineAlpha);
				gl.glDrawArrays(GL10.GL_LINES, 0, chartlines.length / 3);
				gl.glLineWidth(1f);
				//gl.glDisable(GL10.GL_LINE_SMOOTH);
			}

			drawText(gl, glText, zoomLevel, xZoomLevel, bounds);

			// Disable the client state before leaving
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		}
		catch(Exception ex){}
	}

	public void drawSymbols(GL10 gl, RectF bounds, float zoomLevel, float xZoomLevel)
	{
		int x = 0;
		float y = 0;

		if(symbol == null)
		{
			float size = LineChart.mWindow.height() * 0.075f;
			symbol = new Symbol(0, 0, size, size);
			symbol.loadGLTexture(gl, ctx, symbolResID, lineColor);
		}

		final int vLoop = this.size();
		for (int v = 0; v < vLoop; v++) {
			x = v * LineChart.gridSize;
			y = this.get(v).value;

			if (bounds.contains(x, y)) {

				gl.glPushMatrix();
				gl.glTranslatef(x, y, 0);
				gl.glScalef(zoomLevel * xZoomLevel, zoomLevel, 1.0f);

				symbol.draw(gl, zoomLevel, xZoomLevel);

				gl.glPopMatrix();

			}
		}
	}

	public void drawText(GL10 gl, GLText glText, float zoomLevel, float xZoomLevel, RectF bounds)
	{
		gl.glPushMatrix();

		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_BLEND);

		glText.setScale(zoomLevel * xZoomLevel, zoomLevel);

		glText.begin(1.0f, 1.0f, 1.0f, 1.0f);
		final int vLoop = this.size();

		int x = 0;
		float y = 0;

		for (int v = 0; v < vLoop; v++)
		{
			x = v * LineChart.gridSize;
			y = this.get(v).value;
			// TODO:Removed bounds check for xLabels to prevent jumpy CD
			if (xLabels)// && bounds.contains(x, -2f))
			{
				if (v == 0) {
					// TRLOLOLOLOL
					glText.drawCC("        " + this.get(v).xstring, x,
							-(LineChart.mWindow.height() * zoomLevel * 0.025f));
				} else if (v == vLoop - 1) {
					glText.drawCC(this.get(v).xstring + "        ", x,
							-(LineChart.mWindow.height() * zoomLevel * 0.025f));
				} else {
					glText.drawCC(this.get(v).xstring, x, -(LineChart.mWindow.height() * zoomLevel * 0.025f));
				}
			}

			if (bounds.contains(x, y)) {
				glText.drawCC("" + this.get(v).label, x, y);
			}
		}
		glText.end();
		gl.glDisable(GL10.GL_BLEND);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glPopMatrix();
	}
}