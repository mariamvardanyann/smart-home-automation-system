# Release checklist for both team members

Before making the GitHub repo public:

- [ ] Both team members review the names and teamwork wording in the README and CONTRIBUTORS.md.
- [ ] Agree who hosts the repository and whether to add both collaborators with write access.
- [ ] Decide a license together; **no license has been assigned unilaterally**.
- [ ] Rotate original SQL Server passwords and any other shared credentials from earlier project files.
- [ ] Run `python scripts/prepublish_check.py` and a dedicated secret scanner such as Gitleaks.
- [ ] Run JUnit tests (`backend/mvnw.cmd test`) with working Maven dependencies.
- [ ] Test a fresh SQL Server install, rule creation, event processing, logs and trigger conditions.
- [ ] If migrating coursework data, run `08_migrate_legacy.sql` on a backup and resolve every unmatched sensor.
- [ ] Do not upload the old PDF unchanged: it contains sample login passwords and outdated code.
- [ ] After both approve: initialize Git, make first commit and push the shared repository.

This is a **team repository**. The original work and later AI-assisted repository cleanup should never be presented as a solo achievement.
