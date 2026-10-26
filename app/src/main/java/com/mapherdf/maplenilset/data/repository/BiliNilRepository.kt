package com.mapherdf.maplenilset.data.repository

import com.mapherdf.maplenilset.core.bili.BiliInput
import com.mapherdf.maplenilset.core.bili.BiliInputParser
import com.mapherdf.maplenilset.data.downloads.BiliCoverMediaSaver
import com.mapherdf.maplenilset.data.remote.bili.BiliCoverDetails
import com.mapherdf.maplenilset.data.remote.bili.BiliCoverException
import com.mapherdf.maplenilset.data.remote.bili.BiliCoverFile
import com.mapherdf.maplenilset.data.remote.bili.BiliCoverRemoteDataSource
import com.mapherdf.maplenilset.data.remote.bili.BiliInputReference
import com.mapherdf.maplenilset.di.BiliCacheDirectory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiliNilRepository @Inject constructor(
    private val remoteDataSource: BiliCoverRemoteDataSource,
    private val mediaSaver: BiliCoverMediaSaver,
    @BiliCacheDirectory private val cacheDirectory: File,
) {

    suspend fun resolve(rawInput: String): BiliCoverDetails {
        val shortLinkCode = BiliInputParser.parseShortLinkCode(rawInput)
        val input = (shortLinkCode
            ?.let { code ->
                remoteDataSource.resolveShortLink(code).let { reference ->
                    BiliInput(reference.kind, reference.id)
                }
            }
            ?: BiliInputParser.parseContent(rawInput)
                ?: throw BiliCoverException(0, "Invalid Bilibili input"))
        return remoteDataSource.resolve(BiliInputReference(input.kind, input.id))
    }

    suspend fun downloadCover(details: BiliCoverDetails): BiliCoverFile =
        remoteDataSource.downloadCover(details.imageUrl, details.input.displayName)

    suspend fun downloadCoverByUrl(rawUrl: String, displayName: String): BiliCoverFile =
        remoteDataSource.downloadCover(rawUrl, displayName)

    suspend fun saveCover(
        details: BiliCoverDetails,
        coverFile: BiliCoverFile,
    ): Result<String> = mediaSaver.save(
        source = coverFile,
        input = BiliInput(details.input.kind, details.input.id),
    )

    fun cacheDirectory(): File = cacheDirectory
}
