create table feed_items (
    id uuid primary key default gen_random_uuid(),
    recipient_id uuid not null references user_profiles (id) on delete cascade,
    article_id uuid not null references articles (id) on delete cascade,
    author_id uuid not null references user_profiles (id) on delete cascade,
    created_at timestamptz not null default now()
);

create index feed_items_recipient_created_at_idx
    on feed_items (recipient_id, created_at desc);
