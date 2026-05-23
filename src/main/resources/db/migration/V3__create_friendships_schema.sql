create table friendships (
    id uuid primary key default gen_random_uuid(),
    requester_id uuid not null references user_profiles (id) on delete cascade,
    receiver_id uuid not null references user_profiles (id) on delete cascade,
    user_low_id uuid not null references user_profiles (id) on delete cascade,
    user_high_id uuid not null references user_profiles (id) on delete cascade,
    status varchar(20) not null,
    created_at timestamptz not null default now(),
    responded_at timestamptz,
    constraint friendships_status_check
        check (status in ('PENDING', 'ACCEPTED', 'REJECTED', 'BLOCKED')),
    constraint friendships_distinct_users_check
        check (requester_id <> receiver_id and user_low_id <> user_high_id)
);

create index friendships_requester_id_idx
    on friendships (requester_id);

create index friendships_receiver_id_idx
    on friendships (receiver_id);

create index friendships_user_low_high_idx
    on friendships (user_low_id, user_high_id);

create unique index friendships_active_pair_key
    on friendships (user_low_id, user_high_id)
    where status in ('PENDING', 'ACCEPTED', 'BLOCKED');
