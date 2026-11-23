package com.zalmoxis.app

/**
 * Public model files reviewed against the Google AI Edge Gallery model allowlist.
 *
 * Each entry is pinned to a Hugging Face commit and exact file digest. Do not accept
 * arbitrary repository names, revisions, URLs, or filenames from UI/worker input.
 */
data class ModelCatalogEntry(
    val id: String,
    val displayName: String,
    val repoId: String,
    val revision: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val licenseName: String,
    val licenseUrl: String,
    val minimumMemoryGb: Int,
    val modalities: Set<String>,
    val summary: String,
) {
    val downloadUrl: String
        get() = "https://huggingface.co/$repoId/resolve/$revision/$fileName?download=true"
}

object ModelCatalog {
    val entries: List<ModelCatalogEntry> = listOf(
        ModelCatalogEntry(
            id = "qwen2.5-1.5b-instruct",
            displayName = "Qwen 2.5 1.5B Instruct",
            repoId = "litert-community/Qwen2.5-1.5B-Instruct",
            revision = "5e6e7a58d03777898b31330a15d2e0a5a9107de3",
            fileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            sizeBytes = 1_597_922_856L,
            sha256 = "c4a9b7289365ab80fe599b844b77ad253eb3c53fb0e5d963d699146a6f01255e",
            licenseName = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            minimumMemoryGb = 6,
            modalities = setOf("text"),
            summary = "Compact text model prepared for LiteRT-LM.",
        ),
        ModelCatalogEntry(
            id = "gemma-4-e2b-it",
            displayName = "Gemma 4 E2B IT",
            repoId = "litert-community/gemma-4-E2B-it-litert-lm",
            revision = "6e5c4f1e395deb959c494953478fa5cec4b8008f",
            fileName = "gemma-4-E2B-it.litertlm",
            sizeBytes = 2_588_147_712L,
            sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            licenseName = "Apache-2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
            minimumMemoryGb = 8,
            modalities = setOf("text", "image", "audio"),
            summary = "Multimodal Gemma 4 model; verify device/runtime compatibility before loading.",
        ),
    )

    fun find(id: String): ModelCatalogEntry? = entries.firstOrNull { it.id == id }
}
