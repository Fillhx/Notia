package co.edu.univalle.notia.features.teacher

/** RF-13: retroalimentación escrita en una nota. */
const val FEEDBACK_MAX_LENGTH = 500

/** Texto listo para guardar: sin espacios sobrantes; vacío significa quitar el comentario (`null`). */
fun normalizeFeedback(text: String): String? = text.trim().takeIf { it.isNotEmpty() }?.take(FEEDBACK_MAX_LENGTH)

/** Mensaje de cuándo verá el estudiante el comentario, según el estado de la actividad. */
fun feedbackVisibilityText(studentName: String, activityName: String, status: ActivityStatus): String {
    val firstName = studentName.trim().substringBefore(' ')
    return when (status) {
        ActivityStatus.DRAFT ->
            "La nota está en borrador: $firstName verá este comentario junto con su nota cuando publiques $activityName."
        ActivityStatus.PUBLISHED ->
            "$activityName ya está publicada: $firstName verá este comentario de inmediato y recibirá una notificación."
    }
}
