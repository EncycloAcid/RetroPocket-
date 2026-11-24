#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <mutex>
#include <string>
#include <vector>
#include <cstring>
#include <atomic>
#include <cstdint>

#include "libretro.h"

#define LOG_TAG "RetroPocketJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using retro_set_environment_t = void (*)(retro_environment_t);
using retro_set_video_refresh_t = void (*)(retro_video_refresh_t);
using retro_set_audio_sample_t = void (*)(retro_audio_sample_t);
using retro_set_audio_sample_batch_t = void (*)(retro_audio_sample_batch_t);
using retro_set_input_poll_t = void (*)(retro_input_poll_t);
using retro_set_input_state_t = void (*)(retro_input_state_t);
using retro_init_t = void (*)();
using retro_deinit_t = void (*)();
using retro_api_version_t = unsigned (*)();
using retro_get_system_info_t = void (*)(struct retro_system_info*);
using retro_get_system_av_info_t = void (*)(struct retro_system_av_info*);
using retro_set_controller_port_device_t = void (*)(unsigned, unsigned);
using retro_reset_t = void (*)();
using retro_run_t = void (*)();
using retro_serialize_size_t = size_t (*)();
using retro_serialize_t = bool (*)(void*, size_t);
using retro_unserialize_t = bool (*)(const void*, size_t);
using retro_load_game_t = bool (*)(const struct retro_game_info*);
using retro_unload_game_t = void (*)();

struct CoreSymbols {
    void* handle = nullptr;
    retro_set_environment_t set_environment = nullptr;
    retro_set_video_refresh_t set_video_refresh = nullptr;
    retro_set_audio_sample_t set_audio_sample = nullptr;
    retro_set_audio_sample_batch_t set_audio_sample_batch = nullptr;
    retro_set_input_poll_t set_input_poll = nullptr;
    retro_set_input_state_t set_input_state = nullptr;
    retro_init_t init = nullptr;
    retro_deinit_t deinit = nullptr;
    retro_api_version_t api_version = nullptr;
    retro_get_system_info_t get_system_info = nullptr;
    retro_get_system_av_info_t get_system_av_info = nullptr;
    retro_set_controller_port_device_t set_controller_port_device = nullptr;
    retro_reset_t reset = nullptr;
    retro_run_t run = nullptr;
    retro_serialize_size_t serialize_size = nullptr;
    retro_serialize_t serialize = nullptr;
    retro_unserialize_t unserialize = nullptr;
    retro_load_game_t load_game = nullptr;
    retro_unload_game_t unload_game = nullptr;
};

static JavaVM* g_vm = nullptr;
static jobject g_callback = nullptr;
static jmethodID g_onVideo = nullptr;
static jmethodID g_onAudio = nullptr;

static CoreSymbols g_core;
static std::mutex g_mutex;
static std::atomic<bool> g_loaded{false};

static std::vector<uint16_t> g_frameRgb565;
static int g_frameW = 0;
static int g_frameH = 0;
static int g_pixelFormat = RETRO_PIXEL_FORMAT_RGB565;

static int16_t g_joypad[16] = {0};
static std::string g_systemDir;
static std::string g_saveDir;

static JNIEnv* getEnv() {
    JNIEnv* env = nullptr;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (g_vm->AttachCurrentThread(&env, nullptr) != 0) return nullptr;
    }
    return env;
}

