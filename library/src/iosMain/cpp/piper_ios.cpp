/**
 * piper_ios.cpp - C wrapper for Piper TTS (Text-to-Speech) on iOS
 */

#include "../c_interop/include/speech_ios.h"
#include "piper.hpp"

#include <string>
#include <vector>
#include <atomic>
#include <mutex>
#include <fstream>
#include <cstring>
#include <memory>

// ═══════════════════════════════════════════════════════════════
//                          GLOBAL STATE
// ═══════════════════════════════════════════════════════════════

static piper::PiperConfig g_config;
static piper::Voice g_voice;
static bool g_initialized = false;
static std::mutex g_mutex;
static std::atomic<bool> g_cancel_requested{false};

// Configuration
static std::atomic<int> g_speaker_id{-1};
static std::atomic<float> g_speech_rate{1.0f};
static std::atomic<int> g_sample_rate{22050};
static std::atomic<float> g_sentence_silence{0.2f};

// Debug logging
static bool debug_enabled() {
    static int enabled = -1;
    if (enabled < 0) {
        const char *env = getenv("SPEECHKMP_DEBUG");
        enabled = (env && strcmp(env, "1") == 0) ? 1 : 0;
    }
    return enabled == 1;
}

#define LOG_DEBUG(fmt, ...) if (debug_enabled()) fprintf(stderr, "[SpeechKMP-TTS] " fmt "\n", ##__VA_ARGS__)
#define LOG_ERROR(fmt, ...) fprintf(stderr, "[SpeechKMP-TTS ERROR] " fmt "\n", ##__VA_ARGS__)

// ═══════════════════════════════════════════════════════════════
//                      HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

static bool write_wav_file(const std::string &path, const std::vector<int16_t> &samples, int sample_rate) {
    std::ofstream file(path, std::ios::binary);
    if (!file.is_open()) {
        LOG_ERROR("Failed to open file for writing: %s", path.c_str());
        return false;
    }

    uint32_t data_size = samples.size() * sizeof(int16_t);
    uint32_t file_size = 36 + data_size;

    // RIFF header
    file.write("RIFF", 4);
    file.write(reinterpret_cast<char*>(&file_size), 4);
    file.write("WAVE", 4);

    // fmt chunk
    file.write("fmt ", 4);
    uint32_t fmt_size = 16;
    file.write(reinterpret_cast<char*>(&fmt_size), 4);
    uint16_t audio_format = 1;
    file.write(reinterpret_cast<char*>(&audio_format), 2);
    uint16_t num_channels = 1;
    file.write(reinterpret_cast<char*>(&num_channels), 2);
    uint32_t sr = sample_rate;
    file.write(reinterpret_cast<char*>(&sr), 4);
    uint32_t byte_rate = sample_rate * num_channels * sizeof(int16_t);
    file.write(reinterpret_cast<char*>(&byte_rate), 4);
    uint16_t block_align = num_channels * sizeof(int16_t);
    file.write(reinterpret_cast<char*>(&block_align), 2);
    uint16_t bits_per_sample = 16;
    file.write(reinterpret_cast<char*>(&bits_per_sample), 2);

    // data chunk
    file.write("data", 4);
    file.write(reinterpret_cast<char*>(&data_size), 4);
    file.write(reinterpret_cast<const char*>(samples.data()), data_size);

    file.close();
    return true;
}

// ═══════════════════════════════════════════════════════════════
//                        C API FUNCTIONS
// ═══════════════════════════════════════════════════════════════

bool speech_tts_init(const char *model_path, const char *config_path,
                     int speaker_id, float speech_rate,
                     int sample_rate, float sentence_silence) {

    std::lock_guard<std::mutex> lock(g_mutex);

    // Cleanup existing state
    if (g_initialized) {
        piper::terminate(g_config);
        g_initialized = false;
    }

    g_speaker_id = speaker_id;
    g_speech_rate = speech_rate;
    g_sample_rate = sample_rate;
    g_sentence_silence = sentence_silence;

    LOG_DEBUG("Initializing Piper TTS");
    LOG_DEBUG("Model: %s", model_path);
    LOG_DEBUG("Config: %s", config_path);

    try {
        piper::initialize(g_config);

        std::optional<piper::SpeakerId> sid;
        if (speaker_id >= 0) {
            sid = static_cast<piper::SpeakerId>(speaker_id);
        }

        piper::loadVoice(g_config, model_path, config_path, g_voice, sid);

        if (speech_rate != 1.0f) {
            g_voice.synthesisConfig.lengthScale = 1.0f / speech_rate;
        }
        g_voice.synthesisConfig.sentenceSilenceSeconds = sentence_silence;

        g_initialized = true;
        LOG_DEBUG("Piper TTS initialized successfully");
        return true;

    } catch (const std::exception &e) {
        LOG_ERROR("Failed to initialize Piper: %s", e.what());
        return false;
    }
}

