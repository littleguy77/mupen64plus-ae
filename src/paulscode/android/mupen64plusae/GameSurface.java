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
 * Authors: Paul Lamb, littleguy77, Gillou68310
 */
package paulscode.android.mupen64plusae;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Represents a graphical area of memory that can be drawn to.
 */
public class GameSurface extends SurfaceView implements SurfaceHolder.Callback
{
    // Internal EGL objects
    private EGLContext  mEGLContext;
    private EGLSurface mEGLSurface;
    private EGLDisplay mEGLDisplay;
    private EGLConfig   mEGLConfig;
    private int mGLMajor;
    
    private boolean mIsSurfaceReady;
    
    // Startup    
    public GameSurface( Context context, AttributeSet attribs )
    {
        super( context, attribs );
        getHolder().addCallback( this );
        mEGLSurface = EGL10.EGL_NO_SURFACE;
        mEGLContext = EGL10.EGL_NO_CONTEXT;
        mIsSurfaceReady = false;
    }
    
    // Called when we have a valid drawing surface
    @SuppressWarnings("deprecation")
    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        Log.v("SDL", "surfaceCreated()");
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
        // Set mIsSurfaceReady to 'true' *before* any call to handleResume
        mIsSurfaceReady = true;
    }
    
    // Called when we lose the surface
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        Log.v("SDL", "surfaceDestroyed()");
        // Call this *before* setting mIsSurfaceReady to 'false'
        CoreInterface.pauseEmulator(false);
        mIsSurfaceReady = false;
    
        /* We have to clear the current context and destroy the egl surface here
         * Otherwise there's BAD_NATIVE_WINDOW errors coming from eglCreateWindowSurface on resume
         * Ref: http://stackoverflow.com/questions/8762589/eglcreatewindowsurface-on-ics-and-switching-from-2d-to-3d
         */
        
        EGL10 egl = (EGL10)EGLContext.getEGL();
        egl.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(mEGLDisplay, mEGLSurface);
        mEGLSurface = EGL10.EGL_NO_SURFACE;
    }
    
    // Called when the surface is resized
    @SuppressWarnings("deprecation")
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        Log.v("SDL", "surfaceChanged()");
    
        int sdlFormat = 0x15151002; // SDL_PIXELFORMAT_RGB565 by default
        switch (format)
        {
        case PixelFormat.A_8:
            Log.v("SDL", "pixel format A_8");
            break;
        case PixelFormat.LA_88:
            Log.v("SDL", "pixel format LA_88");
            break;
        case PixelFormat.L_8:
            Log.v("SDL", "pixel format L_8");
            break;
        case PixelFormat.RGBA_4444:
            Log.v("SDL", "pixel format RGBA_4444");
            sdlFormat = 0x15421002; // SDL_PIXELFORMAT_RGBA4444
            break;
        case PixelFormat.RGBA_5551:
            Log.v("SDL", "pixel format RGBA_5551");
            sdlFormat = 0x15441002; // SDL_PIXELFORMAT_RGBA5551
            break;
        case PixelFormat.RGBA_8888:
            Log.v("SDL", "pixel format RGBA_8888");
            sdlFormat = 0x16462004; // SDL_PIXELFORMAT_RGBA8888
            break;
        case PixelFormat.RGBX_8888:
            Log.v("SDL", "pixel format RGBX_8888");
            sdlFormat = 0x16261804; // SDL_PIXELFORMAT_RGBX8888
            break;
        case PixelFormat.RGB_332:
            Log.v("SDL", "pixel format RGB_332");
            sdlFormat = 0x14110801; // SDL_PIXELFORMAT_RGB332
            break;
        case PixelFormat.RGB_565:
            Log.v("SDL", "pixel format RGB_565");
            sdlFormat = 0x15151002; // SDL_PIXELFORMAT_RGB565
            break;
        case PixelFormat.RGB_888:
            Log.v("SDL", "pixel format RGB_888");
            // Not sure this is right, maybe SDL_PIXELFORMAT_RGB24 instead?
            sdlFormat = 0x16161804; // SDL_PIXELFORMAT_RGB888
            break;
        default:
            Log.v("SDL", "pixel format unknown " + format);
            break;
        }
    
        CoreInterface.onResize( sdlFormat, width, height );
        Log.v("SDL", "Window size:" + width + "x" + height);
    
        // Set mIsSurfaceReady to 'true' *before* making a call to handleResume
        mIsSurfaceReady = true;
        
        CoreInterface.startupEmulator();
    }
    
    // unused
    @Override
    public void onDraw(Canvas canvas) {}
    
    public void deleteGLContext()
    {
        if (mEGLDisplay != null && mEGLContext != EGL10.EGL_NO_CONTEXT)
        {
            EGL10 egl = (EGL10)EGLContext.getEGL();
            egl.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            egl.eglDestroyContext(mEGLDisplay, mEGLContext);
            mEGLContext = EGL10.EGL_NO_CONTEXT;
    
            if (mEGLSurface != EGL10.EGL_NO_SURFACE)
            {
                egl.eglDestroySurface(mEGLDisplay, mEGLSurface);
                mEGLSurface = EGL10.EGL_NO_SURFACE;
            }
        }
    }
    
    // EGL functions
    public boolean createGLContext(int majorVersion, int minorVersion, int[] attribs)
    {
        try
        {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            
            if (mEGLDisplay == null)
            {
                mEGLDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
                int[] version = new int[2];
                egl.eglInitialize(mEGLDisplay, version);
            }
            
            if (mEGLDisplay != null && mEGLContext == EGL10.EGL_NO_CONTEXT)
            {
                // No current GL context exists, we will create a new one.
                Log.v("SDL", "Starting up OpenGL ES " + majorVersion + "." + minorVersion);
                EGLConfig[] configs = new EGLConfig[128];
                int[] num_config = new int[1];
                if (!egl.eglChooseConfig(mEGLDisplay, attribs, configs, 1, num_config) || num_config[0] == 0)
                {
                    Log.e("SDL", "No EGL config available");
                    return false;
                }
            
                EGLConfig config = null;
                int bestdiff = -1, bitdiff;
                int[] value = new int[1];
                
                // eglChooseConfig returns a number of configurations that match or exceed the requested attribs.
                // From those, we select the one that matches our requirements more closely
                Log.v("SDL", "Got " + num_config[0] + " valid modes from egl");
                for(int i = 0; i < num_config[0]; i++)
                {
                    bitdiff = 0;
                    // Go through some of the attributes and compute the bit difference between what we want and what we get.
                    for (int j = 0; ; j += 2)
                    {
                        if (attribs[j] == EGL10.EGL_NONE)
                            break;
                        
                        if (attribs[j+1] != EGL10.EGL_DONT_CARE && (attribs[j] == EGL10.EGL_RED_SIZE ||
                            attribs[j] == EGL10.EGL_GREEN_SIZE ||
                            attribs[j] == EGL10.EGL_BLUE_SIZE ||
                            attribs[j] == EGL10.EGL_ALPHA_SIZE ||
                            attribs[j] == EGL10.EGL_DEPTH_SIZE ||
                            attribs[j] == EGL10.EGL_STENCIL_SIZE))
                        {
                            egl.eglGetConfigAttrib(mEGLDisplay, configs[i], attribs[j], value);
                            bitdiff += value[0] - attribs[j + 1];// value is always >= attrib
                        }
                    }
                    
                    if (bitdiff < bestdiff || bestdiff == -1)
                    {
                        config = configs[i];
                        bestdiff = bitdiff;
                    }
                    
                    if (bitdiff == 0)
                        break; // we found an exact match!
                }
                
                Log.d("SDL", "Selected mode with a total bit difference of " + bestdiff);

                mEGLConfig = config;
                mGLMajor = majorVersion;
            }
            
            return createEGLSurface();
            
        } 
        catch(Exception e)
        {
            Log.v("SDL", e + "");
            for (StackTraceElement s : e.getStackTrace())
            {
                Log.v("SDL", s.toString());
            }
            return false;
        }
    }
            
    public boolean createEGLContext()
    {
        EGL10 egl = (EGL10)EGLContext.getEGL();
        int EGL_CONTEXT_CLIENT_VERSION=0x3098;
        int contextAttrs[] = new int[] { EGL_CONTEXT_CLIENT_VERSION, mGLMajor, EGL10.EGL_NONE };
        mEGLContext = egl.eglCreateContext(mEGLDisplay, mEGLConfig, EGL10.EGL_NO_CONTEXT, contextAttrs);
        if (mEGLContext == EGL10.EGL_NO_CONTEXT)
        {
            Log.e("SDL", "Couldn't create context");
            return false;
        }
        return true;
    }

    public boolean createEGLSurface()
    {
        if (mEGLDisplay != null && mEGLConfig != null)
        {
            EGL10 egl = (EGL10)EGLContext.getEGL();
            if (mEGLContext == EGL10.EGL_NO_CONTEXT)
                createEGLContext();
            
            if (mEGLSurface == EGL10.EGL_NO_SURFACE)
            {
                Log.v("SDL", "Creating new EGL Surface");
                mEGLSurface = egl.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, this, null);
                if (mEGLSurface == EGL10.EGL_NO_SURFACE)
                {
                    Log.e("SDL", "Couldn't create surface");
                    return false;
                }
            }
            else 
                Log.v("SDL", "EGL Surface remains valid");
            
            if (egl.eglGetCurrentContext() != mEGLContext)
            {
                if (!egl.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext))
                {
                    Log.e("SDL", "Old EGL Context doesnt work, trying with a new one");
                    // TODO: Notify the user via a message that the old context could not be restored, and that textures need to be manually restored.
                    createEGLContext();
                    if (!egl.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext))
                    {
                        Log.e("SDL", "Failed making EGL Context current");
                        return false;
                    }
                }
                else
                    Log.v("SDL", "EGL Context made current");
            }
            else
                Log.v("SDL", "EGL Context remains current");

            return true;
        }
        else
        {
            Log.e("SDL", "Surface creation failed, display = " + mEGLDisplay + ", config = " + mEGLConfig);
            return false;
        }
    }
    
    // EGL buffer flip
    public void flipBuffers()
    {
        try
        {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            
            //egl.eglWaitNative(EGL10.EGL_CORE_NATIVE_ENGINE, null);

            // drawing here

            //egl.eglWaitGL();

            egl.eglSwapBuffers( mEGLDisplay, mEGLSurface );
        }
        catch(Exception e)
        {
            Log.v("SDL", "flipEGL(): " + e);
            for (StackTraceElement s : e.getStackTrace())
            {
                Log.v("SDL", s.toString());
            }
        }
    }
    
    public boolean isSurfaceReady() 
    {
        return mIsSurfaceReady;
    }
}