static void video_refresh(const void* data, unsigned width, unsigned height, size_t pitch) {
    if (!data || width == 0 || height == 0) return;

    g_frameW = static_cast<int>(width);
    g_frameH = static_cast<int>(height);
    g_frameRgb565.resize(static_cast<size_t>(width) * height);

    if (g_pixelFormat == RETRO_PIXEL_FORMAT_XRGB8888) {
        for (unsigned y = 0; y < height; y++) {
            const auto* src = reinterpret_cast<const uint32_t*>(
                static_cast<const uint8_t*>(data) + y * pitch);
            uint16_t* dst = g_frameRgb565.data() + y * width;
            for (unsigned x = 0; x < width; x++) {
                uint32_t c = src[x];
                uint16_t r = (c >> 19) & 0x1F;
                uint16_t g = (c >> 10) & 0x3F;
                uint16_t b = (c >> 3) & 0x1F;
                dst[x] = static_cast<uint16_t>((r << 11) | (g << 5) | b);
            }
        }
    } else if (g_pixelFormat == RETRO_PIXEL_FORMAT_0RGB1555) {
        for (unsigned y = 0; y < height; y++) {
            const auto* src = reinterpret_cast<const uint16_t*>(
                static_cast<const uint8_t*>(data) + y * pitch);
            uint16_t* dst = g_frameRgb565.data() + y * width;
            for (unsigned x = 0; x < width; x++) {
                uint16_t c = src[x];
                uint16_t r = (c >> 10) & 0x1F;
                uint16_t g = (c >> 5) & 0x1F;
                uint16_t b = c & 0x1F;
                dst[x] = static_cast<uint16_t>((r << 11) | (g << 6) | (g >> 1) << 5 | b);
            }
        }
    } else {
        // RGB565 — may need to de-pitch
        if (pitch == width * 2) {
            std::memcpy(g_frameRgb565.data(), data, width * height * 2);
        } else {
            for (unsigned y = 0; y < height; y++) {
                const auto* src = reinterpret_cast<const uint16_t*>(
                    static_cast<const uint8_t*>(data) + y * pitch);
                std::memcpy(g_frameRgb565.data() + y * width, src, width * 2);
            }
        }
    }

    if (!g_callback || !g_onVideo) return;
    JNIEnv* env = getEnv();
    if (!env) return;
    jsize len = static_cast<jsize>(g_frameRgb565.size());
    jshortArray arr = env->NewShortArray(len);
    if (!arr) return;
    env->SetShortArrayRegion(arr, 0, len, reinterpret_cast<const jshort*>(g_frameRgb565.data()));
    env->CallVoidMethod(g_callback, g_onVideo, arr, g_frameW, g_frameH);
    env->DeleteLocalRef(arr);
}

static void audio_sample(int16_t left, int16_t right) {
    if (!g_callback || !g_onAudio) return;
    JNIEnv* env = getEnv();
    if (!env) return;
    jshortArray arr = env->NewShortArray(2);
    if (!arr) return;
    int16_t samples[2] = {left, right};
    env->SetShortArrayRegion(arr, 0, 2, reinterpret_cast<const jshort*>(samples));
    env->CallVoidMethod(g_callback, g_onAudio, arr);
    env->DeleteLocalRef(arr);
}

static size_t audio_sample_batch(const int16_t* data, size_t frames) {
    if (!data || frames == 0 || !g_callback || !g_onAudio) return frames;
    JNIEnv* env = getEnv();
    if (!env) return frames;
    jsize len = static_cast<jsize>(frames * 2);
    jshortArray arr = env->NewShortArray(len);
    if (!arr) return frames;
    env->SetShortArrayRegion(arr, 0, len, reinterpret_cast<const jshort*>(data));
    env->CallVoidMethod(g_callback, g_onAudio, arr);
    env->DeleteLocalRef(arr);
    return frames;
}

static void input_poll() {}

static int16_t input_state(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (port != 0) return 0;
    if (device == RETRO_DEVICE_JOYPAD && id < 16) {
        return g_joypad[id];
    }
    return 0;
}

static bool environment(unsigned cmd, void* data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            auto* fmt = reinterpret_cast<enum retro_pixel_format*>(data);
            if (*fmt == RETRO_PIXEL_FORMAT_RGB565 ||
                *fmt == RETRO_PIXEL_FORMAT_XRGB8888 ||
                *fmt == RETRO_PIXEL_FORMAT_0RGB1555) {
                g_pixelFormat = *fmt;
                return true;
            }
            return false;
        }
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY: {
            auto* dir = reinterpret_cast<const char**>(data);
            *dir = g_systemDir.c_str();
            return !g_systemDir.empty();
        }
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: {
            auto* dir = reinterpret_cast<const char**>(data);
            *dir = g_saveDir.c_str();
            return !g_saveDir.empty();
        }
        case RETRO_ENVIRONMENT_GET_CAN_DUPE: {
            *reinterpret_cast<bool*>(data) = true;
            return true;
        }
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_GET_VARIABLE:
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            return false;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: {
            return false;
        }
        default:
            return false;
    }
}

