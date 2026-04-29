-- ============================================================
--  Run ONCE in your database after the first app start.
--  Hibernate creates the 'departments' table via ddl-auto=update.
--
--  All passwords are BCrypt-hashed (strength 10).
--  Raw password for every department: dept@123
-- ============================================================

INSERT INTO departments (name, type, email, password_hash) VALUES
  ('Electrical Department', 'Electrical', 'electrical@sitpune.edu.in',
   '$2a$10$scmQR9ZYWODsrQUpgYvL8.CZCom4gnS9fkiKvdc4NPfbiqBlI7oJm'),

  ('IT Department',         'IT',         'it@sitpune.edu.in',
   '$2a$10$/oXEBwDz2br4VJJmQkQdQOPQLmlANa3SnCUkzantg4hwr8ILRgcva'),

  ('Cleaning Department',   'Cleaning',   'cleaning@sitpune.edu.in',
   '$2a$10$TkULSCG4apDJflbCFoZzwO53oL1krFQu0PhtQoOZ.AEbKN/Z3cbkC'),

  ('Hostel Department',     'Hostel',     'hostel@sitpune.edu.in',
   '$2a$10$UT1IGZct3WQHyjp.FzgVM.dLyMOknM7dPaZVAQnpqN6RAEYO4s5ji'),

  ('Plumbing Department',   'Plumbing',   'plumbing@sitpune.edu.in',
   '$2a$10$7Zkilc/pDMdazXBipVl6IOi/qqIvWmeinURjLFD/a63ke7plABay2'),

  ('General Department',    'General',    'general@sitpune.edu.in',
   '$2a$10$B/pxBGshJXG8zR9/1WfsU.wi5Q3Wk6bl3vwJD4O4/rmdIFmuVzwRq')

ON CONFLICT (type) DO NOTHING;
