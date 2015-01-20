/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import paulscode.android.mupen64plusae.persistent.AppData;
import android.annotation.TargetApi;
import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;

/**
 * OpenGL ES 2.0 example activity, copied with slight modification from
 * http://www.learnopengles.com/android-lesson-one-getting-started/
 */
public class OffsetFinderActivity extends Activity
{
    private GLSurfaceView mGLSurfaceView;
    
    public OffsetFinderActivity()
    {
    }
    
    @TargetApi( 8 )
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        mGLSurfaceView = new GLSurfaceView( this );
        if( AppData.IS_FROYO )
        {
            mGLSurfaceView.setEGLContextClientVersion( 2 );
            mGLSurfaceView.setRenderer( new Renderer() );
        }
        
        setContentView( mGLSurfaceView );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        mGLSurfaceView.onResume();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        mGLSurfaceView.onPause();
    }
    
    @TargetApi( 8 )
    private static class Renderer implements GLSurfaceView.Renderer
    {
        /**
         * Store the model matrix. This matrix is used to move models from object space (where each
         * model can be thought of being located at the center of the universe) to world space.
         */
        private float[] mModelMatrix = new float[16];
        
        /**
         * Store the view matrix. This can be thought of as our camera. This matrix transforms world
         * space to eye space; it positions things relative to our eye.
         */
        private float[] mViewMatrix = new float[16];
        
        /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
        private float[] mProjectionMatrix = new float[16];
        
        /**
         * Allocate storage for the final combined matrix. This will be passed into the shader
         * program.
         */
        private float[] mMVPMatrix = new float[16];
        
        /** Store our model data in a float buffer. */
        private final FloatBuffer mRedVertices;
        private final FloatBuffer mBlueVertices;
        private final FloatBuffer mGreenVertices;
        
        /** This will be used to pass in the transformation matrix. */
        private int mMVPMatrixHandle;
        
        /** This will be used to pass in model position information. */
        private int mPositionHandle;
        
        /** This will be used to pass in model color information. */
        private int mColorHandle;
        
        /** How many bytes per float. */
        private final int mBytesPerFloat = 4;
        
        /** How many elements per vertex. */
        private final int mStrideBytes = 7 * mBytesPerFloat;
        
        /** Offset of the position data. */
        private final int mPositionOffset = 0;
        
        /** Size of the position data in elements. */
        private final int mPositionDataSize = 3;
        
        /** Offset of the color data. */
        private final int mColorOffset = 3;
        
        /** Size of the color data in elements. */
        private final int mColorDataSize = 4;
        
        /**
         * Initialize the model data.
         */
        public Renderer()
        {
            final float a = 1;
            final float b = 1;//a * 1.01f;
            // @formatter:off
            final float[] redVerticesData = {
            //   X,  Y,  Z,   R, G, B, A,
                -a, -a,  a,   1, 0, 0, 1,
                -a, -a, -a,   1, 0, 0, 1,
                 a, -a,  a,   1, 0, 0, 1,
                 a, -a, -a,   1, 0, 0, 1,
                 a,  a,  a,   1, 0, 0, 1,
                 a,  a, -a,   1, 0, 0, 1,
                -a,  a,  a,   1, 0, 0, 1,
                -a,  a, -a,   1, 0, 0, 1,
                -a, -a,  a,   1, 0, 0, 1,
                -a, -a, -a,   1, 0, 0, 1,
                };
            final float[] blueVerticesData = {
            //   X,  Y,  Z,   R, G, B, A,
                -b,  b, -b,   0, 0, 1, 1,
                -b, -b, -b,   0, 0, 1, 1,
                -b,  b,  b,   0, 0, 1, 1,
                -b, -b,  b,   0, 0, 1, 1,
                 b,  b,  b,   0, 0, 1, 1,
                 b, -b,  b,   0, 0, 1, 1,
                 b,  b, -b,   0, 0, 1, 1,
                 b, -b, -b,   0, 0, 1, 1,
                -b,  b, -b,   0, 0, 1, 1,
                -b, -b, -b,   0, 0, 1, 1,
                };
            final float[] greenVerticesData = {
            //   X,  Y,  Z,   R, G, B, A,
                 a, -a, -a,   0, 1, 0, 1,
                -a, -a, -a,   0, 1, 0, 1,
                 a,  a, -a,   0, 1, 0, 1,
                -a,  a, -a,   0, 1, 0, 1,
                 a,  a,  a,   0, 1, 0, 1,
                -a,  a,  a,   0, 1, 0, 1,
                 a, -a,  a,   0, 1, 0, 1,
                -a, -a,  a,   0, 1, 0, 1,
                 a, -a, -a,   0, 1, 0, 1,
                -a, -a, -a,   0, 1, 0, 1,
                };
            // @formatter:on
            
            
            // Initialize the buffers.
            mRedVertices = ByteBuffer
                    .allocateDirect( redVerticesData.length * mBytesPerFloat )
                    .order( ByteOrder.nativeOrder() ).asFloatBuffer();
            mBlueVertices = ByteBuffer
                    .allocateDirect( blueVerticesData.length * mBytesPerFloat )
                    .order( ByteOrder.nativeOrder() ).asFloatBuffer();
            mGreenVertices = ByteBuffer
                    .allocateDirect( greenVerticesData.length * mBytesPerFloat )
                    .order( ByteOrder.nativeOrder() ).asFloatBuffer();
            
            mRedVertices.put( redVerticesData ).position( 0 );
            mBlueVertices.put( blueVerticesData ).position( 0 );
            mGreenVertices.put( greenVerticesData ).position( 0 );
        }
        
        @Override
        public void onSurfaceCreated( GL10 glUnused, EGLConfig config )
        {
            // Set the background clear color to black.
            GLES20.glClearColor( 0, 0, 0, 0 );
            
            // Enable cull face.
            GLES20.glEnable( GLES20.GL_CULL_FACE );
            //GLES20.glCullFace( GLES20.GL_CCW );
            
            // Enable depth test.
            GLES20.glEnable( GLES20.GL_DEPTH_TEST );
            GLES20.glDepthFunc( GLES20.GL_LEQUAL );
            GLES20.glDepthMask( true );
            
            // Position the eye behind the origin.
            final float eyeX = 0.0f;
            final float eyeY = 0.0f;
            final float eyeZ = 5.0f;
            
            // We are looking toward the distance
            final float lookX = 0.0f;
            final float lookY = 0.0f;
            final float lookZ = -5.0f;
            
            // Set our up vector. This is where our head would be pointing were we holding the
            // camera.
            final float upX = 0.0f;
            final float upY = 1.0f;
            final float upZ = 0.0f;
            
            // Set the view matrix. This matrix can be said to represent the camera position.
            // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
            // view matrix. In OpenGL 2, we can keep track of these matrices separately if we
            // choose.
            Matrix.setLookAtM( mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ );
            
            //@formatter:off
            final String vertexShader =
                    "uniform mat4 u_MVPMatrix;      \n"     // A constant representing the combined model/view/projection matrix.
                 
                  + "attribute vec4 a_Position;     \n"     // Per-vertex position information we will pass in.
                  + "attribute vec4 a_Color;        \n"     // Per-vertex color information we will pass in.
                 
                  + "varying vec4 v_Color;          \n"     // This will be passed into the fragment shader.
                 
                  + "void main()                    \n"     // The entry point for our vertex shader.
                  + "{                              \n"
                  + "   v_Color = a_Color;          \n"     // Pass the color through to the fragment shader.
                                                            // It will be interpolated across the triangle.
                  + "   gl_Position = u_MVPMatrix   \n"     // gl_Position is a special variable used to store the final position.
                  + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
                  + "}                              \n";    // normalized screen coordinates.
            
            final String fragmentShader =
                    "precision mediump float;       \n"     // Set the default precision to medium. We don't need as high of a
                                                            // precision in the fragment shader.
                  + "varying vec4 v_Color;          \n"     // This is the color from the vertex shader interpolated across the
                                                            // triangle per fragment.
                  + "void main()                    \n"     // The entry point for our fragment shader.
                  + "{                              \n"
                  + "   gl_FragColor = v_Color;     \n"     // Pass the color directly through the pipeline.
                  + "}                              \n";
            //@formatter:on
            
            // Load in the vertex shader.
            int vertexShaderHandle = GLES20.glCreateShader( GLES20.GL_VERTEX_SHADER );
            
            if( vertexShaderHandle != 0 )
            {
                // Pass in the shader source.
                GLES20.glShaderSource( vertexShaderHandle, vertexShader );
                
                // Compile the shader.
                GLES20.glCompileShader( vertexShaderHandle );
                
                // Get the compilation status.
                final int[] compileStatus = new int[1];
                GLES20.glGetShaderiv( vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus,
                        0 );
                
                // If the compilation failed, delete the shader.
                if( compileStatus[0] == 0 )
                {
                    GLES20.glDeleteShader( vertexShaderHandle );
                    vertexShaderHandle = 0;
                }
            }
            
            if( vertexShaderHandle == 0 )
            {
                throw new RuntimeException( "Error creating vertex shader." );
            }
            
            // Load in the fragment shader shader.
            int fragmentShaderHandle = GLES20.glCreateShader( GLES20.GL_FRAGMENT_SHADER );
            
            if( fragmentShaderHandle != 0 )
            {
                // Pass in the shader source.
                GLES20.glShaderSource( fragmentShaderHandle, fragmentShader );
                
                // Compile the shader.
                GLES20.glCompileShader( fragmentShaderHandle );
                
                // Get the compilation status.
                final int[] compileStatus = new int[1];
                GLES20.glGetShaderiv( fragmentShaderHandle, GLES20.GL_COMPILE_STATUS,
                        compileStatus, 0 );
                
                // If the compilation failed, delete the shader.
                if( compileStatus[0] == 0 )
                {
                    GLES20.glDeleteShader( fragmentShaderHandle );
                    fragmentShaderHandle = 0;
                }
            }
            
            if( fragmentShaderHandle == 0 )
            {
                throw new RuntimeException( "Error creating fragment shader." );
            }
            
            // Create a program object and store the handle to it.
            int programHandle = GLES20.glCreateProgram();
            
            if( programHandle != 0 )
            {
                // Bind the vertex shader to the program.
                GLES20.glAttachShader( programHandle, vertexShaderHandle );
                
                // Bind the fragment shader to the program.
                GLES20.glAttachShader( programHandle, fragmentShaderHandle );
                
                // Bind attributes
                GLES20.glBindAttribLocation( programHandle, 0, "a_Position" );
                GLES20.glBindAttribLocation( programHandle, 1, "a_Color" );
                
                // Link the two shaders together into a program.
                GLES20.glLinkProgram( programHandle );
                
                // Get the link status.
                final int[] linkStatus = new int[1];
                GLES20.glGetProgramiv( programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0 );
                
                // If the link failed, delete the program.
                if( linkStatus[0] == 0 )
                {
                    GLES20.glDeleteProgram( programHandle );
                    programHandle = 0;
                }
            }
            
            if( programHandle == 0 )
            {
                throw new RuntimeException( "Error creating program." );
            }
            
            // Set program handles. These will later be used to pass in values to the program.
            mMVPMatrixHandle = GLES20.glGetUniformLocation( programHandle, "u_MVPMatrix" );
            mPositionHandle = GLES20.glGetAttribLocation( programHandle, "a_Position" );
            mColorHandle = GLES20.glGetAttribLocation( programHandle, "a_Color" );
            
            // Tell OpenGL to use this program when rendering.
            GLES20.glUseProgram( programHandle );
        }
        
        @Override
        public void onSurfaceChanged( GL10 glUnused, int width, int height )
        {
            // Set the OpenGL viewport to the same size as the surface.
            GLES20.glViewport( 0, 0, width, height );
            
            // Create a new perspective projection matrix. The height will stay the same
            // while the width will vary as per aspect ratio.
            final float ratio = (float) width / height;
            final float left = -ratio;
            final float right = ratio;
            final float bottom = -1.0f;
            final float top = 1.0f;
            final float near = 1.0f;
            final float far = 10.0f;
            
            Matrix.frustumM( mProjectionMatrix, 0, left, right, bottom, top, near, far );
        }
        
        @Override
        public void onDrawFrame( GL10 glUnused )
        {
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT );
            
            // Do a complete rotation every 10 seconds.
            long time = SystemClock.uptimeMillis() % 10000L;
            float angleInDegrees = ( 360.0f / 10000.0f ) * ( (int) time );
            
            // Draw the red polygons.
            Matrix.setIdentityM( mModelMatrix, 0 );
            Matrix.rotateM( mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 1.0f );
            drawTriangleStrip( mRedVertices );
            
            // Draw the blue polygons with offset.
            GLES20.glPolygonOffset( 1.5f, 1.5f );
            GLES20.glEnable( GLES20.GL_POLYGON_OFFSET_FILL );
            drawTriangleStrip( mBlueVertices );
            GLES20.glDisable( GLES20.GL_POLYGON_OFFSET_FILL );
            
            // Draw the green polygons with offset.
            GLES20.glPolygonOffset( -1.5f, -1.5f );
            GLES20.glEnable( GLES20.GL_POLYGON_OFFSET_FILL );
            drawTriangleStrip( mGreenVertices );
            GLES20.glDisable( GLES20.GL_POLYGON_OFFSET_FILL );
        }
        
        /**
         * Draws a triangle strip from the given vertex data.
         * 
         * @param aTriangleBuffer The buffer containing the vertex data.
         */
        private void drawTriangleStrip( final FloatBuffer aTriangleBuffer )
        {
            // Pass in the position information
            aTriangleBuffer.position( mPositionOffset );
            GLES20.glVertexAttribPointer( mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT,
                    false, mStrideBytes, aTriangleBuffer );
            
            GLES20.glEnableVertexAttribArray( mPositionHandle );
            
            // Pass in the color information
            aTriangleBuffer.position( mColorOffset );
            GLES20.glVertexAttribPointer( mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                    mStrideBytes, aTriangleBuffer );
            
            GLES20.glEnableVertexAttribArray( mColorHandle );
            
            // This multiplies the view matrix by the model matrix, and stores the result in the MVP
            // matrix (which currently contains model * view).
            Matrix.multiplyMM( mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0 );
            
            // This multiplies the modelview matrix by the projection matrix, and stores the result
            // in the MVP matrix (which now contains model * view * projection).
            Matrix.multiplyMM( mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0 );
            
            GLES20.glUniformMatrix4fv( mMVPMatrixHandle, 1, false, mMVPMatrix, 0 );
            GLES20.glDrawArrays( GLES20.GL_TRIANGLE_STRIP, 0, 10 );
        }
    }
}