int16_t *speech_tts_synthesize(const char *text, int *out_length) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_initialized) {
        LOG_ERROR("Piper not initialized");
        *out_length = 0;
        return nullptr;
    }

    g_cancel_requested = false;

    try {
        std::vector<int16_t> audio;
        piper::SynthesisResult result;

        piper::textToAudio(g_config, g_voice, text, audio, result, nullptr);

        if (audio.empty()) {
            LOG_ERROR("Synthesis produced no audio");
            *out_length = 0;
            return nullptr;
        }

        LOG_DEBUG("Synthesized %zu samples", audio.size());

        // Allocate and copy
        int16_t *samples = static_cast<int16_t*>(malloc(audio.size() * sizeof(int16_t)));
        if (samples) {
            memcpy(samples, audio.data(), audio.size() * sizeof(int16_t));
            *out_length = audio.size();
        } else {
            *out_length = 0;
        }

        return samples;

    } catch (const std::exception &e) {
        LOG_ERROR("Synthesis failed: %s", e.what());
        *out_length = 0;
        return nullptr;
    }
}

bool speech_tts_synthesize_to_file(const char *text, const char *output_path) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_initialized) {
        LOG_ERROR("Piper not initialized");
        return false;
    }

    g_cancel_requested = false;

    try {
        std::vector<int16_t> audio;
        piper::SynthesisResult result;

        piper::textToAudio(g_config, g_voice, text, audio, result, nullptr);

        if (audio.empty()) {
            LOG_ERROR("Synthesis produced no audio");
            return false;
        }

        int sr = g_voice.synthesisConfig.sampleRate;
        if (!write_wav_file(output_path, audio, sr)) {
            return false;
        }

        LOG_DEBUG("Wrote %zu samples to %s", audio.size(), output_path);
        return true;

    } catch (const std::exception &e) {
        LOG_ERROR("Synthesis failed: %s", e.what());
        return false;
    }
}

void speech_tts_synthesize_stream(const char *text,
                                   tts_on_chunk on_chunk,
                                   tts_on_complete on_complete,
                                   tts_on_error on_error,
                                   void *user) {

    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_initialized) {
        if (on_error) on_error("Piper not initialized", user);
        return;
    }

    g_cancel_requested = false;

    try {
        std::vector<int16_t> audio;
        piper::SynthesisResult result;

        auto audioCallback = [&](std::vector<int16_t> &chunk) {
            if (g_cancel_requested) {
                return false;
            }

            if (on_chunk && !chunk.empty()) {
                on_chunk(chunk.data(), chunk.size(), user);
            }

            return true;
        };

        piper::textToAudio(g_config, g_voice, text, audio, result, audioCallback);

        if (!g_cancel_requested) {
            // Send remaining audio
            if (on_chunk && !audio.empty()) {
                on_chunk(audio.data(), audio.size(), user);
            }

            if (on_complete) {
                on_complete(user);
            }
        }

    } catch (const std::exception &e) {
        if (on_error) on_error(e.what(), user);
    }
}

void speech_tts_cancel(void) {
    g_cancel_requested = true;
}

void speech_tts_shutdown(void) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (g_initialized) {
        LOG_DEBUG("Shutting down Piper TTS");
        piper::terminate(g_config);
        g_initialized = false;
    }
}

void speech_free_audio(int16_t *ptr) {
    if (ptr) {
        free(ptr);
    }
}

void speech_shutdown_all(void) {
    speech_stt_shutdown();
    speech_tts_shutdown();
}
