create extension if not exists pgcrypto;

insert into user_profiles (id, username, display_name, bio, avatar_url, created_at, updated_at)
values
    (
        '00000000-0000-4000-8000-000000000001',
        'seed-alice',
        'Seed Alice',
        'Writes about Spring, modular monoliths, and backend architecture.',
        'https://example.com/seed-alice.png',
        '2026-01-01T10:00:00Z',
        '2026-01-01T10:00:00Z'
    ),
    (
        '00000000-0000-4000-8000-000000000002',
        'seed-bob',
        'Seed Bob',
        'Reads feeds, reacts to articles, and leaves useful comments.',
        'https://example.com/seed-bob.png',
        '2026-01-01T10:05:00Z',
        '2026-01-01T10:05:00Z'
    ),
    (
        '00000000-0000-4000-8000-000000000003',
        'seed-carol',
        'Seed Carol',
        'Tests notifications, replies, and archived article behavior.',
        'https://example.com/seed-carol.png',
        '2026-01-01T10:10:00Z',
        '2026-01-01T10:10:00Z'
    )
on conflict (id) do update
set username = excluded.username,
    display_name = excluded.display_name,
    bio = excluded.bio,
    avatar_url = excluded.avatar_url,
    updated_at = excluded.updated_at;

insert into user_identities (id, user_id, provider, provider_subject, email, created_at)
values
    (
        '00000000-0000-4000-8000-000000000011',
        '00000000-0000-4000-8000-000000000001',
        'CUSTOM',
        'seed-alice@example.com',
        'seed-alice@example.com',
        '2026-01-01T10:00:00Z'
    ),
    (
        '00000000-0000-4000-8000-000000000012',
        '00000000-0000-4000-8000-000000000002',
        'CUSTOM',
        'seed-bob@example.com',
        'seed-bob@example.com',
        '2026-01-01T10:05:00Z'
    ),
    (
        '00000000-0000-4000-8000-000000000013',
        '00000000-0000-4000-8000-000000000003',
        'CUSTOM',
        'seed-carol@example.com',
        'seed-carol@example.com',
        '2026-01-01T10:10:00Z'
    )
on conflict (provider, provider_subject) do update
set user_id = excluded.user_id,
    email = excluded.email;

insert into friendships (
    id,
    requester_id,
    receiver_id,
    user_low_id,
    user_high_id,
    status,
    created_at,
    responded_at
)
values
    (
        '00000000-0000-4000-8000-000000000101',
        '00000000-0000-4000-8000-000000000002',
        '00000000-0000-4000-8000-000000000001',
        '00000000-0000-4000-8000-000000000001',
        '00000000-0000-4000-8000-000000000002',
        'ACCEPTED',
        '2026-01-01T11:00:00Z',
        '2026-01-01T11:10:00Z'
    ),
    (
        '00000000-0000-4000-8000-000000000102',
        '00000000-0000-4000-8000-000000000003',
        '00000000-0000-4000-8000-000000000001',
        '00000000-0000-4000-8000-000000000001',
        '00000000-0000-4000-8000-000000000003',
        'PENDING',
        '2026-01-01T11:20:00Z',
        null
    )
on conflict (id) do update
set requester_id = excluded.requester_id,
    receiver_id = excluded.receiver_id,
    user_low_id = excluded.user_low_id,
    user_high_id = excluded.user_high_id,
    status = excluded.status,
    responded_at = excluded.responded_at;

insert into articles (
    id,
    author_id,
    title,
    slug,
    summary,
    body,
    status,
    read_time_minutes,
    view_count,
    created_at,
    published_at,
    updated_at,
    version
)
values
    (
        '00000000-0000-4000-8000-000000000201',
        '00000000-0000-4000-8000-000000000001',
        'Spring Events In Practice',
        'seed-spring-events-in-practice',
        'A seeded published article about internal Spring events.',
        'Spring application events keep modules focused while allowing feed, reaction, comment, and notification modules to react after commit.',
        'PUBLISHED',
        1,
        42,
        '2026-01-01T12:00:00Z',
        '2026-01-01T12:30:00Z',
        '2026-01-01T12:30:00Z',
        1
    ),
    (
        '00000000-0000-4000-8000-000000000202',
        '00000000-0000-4000-8000-000000000002',
        'Draft Feed Ideas',
        'seed-draft-feed-ideas',
        'A seeded draft article that should not appear publicly.',
        'Drafts stay private to their author until the author publishes them.',
        'DRAFT',
        1,
        0,
        '2026-01-01T13:00:00Z',
        null,
        '2026-01-01T13:00:00Z',
        0
    ),
    (
        '00000000-0000-4000-8000-000000000203',
        '00000000-0000-4000-8000-000000000003',
        'Archived Notification Notes',
        'seed-archived-notification-notes',
        'A seeded archived article for restore/manual testing.',
        'Archived articles are hidden from public reads until restored.',
        'ARCHIVED',
        1,
        7,
        '2026-01-01T14:00:00Z',
        '2026-01-01T14:30:00Z',
        '2026-01-01T15:00:00Z',
        2
    )
on conflict (id) do update
set author_id = excluded.author_id,
    title = excluded.title,
    slug = excluded.slug,
    summary = excluded.summary,
    body = excluded.body,
    status = excluded.status,
    read_time_minutes = excluded.read_time_minutes,
    view_count = excluded.view_count,
    published_at = excluded.published_at,
    updated_at = excluded.updated_at,
    version = excluded.version;

