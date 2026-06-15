package tp3.grupo1.hci.itba.edu.ar.data.notifications

import java.util.concurrent.ConcurrentHashMap

// Suprime notificaciones de acciones que hizo el propio usuario: los eventos en tiempo real se emiten a todos los clientes, incluido el que las disparo.
// El repositorio registra una firma antes de la accion y el handler la consume al llegar el evento; las firmas expiran tras windowMillis para que un evento posterior (ej. otro usuario) si notifique.
class SelfActionTracker(private val windowMillis: Long = 10_000L) {

    private val recent = ConcurrentHashMap<String, Long>()

    fun record(key: String) {
        recent[key] = System.currentTimeMillis()
    }

    // Devuelve true si key fue una self-action reciente; la elimina en cualquier caso.
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