static void unloadLocked() {
    if (g_core.handle) {
        if (g_loaded && g_core.unload_game) g_core.unload_game();
        if (g_core.deinit) g_core.deinit();
        dlclose(g_core.handle);
    }
    g_core = CoreSymbols{};
    g_loaded = false;
    g_frameRgb565.clear();
    std::memset(g_joypad, 0, sizeof(g_joypad));
}

template <typename T>
static T loadSym(void* handle, const char* name) {
    return reinterpret_cast<T>(dlsym(handle, name));
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_vm = vm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeSetCallback(
    JNIEnv* env, jobject /*thiz*/, jobject callback) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_callback) {
        env->DeleteGlobalRef(g_callback);
        g_callback = nullptr;
    }
    if (callback) {
        g_callback = env->NewGlobalRef(callback);
        jclass cls = env->GetObjectClass(callback);
        g_onVideo = env->GetMethodID(cls, "onVideoFrame", "([SII)V");
        g_onAudio = env->GetMethodID(cls, "onAudioSamples", "([S)V");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeSetDirectories(
    JNIEnv* env, jobject /*thiz*/, jstring systemDir, jstring saveDir) {
    const char* sys = env->GetStringUTFChars(systemDir, nullptr);
    const char* save = env->GetStringUTFChars(saveDir, nullptr);
    g_systemDir = sys ? sys : "";
    g_saveDir = save ? save : "";
    if (sys) env->ReleaseStringUTFChars(systemDir, sys);
    if (save) env->ReleaseStringUTFChars(saveDir, save);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeLoadCore(
    JNIEnv* env, jobject /*thiz*/, jstring corePath) {
    std::lock_guard<std::mutex> lock(g_mutex);
    unloadLocked();

    const char* path = env->GetStringUTFChars(corePath, nullptr);
    void* handle = dlopen(path, RTLD_NOW);
    if (!handle) {
        LOGE("dlopen failed for %s: %s", path, dlerror());
        env->ReleaseStringUTFChars(corePath, path);
        return JNI_FALSE;
    }
    env->ReleaseStringUTFChars(corePath, path);

    g_core.handle = handle;
    g_core.set_environment = loadSym<retro_set_environment_t>(handle, "retro_set_environment");
    g_core.set_video_refresh = loadSym<retro_set_video_refresh_t>(handle, "retro_set_video_refresh");
    g_core.set_audio_sample = loadSym<retro_set_audio_sample_t>(handle, "retro_set_audio_sample");
    g_core.set_audio_sample_batch = loadSym<retro_set_audio_sample_batch_t>(handle, "retro_set_audio_sample_batch");
    g_core.set_input_poll = loadSym<retro_set_input_poll_t>(handle, "retro_set_input_poll");
    g_core.set_input_state = loadSym<retro_set_input_state_t>(handle, "retro_set_input_state");
    g_core.init = loadSym<retro_init_t>(handle, "retro_init");
    g_core.deinit = loadSym<retro_deinit_t>(handle, "retro_deinit");
    g_core.api_version = loadSym<retro_api_version_t>(handle, "retro_api_version");
    g_core.get_system_info = loadSym<retro_get_system_info_t>(handle, "retro_get_system_info");
    g_core.get_system_av_info = loadSym<retro_get_system_av_info_t>(handle, "retro_get_system_av_info");
    g_core.set_controller_port_device = loadSym<retro_set_controller_port_device_t>(handle, "retro_set_controller_port_device");
    g_core.reset = loadSym<retro_reset_t>(handle, "retro_reset");
    g_core.run = loadSym<retro_run_t>(handle, "retro_run");
    g_core.serialize_size = loadSym<retro_serialize_size_t>(handle, "retro_serialize_size");
    g_core.serialize = loadSym<retro_serialize_t>(handle, "retro_serialize");
    g_core.unserialize = loadSym<retro_unserialize_t>(handle, "retro_unserialize");
    g_core.load_game = loadSym<retro_load_game_t>(handle, "retro_load_game");
    g_core.unload_game = loadSym<retro_unload_game_t>(handle, "retro_unload_game");

    if (!g_core.set_environment || !g_core.init || !g_core.load_game || !g_core.run) {
        LOGE("Missing required libretro symbols");
        unloadLocked();
        return JNI_FALSE;
    }

    g_core.set_environment(environment);
    g_core.set_video_refresh(video_refresh);
    g_core.set_audio_sample(audio_sample);
    g_core.set_audio_sample_batch(audio_sample_batch);
    g_core.set_input_poll(input_poll);
    g_core.set_input_state(input_state);
    g_core.init();
    LOGI("Core loaded api=%u", g_core.api_version ? g_core.api_version() : 0);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeLoadGame(
    JNIEnv* env, jobject /*thiz*/, jstring path, jbyteArray romBytes) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_core.load_game) return JNI_FALSE;

    retro_system_info info{};
    if (g_core.get_system_info) g_core.get_system_info(&info);

    const char* cpath = env->GetStringUTFChars(path, nullptr);
    retro_game_info game{};
    game.path = cpath;

    jbyte* bytes = nullptr;
    jsize len = 0;
    std::vector<char> owned;
    if (romBytes != nullptr && !info.need_fullpath) {
        len = env->GetArrayLength(romBytes);
        bytes = env->GetByteArrayElements(romBytes, nullptr);
        game.data = bytes;
        game.size = static_cast<size_t>(len);
    } else if (!info.need_fullpath && romBytes == nullptr) {
        // Read from path into memory
        FILE* f = fopen(cpath, "rb");
        if (f) {
            fseek(f, 0, SEEK_END);
            long sz = ftell(f);
            fseek(f, 0, SEEK_SET);
            if (sz > 0) {
                owned.resize(static_cast<size_t>(sz));
                fread(owned.data(), 1, owned.size(), f);
                game.data = owned.data();
                game.size = owned.size();
            }
            fclose(f);
        }
    }

    bool ok = g_core.load_game(&game);
    if (bytes) env->ReleaseByteArrayElements(romBytes, bytes, JNI_ABORT);
    env->ReleaseStringUTFChars(path, cpath);

    if (!ok) {
        LOGE("retro_load_game failed");
        return JNI_FALSE;
    }
    g_loaded = true;
    if (g_core.set_controller_port_device) {
        g_core.set_controller_port_device(0, RETRO_DEVICE_JOYPAD);
    }
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeRunFrame(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_loaded && g_core.run) g_core.run();
}

extern "C" JNIEXPORT void JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeReset(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_loaded && g_core.reset) g_core.reset();
}

