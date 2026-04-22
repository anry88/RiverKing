package service

import app.Env
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val POLICY_RESOURCE_PATH = "android-update-policy.json"
private const val DEFAULT_APK_FILE_PREFIX = "riverking"

class AndroidUpdateService(
    private val env: Env,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    @Serializable
    private data class PolicyDocument(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val minSupportedVersionCode: Int,
        val requireVersionHeaders: Boolean = false,
        val releaseNotes: LocalizedNotes = LocalizedNotes(),
    )

    @Serializable
    private data class LocalizedNotes(
        val en: List<String> = emptyList(),
        val ru: List<String> = emptyList(),
    )

    @Serializable
    data class AppUpdateResponse(
        val status: String,
        val latestVersionCode: Int,
        val latestVersionName: String,
        val minSupportedVersionCode: Int,
        val mandatory: Boolean,
        val releaseNotes: List<String> = emptyList(),
        val installMode: String,
        val installUrl: String,
        val fallbackUrl: String? = null,
        val downloadFileName: String? = null,
    )

    private data class InstallTarget(
        val mode: String,
        val url: String,
        val fallbackUrl: String? = null,
        val downloadFileName: String? = null,
    )

    private val policy: PolicyDocument by lazy(::loadPolicy)

    fun checkFromHeaders(headers: Headers): AppUpdateResponse =
        check(
            versionCode = headers[HEADER_VERSION_CODE]?.toIntOrNull(),
            versionName = headers[HEADER_VERSION_NAME],
            channel = headers[HEADER_CHANNEL],
            localeHint = headers[HttpHeaders.AcceptLanguage],
        )

    @Suppress("UNUSED_PARAMETER")
    fun check(
        versionCode: Int?,
        versionName: String?,
        channel: String?,
        localeHint: String?,
    ): AppUpdateResponse {
        val normalizedChannel = normalizeChannel(channel)
        val installTarget = installTarget(normalizedChannel)
        val status = when {
            versionCode == null && policy.requireVersionHeaders -> STATUS_UPDATE_REQUIRED
            versionCode == null -> STATUS_UPDATE_AVAILABLE
            versionCode < policy.minSupportedVersionCode -> STATUS_UPDATE_REQUIRED
            versionCode < policy.latestVersionCode -> STATUS_UPDATE_AVAILABLE
            else -> STATUS_UP_TO_DATE
        }
        return AppUpdateResponse(
            status = status,
            latestVersionCode = policy.latestVersionCode,
            latestVersionName = policy.latestVersionName,
            minSupportedVersionCode = policy.minSupportedVersionCode,
            mandatory = status == STATUS_UPDATE_REQUIRED,
            releaseNotes = localizedNotes(localeHint),
            installMode = installTarget.mode,
            installUrl = installTarget.url,
            fallbackUrl = installTarget.fallbackUrl,
            downloadFileName = installTarget.downloadFileName,
        )
    }

    private fun localizedNotes(localeHint: String?): List<String> {
        val prefersRussian = localeHint
            ?.lowercase()
            ?.split(',', ';')
            ?.firstOrNull()
            ?.trim()
            ?.startsWith("ru") == true
        return if (prefersRussian) {
            policy.releaseNotes.ru.ifEmpty { policy.releaseNotes.en }
        } else {
            policy.releaseNotes.en.ifEmpty { policy.releaseNotes.ru }
        }
    }

    private fun installTarget(channel: String): InstallTarget {
        val supportFallback = "${env.publicBaseUrl.trimEnd('/')}/support"
        val itchPage = env.itchProjectUrl.ifBlank { supportFallback }
        val playStore = env.playStoreUrl.ifBlank {
            env.googlePlayPackageName
                .takeIf { it.isNotBlank() }
                ?.let { "https://play.google.com/store/apps/details?id=$it" }
                ?: itchPage
        }
        return if (channel == CHANNEL_DIRECT && env.androidDirectDownloadUrl.isNotBlank()) {
            InstallTarget(
                mode = INSTALL_MODE_DOWNLOAD_APK,
                url = env.androidDirectDownloadUrl,
                fallbackUrl = itchPage.takeIf { it != env.androidDirectDownloadUrl },
                downloadFileName = "$DEFAULT_APK_FILE_PREFIX-${policy.latestVersionName}.apk",
            )
        } else if (channel == CHANNEL_PLAY) {
            InstallTarget(
                mode = INSTALL_MODE_EXTERNAL,
                url = playStore,
                fallbackUrl = itchPage.takeIf { it != playStore },
            )
        } else {
            InstallTarget(
                mode = INSTALL_MODE_EXTERNAL,
                url = itchPage,
            )
        }
    }

    private fun normalizeChannel(channel: String?): String =
        if (channel.equals(CHANNEL_PLAY, ignoreCase = true)) CHANNEL_PLAY else CHANNEL_DIRECT

    private fun loadPolicy(): PolicyDocument {
        val raw = AndroidUpdateService::class.java.classLoader
            .getResourceAsStream(POLICY_RESOURCE_PATH)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("$POLICY_RESOURCE_PATH not found")
        return json.decodeFromString(PolicyDocument.serializer(), raw)
    }

    companion object {
        const val HEADER_PLATFORM = "X-RiverKing-App-Platform"
        const val HEADER_CHANNEL = "X-RiverKing-App-Channel"
        const val HEADER_VERSION_CODE = "X-RiverKing-App-Version-Code"
        const val HEADER_VERSION_NAME = "X-RiverKing-App-Version-Name"
        const val PLATFORM_ANDROID = "android"

        const val CHANNEL_DIRECT = "direct"
        const val CHANNEL_PLAY = "play"

        const val STATUS_UP_TO_DATE = "up_to_date"
        const val STATUS_UPDATE_AVAILABLE = "update_available"
        const val STATUS_UPDATE_REQUIRED = "update_required"

        const val INSTALL_MODE_EXTERNAL = "external"
        const val INSTALL_MODE_DOWNLOAD_APK = "download_apk"
    }
}
