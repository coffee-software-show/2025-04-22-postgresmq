create table int_message
(
    message_id    char(36)     not null,
    region        varchar(100) not null,
    created_date  timestamp    not null,
    message_bytes bytea,
    constraint int_message_pk primary key (message_id, region)
);

create index int_message_ix1 on int_message (created_date);

create table int_group_to_message
(
    group_key  char(36) not null,
    message_id char(36) not null,
    region     varchar(100),
    constraint int_group_to_message_pk primary key (group_key, message_id, region)
);

create table int_message_group
(
    group_key              char(36)     not null,
    region                 varchar(100) not null,
    group_condition        varchar(255),
    complete               bigint,
    last_released_sequence bigint,
    created_date           timestamp    not null,
    updated_date           timestamp default null,
    constraint int_message_group_pk primary key (group_key, region)
);

create table int_lock
(
    lock_key     char(36)     not null,
    region       varchar(100) not null,
    client_id    char(36),
    created_date timestamp    not null,
    constraint int_lock_pk primary key (lock_key, region)
);

create sequence int_message_seq start with 1 increment by 1 no cycle;

create table int_channel_message
(
    message_id       char(36)     not null,
    group_key        char(36)     not null,
    created_date     bigint       not null,
    message_priority bigint,
    message_sequence bigint       not null default nextval('int_message_seq'),
    message_bytes    bytea,
    region           varchar(100) not null,
    constraint int_channel_message_pk primary key (region, group_key, created_date, message_sequence)
);

create index int_channel_msg_delete_idx on int_channel_message (region, group_key, message_id);
-- this is only needed if the message group store property 'priorityenabled' is true
-- create unique index int_channel_msg_priority_idx on int_channel_message (region, group_key, message_priority desc nulls last, created_date, message_sequence);


create table int_metadata_store
(
    metadata_key   varchar(255) not null,
    metadata_value varchar(4000),
    region         varchar(100) not null,
    constraint int_metadata_store_pk primary key (metadata_key, region)
);

-- this is only needed if using postgreschannelmessagesubscriber

create function int_channel_message_notify_fct()
    returns trigger as
$body$
begin
    perform pg_notify('int_channel_message_notify', new.region || ' ' || new.group_key);
    return new;
end;
$body$
    language plpgsql;

create trigger  int_channel_message_notify_trg
    after insert
    on int_channel_message
    for each row
execute procedure int_channel_message_notify_fct();
