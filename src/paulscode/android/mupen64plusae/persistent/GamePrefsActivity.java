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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae.persistent;

import java.io.File;
import java.util.ArrayList;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.cheat.CheatEditorActivity;
import paulscode.android.mupen64plusae.cheat.CheatFile;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatSection;
import paulscode.android.mupen64plusae.cheat.CheatPreference;
import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.hack.MogaHack;
import paulscode.android.mupen64plusae.preference.PlayerMapPreference;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.preference.ProfilePreference;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;
import paulscode.android.mupen64plusae.util.RomHeader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.bda.controller.Controller;

public class GamePrefsActivity extends AppCompatPreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences_play.xml
    private static final String SCREEN_ROOT = "screenRoot";
    private static final String SCREEN_CHEATS = "screenCheats";
    private static final String CATEGORY_CHEATS = "categoryCheats";
    
    private static final String ACTION_CHEAT_EDITOR = "actionCheatEditor";
    private static final String ACTION_WIKI = "actionWiki";
    private static final String ACTION_RESET_GAME_PREFS = "actionResetGamePrefs";
    
    private static final String EMULATION_PROFILE = "emulationProfile";
    private static final String TOUCHSCREEN_PROFILE = "touchscreenProfile";
    private static final String CONTROLLER_PROFILE1 = "controllerProfile1";
    private static final String CONTROLLER_PROFILE2 = "controllerProfile2";
    private static final String CONTROLLER_PROFILE3 = "controllerProfile3";
    private static final String CONTROLLER_PROFILE4 = "controllerProfile4";
    private static final String PLAYER_MAP = "playerMap";
    private static final String PLAY_SHOW_CHEATS = "playShowCheats";
    
    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private GamePrefs mGamePrefs = null;
    private SharedPreferences mPrefs = null;
    
    // ROM info
    private String mRomPath = null;
    private String mRomMd5 = null;
    private RomHeader mRomHeader = null;
    private RomDatabase mRomDatabase = null;
    private RomDetail mRomDetail = null;
    
    // Preference menu items
    private ProfilePreference mEmulationProfile = null;
    private ProfilePreference mTouchscreenProfile = null;
    private ProfilePreference mControllerProfile1 = null;
    private ProfilePreference mControllerProfile2 = null;
    private ProfilePreference mControllerProfile3 = null;
    private ProfilePreference mControllerProfile4 = null;
    private PreferenceGroup mScreenCheats = null;
    private PreferenceGroup mCategoryCheats = null;
    
    // MOGA controller interface
    private Controller mMogaController = Controller.getInstance( this );
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get the ROM path and MD5 that was passed to the activity
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle" );
        mRomPath = extras.getString( ActivityHelper.Keys.ROM_PATH );
        mRomMd5 = extras.getString( ActivityHelper.Keys.ROM_MD5 );
        if( TextUtils.isEmpty( mRomPath ) || TextUtils.isEmpty( mRomMd5 ) )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle" );
        
        // Initialize MOGA controller API
        // TODO: Remove hack after MOGA SDK is fixed
        // mMogaController.init();
        MogaHack.init( mMogaController, this );
        
        // Get app data and user preferences
        mAppData = new AppData( this );
        mRomHeader = new RomHeader( mRomPath );
        mGlobalPrefs = new GlobalPrefs( this );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomHeader );
        mGlobalPrefs.enforceLocale( this );
        mPrefs = getSharedPreferences( mGamePrefs.sharedPrefsName, MODE_PRIVATE );
        
        // Get the detailed info about the ROM
        mRomDatabase = new RomDatabase( mAppData.mupen64plus_ini );
        mRomDetail = mRomDatabase.lookupByMd5WithFallback( mRomMd5, new File( mRomPath ) );
        
        // Load user preference menu structure from XML and update view
        getPreferenceManager().setSharedPreferencesName( mGamePrefs.sharedPrefsName );
        addPreferencesFromResource( R.xml.preferences_game );
        mEmulationProfile = (ProfilePreference) findPreference( EMULATION_PROFILE );
        mTouchscreenProfile = (ProfilePreference) findPreference( TOUCHSCREEN_PROFILE );
        mControllerProfile1 = (ProfilePreference) findPreference( CONTROLLER_PROFILE1 );
        mControllerProfile2 = (ProfilePreference) findPreference( CONTROLLER_PROFILE2 );
        mControllerProfile3 = (ProfilePreference) findPreference( CONTROLLER_PROFILE3 );
        mControllerProfile4 = (ProfilePreference) findPreference( CONTROLLER_PROFILE4 );
        mScreenCheats = (PreferenceGroup) findPreference( SCREEN_CHEATS );
        mCategoryCheats = (PreferenceGroup) findPreference( CATEGORY_CHEATS );
        
        // Set some game-specific strings
        setTitle( mRomDetail.goodName );
        
        // Handle certain menu items that require extra processing or aren't actually preferences
        PrefUtil.setOnPreferenceClickListener( this, ACTION_CHEAT_EDITOR, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_WIKI, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESET_GAME_PREFS, this );
        
        // Remove wiki menu item if not applicable
        if( TextUtils.isEmpty( mRomDetail.wikiUrl ) )
        {
            PrefUtil.removePreference( this, SCREEN_ROOT, ACTION_WIKI );
        }
        
        // Setup controller profiles settings based on ROM's number of players
        if( mRomDetail.players == 1 )
        {
            // Simplify name of "controller 1" to just "controller" to eliminate confusion
            findPreference( CONTROLLER_PROFILE1 ).setTitle( R.string.controllerProfile_title );
            
            // Remove unneeded preference items
            PrefUtil.removePreference( this, SCREEN_ROOT, CONTROLLER_PROFILE2 );
            PrefUtil.removePreference( this, SCREEN_ROOT, CONTROLLER_PROFILE3 );
            PrefUtil.removePreference( this, SCREEN_ROOT, CONTROLLER_PROFILE4 );
            PrefUtil.removePreference( this, SCREEN_ROOT, PLAYER_MAP );
        }
        else
        {
            // Remove unneeded preference items
            if( mRomDetail.players < 4 )
                PrefUtil.removePreference( this, SCREEN_ROOT, CONTROLLER_PROFILE4 );
            if( mRomDetail.players < 3 )
                PrefUtil.removePreference( this, SCREEN_ROOT, CONTROLLER_PROFILE3 );
            
            // Configure the player map preference
            PlayerMapPreference playerPref = (PlayerMapPreference) findPreference( PLAYER_MAP );
            playerPref.setMogaController( mMogaController );
        }
        
        // Build the cheats category as needed
        refreshCheatsCategory();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener( this );
        mMogaController.onResume();
        refreshViews();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
        mMogaController.onPause();
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mMogaController.exit();
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        refreshViews();
        if( key.equals( PLAY_SHOW_CHEATS ) )
        {
            refreshCheatsCategory();
        }
    }
    
    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        if( requestCode == 111 )
            refreshCheatsCategory();
    }
    
    private void refreshViews()
    {
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
        
        // Refresh the preferences objects
        mGlobalPrefs = new GlobalPrefs( this );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomHeader );
        
        // Populate the profile preferences
        mEmulationProfile.populateProfiles( mAppData.emulationProfiles_cfg,
                mGlobalPrefs.emulationProfiles_cfg, mGlobalPrefs.getEmulationProfileDefault() );
        mTouchscreenProfile.populateProfiles( mAppData.touchscreenProfiles_cfg,
                mGlobalPrefs.touchscreenProfiles_cfg, mGlobalPrefs.getTouchscreenProfileDefault() );
        mControllerProfile1.populateProfiles( mAppData.controllerProfiles_cfg,
                mGlobalPrefs.controllerProfiles_cfg, mGlobalPrefs.getControllerProfileDefault() );
        mControllerProfile2.populateProfiles( mAppData.controllerProfiles_cfg,
                mGlobalPrefs.controllerProfiles_cfg, "" );
        mControllerProfile3.populateProfiles( mAppData.controllerProfiles_cfg,
                mGlobalPrefs.controllerProfiles_cfg, "" );
        mControllerProfile4.populateProfiles( mAppData.controllerProfiles_cfg,
                mGlobalPrefs.controllerProfiles_cfg, "" );
        
        // Refresh the preferences objects in case populate* changed a value
        mGlobalPrefs = new GlobalPrefs( this );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomHeader );
        
        // Set cheats screen summary text
        mScreenCheats.setSummary( mGamePrefs.isCheatOptionsShown
                ? R.string.screenCheats_summaryEnabled
                : R.string.screenCheats_summaryDisabled );
        
        // Enable/disable player map item as necessary
        PrefUtil.enablePreference( this, PLAYER_MAP, mGamePrefs.playerMap.isEnabled() );
        
        // Define which buttons to show in player map dialog
        @SuppressWarnings( "deprecation" )
        PlayerMapPreference playerPref = (PlayerMapPreference) findPreference( PLAYER_MAP );
        if( playerPref != null )
        {
            // Check null in case preference has been removed
            boolean enable1 = mGamePrefs.isControllerEnabled1;
            boolean enable2 = mGamePrefs.isControllerEnabled2 && mRomDetail.players > 1;
            boolean enable3 = mGamePrefs.isControllerEnabled3 && mRomDetail.players > 2;
            boolean enable4 = mGamePrefs.isControllerEnabled4 && mRomDetail.players > 3;
            playerPref.setControllersEnabled( enable1, enable2, enable3, enable4 );
        }
        
        mPrefs.registerOnSharedPreferenceChangeListener( this );
    }
    
    private void refreshCheatsCategory()
    {
        if( mGamePrefs.isCheatOptionsShown )
        {
            // Populate menu items
            buildCheatsCategory( mRomHeader.crc );
            
            // Show the cheats category
            mScreenCheats.addPreference( mCategoryCheats );
        }
        else
        {
            // Hide the cheats category
            mScreenCheats.removePreference( mCategoryCheats );
        }
    }
    
    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        String key = preference.getKey();
        if( key.equals( ACTION_CHEAT_EDITOR ) )
        {
            Intent intent = new Intent( this, CheatEditorActivity.class );
            intent.putExtra( ActivityHelper.Keys.ROM_PATH, mRomPath );
            startActivityForResult( intent, 111 );
        }
        else if( key.equals( ACTION_WIKI ) )
        {
            ActivityHelper.launchUri( this, mRomDetail.wikiUrl );
        }
        else if( key.equals( ACTION_RESET_GAME_PREFS ) )
        {
            actionResetGamePrefs();
        }
        return false;
    }
    
    private void buildCheatsCategory( final String crc )
    {
        mCategoryCheats.removeAll();
        
        Log.v( "GamePrefsActivity", "building from CRC = " + crc );
        if( crc == null )
            return;
        
        // Get the appropriate section of the config file, using CRC as the key
        CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt );
        CheatSection cheatSection = mupencheat_txt.match( "^" + crc.replace( ' ', '-' ) + ".*" );
        if( cheatSection == null )
        {
            Log.w( "GamePrefsActivity", "No cheat section found for '" + crc + "'" );
            return;
        }
        ArrayList<Cheat> cheats = new ArrayList<Cheat>();
        cheats.addAll( CheatUtils.populate( crc, mupencheat_txt, true, this ) );
        CheatUtils.reset();
        
        // Layout the menu, populating it with appropriate cheat options
        for( int i = 0; i < cheats.size(); i++ )
        {
            // Get the short title of the cheat (shown in the menu)
            String title;
            if( cheats.get( i ).name == null )
            {
                // Title not available, just use a default string for the menu
                title = getString( R.string.cheats_defaultName, i );
            }
            else
            {
                // Title available, remove the leading/trailing quotation marks
                title = cheats.get( i ).name;
            }
            String notes = cheats.get( i ).desc;
            String options = cheats.get( i ).option;
            String[] optionStrings = null;
            if( !TextUtils.isEmpty( options ) )
            {
                optionStrings = options.split( "\n" );
            }
            
            // Create the menu item associated with this cheat
            CheatPreference pref = new CheatPreference( this, title, notes, optionStrings );
            pref.setKey( crc + " Cheat" + i );
            
            // Add the preference menu item to the cheats category
            mCategoryCheats.addPreference( pref );
        }
    }
    
    private void actionResetGamePrefs()
    {
        String title = getString( R.string.confirm_title );
        String message = getString( R.string.actionResetGamePrefs_popupMessage );
        Prompt.promptConfirm( this, title, message, new PromptConfirmListener()
        {
            @Override
            public void onConfirm()
            {
                // Reset the user preferences
                mPrefs.unregisterOnSharedPreferenceChangeListener( GamePrefsActivity.this );
                mPrefs.edit().clear().commit();
                PreferenceManager.setDefaultValues( GamePrefsActivity.this, R.xml.preferences_game, true );
                
                // Also reset any manual overrides the user may have made in the config file
                File configFile = new File( mGamePrefs.mupen64plus_cfg );
                if( configFile.exists() )
                    configFile.delete();
                
                // Rebuild the menu system by restarting the activity
                ActivityHelper.restartActivity( GamePrefsActivity.this );
            }
        } );
    }
}
