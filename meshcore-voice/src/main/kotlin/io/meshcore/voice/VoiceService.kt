package io.meshcore.voice

import io.meshcore.core.lifecycle.MeshComponent
import io.meshcore.core.logging.MeshLogger

private val log = MeshLogger.getLogger("io.meshcore.voice")

/**
 * Voice call service.
 *
 * TODO: Implement real-time voice communication:
 *   - Opus audio codec for compression
 *   - RTP/SRTP for packetization and encryption
 *   - Jitter buffer for smooth playback
 *   - Echo cancellation (requires platform-specific DSP)
 *   - Platform audio capture: javax.sound.sampled on desktop, AudioRecord on Android/Termux
 *
 * Constraints: javax.sound.sampled is available on Linux JVM but NOT on Android/Termux.
 * Android-specific implementation requires a separate Termux/AAR variant.
 */
class VoiceService : MeshComponent {

    override val name: String = "VoiceService"

    override suspend fun start() {
        log.info("Voice service started (stub – not yet implemented)")
    }

    override suspend fun stop() {
        log.info("Voice service stopped")
    }
}
