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

#include <stdlib.h>
#include <dlfcn.h>
#include "SDL.h"
#include "m64p_types.h"
#include "ae_imports.h"

#ifdef M64P_BIG_ENDIAN
  #define sl(mot) mot
#else
  #define sl(mot) (((mot & 0xFF) << 24) | ((mot & 0xFF00) <<  8) | ((mot & 0xFF0000) >>  8) | ((mot & 0xFF000000) >> 24))
#endif


/*******************************************************************************
 Variables used internally
 *******************************************************************************/

// JNI objects
static JavaVM* mVm;
static void* mReserved;

// Library handles
static void *handleAEI;         // libae-imports.so
static void *handleSDL;         // libSDL2.so
static void *handleCore;        // libmupen64plus-core.so
static void *handleFront;       // libmupen64plus-ui-console.so

// Function types
typedef jint        (*pJNI_OnLoad)      (JavaVM* vm, void* reserved);
typedef int         (*pAeiInit)         (JNIEnv* env, jclass cls);
typedef int         (*pSdlInit)         (JNIEnv* env, jclass cls);
typedef void        (*pSdlSetScreen)    (int width, int height, Uint32 format);
typedef void        (*pVoidFunc)        ();
typedef m64p_error  (*pCoreDoCommand)   (m64p_command, int, void *);
typedef int         (*pFrontMain)       (int argc, char* argv[]);
typedef void        (*pNativeResume)    (JNIEnv* env, jclass cls);
typedef void        (*pNativePause)     (JNIEnv* env, jclass cls);

// Function pointers
static pAeiInit         aeiInit         = NULL;
static pSdlInit         sdlInit         = NULL;
static pSdlSetScreen    sdlSetScreen    = NULL;
static pVoidFunc        sdlMainReady    = NULL;
static pCoreDoCommand   coreDoCommand   = NULL;
static pFrontMain       frontMain       = NULL;
static pNativeResume    nativeResume    = NULL;
static pNativePause     nativePause     = NULL;

/*******************************************************************************
 Functions called automatically by JNI framework
 *******************************************************************************/

extern jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    mVm = vm;
    mReserved = reserved;
    return JNI_VERSION_1_4;
}

/*******************************************************************************
 Functions called by Java code
 *******************************************************************************/

extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_jni_NativeExports_loadLibraries(JNIEnv* env, jclass cls, jstring jlibPath)
{
    LOGI("Loading native libraries");

    // Construct the library paths
    const char *libPath = env->GetStringUTFChars(jlibPath, 0);
    char pathAEI[256];
    char pathSDL[256];
    char pathCore[256];
    char pathFront[256];
    sprintf(pathAEI,    "%s/libae-imports.so",              libPath);
    sprintf(pathSDL,    "%s/libSDL2.so",                    libPath);
    sprintf(pathCore,   "%s/libmupen64plus-core.so",        libPath);
    sprintf(pathFront,  "%s/libmupen64plus-ui-console.so",	libPath);
    env->ReleaseStringUTFChars(jlibPath, libPath);

    // Open shared libraries
    handleAEI   = dlopen(pathAEI,   RTLD_NOW);
    handleSDL   = dlopen(pathSDL,   RTLD_NOW);
    handleCore  = dlopen(pathCore,  RTLD_NOW);
    handleFront = dlopen(pathFront, RTLD_NOW);

    // Make sure we don't have any typos
    if (!handleAEI || !handleSDL || !handleCore || !handleFront)
    {
        LOGE("Could not load libraries: be sure the paths are correct");
    }

    // Find and call the JNI_OnLoad functions manually since we aren't loading the libraries from Java
    pJNI_OnLoad JNI_OnLoad0 = (pJNI_OnLoad) dlsym(handleAEI, "JNI_OnLoad");
    pJNI_OnLoad JNI_OnLoad1 = (pJNI_OnLoad) dlsym(handleSDL, "JNI_OnLoad");
    JNI_OnLoad0(mVm, mReserved);
    JNI_OnLoad1(mVm, mReserved);
    JNI_OnLoad0 = NULL;
    JNI_OnLoad1 = NULL;

    // Find library functions
    aeiInit         = (pAeiInit)        dlsym(handleAEI,    "Android_JNI_InitImports");
    sdlInit         = (pSdlInit)        dlsym(handleSDL,    "SDL_Android_Init");
    sdlSetScreen    = (pSdlSetScreen)   dlsym(handleSDL,    "Android_SetScreenResolution");
    sdlMainReady    = (pVoidFunc)       dlsym(handleSDL,    "SDL_SetMainReady");
    coreDoCommand   = (pCoreDoCommand)  dlsym(handleCore,   "CoreDoCommand");
    frontMain       = (pFrontMain)      dlsym(handleFront,  "SDL_main");
    nativeResume    = (pNativeResume)   dlsym(handleSDL,    "Java_org_libsdl_app_SDLActivity_nativeResume");
    nativePause     = (pNativePause)    dlsym(handleSDL,    "Java_org_libsdl_app_SDLActivity_nativePause");

    // Make sure we don't have any typos
    if (!aeiInit || !sdlInit || !sdlSetScreen || !sdlMainReady || !coreDoCommand || !frontMain || !nativeResume || !nativePause)
    {
        LOGE("Could not load library functions: be sure they are named and typedef'd correctly");
    }
}

extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_jni_NativeExports_unloadLibraries(JNIEnv* env, jclass cls)
{
    // Unload the libraries to ensure that static variables are re-initialized next time
    LOGI("Unloading native libraries");

    // Nullify function pointers
    aeiInit         = NULL;
    sdlInit         = NULL;
    sdlSetScreen    = NULL;
    sdlMainReady    = NULL;
    coreDoCommand   = NULL;
    frontMain       = NULL;
    nativeResume    = NULL;
    nativePause     = NULL;

    // Close shared libraries
    if (handleFront) dlclose(handleFront);
    if (handleCore)  dlclose(handleCore);
    if (handleSDL)   dlclose(handleSDL);
    if (handleAEI)   dlclose(handleAEI);

    // Nullify handles
    handleFront     = NULL;
    handleCore      = NULL;
    handleSDL       = NULL;
    handleAEI       = NULL;
}

extern "C" DECLSPEC void SDLCALL Java_paulscode_android_mupen64plusae_jni_NativeExports_emuStart(JNIEnv* env, jclass cls, jstring juserDataPath, jstring juserCachePath, jobjectArray jargv)
{
    // Define some environment variables needed by rice video plugin
    const char *userDataPath = env->GetStringUTFChars(juserDataPath, 0);
    const char *userCachePath = env->GetStringUTFChars(juserCachePath, 0);
    setenv( "XDG_DATA_HOME", userDataPath, 1 );
    setenv( "XDG_CACHE_HOME", userCachePath, 1 );
    env->ReleaseStringUTFChars(juserDataPath, userDataPath);
    env->ReleaseStringUTFChars(juserCachePath, userCachePath);

    // Initialize dependencies
    jclass nativeImports = env->FindClass("paulscode/android/mupen64plusae/jni/NativeImports");
    jclass nativeSDL = env->FindClass("paulscode/android/mupen64plusae/jni/NativeSDL");
    aeiInit(env, nativeImports);
    sdlInit(env, nativeSDL);
    sdlSetScreen(0, 0, SDL_PIXELFORMAT_RGB565);
    sdlMainReady();

    // Repackage the command-line args
    int argc = env->GetArrayLength(jargv);
    char **argv = (char **) malloc(sizeof(char *) * argc);
    for (int i = 0; i < argc; i++)
    {
        jstring jarg = (jstring) env->GetObjectArrayElement(jargv, i);
        const char *arg = env->GetStringUTFChars(jarg, 0);
        argv[i] = strdup(arg);
        env->ReleaseStringUTFChars(jarg, arg);
    }

    // Launch main emulator loop (continues until emuStop is called)
    frontMain(argc, argv);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuStop(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_STOP, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuResume(JNIEnv* env, jclass cls)
{
    nativeResume(env, cls);
    if (coreDoCommand) coreDoCommand(M64CMD_RESUME, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuPause(JNIEnv* env, jclass cls)
{
    nativePause(env, cls);
    if (coreDoCommand) coreDoCommand(M64CMD_PAUSE, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuAdvanceFrame(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_ADVANCE_FRAME, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuSetSpeed(JNIEnv* env, jclass cls, jint percent)
{
    int speed_factor = (int) percent;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_SET, M64CORE_SPEED_FACTOR, &speed_factor);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuSetFramelimiter(JNIEnv* env, jclass cls, jboolean enabled)
{
    int e = enabled == JNI_TRUE ? 1 : 0;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_SET, M64CORE_SPEED_LIMITER, &e);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuSetSlot(JNIEnv* env, jclass cls, jint slotID)
{
    if (coreDoCommand) coreDoCommand(M64CMD_STATE_SET_SLOT, (int) slotID, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuLoadSlot(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_STATE_LOAD, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuSaveSlot(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_STATE_SAVE, 1, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuLoadFile(JNIEnv* env, jclass cls, jstring filename)
{
    const char *nativeString = env->GetStringUTFChars(filename, 0);
    if (coreDoCommand) coreDoCommand(M64CMD_STATE_LOAD, 0, (void *) nativeString);
    env->ReleaseStringUTFChars(filename, nativeString);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuSaveFile(JNIEnv* env, jclass cls, jstring filename)
{
    const char *nativeString = env->GetStringUTFChars(filename, 0);
    if (coreDoCommand) coreDoCommand(M64CMD_STATE_SAVE, 1, (void *) nativeString);
    env->ReleaseStringUTFChars(filename, nativeString);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuScreenshot(JNIEnv* env, jclass cls)
{
    if (coreDoCommand) coreDoCommand(M64CMD_TAKE_NEXT_SCREENSHOT, 0, NULL);
}

extern "C" DECLSPEC void Java_paulscode_android_mupen64plusae_jni_NativeExports_emuGameShark(JNIEnv* env, jclass cls, jboolean pressed)
{
    int p = pressed == JNI_TRUE ? 1 : 0;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_SET, M64CORE_INPUT_GAMESHARK, &p);
}

extern "C" DECLSPEC jint Java_paulscode_android_mupen64plusae_jni_NativeExports_emuGetState(JNIEnv* env, jclass cls)
{
    int state = 0;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_QUERY, M64CORE_EMU_STATE, &state);
    if (state == M64EMU_STOPPED)
        return (jint) 1;
    else if (state == M64EMU_RUNNING)
        return (jint) 2;
    else if (state == M64EMU_PAUSED)
        return (jint) 3;
    else
        return (jint) 0;
}

extern "C" DECLSPEC jint Java_paulscode_android_mupen64plusae_jni_NativeExports_emuGetSpeed(JNIEnv* env, jclass cls)
{
    int speed = 100;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_QUERY, M64CORE_SPEED_FACTOR, &speed);
    return (jint) speed;
}

extern "C" DECLSPEC jboolean Java_paulscode_android_mupen64plusae_jni_NativeExports_emuGetFramelimiter(JNIEnv* env, jclass cls)
{
    int e = 1;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_QUERY, M64CORE_SPEED_LIMITER, &e);
    return (jboolean) (e ? JNI_TRUE : JNI_FALSE);
}

extern "C" DECLSPEC jint Java_paulscode_android_mupen64plusae_jni_NativeExports_emuGetSlot(JNIEnv* env, jclass cls)
{
    int slot = 0;
    if (coreDoCommand) coreDoCommand(M64CMD_CORE_STATE_QUERY, M64CORE_SAVESTATE_SLOT, &slot);
    return (jint) slot;
}
