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
package paulscode.android.mupen64plusae.profile;

import java.io.File;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.PrefUtil;
import paulscode.android.mupen64plusae.util.TaskHandler;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;

public class EmulationProfileActivity extends ProfileActivity
{
    // These constants must match the keys found in preferences_emulation.xml
    private static final String SCREEN_ROOT = "screenRoot";
    private static final String CATEGORY_RICE = "categoryRice";
    private static final String CATEGORY_GLN64 = "categoryGln64";
    private static final String CATEGORY_GLIDE64 = "categoryGlide64";
    private static final String VIDEO_PLUGIN = "videoPlugin";
    private static final String DISPLAY_IMMERSIVE_MODE = "displayImmersiveMode";
    private static final String DISPLAY_SCALING = "displayScaling";
    private static final String DISPLAY_CROP_WIDTH = "displayCropWidth";
    private static final String DISPLAY_CROP_HEIGHT = "displayCropHeight";
    

    private static final String PATH_HI_RES_TEXTURES = "pathHiResTextures";
    
    // These constants must match the entry-values found in arrays.xml
    private static final String LIBGLIDE64_SO = "libmupen64plus-video-glide64mk2.so";
    private static final String LIBRICE_SO = "libmupen64plus-video-rice.so";
    private static final String LIBGLN64_SO = "libmupen64plus-video-gln64.so";
    
    // Preference menu items
    private PreferenceGroup mScreenRoot = null;
    private Preference mCategoryN64 = null;
    private Preference mCategoryRice = null;
    private Preference mCategoryGlide64 = null;
    private Preference mDisplayImmersiveMode = null;
    private Preference mDisplayCropWidth = null;
    private Preference mDisplayCropHeight = null;
    
    @Override
    protected int getPrefsResId()
    {
        return R.xml.profile_emulation;
    }
    
    @Override
    protected String getConfigFilePath()
    {
        return new UserPrefs( this ).emulationProfiles_cfg;
    }
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Ensure that selected plugin names and other list preferences are valid
        Resources res = getResources();
        PrefUtil.validateListPreference( res, mPrefs, VIDEO_PLUGIN, R.string.videoPlugin_default,
                R.array.videoPlugin_values );
        
        // Get some menu items for use later
        mScreenRoot = (PreferenceGroup) findPreference( SCREEN_ROOT );
        mCategoryN64 = findPreference( CATEGORY_GLN64 );
        mCategoryRice = findPreference( CATEGORY_RICE );
        mCategoryGlide64 = findPreference( CATEGORY_GLIDE64 );
        mDisplayImmersiveMode = findPreference( DISPLAY_IMMERSIVE_MODE );
        mDisplayCropWidth = findPreference( DISPLAY_CROP_WIDTH );
        mDisplayCropHeight = findPreference( DISPLAY_CROP_HEIGHT );
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        super.onSharedPreferenceChanged( sharedPreferences, key );
        if( key.equals( PATH_HI_RES_TEXTURES ) )
            processTexturePak( sharedPreferences.getString( PATH_HI_RES_TEXTURES, "" ) );
    }
    
    @Override
    protected void refreshViews()
    {
        // Get the current values
        String videoPlugin = mPrefs.getString( VIDEO_PLUGIN, null );
        String displayScaling = mPrefs.getString( DISPLAY_SCALING, null );
        
        // Hide certain categories altogether if they're not applicable. Normally we just rely on
        // the built-in dependency disabler, but here the categories are so large that hiding them
        // provides a better user experience.
        
        if( !AppData.IS_KITKAT )
            mScreenRoot.removePreference( mDisplayImmersiveMode );
        
        if( displayScaling.equals("crop"))
        {
            mScreenRoot.addPreference( mDisplayCropWidth );
            mScreenRoot.addPreference( mDisplayCropHeight );
        }
        else
        {
            mScreenRoot.removePreference( mDisplayCropWidth );
            mScreenRoot.removePreference( mDisplayCropHeight );
        }
        
        if( LIBGLN64_SO.equals( videoPlugin ) )
            mScreenRoot.addPreference( mCategoryN64 );
        else
            mScreenRoot.removePreference( mCategoryN64 );
        
        if( LIBRICE_SO.equals( videoPlugin ) )
            mScreenRoot.addPreference( mCategoryRice );
        else
            mScreenRoot.removePreference( mCategoryRice );
        
        if( LIBGLIDE64_SO.equals( videoPlugin ) )
            mScreenRoot.addPreference( mCategoryGlide64 );
        else
            mScreenRoot.removePreference( mCategoryGlide64 );
    }
    
    private void processTexturePak( final String filename )
    {
        if( TextUtils.isEmpty( filename ) )
        {
            Log.e( "EmulationProfileEditActivity", "Filename not specified in processTexturePak" );
            Notifier.showToast( this, R.string.pathHiResTexturesTask_errorMessage );
            return;
        }
        
        TaskHandler.Task task = new TaskHandler.Task()
        {
            private boolean success = false;
            
            @Override
            public void run()
            {
                String headerName = Utility.getTexturePackName( filename );
                if( !TextUtils.isEmpty( headerName ) )
                {
                    UserPrefs userPrefs = new UserPrefs( EmulationProfileActivity.this );
                    String outputFolder = userPrefs.hiResTextureDir + headerName;
                    FileUtil.deleteFolder( new File( outputFolder ) );
                    success = Utility.unzipAll( new File( filename ), outputFolder );
                }
            }
            
            @Override
            public void onComplete()
            {
                if( !success )
                    Notifier.showToast( EmulationProfileActivity.this,
                            R.string.pathHiResTexturesTask_errorMessage );
            }
        };
        
        String title = getString( R.string.pathHiResTexturesTask_title );
        String message = getString( R.string.pathHiResTexturesTask_message );
        TaskHandler.run( this, title, message, task );
    }
}
