/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.froemling.bsremote;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

public class GLRenderer implements GLSurfaceView.Renderer {

  private Context _context;

  GLRenderer(Context c) {
    _context = c;
  }

  // Get a new texture id:
  private static int newTextureID() {
    int[] temp = new int[1];
    GLES20.glGenTextures(1, temp, 0);
    return temp[0];
  }

  // Will load a texture out of a drawable resource file, and return an
  // OpenGL texture ID:
  private int loadTexture(int resource) {

    // In which ID will we be storing this texture?
    int id = newTextureID();

    // We need to flip the textures vertically:
    android.graphics.Matrix flip = new android.graphics.Matrix();
    flip.postScale(1f, -1f);

    // This will tell the BitmapFactory to not scale based on the device's
    // pixel density:
    // (Thanks to Matthew Marshall for this bit)
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inScaled = false;

    // Load up, and flip the texture:
    Bitmap temp =
        BitmapFactory.decodeResource(_context.getResources(), resource, opts);
    Bitmap bmp = Bitmap
        .createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), flip,
            true);
    temp.recycle();

    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);

    // Set all of our texture parameters:
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
        GLES20.GL_LINEAR_MIPMAP_LINEAR);
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
        GLES20.GL_LINEAR);
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
        GLES20.GL_REPEAT);
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
        GLES20.GL_REPEAT);

    // Generate, and load up all of the mipmaps:
    for (int level = 0, height = bmp.getHeight(), width =
         bmp.getWidth(); true; level++) {
      // Push the bitmap onto the GPU:
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, level, bmp, 0);

      // We need to stop when the texture is 1x1:
      if (height == 1 && width == 1) {
        break;
      }

      // Resize, and let's go again:
      width >>= 1;
      height >>= 1;
      if (width < 1) {
        width = 1;
      }
      if (height < 1) {
        height = 1;
      }

      Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, width, height, true);
      bmp.recycle();
      bmp = bmp2;
    }
    bmp.recycle();

    return id;
  }

  //	public static void printMat(float[] matrix,String name){
  //        Log.v(TAG,name+ ":\n   ["+matrix[0]+", "+matrix[1]+",
  // "+matrix[2]+", "+matrix[3]+"]"
  //        				+"\n   ["+matrix[4]+", "+matrix[5]+",
  // "+matrix[6]+", "+matrix[7]+"]"
  //						+"\n   ["+matrix[8]+", "+matrix[9]+",
  // "+matrix[10]+", "+matrix[11]+"]"
  //						+"\n   ["+matrix[12]+", "+matrix[13]+",
  // "+matrix[14]+", "+matrix[15]+"]");
  //
  //
  //	}

  private static final String TAG = "GL";
  private Square mSquare;
  private SquareTex mSquareTex;

  private int _bgTex;
  private int _buttonBombTex;
  private int _buttonBombPressedTex;
  private int _buttonJumpTex;
  private int _buttonJumpPressedTex;
  private int _buttonPunchTex;
  private int _buttonPunchPressedTex;
  private int _buttonThrowTex;
  private int _buttonThrowPressedTex;
  private int _centerTex;
  private int _thumbTex;
  private int _thumbPressedTex;
  private int _buttonLeaveTex;
  private int _buttonSettingsTex;
  private int _buttonStartTex;

  private final float[] mMVPMatrix = new float[16];
  private final float[] mProjMatrix = new float[16];
  private final float[] mVMatrix = new float[16];

  volatile float quitButtonX;
  volatile float quitButtonY;
  volatile float quitButtonWidth;
  volatile float quitButtonHeight;
  volatile boolean quitButtonPressed;

  volatile float prefsButtonX;
  volatile float prefsButtonY;
  volatile float prefsButtonWidth;
  volatile float prefsButtonHeight;
  volatile boolean prefsButtonPressed;

  volatile float startButtonX;
  volatile float startButtonY;
  volatile float startButtonWidth;
  volatile float startButtonHeight;
  volatile boolean startButtonPressed;

  volatile float bombButtonX;
  volatile float bombButtonY;
  volatile float bombButtonWidth;
  volatile float bombButtonHeight;

  volatile float punchButtonX;
  volatile float punchButtonY;
  volatile float punchButtonWidth;
  volatile float punchButtonHeight;

  volatile float throwButtonX;
  volatile float throwButtonY;
  volatile float throwButtonWidth;
  volatile float throwButtonHeight;

  volatile float jumpButtonX;
  volatile float jumpButtonY;
  volatile float jumpButtonWidth;
  volatile float jumpButtonHeight;

  volatile float joystickCenterX;
  volatile float joystickCenterY;
  volatile float joystickWidth;
  volatile float joystickHeight;

  volatile float joystickX;
  volatile float joystickY;

  volatile boolean jumpPressed;
  volatile boolean punchPressed;
  volatile boolean throwPressed;
  volatile boolean bombPressed;
  volatile boolean thumbPressed;

  private float _ratio = 1.0f;

  @Override
  public void onSurfaceCreated(GL10 unused, EGLConfig config) {

    // Set the background frame color
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

    new Triangle();
    mSquare = new Square();
    mSquareTex = new SquareTex();

    // load our textures
    _bgTex = loadTexture(R.drawable.controllerbg);
    checkGlError("loadTexture");
    _buttonBombTex = loadTexture(R.drawable.button_bomb);
    checkGlError("loadTexture");
    _buttonBombPressedTex = loadTexture(R.drawable.button_bomb_pressed);
    checkGlError("loadTexture");
    _buttonJumpTex = loadTexture(R.drawable.button_jump);
    checkGlError("loadTexture");
    _buttonJumpPressedTex = loadTexture(R.drawable.button_jump_pressed);
    checkGlError("loadTexture");
    _buttonPunchTex = loadTexture(R.drawable.button_punch);
    checkGlError("loadTexture");
    _buttonPunchPressedTex = loadTexture(R.drawable.button_punch_pressed);
    checkGlError("loadTexture");
    _buttonThrowTex = loadTexture(R.drawable.button_throw);
    checkGlError("loadTexture");
    _buttonThrowPressedTex = loadTexture(R.drawable.button_throw_pressed);
    checkGlError("loadTexture");
    _centerTex = loadTexture(R.drawable.center);
    checkGlError("loadTexture");
    _thumbTex = loadTexture(R.drawable.thumb);
    checkGlError("loadTexture");
    _thumbPressedTex = loadTexture(R.drawable.thumb_pressed);
    checkGlError("loadTexture");
    _buttonStartTex = loadTexture(R.drawable.button_start);
    checkGlError("loadTexture");
    _buttonLeaveTex = loadTexture(R.drawable.button_leave);
    checkGlError("loadTexture");
    _buttonSettingsTex = loadTexture(R.drawable.button_settings);
    checkGlError("loadTexture");
  }

  private void _drawBG(float r, float g, float b, float a, int tex) {
    float[] m = new float[16];
    Matrix.setIdentityM(m, 0);
    Matrix.scaleM(m, 0, -2.0f, 2.0f, 1.0f);
    if (tex == -1) {
      mSquare.draw(m, r, g, b, a);
    } else {
      mSquareTex.draw(m, r, g, b, a, _bgTex);
    }
    checkGlError("draw");
  }

  private void _drawBox(float x, float y, float sx, float sy, float r, float g,
                        float b, float a, int tex) {
    // scale and translate
    float[] m = new float[16];
    Matrix.setIdentityM(m, 0);
    Matrix.scaleM(m, 0, 2.0f * sx, 2.0f * sy, 1.0f);
    m[3] += (1.0 - 2.0 * x);
    m[7] += (1.0 / _ratio - 2.0 * y);
    float[] m2 = new float[16];
    Matrix.multiplyMM(m2, 0, m, 0, mMVPMatrix, 0);
    if (tex == -1) {
      mSquare.draw(m2, r, g, b, a);
    } else {
      mSquareTex.draw(m2, r, g, b, a, tex);
    }
    checkGlError("draw");
  }

  @Override
  public void onDrawFrame(GL10 unused) {

    // Draw background color
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

    // Set the camera position (View matrix)
    Matrix.setLookAtM(mVMatrix, 0, 0, 0, -1, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

    // Calculate the projection and view transformation
    Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

    GLES20.glDisable(GLES20.GL_BLEND);
    _drawBG(1, 1, 1, 1, _bgTex);

    // actual graphics
    GLES20.glEnable(GLES20.GL_BLEND);
    GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    float bs = 2.8f;
    _drawBox(bombButtonX, bombButtonY, bombButtonWidth * bs,
        bombButtonHeight * bs, 1, 1, 1, 1.0f,
        bombPressed ? _buttonBombPressedTex : _buttonBombTex);
    _drawBox(punchButtonX, punchButtonY, punchButtonWidth * bs,
        punchButtonHeight * bs, 1, 1, 1, 1.0f,
        punchPressed ? _buttonPunchPressedTex : _buttonPunchTex);
    _drawBox(jumpButtonX, jumpButtonY, jumpButtonWidth * bs,
        jumpButtonHeight * bs, 1, 1, 1, 1.0f,
        jumpPressed ? _buttonJumpPressedTex : _buttonJumpTex);
    _drawBox(throwButtonX, throwButtonY, throwButtonWidth * bs,
        throwButtonHeight * bs, 1, 1, 1, 1.0f,
        throwPressed ? _buttonThrowPressedTex : _buttonThrowTex);

    float cs = 2.2f;
    _drawBox(joystickCenterX, joystickCenterY, joystickWidth * cs,
        joystickHeight * cs, 1, 1, 1, 1.0f, _centerTex);

    float ts = 0.9f;
    _drawBox(joystickX, joystickY, joystickWidth * ts, joystickHeight * ts, 1,
        1, 1, 1.0f, thumbPressed ? _thumbPressedTex : _thumbTex);

    float tbsx = 1.1f;
    float tbsy = 1.6f;
    float tboy = 0.15f * quitButtonHeight * tbsy;

    float b;
    b = quitButtonPressed ? 2.0f : 1.0f;
    _drawBox(quitButtonX, quitButtonY + tboy, quitButtonWidth * tbsx,
        quitButtonHeight * tbsy, b, b, b, 1.0f, _buttonLeaveTex);
    b = prefsButtonPressed ? 2.0f : 1.0f;
    _drawBox(prefsButtonX, prefsButtonY + tboy, prefsButtonWidth * tbsx,
        prefsButtonHeight * tbsy, b, b, b, 1.0f, _buttonSettingsTex);
    b = startButtonPressed ? 2.0f : 1.0f;
    _drawBox(startButtonX, startButtonY + tboy, startButtonWidth * tbsx,
        startButtonHeight * tbsy, b, b, b, 1.0f, _buttonStartTex);

  }

  @Override
  public void onSurfaceChanged(GL10 unused, int width, int height) {
    // Adjust the viewport based on geometry changes,
    // such as screen rotation
    GLES20.glViewport(0, 0, width, height);

    _ratio = (float) width / height;
    //Log.v(TAG,"SETTING RATIO "+_ratio);
    // this projection matrix is applied to object coordinates
    // in the onDrawFrame() method
    //Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    Matrix.orthoM(mProjMatrix, 0, -1, 1, -1 / _ratio, 1 / _ratio, -1, 1);
    //printMat(mProjMatrix,"ORTHO");
  }

  static int loadShader(int type, String shaderCode) {

    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
    int shader = GLES20.glCreateShader(type);

    // add the source code to the shader and compile it
    GLES20.glShaderSource(shader, shaderCode);
    GLES20.glCompileShader(shader);

    return shader;
  }

  /**
   * Utility method for debugging OpenGL calls. Provide the name of the call
   * just after making it:
   *
   * <pre>
   * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
   * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
   * <p>
   * If the operation is not successful, the check throws an error.
   *
   * @param glOperation - Name of the OpenGL call to check.
   */
  static void checkGlError(String glOperation) {
    int error;
    if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
      Log.e(TAG, glOperation + ": glError " + error);
      throw new RuntimeException(glOperation + ": glError " + error);
    }
  }
}

