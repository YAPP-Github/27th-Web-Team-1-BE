CREATE INDEX IF NOT EXISTS idx_photo_location_gist_active ON photo USING gist(location) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_photo_location_album_active ON photo USING gist(location) WHERE is_deleted = false AND album_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_photo_taken_at_desc_active ON photo (taken_at DESC) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_photo_album_active ON photo (album_id) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_photo_uploaded_by_active ON photo (uploaded_by) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_photo_album_taken_at_desc ON photo (album_id, taken_at DESC) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_photo_created_at ON photo USING BRIN(created_at);

CREATE INDEX IF NOT EXISTS idx_album_updated_created_desc ON album (updated_at DESC, created_at DESC) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_album_photo_added_created_desc ON album (photo_added_at DESC NULLS LAST, created_at DESC) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_album_couple_default ON album (couple_id, is_default) WHERE is_default = true AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_album_couple_title ON album (couple_id, title) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_album_photo_added_at ON album USING BRIN(photo_added_at);

CREATE INDEX IF NOT EXISTS idx_couple_user_covering_active ON couple_user (user_id) INCLUDE (couple_id) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON refresh_token (expires_at);

CREATE INDEX IF NOT EXISTS idx_album_couple_id_active ON album (couple_id) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_couple_invite_code_active ON couple (invite_code) WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_photo_couple_location_active ON photo USING gist(location) WHERE is_deleted = false AND couple_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS unique_user_email_is_deleted ON users (email, is_deleted);
