package io.meshcore.video

import io.meshcore.core.lifecycle.MeshComponent
import io.meshcore.core.logging.MeshLogger

private val log = MeshLogger.getLogger("io.meshcore.video")

/**
 * Video call service.
 *
 * TODO: Implement real-time video communication:
 *   - H.264 or VP8/VP9 video codec
 *   - WebRTC-compatible signaling
 *   - Platform video capture: V4L2 on Linux, Camera2 on Android/Termux
 *   - Screen sharing support
 *
 * Constraints: Video on JVM requires native libraries (e.g., GStreamer via JNI,
 * or Xuggler/FFmpeg wrapper). This is a significant platform-dependent dependency.
 * Full implementation deferred to a dedicated platform integration sprint.
 */
class VideoService : MeshComponent {

    override val name: String = "VideoService"

    override suspend fun start() {
        log.info("Video service started (stub – not yet implemented)")
    }

    override suspend fun stop() {
        log.info("Video service stopped")
    }
}
