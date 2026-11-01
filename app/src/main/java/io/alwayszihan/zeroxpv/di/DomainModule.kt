package io.alwayszihan.zeroxpv.di

import io.alwayszihan.zeroxpv.domain.anime4k.Anime4KManager
import io.alwayszihan.zeroxpv.domain.hdr.HdrToysManager
import io.alwayszihan.zeroxpv.domain.thumbnail.CoilVideoThumbnailDecoder
import io.alwayszihan.zeroxpv.domain.thumbnail.toThumbnailStrategy
import io.alwayszihan.zeroxpv.network.AndroidCookieJar
import io.alwayszihan.zeroxpv.preferences.BrowserPreferences
import io.alwayszihan.zeroxpv.repository.IntroDbRepository
import io.alwayszihan.zeroxpv.preferences.AiPreferences
import io.alwayszihan.zeroxpv.repository.ai.AiClient
import io.alwayszihan.zeroxpv.repository.ai.AiService
import io.alwayszihan.zeroxpv.repository.ai.AnthropicClient
import io.alwayszihan.zeroxpv.repository.ai.GroqClient
import io.alwayszihan.zeroxpv.repository.ai.GroqSpeechClient
import io.alwayszihan.zeroxpv.repository.ai.LlmInference
import io.alwayszihan.zeroxpv.repository.ai.LlamaCppInference
import io.alwayszihan.zeroxpv.repository.ai.LocalAiClient
import io.alwayszihan.zeroxpv.repository.ai.ModelDownloadManager
import io.alwayszihan.zeroxpv.repository.ai.OpenAiClient
import io.alwayszihan.zeroxpv.repository.ai.OpenCodeClient
import io.alwayszihan.zeroxpv.repository.ai.OpenRouterClient
import io.alwayszihan.zeroxpv.repository.ai.OpenRouterSpeechClient
import io.alwayszihan.zeroxpv.repository.ai.RealtimeSubtitleService
import io.alwayszihan.zeroxpv.repository.ai.SubtitleGenerationService
import io.alwayszihan.zeroxpv.repository.ai.TogetherClient
import io.alwayszihan.zeroxpv.repository.subtitle.OnlineSubtitleFileStore
import io.alwayszihan.zeroxpv.repository.subtitle.OnlineSubtitleOrchestrator
import io.alwayszihan.zeroxpv.repository.subtitlehub.ZeroXPVSubtitleHubRepository
import io.alwayszihan.zeroxpv.repository.wyzie.WyzieSearchRepository
import kotlinx.serialization.json.Json
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.FileSystem
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val domainModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(AndroidCookieJar())
            .build()
    }
    single<ImageLoader> {
        val context = androidContext()
        val browserPreferences = get<BrowserPreferences>()

        val imageHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val host = original.url.host
                val request = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .header("Referer", "https://$host")
                    .build()
                chain.proceed(request)
            })
            .build()

        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { imageHttpClient }))
                add(
                    CoilVideoThumbnailDecoder.Factory(
                        thumbnailStrategy = {
                            browserPreferences.thumbnailMode.get().toThumbnailStrategy(
                                browserPreferences.thumbnailFramePosition.get()
                            )
                        }
                    )
                )
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache(
                DiskCache.Builder()
                    .fileSystem(FileSystem.SYSTEM)
                    .directory(context.filesDir.resolve("thumbnails"))
                    .maxSizePercent(1.0)
                    .build()
            )
            .crossfade(true)
            .build()
    }
    single { Anime4KManager(androidContext()) }
    single { HdrToysManager(androidContext()) }
    single { OnlineSubtitleFileStore(androidContext(), get()) }
    single { WyzieSearchRepository(androidContext(), get(), get(), get(), get()) }
    single { ZeroXPVSubtitleHubRepository(get(), get(), get(), get()) }
    single { OnlineSubtitleOrchestrator(get<WyzieSearchRepository>(), get<ZeroXPVSubtitleHubRepository>()) }
    single { IntroDbRepository(get(), get()) }
    single { OnlineSubtitleOrchestrator(get<WyzieSearchRepository>(), get<ZeroXPVSubtitleHubRepository>()) }

    // ── AI integration ────────────────────────────────────────
    single { AiPreferences(get()) }
    single { OpenCodeClient(get(), get()) }
    single { GroqClient(get(), get()) }
    single { OpenAiClient(get(), get()) }
    single { AnthropicClient(get(), get()) }
    single { OpenRouterClient(get(), get()) }
    single { TogetherClient(get(), get()) }
    single { GroqSpeechClient(get(), get()) }
    single { OpenRouterSpeechClient(get(), get()) }
    single<LlmInference> { LlamaCppInference() }
    single<AiClient>(named("opencode")) { OpenCodeClient(get(), get()) }
    single<AiClient>(named("groq")) { GroqClient(get(), get()) }
    single<AiClient>(named("openai")) { OpenAiClient(get(), get()) }
    single<AiClient>(named("anthropic")) { AnthropicClient(get(), get()) }
    single<AiClient>(named("openrouter")) { OpenRouterClient(get(), get()) }
    single<AiClient>(named("together")) { TogetherClient(get(), get()) }
    single { LocalAiClient(get()) }
    single { ModelDownloadManager(get()) }
    single { SubtitleGenerationService(androidContext(), get(), get(), get(), get(), get()) }
    single { RealtimeSubtitleService(androidContext(), get(), get(), get(), get(), get()) }
    single {
        AiService(
            androidContext(),
            get<AiPreferences>(),
            get<AiClient>(named("opencode")),
            get<AiClient>(named("groq")),
            get<AiClient>(named("openai")),
            get<AiClient>(named("anthropic")),
            get<AiClient>(named("openrouter")),
            get<AiClient>(named("together")),
            get<LocalAiClient>(),
            get<ModelDownloadManager>(),
            get<Json>(),
        )
    }
}
