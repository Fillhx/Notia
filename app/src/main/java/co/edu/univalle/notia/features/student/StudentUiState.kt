package co.edu.univalle.notia.features.student

/** Estado de carga simple para las pantallas del estudiante. */
sealed interface StudentLoadState<out T> {
    object Loading : StudentLoadState<Nothing>
    data class Success<T>(val data: T) : StudentLoadState<T>
    data class Error(val message: String) : StudentLoadState<Nothing>
}