class Triangle {

  // number of coordinates per vertex in this array
  private static float triangleCoords[] = { // in counterclockwise order:
      0.0f, 0.622008459f, 0.0f,   // top
      -0.5f, -0.311004243f, 0.0f,   // bottom left
      0.5f, -0.311004243f, 0.0f    // bottom right
  };

  Triangle() {
    // initialize vertex byte buffer for shape coordinates
    ByteBuffer bb = ByteBuffer.allocateDirect(
        // (number of coordinate values * 4 bytes per float)
        triangleCoords.length * 4);
    // use the device hardware's native byte order
    bb.order(ByteOrder.nativeOrder());

    // create a floating point buffer from the ByteBuffer
    FloatBuffer vertexBuffer = bb.asFloatBuffer();
    // add the coordinates to the FloatBuffer
    vertexBuffer.put(triangleCoords);
    // set the buffer to read the first coordinate
    vertexBuffer.position(0);

    // prepare shaders and OpenGL program
    String vertexShaderCode = "uniform mat4 uMVPMatrix;" +

        "attribute vec4 vPosition;" + "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  gl_Position = vPosition * uMVPMatrix;" + "}";
    int vertexShader =
        GLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
    String fragmentShaderCode =
        "precision mediump float;" + "uniform vec4 " + "vColor;" + "void " +
            "main() {" + "  gl_FragColor = vColor;" + "}";
    int fragmentShader =
        GLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

    int mProgram = GLES20.glCreateProgram();
    GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader
    // to program
    GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment
    // shader to program
    GLES20.glLinkProgram(mProgram);                  // create OpenGL program
    // executables

  }
}

