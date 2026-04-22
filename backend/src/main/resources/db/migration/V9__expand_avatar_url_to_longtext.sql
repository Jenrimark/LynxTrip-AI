-- Allow storing base64 image data URLs for profile avatars.
ALTER TABLE users
  MODIFY COLUMN avatar_url LONGTEXT NULL;