extern "C" JNIEXPORT void JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeUnload(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    unloadLocked();
}

extern "C" JNIEXPORT void JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeSetButton(
    JNIEnv*, jobject, jint id, jboolean pressed) {
    if (id < 0 || id >= 16) return;
    g_joypad[id] = pressed ? 1 : 0;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeGetAvInfo(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    jfloatArray arr = env->NewFloatArray(5);
    float values[5] = {320.f, 240.f, 60.f, 48000.f, 1.333f};
    if (g_core.get_system_av_info) {
        retro_system_av_info av{};
        g_core.get_system_av_info(&av);
        values[0] = static_cast<float>(av.geometry.base_width);
        values[1] = static_cast<float>(av.geometry.base_height);
        values[2] = static_cast<float>(av.timing.fps);
        values[3] = static_cast<float>(av.timing.sample_rate);
        values[4] = av.geometry.aspect_ratio > 0 ? av.geometry.aspect_ratio
            : (values[1] > 0 ? values[0] / values[1] : 1.333f);
    }
    env->SetFloatArrayRegion(arr, 0, 5, values);
    return arr;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeSerialize(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_loaded || !g_core.serialize_size || !g_core.serialize) return nullptr;
    size_t size = g_core.serialize_size();
    if (size == 0) return nullptr;
    std::vector<uint8_t> buf(size);
    if (!g_core.serialize(buf.data(), size)) return nullptr;
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(size));
    env->SetByteArrayRegion(arr, 0, static_cast<jsize>(size), reinterpret_cast<const jbyte*>(buf.data()));
    return arr;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retropocket_libretro_LibretroBridge_nativeUnserialize(
    JNIEnv* env, jobject, jbyteArray data) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_loaded || !g_core.unserialize || data == nullptr) return JNI_FALSE;
    jsize len = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    bool ok = g_core.unserialize(bytes, static_cast<size_t>(len));
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}
