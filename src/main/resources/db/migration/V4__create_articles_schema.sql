create table articles (
    id uuid primary key default gen_random_uuid(),
    author_id uuid not null references user_profiles (id) on delete cascade,
    title varchar(200) not null,
    slug varchar(255) not null,
    summary varchar(500),
    body text not null,
    status varchar(20) not null,
    read_time_minutes integer not null default 1,
    view_count bigint not null default 0,
    created_at timestamptz not null default now(),
    published_at timestamptz,
    updated_at timestamptz not null default now(),
    version bigint not null default 0,
    constraint articles_status_check
        check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    constraint articles_read_time_minutes_check
        check (read_time_minutes > 0),
    constraint articles_view_count_check
        check (view_count >= 0),
    constraint articles_version_check
        check (version >= 0)
);

create table article_tags (
    article_id uuid not null references articles (id) on delete cascade,
    tag varchar(50) not null,
    primary key (article_id, tag),
    constraint article_tags_tag_check
        check (length(trim(tag)) > 0)
);

create table article_versions (
    id uuid primary key default gen_random_uuid(),
    article_id uuid not null references articles (id) on delete cascade,
    body text not null,
    edited_at timestamptz not null default now()
);

create index articles_author_status_published_at_idx
    on articles (author_id, status, published_at desc);

create unique index articles_slug_key
    on articles (slug);

create index article_tags_tag_idx
    on article_tags (tag);

create index articles_search_idx
    on articles using gin (
        to_tsvector(
            'english',
            coalesce(title, '') || ' ' || coalesce(summary, '') || ' ' || coalesce(body, '')
        )
    );
