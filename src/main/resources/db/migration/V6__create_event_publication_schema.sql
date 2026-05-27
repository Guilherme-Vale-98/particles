create table event_publication (
    id uuid primary key,
    publication_date timestamptz not null,
    listener_id varchar(512) not null,
    serialized_event text not null,
    event_type varchar(512) not null,
    completion_date timestamptz,
    last_resubmission_date timestamptz,
    completion_attempts integer not null default 0,
    status varchar(32) not null
);

create index event_publication_status_idx
    on event_publication (status);

create index event_publication_listener_publication_date_idx
    on event_publication (listener_id, publication_date desc);

create table event_publication_archive (
    id uuid primary key,
    publication_date timestamptz not null,
    listener_id varchar(512) not null,
    serialized_event text not null,
    event_type varchar(512) not null,
    completion_date timestamptz,
    last_resubmission_date timestamptz,
    completion_attempts integer not null default 0,
    status varchar(32) not null
);

create index event_publication_archive_status_idx
    on event_publication_archive (status);
