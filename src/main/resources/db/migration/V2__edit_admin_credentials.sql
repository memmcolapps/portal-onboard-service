-- Update password for existing operator
UPDATE portal_users
SET password = '$2a$10$vpdTuh9gul9V3YTye2DNXeKOz4QKU1JFssIgcXm0YSrn.ySonEnrO',
    updated_at = NOW()
WHERE email = 'memmcolapp@gmail.com';

--$2a$10$vpdTuh9gul9V3YTye2DNXeKOz4QKU1JFssIgcXm0YSrn.ySonEnrO