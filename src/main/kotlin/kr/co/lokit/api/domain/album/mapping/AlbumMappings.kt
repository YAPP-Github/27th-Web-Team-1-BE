package kr.co.lokit.api.domain.album.mapping

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.dto.SelectableAlbumResponse
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.photo.mapping.toDomain
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceEntity

fun AlbumEntity.toDomain(): Album =
    Album(
        id = this.id,
        title = this.title,
        workspaceId = this.workspace.id,
        photoCount = this.photoCount,
    ).apply {
        this.photos = this@toDomain.photos.map { it.toDomain() }
        this.thumbnail = this@toDomain.thumbnail?.toDomain()
        this.thumbnails = this@toDomain.photos.take(4).map { it.toDomain() }
    }

fun Album.toEntity(workspace: WorkspaceEntity): AlbumEntity =
    AlbumEntity(
        title = this.title,
        workspace = workspace,
    )

fun AlbumRequest.toDomain(workspaceId: Long): Album =
    Album(
        title = this.title,
        workspaceId = workspaceId,
    )

fun List<Album>.toSelectableResponse(): SelectableAlbumResponse =
    SelectableAlbumResponse(map {
        SelectableAlbumResponse.SelectableAlbum(
            id = it.id,
            title = it.title,
            photoCount = it.photoCount,
            thumbnailUrls = it.thumbnails.mapNotNull { photo -> photo.url },
        )
    })
