ALTER TABLE statistic ADD COLUMN title VARCHAR(255) NULL;
UPDATE statistic s JOIN workplace w ON s.workplace_id = w.id SET s.title = w.title;
