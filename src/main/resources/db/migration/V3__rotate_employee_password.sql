UPDATE accounts
SET password_hash = '${employeehash}'
WHERE email = 'employee@bookstore.local'
  AND role = 'EMPLOYEE';
