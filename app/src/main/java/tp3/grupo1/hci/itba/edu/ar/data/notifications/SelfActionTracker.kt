package tp3.grupo1.hci.itba.edu.ar.data.notifications

import java.util.concurrent.ConcurrentHashMap

/**
 * Suppresses notifications for actions the user performed from this app.
 *
 * Real-time events are broadcast to every client, including the one that
 * triggered them, so toggling a lock here would otherwise raise a notification
 * about your own action. Before issuing an action the repository [record]s a
 * signature; when the echoed event arrives the handler [consume]s it and skips
 * the notification. Signatures expire after [windowMillis] so a later event for
 * the same device (e.g. another user) still notifies.
 */
class SelfActionTracker(private val windowMillis: Long = 10_000L) {

    private val recent = ConcurrentHashMap<String, Long>()

    fun record(key: String) {
        recent[key] = System.currentTimeMillis()
    }

    /** Returns true when [key] was a recent self-action; removes it either way. */
    fun consume(key: String): Boolean {
        val recordedAt = recent.remove(key) ?: return false
        return System.currentTimeMillis() - recordedAt <= windowMillis
    }

    companion object {
        fun deviceState(deviceId: String) = "state:$deviceId"
        fun deviceCreated(deviceId: String) = "created:$deviceId"
        fun deviceDeleted(deviceId: String) = "deleted:$deviceId"
    }
}