class Square {

  private final FloatBuffer vertexBuffer;
  private final ShortBuffer drawListBuffer;
  private final int mProgram;

  // number of coordinates per vertex in this array
  private static final int COORDS_PER_VERTEX = 3;
  private static float squareCoords[] = {-0.5f, 0.5f, 0.0f,   // top left
      -0.5f, -0.5f, 0.0f,   // bottom left
      0.5f, -0.5f, 0.0f,   // bottom right
      0.5f, 0.5f, 0.0f}; // top right

  private final short drawOrder[] = {0, 1, 2, 0, 2, 3}; // order to draw
  // vertices

  Square() {
    // initialize vertex byte buffer for shape coordinates
    ByteBuffer bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
        squareCoords.length * 4);
    bb.order(ByteOrder.nativeOrder());
    vertexBuffer = bb.asFloatBuffer();
    vertexBuffer.put(squareCoords);
    vertexBuffer.position(0);

    // initialize byte buffer for the draw list
    ByteBuffer dlb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 2 bytes per short)
        drawOrder.length * 2);
    dlb.order(ByteOrder.nativeOrder());
    drawListBuffer = dlb.asShortBuffer();
    drawListBuffer.put(drawOrder);
    drawListBuffer.position(0);

    // prepare shaders and OpenGL program
    String vertexShaderCode = "uniform mat4 uMVPMatrix;" +

        "attribute vec4 vPosition;" + "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  gl_Position = vPosition * uMVPMatrix;" + "}";
    int vertexShader =
        GLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
    String fragmentShaderCode =
        "precision mediump float;" + "uniform vec4 " + "vColor;" + "void " +
            "main() {" + "  gl_FragColor = vColor;" + "}";
    int fragmentShader =
        GLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

    mProgram = GLES20.glCreateProgram();             // create empty OpenGL
    // Program
    GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader
    // to program
    GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment
    // shader to program
    GLES20.glLinkProgram(mProgram);                  // create OpenGL program
    // executables
  }

  void draw(float[] mvpMatrix, float r, float g, float b, float a) {
    // Add program to OpenGL environment
    GLES20.glUseProgram(mProgram);

    // get handle to vertex shader's vPosition member
    int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

    // Enable a handle to the triangle vertices
    GLES20.glEnableVertexAttribArray(mPositionHandle);

    // Prepare the triangle coordinate data
    int vertexStride = COORDS_PER_VERTEX * 4;
    GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
        GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

    // get handle to fragment shader's vColor member
    int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

    // Set color for drawing the triangle
    float[] color = {r, g, b, a};

    GLES20.glUniform4fv(mColorHandle, 1, color, 0);

    // get handle to shape's transformation matrix
    int mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
    GLRenderer.checkGlError("glGetUniformLocation");

    // Apply the projection and view transformation
    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
    GLRenderer.checkGlError("glUniformMatrix4fv");

    // Draw the square
    GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
        GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

    // Disable vertex array
    GLES20.glDisableVertexAttribArray(mPositionHandle);
  }
}

