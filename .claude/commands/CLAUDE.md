# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is the **Edusign Database** repository - a centralized package for database migrations used across the EduSign ecosystem. It is designed to be included in other repositories as a **git submodule**.

## Usage as Git Submodule

```bash
# In host repos
git submodule add <repo-url> database
git submodule update --init --recursive
```

Host repos provide:
- **knexfile.ts** - database connection configuration
- **@eduTypes/** - business logic types and constants via path aliases

This repo provides:
- **Migrations** - all database schema evolution
- **Table names** - `KNEX_TABLE` constants (local copy)

## Import Strategy

### Table Names → Local (Snapshot)

Table names are kept in `src/tables/table_names.ts` within this repo.

**Why?** Migrations are snapshots of database state at a point in time. Table names are structural identifiers that:
- Rarely change (renaming a table requires a new migration)
- Should not break if external packages evolve
- Make the submodule self-contained for its core purpose

```typescript
// In migrations - use local table names
import { KNEX_TABLE } from '../tables/table_names';

await knex.schema.createTable(KNEX_TABLE.STUDENTS, (table) => { ... });
```

### Business Logic → From Host Repo (@eduTypes)

Business logic constants, enums, and types come from `@eduTypes/*` (provided by host repos).

**Why?** These represent application logic that:
- May evolve with business requirements
- Should stay in sync with the consuming application
- Are not structural database identifiers

```typescript
// In migrations - business logic from host
import { SchoolPackOptionsDefaultTrialActivated } from '@eduTypes/payment-v3';
```

## Development Commands

### Code Quality
```bash
npm run lint               # ESLint with max 0 warnings
npm run lint:fix           # Auto-fix ESLint issues
npm run format             # Format with Prettier
npm run format:check       # Check Prettier formatting
```

Pre-commit hook runs `npx prettier . --write`.

### Migrations

Migrations run from **host repos** using their knexfile:

```bash
# From host repo
knex migrate:latest --knexfile src/knexfile.ts
knex migrate:make <name> --knexfile src/knexfile.ts -x ts
```

## Repository Structure

```
src/
├── tables/
│   └── table_names.ts      # KNEX_TABLE constants (local snapshot)
├── migrations/             # Knex migration files (.ts)
│   └── *.ts               # Format: YYYYMMDDHHMMSS_description.ts
├── mysql-init-scripts/     # SQL initialization scripts
│   ├── 1.init.sql
│   └── 2.edusign-schema.sql
├── validators/             # Zod validators
│   └── dates.ts
└── index.ts
```

## Migration Patterns

### Standard Structure

```typescript
import type { Knex } from 'knex';
import { KNEX_TABLE } from '../tables/table_names';

export async function up(knex: Knex): Promise<void> {
  if (!(await knex.schema.hasTable(KNEX_TABLE.MY_TABLE))) {
    await knex.schema.createTable(KNEX_TABLE.MY_TABLE, (table) => {
      table.string('ID', 26).primary();
      table.string('SCHOOL_ID', 20).collate('latin1_swedish_ci').notNullable();
      table.dateTime('DATE_CREATED').notNullable().defaultTo(knex.fn.now());

      // SQLite vs MySQL DATE_UPDATED handling
      if (knex.client.config.client === 'sqlite3') {
        table.dateTime('DATE_UPDATED').notNullable().defaultTo(knex.fn.now());
      } else {
        table.dateTime('DATE_UPDATED').notNullable()
          .defaultTo(knex.raw('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'));
      }

      table.foreign('SCHOOL_ID').references('SCHOOLS.ID')
        .onDelete('CASCADE').onUpdate('CASCADE');
      table.index('SCHOOL_ID');
    });

    // SQLite trigger for DATE_UPDATED
    if (knex.client.config.client === 'sqlite3') {
      await knex.schema.raw(`
        CREATE TRIGGER update_MY_TABLE_timestamp
        AFTER UPDATE ON ${KNEX_TABLE.MY_TABLE}
        FOR EACH ROW
        BEGIN
          UPDATE ${KNEX_TABLE.MY_TABLE} SET DATE_UPDATED = CURRENT_TIMESTAMP WHERE ID = OLD.ID;
        END;
      `);
    }
  }
}

export async function down(knex: Knex): Promise<void> {
  if (await knex.schema.hasTable(KNEX_TABLE.MY_TABLE)) {
    if (knex.client.config.client === 'sqlite3') {
      await knex.schema.raw(`DROP TRIGGER IF EXISTS update_MY_TABLE_timestamp`);
    }
    await knex.schema.dropTable(KNEX_TABLE.MY_TABLE);
  }
}
```

### Database Conventions

- **Table Names**: UPPERCASE with underscores (`STUDENTS`, `COURSES`)
- **Column Names**: UPPERCASE with underscores (`DATE_CREATED`, `SCHOOL_ID`)
- **Primary Keys**: String IDs with varying lengths (20, 26, 28 chars)
- **Collation**: `latin1_swedish_ci` for IDs referencing SCHOOLS/STUDENTS
- **Foreign Keys**: Always include `.onDelete()` and `.onUpdate()` clauses

### Multi-Database Support

Migrations support both **MySQL** (production) and **SQLite3** (testing):
- MySQL: `ON UPDATE CURRENT_TIMESTAMP` for auto-updating timestamps
- SQLite: Manual triggers for `DATE_UPDATED`
- Always check `knex.client.config.client` for conditional logic

## Host Repo Requirements

For this submodule to work, host repos must provide:

1. **tsconfig.json path aliases**:
```json
{
  "compilerOptions": {
    "paths": {
      "@eduTypes/*": ["./src/types/*"]
    }
  }
}
```

2. **knexfile.ts** with migrations directory pointing to submodule:
```typescript
export default {
  migrations: {
    directory: './database/src/migrations'
  }
}
```

## Common Gotchas

1. **SQLite Triggers**: Drop triggers BEFORE dropping tables in `down()`
2. **Collation**: Use `latin1_swedish_ci` for FK columns to SCHOOLS/STUDENTS
3. **Boolean Fields**: MySQL stores as TINYINT(1), use `.boolean()`
4. **Migration Order**: Migrations run in timestamp order - ensure dependencies exist
5. **Table Existence**: Always check `hasTable()` before create/drop operations
