package com.korealm.lumina.ui.people

/**
 * User intents emitted by the people composable.
 *
 * Responsibility: bundle the people screen's callbacks so the composable stays a pure renderer
 * (FE-INV-051). The composable never calls a repository directly; it only invokes these.
 *
 * @param onNameChange the "add person" name field changed.
 * @param onRefresh the "Actualizar" button was pressed (`people.list`).
 * @param onEnrollFromCamera "Con la cámara" was pressed.
 * @param onImagesPicked the photo picker returned these raw images (empty when cancelled).
 * @param onCancelEnrollment "Cancelar" was pressed during an enrollment.
 * @param onStartRuntime the "Iniciar Lúmina" recovery button was pressed.
 * @param onDismissMessage the shown message should be cleared.
 */
data class PeopleActions(
    val onNameChange: (String) -> Unit,
    val onRefresh: () -> Unit,
    val onEnrollFromCamera: () -> Unit,
    val onImagesPicked: (List<ByteArray>) -> Unit,
    val onCancelEnrollment: () -> Unit,
    val onStartRuntime: () -> Unit,
    val onDismissMessage: () -> Unit,
)
