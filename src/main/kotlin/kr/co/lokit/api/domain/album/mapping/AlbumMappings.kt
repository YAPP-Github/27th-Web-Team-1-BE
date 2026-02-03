package kr.co.lokit.api.domain.album.mapping

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.dto.SelectableAlbumResponse
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.photo.mapping.toDomain
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceEntity

fun AlbumEntity.toDomain(): Album =
    Album(
        id = this.nonNullId(),
        title = this.title,
        workspaceId = this.workspace.nonNullId(),
        photoCount = this.photoCount,
        isDefault = this.isDefault,
    ).apply {
        this.photos = this@toDomain.photos.map { it.toDomain() }
    }

fun Album.toEntity(workspace: WorkspaceEntity): AlbumEntity =
    AlbumEntity(
        title = this.title,
        workspace = workspace,
        isDefault = this.isDefault,
    )

fun AlbumRequest.toDomain(): Album =
    Album(
        title = this.title,
        workspaceId = 0,
    )

fun List<Album>.toSelectableResponse(): SelectableAlbumResponse =
    SelectableAlbumResponse(map {
        SelectableAlbumResponse.SelectableAlbum(
            id = it.id,
            title = it.title,
            photoCount = it.photoCount,
            thumbnailUrl = it.thumbnail?.url,
        )
    })
