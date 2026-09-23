Elza DMS Storage
================

This directory holds all the files Elza manages: attachments of
archival files (arr_file), generated outputs (arr_output_file),
publication exports and source files of import batches.

The layout is fixed:

  dms/<year>/<month>/<block>/<id>.<ext>

where <id> is the value of dms_file.file_id. The authoritative
path is stored in the dms_file.storage_path column; the extension
in the file name is only a hint for the operator. Do not rename,
move or delete these files by hand — Elza will find them only
where it put them itself.

Special names:

  _trash/<date>/    files awaiting physical deletion together with
                    report.txt (name;size;time;reason). They are
                    purged automatically after the retention period
                    (elza.dms.trashRetentionDays, default 30 days).
                    Within retention the delete is reversible.
  _migration-*.txt  reports from the migration off the old flat
                    layout; keep them for later analysis.
  *.tmp             a write in progress. Orphan *.tmp files older
                    than elza.dms.orphanMinAgeMinutes are cleaned
                    up by the consistency check (default 60 min).

The dms/ directory and the Elza database form a single backup
unit and must be backed up and restored together.