insert into article_tags (article_id, tag)
values
    ('00000000-0000-4000-8000-000000000201', 'spring'),
    ('00000000-0000-4000-8000-000000000201', 'events'),
    ('00000000-0000-4000-8000-000000000201', 'modulith'),
    ('00000000-0000-4000-8000-000000000202', 'feed'),
    ('00000000-0000-4000-8000-000000000202', 'draft'),
    ('00000000-0000-4000-8000-000000000203', 'notifications'),
    ('00000000-0000-4000-8000-000000000203', 'archive')
on conflict (article_id, tag) do nothing;

insert into article_versions (id, article_id, body, edited_at)
values
    (
        '00000000-0000-4000-8000-000000000211',
        '00000000-0000-4000-8000-000000000201',
        'Original seeded article body before the published revision.',
        '2026-01-01T12:15:00Z'
    ),
    (
        '00000000-0000-4000-8000-000000000212',
        '00000000-0000-4000-8000-000000000203',
        'Original archived article body before it was archived.',
        '2026-01-01T14:45:00Z'
    )
on conflict (id) do update
set article_id = excluded.article_id,
    body = excluded.body,
    edited_at = excluded.edited_at;

insert into feed_items (id, recipient_id, article_id, author_id, created_at)
values
    (
        '00000000-0000-4000-8000-000000000221',
        '00000000-0000-4000-8000-000000000002',
        '00000000-0000-4000-8000-000000000201',
        '00000000-0000-4000-8000-000000000001',
        '2026-01-01T12:30:00Z'
    )
on conflict (id) do update
set recipient_id = excluded.recipient_id,
    article_id = excluded.article_id,
    author_id = excluded.author_id,
    created_at = excluded.created_at;

insert into reactions (id, user_id, article_id, type, created_at, updated_at)
values
    (
        '00000000-0000-4000-8000-000000000301',
        '00000000-0000-4000-8000-000000000002',
        '00000000-0000-4000-8000-000000000201',
        'LIKE',
        '2026-01-01T12:40:00Z',
        '2026-01-01T12:40:00Z'
    ),
    (
        '00000000-0000-4000-8000-000000000302',
        '00000000-0000-4000-8000-000000000003',
        '00000000-0000-4000-8000-000000000201',
        'INSIGHTFUL',
        '2026-01-01T12:45:00Z',
        '2026-01-01T12:45:00Z'
    )
on conflict (user_id, article_id) do update
set type = excluded.type,
    updated_at = excluded.updated_at;

insert into article_reaction_counts (article_id, reaction_type, count)
values
    ('00000000-0000-4000-8000-000000000201', 'LIKE', 1),
    ('00000000-0000-4000-8000-000000000201', 'INSIGHTFUL', 1)
on conflict (article_id, reaction_type) do update
set count = excluded.count;

insert into comments (id, article_id, author_id, parent_comment_id, body, deleted, created_at, edited_at)
values
    (
        '00000000-0000-4000-8000-000000000401',
        '00000000-0000-4000-8000-000000000201',
        '00000000-0000-4000-8000-000000000002',
        null,
        'This seed article is a useful way to test comment threads.',
        false,
        '2026-01-01T12:50:00Z',
        null
    ),
    (
        '00000000-0000-4000-8000-000000000402',
        '00000000-0000-4000-8000-000000000201',
        '00000000-0000-4000-8000-000000000003',
        '00000000-0000-4000-8000-000000000401',
        'Agreed. The event flow is easier to understand with sample data.',
        false,
        '2026-01-01T12:55:00Z',
        null
    )
on conflict (id) do update
set article_id = excluded.article_id,
    author_id = excluded.author_id,
    parent_comment_id = excluded.parent_comment_id,
    body = excluded.body,
    deleted = excluded.deleted,
    edited_at = excluded.edited_at;

insert into notifications (
    id,
    recipient_id,
    actor_id,
    type,
    reference_id,
    secondary_reference_id,
    read,
    created_at,
    read_at
)
values
    (
        '00000000-0000-4000-8000-000000000501',
        '00000000-0000-4000-8000-000000000001',
        '00000000-0000-4000-8000-000000000002',
        'ARTICLE_REACTION',
        '00000000-0000-4000-8000-000000000201',
        '00000000-0000-4000-8000-000000000301',
        false,
        '2026-01-01T12:40:00Z',
        null
    ),
    (
        '00000000-0000-4000-8000-000000000502',
        '00000000-0000-4000-8000-000000000001',
        '00000000-0000-4000-8000-000000000002',
        'ARTICLE_COMMENT',
        '00000000-0000-4000-8000-000000000201',
        '00000000-0000-4000-8000-000000000401',
        false,
        '2026-01-01T12:50:00Z',
        null
    ),
    (
        '00000000-0000-4000-8000-000000000503',
        '00000000-0000-4000-8000-000000000002',
        '00000000-0000-4000-8000-000000000003',
        'COMMENT_REPLY',
        '00000000-0000-4000-8000-000000000201',
        '00000000-0000-4000-8000-000000000402',
        false,
        '2026-01-01T12:55:00Z',
        null
    ),
    (
        '00000000-0000-4000-8000-000000000504',
        '00000000-0000-4000-8000-000000000001',
        '00000000-0000-4000-8000-000000000003',
        'FRIEND_REQUEST',
        '00000000-0000-4000-8000-000000000102',
        null,
        false,
        '2026-01-01T11:20:00Z',
        null
    )
on conflict (id) do update
set recipient_id = excluded.recipient_id,
    actor_id = excluded.actor_id,
    type = excluded.type,
    reference_id = excluded.reference_id,
    secondary_reference_id = excluded.secondary_reference_id,
    read = excluded.read,
    created_at = excluded.created_at,
    read_at = excluded.read_at;