class SquareTex {

  private final FloatBuffer vertexBuffer;
  private final FloatBuffer uvBuffer;

  private final ShortBuffer drawListBuffer;
  private final int mProgram;

  // number of coordinates per vertex in this array
  private static final int COORDS_PER_VERTEX = 3;
  private static final int COORDS_PER_UV = 2;

  private static float squareCoords[] = {-0.5f, 0.5f, 0.0f,   // top left
      -0.5f, -0.5f, 0.0f,   // bottom left
      0.5f, -0.5f, 0.0f,   // bottom right
      0.5f, 0.5f, 0.0f}; // top right

  private static float squareUVs[] =
      {1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};

  private final short drawOrder[] = {0, 1, 2, 0, 2, 3}; // order to draw
  // vertices

  SquareTex() {
    // initialize vertex byte buffer for shape coordinates
    ByteBuffer bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
        squareCoords.length * 4);
    bb.order(ByteOrder.nativeOrder());
    vertexBuffer = bb.asFloatBuffer();
    vertexBuffer.put(squareCoords);
    vertexBuffer.position(0);

    bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
        squareUVs.length * 4);
    bb.order(ByteOrder.nativeOrder());
    uvBuffer = bb.asFloatBuffer();
    uvBuffer.put(squareUVs);
    uvBuffer.position(0);

    // initialize byte buffer for the draw list
    ByteBuffer dlb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 2 bytes per short)
        drawOrder.length * 2);
    dlb.order(ByteOrder.nativeOrder());
    drawListBuffer = dlb.asShortBuffer();
    drawListBuffer.put(drawOrder);
    drawListBuffer.position(0);

    // prepare shaders and OpenGL program
    String vertexShaderCode =
        "uniform mat4 uMVPMatrix;" + "attribute vec4 " + "vPosition;" +
            "attribute vec2 uv;" + "varying vec2 vUV;\n" + "void main" + "() " +
            "{" +
            // the matrix must be included as a modifier of gl_Position
            "  gl_Position = vPosition * uMVPMatrix;" + "  vUV = uv;" + "}";
    int vertexShader =
        GLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
    String fragmentShaderCode =
        "precision mediump float;" + "uniform vec4 " + "vColor;" + "uniform " +
            "sampler2D tex;" + "varying mediump vec2 vUV;" + "void main() {" +
            "  gl_FragColor = vColor * (texture2D" + "(tex," + "vUV));" + "}";
    int fragmentShader =
        GLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

    mProgram = GLES20.glCreateProgram();             // create empty OpenGL
    // Program
    GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader
    // to program
    GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment
    // shader to program
    GLES20.glLinkProgram(mProgram);                  // create OpenGL program
    // executables
  }

  void draw(float[] mvpMatrix, float r, float g, float b, float a, int tex) {
    // Add program to OpenGL environment
    GLES20.glUseProgram(mProgram);

    // get handle to vertex shader's vPosition member
    int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

    // Enable a handle to the triangle vertices
    GLES20.glEnableVertexAttribArray(mPositionHandle);

    // Prepare the triangle coordinate data
    int vertexStride = COORDS_PER_VERTEX * 4;
    GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
        GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

    // get handle to vertex shader's vPosition member
    int mUVHandle = GLES20.glGetAttribLocation(mProgram, "uv");
    GLRenderer.checkGlError("glGetUniformLocation");

    // Enable a handle to the triangle vertices
    GLES20.glEnableVertexAttribArray(mUVHandle);

    // Prepare the triangle coordinate data
    int uvStride = COORDS_PER_UV * 4;
    GLES20
        .glVertexAttribPointer(mUVHandle, COORDS_PER_UV, GLES20.GL_FLOAT, false,
            uvStride, uvBuffer);

    // get handle to fragment shader's vColor member
    int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
    GLRenderer.checkGlError("glGetUniformLocation");

    // Set color for drawing the triangle
    float[] color = {r, g, b, a};
    GLES20.glUniform4fv(mColorHandle, 1, color, 0);

    int mTexHandle = GLES20.glGetUniformLocation(mProgram, "tex");
    GLRenderer.checkGlError("glGetUniformLocation");
    GLES20.glUniform1i(mTexHandle, 0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
    GLRenderer.checkGlError("glBindTexture");

    // get handle to shape's transformation matrix
    int mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
    GLRenderer.checkGlError("glGetUniformLocation");
    // Apply the projection and view transformation
    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
    GLRenderer.checkGlError("glUniformMatrix4fv");

    // Draw the square
    GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
        GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

    // Disable vertex array
    GLES20.glDisableVertexAttribArray(mPositionHandle);
  }
}

