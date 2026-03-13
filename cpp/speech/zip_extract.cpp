/**
 * zip_extract.cpp
 *
 * Minimal ZIP extractor for unpacking ggml-*-encoder.mlmodelc.zip bundles.
 * Supports STORE (method 0) and DEFLATE (method 8) entries.
 * Uses only zlib (system libz — always available on Apple platforms, Android NDK, Linux).
 *
 * Exposed as dai_extract_zip() in deviceai_speech_engine.h.
 */

#include "deviceai_speech_engine.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cerrno>
#include <sys/stat.h>
#include <zlib.h>

#ifdef _WIN32
#  include <direct.h>
#  define DAI_MKDIR(p) _mkdir(p)
#else
#  include <unistd.h>
#  define DAI_MKDIR(p) mkdir((p), 0755)
#endif

extern "C" {

// ─── Local file header layout ─────────────────────────────────────────────
//  Signature  4  0x04034b50
//  Version    2
//  Flags      2
//  Method     2   (0=store, 8=deflate)
//  ModTime    2
//  ModDate    2
//  CRC32      4
//  CompSize   4
//  UncompSize 4
//  FNameLen   2
//  ExtraLen   2
//  FileName   FNameLen bytes
//  ExtraData  ExtraLen bytes
//  Data       CompSize bytes

static const uint32_t ZIP_LOCAL_MAGIC = 0x04034b50u;

// Read little-endian u16/u32 without alignment assumptions
static inline uint16_t read_u16(const unsigned char *p) {
    return (uint16_t)(p[0] | ((unsigned)p[1] << 8));
}
static inline uint32_t read_u32(const unsigned char *p) {
    return (uint32_t)(p[0] | ((unsigned)p[1] << 8) |
                      ((unsigned)p[2] << 16) | ((unsigned)p[3] << 24));
}

// Recursively create all directory components in `path`.
static int make_dirs(const char *path) {
    char tmp[4096];
    strncpy(tmp, path, sizeof(tmp) - 1);
    tmp[sizeof(tmp) - 1] = '\0';

    for (char *p = tmp + 1; *p; p++) {
        if (*p == '/') {
            *p = '\0';
            if (DAI_MKDIR(tmp) != 0 && errno != EEXIST) return 0;
            *p = '/';
        }
    }
    return 1;
}

// Extract one DEFLATE-compressed entry using raw zlib inflate.
static int inflate_entry(FILE *src, FILE *dst, uint32_t comp_size) {
    const size_t BUF = 65536u;
    unsigned char *in_buf  = (unsigned char *)malloc(BUF);
    unsigned char *out_buf = (unsigned char *)malloc(BUF);
    if (!in_buf || !out_buf) { free(in_buf); free(out_buf); return 0; }

    z_stream zs;
    memset(&zs, 0, sizeof(zs));
    // -15 = raw deflate (no zlib/gzip wrapper)
    if (inflateInit2(&zs, -15) != Z_OK) {
        free(in_buf); free(out_buf); return 0;
    }

    uint32_t remaining = comp_size;
    int ok = 1;

    while (remaining > 0 && ok) {
        size_t to_read = (remaining < BUF) ? (size_t)remaining : BUF;
        size_t nread   = fread(in_buf, 1, to_read, src);
        if (nread == 0) { ok = 0; break; }
        remaining -= (uint32_t)nread;

        zs.next_in  = in_buf;
        zs.avail_in = (uInt)nread;

        do {
            zs.next_out  = out_buf;
            zs.avail_out = (uInt)BUF;

            int ret = inflate(&zs, Z_NO_FLUSH);
            if (ret == Z_STREAM_ERROR || ret == Z_DATA_ERROR || ret == Z_MEM_ERROR) {
                ok = 0; break;
            }

            size_t have = BUF - zs.avail_out;
            if (have > 0 && fwrite(out_buf, 1, have, dst) != have) {
                ok = 0; break;
            }
        } while (zs.avail_out == 0);
    }

    inflateEnd(&zs);
    free(in_buf);
    free(out_buf);
    return ok;
}

// Copy `size` bytes verbatim from src to dst (method=STORE).
static int copy_entry(FILE *src, FILE *dst, uint32_t size) {
    const size_t BUF = 65536u;
    unsigned char *buf = (unsigned char *)malloc(BUF);
    if (!buf) return 0;

    uint32_t remaining = size;
    int ok = 1;

    while (remaining > 0 && ok) {
        size_t to_read = (remaining < BUF) ? (size_t)remaining : BUF;
        size_t nread   = fread(buf, 1, to_read, src);
        if (nread == 0) { ok = 0; break; }
        if (fwrite(buf, 1, nread, dst) != nread) { ok = 0; break; }
        remaining -= (uint32_t)nread;
    }

    free(buf);
    return ok;
}

int dai_extract_zip(const char *zip_path, const char *dest_dir) {
    if (!zip_path || !dest_dir) return 0;

    FILE *f = fopen(zip_path, "rb");
    if (!f) return 0;

    // Ensure destination directory exists
    if (DAI_MKDIR(dest_dir) != 0 && errno != EEXIST) {
        fclose(f);
        return 0;
    }

    unsigned char hdr[30];     // fixed part of local file header
    char fname[2048];
    int entry_count = 0;
    int ok = 1;

    while (ok) {
        // Read the 4-byte signature
        if (fread(hdr, 1, 4, f) != 4) break;

        uint32_t sig = read_u32(hdr);
        if (sig != ZIP_LOCAL_MAGIC) break;   // central dir / end-of-central-dir

        // Read remaining 26 bytes of fixed header
        if (fread(hdr + 4, 1, 26, f) != 26) { ok = 0; break; }

        uint16_t method     = read_u16(hdr + 8);
        uint32_t comp_size  = read_u32(hdr + 18);
        uint32_t uncomp_size = read_u32(hdr + 22);
        uint16_t fname_len  = read_u16(hdr + 26);
        uint16_t extra_len  = read_u16(hdr + 28);

        if (fname_len == 0 || fname_len >= (uint16_t)sizeof(fname)) {
            ok = 0; break;
        }

        if (fread(fname, 1, fname_len, f) != fname_len) { ok = 0; break; }
        fname[fname_len] = '\0';

        // Skip extra field
        if (extra_len > 0) fseek(f, extra_len, SEEK_CUR);

        // Build full output path: dest_dir/fname
        char out_path[4096];
        snprintf(out_path, sizeof(out_path), "%s/%s", dest_dir, fname);

        bool is_dir = (fname[fname_len - 1] == '/');

        if (is_dir) {
            make_dirs(out_path);
            // No data bytes for directory entries
        } else {
            // Ensure parent directories exist
            char parent[4096];
            strncpy(parent, out_path, sizeof(parent) - 1);
            parent[sizeof(parent) - 1] = '\0';
            char *last_slash = strrchr(parent, '/');
            if (last_slash) {
                *last_slash = '\0';
                make_dirs(parent);
            }

            FILE *out = fopen(out_path, "wb");
            if (!out) { ok = 0; break; }

            if (method == 0) {
                ok = copy_entry(f, out, comp_size);
            } else if (method == 8) {
                ok = inflate_entry(f, out, comp_size);
            } else {
                // Unknown compression — skip
                fseek(f, comp_size, SEEK_CUR);
            }

            fclose(out);
            entry_count++;
        }
    }

    fclose(f);
    return (ok && entry_count > 0) ? 1 : 0;
}

} // extern "C"
