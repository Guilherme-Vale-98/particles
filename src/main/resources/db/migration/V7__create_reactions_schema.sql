create table reactions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references user_profiles (id) on delete cascade,
    article_id uuid not null references articles (id) on delete cascade,
    type varchar(30) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint reactions_user_article_key
        unique (user_id, article_id),
    constraint reactions_type_check
        check (type in ('LIKE', 'INSIGHTFUL', 'CLAP'))
);

create table article_reaction_counts (
    article_id uuid not null references articles (id) on delete cascade,
    reaction_type varchar(30) not null,
    count bigint not null default 0,
    primary key (article_id, reaction_type),
    constraint article_reaction_counts_type_check
        check (reaction_type in ('LIKE', 'INSIGHTFUL', 'CLAP')),
    constraint article_reaction_counts_count_check
        check (count >= 0)
);
