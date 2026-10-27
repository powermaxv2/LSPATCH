package com.powermax.patchstudio.core

/** Application cible prise en charge (cosmétique uniquement). */
data class TargetApp(
    val id: String,
    val packageName: String,
    val displayName: String,
    val tagline: String,
    val brandColor: Int,
    val brandHueRange: ClosedFloatingPointRange<Float>,
)

object TargetApps {
    val DEEZER: TargetApp = TargetApp(
        id = "deezer",
        packageName = "deezer.android.app",
        displayName = "Deezer",
        tagline = "Thèmes, polices et animations pour ton appli musique",
        brandColor = 0xFFA238FF.toInt(),
        brandHueRange = 250f..305f,
    )

    val ALL: List<TargetApp> = listOf(DEEZER)

    fun byId(id: String): TargetApp? = ALL.firstOrNull { it.id == id }

    fun byPackage(pkg: String): TargetApp? = ALL.firstOrNull { it.packageName == pkg }
}
