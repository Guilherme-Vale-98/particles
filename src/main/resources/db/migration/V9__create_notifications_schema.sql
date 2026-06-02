create table notifications (
    id uuid primary key default gen_random_uuid(),
    recipient_id uuid not null references user_profiles (id) on delete cascade,
    actor_id uuid not null references user_profiles (id) on delete cascade,
    type varchar(40) not null,
    reference_id uuid not null,
    secondary_reference_id uuid,
    read boolean not null default false,
    created_at timestamptz not null default now(),
    read_at timestamptz,
    constraint notifications_type_check
        check (type in (
            'FRIEND_REQUEST',
            'FRIEND_ACCEPTED',
            'ARTICLE_REACTION',
            'ARTICLE_COMMENT',
            'COMMENT_REPLY'
        )),
    constraint notifications_read_at_check
        check (
            (read = false and read_at is null)
            or (read = true)
        )
);

create index notifications_recipient_created_at_idx
    on notifications (recipient_id, created_at desc, id desc);

create index notifications_recipient_read_created_at_idx
    on notifications (recipient_id, read, created_at desc, id desc);
