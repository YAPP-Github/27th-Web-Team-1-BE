package kr.co.lokit.api.domain.album.presentation.mapping

import kr.co.lokit.api.common.permission.EditabilityPolicy
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.dto.SelectableAlbumResponse

fun AlbumRequest.toDomain(): Album =
    Album(
        title = title,
    )

fun List<Album>.toSelectableResponse(viewerUserId: Long): SelectableAlbumResponse =
    SelectableAlbumResponse(
        map {
            val actualPhotoCount =
                if (it.isDefault) {
                    it.photos.size
                } else {
                    it.photoCount
                }

            SelectableAlbumResponse.SelectableAlbum(
                id = it.id,
                title = it.title,
                photoCount = actualPhotoCount,
                thumbnailUrl = it.thumbnail?.url,
                isEditable =
                    EditabilityPolicy.canEditAlbum(
                        viewerUserId = viewerUserId,
                        createdByUserId = it.createdById,
                        isDefault = it.isDefault,
                    ),
            )
        },
    )
