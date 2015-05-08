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
package paulscode.android.mupen64plusae.game;

import paulscode.android.mupen64plusae.jni.CoreInterface;
import android.annotation.TargetApi;
import android.app.NativeActivity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

@TargetApi( 9 )
public class GameActivityXperiaPlay extends NativeActivity
{
    private GameLifecycleHandler mLifecycleHandler;
    private GameMenuHandler mMenuHandler;
    private AppCompatDelegate mAppCompat;
    
    public GameActivityXperiaPlay()
    {
        System.loadLibrary( "xperia-touchpad" );
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        mMenuHandler.onCreateOptionsMenu( menu );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        mMenuHandler.onPrepareOptionsMenu( menu );
        return super.onPrepareOptionsMenu( menu );
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        mMenuHandler.onOptionsItemSelected( item );
        return super.onOptionsItemSelected( item );
    }
    
    @Override
    public void onWindowFocusChanged( boolean hasFocus )
    {
        super.onWindowFocusChanged( hasFocus );
        mLifecycleHandler.onWindowFocusChanged( hasFocus );
    }
    
    @Override
    public void onConfigurationChanged( Configuration newConfig )
    {
        super.onConfigurationChanged( newConfig );
        getAppCompatDelegate().onConfigurationChanged( newConfig );
    }
    
    @Override
    protected void onTitleChanged( CharSequence title, int color )
    {
        super.onTitleChanged( title, color );
        getAppCompatDelegate().setTitle( title );
    }
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        mLifecycleHandler = new GameLifecycleHandler( this );
        mLifecycleHandler.onCreateBegin( savedInstanceState );
        
        getAppCompatDelegate().installViewFactory();
        getAppCompatDelegate().onCreate( savedInstanceState );
        
        mMenuHandler = new GameMenuHandler( this );
        CoreInterface.addOnStateCallbackListener( mMenuHandler );
        
        super.onCreate( savedInstanceState );
        mLifecycleHandler.onCreateEnd( savedInstanceState, getSupportActionBar() );
    }
    
    @Override
    protected void onStart()
    {
        super.onStart();
        mLifecycleHandler.onStart();
    }
    
    @Override
    protected void onPostCreate( Bundle savedInstanceState )
    {
        super.onPostCreate( savedInstanceState );
        getAppCompatDelegate().onPostCreate( savedInstanceState );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        mLifecycleHandler.onResume();
    }
    
    @Override
    protected void onPostResume()
    {
        super.onPostResume();
        getAppCompatDelegate().onPostResume();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        mLifecycleHandler.onPause();
    }
    
    @Override
    protected void onStop()
    {
        super.onStop();
        mLifecycleHandler.onStop();
        getAppCompatDelegate().onStop();
    }
    
    @Override
    protected void onDestroy()
    {
        CoreInterface.removeOnStateCallbackListener( mMenuHandler );
        
        super.onDestroy();
        mLifecycleHandler.onDestroy();
        getAppCompatDelegate().onDestroy();
    }
    
    @Override
    public void setContentView( int layoutResID )
    {
        getAppCompatDelegate().setContentView( layoutResID );
    }
    
    @Override
    public void setContentView( View view )
    {
        getAppCompatDelegate().setContentView( view );
    }
    
    @Override
    public void setContentView( View view, LayoutParams params )
    {
        getAppCompatDelegate().setContentView( view, params );
    }
    
    @Override
    public void addContentView( View view, LayoutParams params )
    {
        getAppCompatDelegate().addContentView( view, params );
    }
    
    @Override
    public void invalidateOptionsMenu()
    {
        getAppCompatDelegate().invalidateOptionsMenu();
    }
    
    @Override
    public MenuInflater getMenuInflater()
    {
        return getAppCompatDelegate().getMenuInflater();
    }
    
    public void setSupportActionBar( Toolbar toolbar )
    {
        getAppCompatDelegate().setSupportActionBar( toolbar );
    }
    
    public ActionBar getSupportActionBar()
    {
        return getAppCompatDelegate().getSupportActionBar();
    }
    
    public AppCompatDelegate getAppCompatDelegate()
    {
        if( mAppCompat == null )
            mAppCompat = AppCompatDelegate.create( this, null );
        return mAppCompat;
    }
}
