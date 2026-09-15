package com.hjinlabs.sengkode.core.model

/**
 * User-facing export configuration. Sizes map to the three supported
 * output resolutions; PngFormat is the only v1 format.
 */
data class ExportSpec(
    val displayName: String,
    val sizePx: Int = DEFAULT_SIZE,
) {
    init {
        require(sizePx in MIN_SIZE..MAX_SIZE) {
            "Export size must be between $MIN_SIZE and $MAX_SIZE, was $sizePx"
        }
    }

    companion object {
        const val SMALL = 512
        const val MEDIUM = 1024
        const val LARGE = 2048
        const val DEFAULT_SIZE = MEDIUM

        // ExportSpec is constructed only from the UI's fixed choices;
        // the bounds are defensive, not a UX surface.
        private const val MIN_SIZE = 256
        private const val MAX_SIZE = 4096
    }
}
