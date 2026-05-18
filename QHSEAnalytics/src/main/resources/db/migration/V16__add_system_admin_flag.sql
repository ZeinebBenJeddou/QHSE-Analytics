-- Marque l'administrateur système avec un flag immuable, indépendant de l'email
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_system_admin BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users
SET is_system_admin = TRUE
WHERE role = 'ADMIN'
  AND created_at = (SELECT MIN(created_at) FROM users WHERE role = 'ADMIN');
