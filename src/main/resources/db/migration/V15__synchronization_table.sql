create table synchronization
(
    id                 uuid primary key,
    data_provider      varchar           not null,
    table_name         varchar not null,
    row_id             uuid unique,
    last_sync          timestamp with time zone not null default current_timestamp,
    created_at         timestamp with time zone not null default current_timestamp
);

