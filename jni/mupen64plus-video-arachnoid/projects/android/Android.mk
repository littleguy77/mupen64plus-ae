LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
SRCDIR := ../../src

LOCAL_MODULE := mupen64plus-video-arachnoid
LOCAL_SHARED_LIBRARIES := ae-imports SDL2 core
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES :=                     \
    $(SRCDIR)/Assembler \
    $(SRCDIR)/Combiner \
    $(SRCDIR)/config \
    $(SRCDIR)/framebuffer \
    $(SRCDIR)/GBI \
    $(SRCDIR)/hash \
    $(SRCDIR)/log \
    $(SRCDIR)/math \
    $(SRCDIR)/RDP \
    $(SRCDIR)/renderer \
    $(SRCDIR)/RSP \
    $(SRCDIR)/texture \
    $(SRCDIR)/ucodes \
    $(SRCDIR)/utils \
    $(M64P_API_INCLUDES)                \
    $(SDL_INCLUDES)                     \
    $(AE_BRIDGE_INCLUDES)               \


LOCAL_SRC_FILES :=                      \
   $(SRCDIR)/main.cpp \
	$(SRCDIR)/log/Logger.cpp \
	$(SRCDIR)/config/Config.cpp \
	$(SRCDIR)/config/StringFunctions.cpp \
	$(SRCDIR)/GraphicsPlugin.cpp \
	$(SRCDIR)/OpenGLManager.cpp \
	$(SRCDIR)/renderer/OpenGLRenderer.cpp \
	$(SRCDIR)/framebuffer/FrameBuffer.cpp \
	$(SRCDIR)/FogManager.cpp \
	$(SRCDIR)/MultiTexturingExt.cpp \
	$(SRCDIR)/ExtensionChecker.cpp \
	$(SRCDIR)/SecondaryColorExt.cpp \
	$(SRCDIR)/Memory.cpp \
	$(SRCDIR)/math/Matrix4.cpp \
	$(SRCDIR)/texture/CachedTexture.cpp \
	$(SRCDIR)/texture/TextureCache.cpp \
	$(SRCDIR)/texture/ImageFormatSelector.cpp \
	$(SRCDIR)/hash/CRCCalculator.cpp \
	$(SRCDIR)/hash/CRCCalculator2.cpp \
	$(SRCDIR)/texture/TextureLoader.cpp \
	$(SRCDIR)/DisplayListParser.cpp \
	$(SRCDIR)/VI.cpp \
	$(SRCDIR)/ucodes/UCodeSelector.cpp \
	$(SRCDIR)/ucodes/UCode0.cpp \
	$(SRCDIR)/ucodes/UCode1.cpp \
	$(SRCDIR)/ucodes/UCode2.cpp \
	$(SRCDIR)/ucodes/UCode3.cpp \
	$(SRCDIR)/ucodes/UCode4.cpp \
	$(SRCDIR)/ucodes/UCode5.cpp \
	$(SRCDIR)/ucodes/UCode6.cpp \
	$(SRCDIR)/ucodes/UCode7.cpp \
	$(SRCDIR)/ucodes/UCode8.cpp \
	$(SRCDIR)/ucodes/UCode9.cpp \
	$(SRCDIR)/ucodes/UCode10.cpp \
	$(SRCDIR)/GBI/GBI.cpp \
	$(SRCDIR)/RSP/RSP.cpp \
	$(SRCDIR)/RSP/RSPMatrixManager.cpp \
	$(SRCDIR)/RSP/RSPVertexManager.cpp \
	$(SRCDIR)/RSP/RSPLightManager.cpp \
	$(SRCDIR)/Combiner/AdvancedCombinerManager.cpp \
	$(SRCDIR)/Combiner/CombinerBase.cpp \
	$(SRCDIR)/Combiner/AdvancedTexEnvCombiner.cpp \
	$(SRCDIR)/Combiner/SimpleTexEnvCombiner.cpp \
	$(SRCDIR)/Combiner/DummyCombiner.cpp \
	$(SRCDIR)/Combiner/CombinerStageMerger.cpp \
	$(SRCDIR)/Combiner/CombinerStageCreator.cpp \
	$(SRCDIR)/Combiner/CombinerCache.cpp \
	$(SRCDIR)/RomDetector.cpp \
	$(SRCDIR)/RDP/RDP.cpp \
	$(SRCDIR)/RDP/RDPInstructions.cpp		\
	$(SRCDIR)/renderer/OpenGL2DRenderer.cpp \
	$(SRCDIR)/osal_dynamiclib_unix.cpp \

LOCAL_CFLAGS :=         \
    $(COMMON_CFLAGS)    \
    -O3 \
    -DANDROID           \
    -DNO_ASM            \
    -DHAVE_GLES \
    -DPAULSCODE         \
    -fsigned-char       \
    #-DBGR_SHADER        \
    #-DSDL_NO_COMPAT     \
    
LOCAL_CPPFLAGS := $(COMMON_CPPFLAGS) -O3
    
LOCAL_CPP_FEATURES := exceptions

LOCAL_LDFLAGS := -Wl,-version-script,$(LOCAL_PATH)/$(SRCDIR)/video_api_export.ver

LOCAL_LDLIBS :=         \
    -lGLESv1_CM            \
    -lEGL \
    -landroid \
    -llog               \

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    # Use for ARM7a:
    LOCAL_CFLAGS += -mfpu=vfp
    LOCAL_CFLAGS += -mfloat-abi=softfp
    
else ifeq ($(TARGET_ARCH_ABI), armeabi)
    # Use for pre-ARM7a:
    
else ifeq ($(TARGET_ARCH_ABI), x86)
    # TODO: set the proper flags here
    
else
    # Any other architectures that Android could be running on?
    
endif

include $(BUILD_SHARED_LIBRARY)
