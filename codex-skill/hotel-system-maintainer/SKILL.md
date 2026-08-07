---
name: hotel-system-maintainer
description: Maintain and extend the HotelSystem Spring Boot application at the current workspace. Use for requests involving HotelSystem pages, PostgreSQL schema, master data, payments, bookings, guests, monthly rent, deposit refunds, reports, JasperReports, dark mode, Thai labels, build, clean, restart, or debugging page errors.
---

# HotelSystem Maintainer

Work as a senior maintainer of this existing Spring Boot hotel-management system. Read the current code and database-facing configuration before editing. Keep changes focused and preserve unrelated dirty-worktree changes.

## Non-negotiable rules

- Never revert user changes or use destructive git reset/checkout commands.
- Never reintroduce Java enums or enum-like database columns for business statuses/types. Use a master table with an ID foreign key and keep code values only for internal compatibility/conditions.
- Display Thai master names in UI; do not render internal codes such as `PAID`, `PENDING`, `MONTHLY`, or `AVAILABLE` to users.
- When a label/status is shown in a template, expose a label getter or master entity. Keep the code getter for filtering and business logic.
- Use existing repository/service/controller patterns. Do not create duplicate queries when an existing service already supplies the data.
- Treat transaction data as real data. Do not clear, migrate, or modify it unless the user explicitly requests it and the operation is reviewed first.
- Use `apply_patch` for manual source edits.

## Workflow

1. Inspect `git status`, relevant controller, model, repository, template, schema initializer, and current logs.
2. Trace the full path: template -> controller -> service -> repository -> database/master relation.
3. Make the smallest coherent change. Update both display labels and any code paths that depend on the old field.
4. Build with the project Maven wrapper/runtime:

   ```powershell
   .\.maven\apache-maven-3.9.9\bin\mvn.cmd -q -DskipTests package
   ```

5. If stale generated classes or a locked jar are involved, stop port 8080 first, then use:

   ```powershell
   .\.maven\apache-maven-3.9.9\bin\mvn.cmd -q clean package -DskipTests
   ```

6. Start port 8080 without a visible command window:

   ```powershell
   $java='C:\Program Files\Java\jdk-23\bin\java.exe'
   $jar=(Resolve-Path 'target\hotel-system-0.0.1-SNAPSHOT.jar').Path
   Start-Process -FilePath $java -ArgumentList ('-jar "'+$jar+'"') -WindowStyle Hidden `
     -RedirectStandardOutput 'target\hotel-system.log' `
     -RedirectStandardError 'target\hotel-system.err'
   ```

7. Verify port 8080 is listening and inspect the final startup log for errors. For page fixes, request the route and distinguish expected 302 authentication redirects from 500 errors.

## UI conventions

- Page headings come from `t_menu`; do not hardcode a different heading or add an unnecessary description.
- All modals must close from the backdrop and ESC, with correct z-index above the header and toast alerts.
- Keep loading/progress visible during save/check-in/check-out/report generation.
- Use existing theme colors, icons, pagination, sortable tables, and Thai/English bilingual document labels.
- Do not display a raw status/code in a table, badge, modal, report, or dropdown when a master Thai name exists.

## Data and reports

- Payments are linked to receipts; payment details use `t_payment_detail` linked to `t_payment_item`.
- Running numbers follow the current configured type-code format. Do not invent a second numbering path.
- Jasper report changes require editing the matching `.jrxml`, compiling the matching `.jasper`, then restarting the application. Reuse the same data service/query as the datatable when that is the established design.
- Check report parameters, resource paths, font extensions, page count expressions, and PDF response headers when a report downloads incorrectly or preview fails.
- For schema changes, update the initializer/migration, model mapping, repository queries, seed/master data, and existing data migration together.

## Debugging checklist

- A 500 after a schema change usually means a stale column reference, lazy master relation, repository query still using a removed field, or a template property that no longer exists.
- Search all Java, HTML, SQL, JRXML, and configuration files for the old field/name before declaring a migration complete.
- Verify master IDs and Thai names in PostgreSQL, not only in seed code.
- After a pull, re-read changed files and rebuild; do not assume the previous compiled jar contains the new code.
- If a class is suspected unused, distinguish an unused source class from a stale `target/classes` file. Run `mvn clean` to remove stale generated classes; delete source only after checking annotations, reflection, repositories, templates, initializers, and configuration references.
