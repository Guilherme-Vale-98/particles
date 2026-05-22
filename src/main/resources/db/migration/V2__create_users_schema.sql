create table user_profiles (
    id uuid primary key default gen_random_uuid(),
    username varchar(50) not null,
    display_name varchar(100) not null,
    bio varchar(500),
    avatar_url varchar(2048),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index user_profiles_username_key
    on user_profiles (username);

create table user_identities (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references user_profiles (id) on delete cascade,
    provider varchar(20) not null,
    provider_subject varchar(255) not null,
    email varchar(320),
    created_at timestamptz not null default now()
);

create unique index user_identities_provider_subject_key
    on user_identities (provider, provider_subject);

create index user_identities_user_id_idx
    on user_identities (user_id);
