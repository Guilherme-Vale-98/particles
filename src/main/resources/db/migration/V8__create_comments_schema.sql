create table comments (
    id uuid primary key default gen_random_uuid(),
    article_id uuid not null references articles (id) on delete cascade,
    author_id uuid not null references user_profiles (id) on delete cascade,
    parent_comment_id uuid references comments (id) on delete cascade,
    body text not null,
    deleted boolean not null default false,
    created_at timestamptz not null default now(),
    edited_at timestamptz,
    constraint comments_body_check
        check (length(trim(body)) > 0)
);

create index comments_article_parent_created_at_idx
    on comments (article_id, parent_comment_id, created_at);

create index comments_parent_created_at_idx
    on comments (parent_comment_id, created_at);

create index comments_author_created_at_idx
    on comments (author_id, created_at desc);
