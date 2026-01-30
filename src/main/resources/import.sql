-- GiST 인덱스: JPA @Index는 btree만 지원하므로 별도 생성 필요
DROP INDEX IF EXISTS idx_photo_location;
CREATE INDEX idx_photo_location_gist ON photo USING gist(location);
