-- Baseline generated from actual Hibernate-created PostgreSQL schema (not hand-written):
-- 1) docker compose up -d
-- 2) start app with spring.jpa.hibernate.ddl-auto=update
-- 3) pg_dump -U kemet -d kemet --schema-only --no-owner --no-privileges

CREATE TABLE public.app_user (
    id uuid NOT NULL,
    active_faculty_id character varying(255) NOT NULL,
    auth0_subject character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    display_name character varying(255) NOT NULL
);

CREATE TABLE public.chat_message (
    id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    role character varying(255) NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE public.faculty_content (
    id character varying(255) NOT NULL,
    content_json text NOT NULL,
    display_name character varying(255) NOT NULL
);

CREATE TABLE public.practice_state (
    id uuid NOT NULL,
    completed_days integer NOT NULL,
    faculty_id character varying(255) NOT NULL,
    journal_analysis_opt_in boolean NOT NULL,
    last_practiced_at timestamp(6) with time zone,
    user_id uuid NOT NULL
);

ALTER TABLE public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);

ALTER TABLE public.chat_message
    ADD CONSTRAINT chat_message_pkey PRIMARY KEY (id);

ALTER TABLE public.faculty_content
    ADD CONSTRAINT faculty_content_pkey PRIMARY KEY (id);

ALTER TABLE public.practice_state
    ADD CONSTRAINT practice_state_pkey PRIMARY KEY (id);

ALTER TABLE public.app_user
    ADD CONSTRAINT ukfye3cht4prfl3mgd42nptiosd UNIQUE (auth0_subject);
