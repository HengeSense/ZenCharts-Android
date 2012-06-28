package zencharts.engine;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLUtils;

public class Symbol  implements Serializable {

	private int lineColor;

	private FloatBuffer vertexBuffer; // buffer holding the vertices
	private float vertices[];

	private FloatBuffer textureBuffer; // buffer holding the texture coordinates
	private float texture[] = {
			// Mapping coordinates for the vertices
			0.0f, 1.0f, // top left (V2)
			0.0f, 0.0f, // bottom left (V1)
			1.0f, 1.0f, // top right (V4)
			1.0f, 0.0f // bottom right (V3)
	};

	/** The texture pointer */
	private int[] textures = { -1 };

	public Symbol(float x, float y, float width, float height)
	{
		vertices = new float[] {
				x - (width / 2), y - (height / 2), 0.0f, // V1 - bottom left
				x - (width / 2), y + (height / 2), 0.0f, // V2 - top left
				x + (width / 2), y - (height / 2), 0.0f, // V3 - bottom right
				x + (width / 2), y + (height / 2), 0.0f }; // V4 - top right

		// a float has 4 bytes so we allocate for each coordinate 4 bytes
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());

		// allocates the memory from the byte buffer
		vertexBuffer = byteBuffer.asFloatBuffer();

		// fill the vertexBuffer with the vertices
		vertexBuffer.put(vertices);

		// set the cursor position to the beginning of the buffer
		vertexBuffer.position(0);

		byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		textureBuffer = byteBuffer.asFloatBuffer();
		textureBuffer.put(texture);
		textureBuffer.position(0);
	}

	/**
	 * Load the texture for the square
	 * 
	 * @param gl
	 * @param context
	 */
	public void loadGLTexture(GL10 gl, Context context, int resID, int color) {
		// loading texture
		lineColor = color;
		Bitmap bmpSource = BitmapFactory.decodeResource(context.getResources(), resID);
		Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setColor(color);

		if (bmpSource != null)
		{
			c.drawBitmap(bmpSource.extractAlpha(), null, new Rect(0, 0, 64, 64), paint);
			bmpSource.recycle();
		}

		// generate one texture pointer
		gl.glGenTextures(1, textures, 0);
		// ...and bind it to our array
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

		// create nearest filtered texture
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		// Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
		// gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
		// GL10.GL_REPEAT);
		// gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
		// GL10.GL_REPEAT);

		// Use Android GLUtils to specify a two-dimensional texture image from
		// our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

		// Clean up
		bitmap.recycle();
	}

	/** The draw method for the square with the GL context */
	public void draw(GL10 gl, float zoomLevel, float xZoomLevel) {

		gl.glEnable(GL10.GL_TEXTURE_2D);
		// gl.glEnable(GL10.GL_BLEND);
		gl.glColor4f(1f, 1f, 1f, 1f);

		// bind the previously generated texture
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

		// Point to our buffers
		// gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		// Set the face rotation
		gl.glFrontFace(GL10.GL_CW);

		// Point to our vertex buffer
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);

		// Draw the vertices as triangle strip
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);

		// Disable the client state before leaving
		// gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		gl.glDisable(GL10.GL_TEXTURE_2D);
		// gl.glDisable(GL10.GL_BLEND);
	}
}