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
 * Authors: Paul Lamb, littleguy77
 */
package paulscode.android.mupen64plusae.jni;

import paulscode.android.mupen64plusae.CoreInterface;
import paulscode.android.mupen64plusae.util.SafeMethods;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Call-ins made from the native SDL2 library to Java. The function names are defined by SDL2 and
 * should not be changed.
 * 
 * @see jni/SDL2/src/core/android/SDL_android.cpp
 * @see CoreInterface
 */
public class NativeSDL extends CoreInterface
{
    /**
     * Creates a GL context for SDL2. If the GL context creation fails the first time, this method
     * will fall back to using legacy GL context creation, ignoring the specified configSpec, and
     * attempt to make a valid context.
     * 
     * @param majorVersion The major GL version number.
     * @param minorVersion The minor GL version number.
     * @param configSpec   The configuration to use.
     * 
     * @return True if the context was able to be created. False if not.
     * @see jni/SDL2/src/core/android/SDL_android.cpp
     */
    public static boolean createGLContext( int majorVersion, int minorVersion, int[] configSpec )
    {
        return sSurface.createGLContext( majorVersion, minorVersion, configSpec );
    }
    
    /**
     * Destroys the GL context for SDL2. If the surface is already destroyed this is a no-op.
     * 
     * @see jni/SDL2/src/core/android/SDL_android.cpp
     */
    public static void deleteGLContext()
    {
        sSurface.deleteGLContext();
    }
    
    /**
     * Swaps the GL buffers of the GameSurface in use.
     * 
     * @see jni/SDL2/src/core/android/SDL_android.cpp
     */
    public static void flipBuffers()
    {
        sSurface.flipBuffers();
        
        // Update frame rate info
        if( sFpsRecalcPeriod > 0 && sFpsListener != null )
        {
            sFrameCount++;
            if( sFrameCount >= sFpsRecalcPeriod )
            {
                long currentTime = System.currentTimeMillis();
                float fFPS = ( (float) sFrameCount / (float) ( currentTime - sLastFpsTime ) ) * 1000.0f;
                sFpsListener.onFpsChanged( Math.round( fFPS ) );
                sFrameCount = 0;
                sLastFpsTime = currentTime;
            }
        }
    }
    
    /**
     * Initializes the audio subsystem. Calling this on an audio subsystem that is already
     * initialized is a no-op.
     * 
     * @param sampleRate    The sample rate for playback in hertz.
     * @param is16Bit       Whether or not the audio is 16 bits per sample.
     * @param isStereo      Whether or not the audio is stereo or mono.
     * @param desiredFrames The desired frames per sample.
     * 
     * @return 0 on success, -1 if audio track is invalid.
     * @see jni/SDL2/src/core/android/SDL_android.cpp
     */
    public static int audioInit( int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames )
    {
        if( sAudioTrack == null )
        {
            // Audio configuration
            int channelConfig = isStereo ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
            int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
            int frameSize = ( isStereo ? 2 : 1 ) * ( is16Bit ? 2 : 1 );
            
            // Let the user pick a larger buffer if they really want -- but ye gods they probably
            // shouldn't, the minimums are horrifyingly high latency already
            int minBufSize = AudioTrack.getMinBufferSize( sampleRate, channelConfig, audioFormat );
            int defaultFrames = ( minBufSize + frameSize - 1 ) / frameSize;
            desiredFrames = Math.max( desiredFrames, defaultFrames );
            
            sAudioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, desiredFrames
                    * frameSize, AudioTrack.MODE_STREAM );
            
            // Instantiating AudioTrack can "succeed" without an exception and the track may still be invalid.
            // Ref: http://goo.gl/jPX4Al
            // Ref: http://developer.android.com/reference/android/media/AudioTrack.html#getState()
            
            if( sAudioTrack.getState() != AudioTrack.STATE_INITIALIZED )
            {
                Log.e( "CoreInterfaceNative", "Failed during initialization of audio track" );
                sAudioTrack = null;
                return -1;
            }
            
            sAudioTrack.play();
        }
        return 0;
    }
    
    /**
     * Writes audio data into a given short buffer.
     * 
     * @param buffer The short array which the audio data will be written to.
     * @see jni/SDL2/src/core/android/SDL_android.cpp
     */
    public static void audioWriteShortBuffer( short[] buffer )
    {
        for( int i = 0; i < buffer.length; )
        {
            int result = sAudioTrack.write( buffer, i, buffer.length - i );
            if( result > 0 )
            {
                i += result;
            }
            else if( result == 0 )
            {
                SafeMethods.sleep( 1 );
            }
            else
            {
                Log.w( "CoreInterfaceNative", "SDL Audio: Error returned from write(short[])" );
                return;
            }
        }
    }
    
    /**
     * Writes audio data into a given byte buffer.
     * 
     * @param buffer The byte array which the audio data will be written to.
     * @see jni/SDL2/src/core/android/SDL_android.cpp
     */
    public static void audioWriteByteBuffer( byte[] buffer )
    {
        for( int i = 0; i < buffer.length; )
        {
            int result = sAudioTrack.write( buffer, i, buffer.length - i );
            if( result > 0 )
            {
                i += result;
            }
            else if( result == 0 )
            {
                SafeMethods.sleep( 1 );
            }
            else
            {
                Log.w( "CoreInterfaceNative", "SDL Audio: Error returned from write(byte[])" );
                return;
            }
        }
    }
    
    /**
     * Shuts down the audio thread. Calling this on a thread that is already shut down is a no-op.
     * 
     * @see jni/SDL2/src/core/android/SDL_android.cpp
     */
    public static void audioQuit()
    {
        if( sAudioTrack != null )
        {
            sAudioTrack.stop();
            sAudioTrack.release();
            sAudioTrack = null;
        }
    }
    
    /**
     * No-op implementation of SDL2 interface.
     * 
     * @see jni/SDL2/src/core/android/SDL_android.cpp
     */
    public static boolean setActivityTitle( String title )
    {
        return true;
    }
    
    /**
     * No-op implementation of SDL2 interface.
     * 
     * @see jni/SDL2/src/core/android/SDL_android.cpp
     */
    public static boolean sendMessage( int command, int param )
    {
        return true;
    }
}
