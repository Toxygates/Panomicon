--
-- PostgreSQL database cluster dump
--

SET default_transaction_read_only = off;

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

--
-- Roles
--

CREATE ROLE fusionauth;
ALTER ROLE fusionauth WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'md5db4b8aeb08b93606f134767f7ef35982';

--
-- User Configurations
--






--
-- Databases
--

--
-- Database "template1" dump
--

\connect template1

--
-- PostgreSQL database dump
--

-- Dumped from database version 12.9 (Debian 12.9-1.pgdg110+1)
-- Dumped by pg_dump version 15.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

--
-- Database "fusionauth" dump
--

--
-- PostgreSQL database dump
--

-- Dumped from database version 12.9 (Debian 12.9-1.pgdg110+1)
-- Dumped by pg_dump version 15.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: fusionauth; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE fusionauth WITH TEMPLATE = template0 ENCODING = 'UTF8';


ALTER DATABASE fusionauth OWNER TO postgres;

\connect fusionauth

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: application_daily_active_users; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.application_daily_active_users (
    applications_id uuid NOT NULL,
    count integer NOT NULL,
    day integer NOT NULL
);


ALTER TABLE public.application_daily_active_users OWNER TO fusionauth;

--
-- Name: application_monthly_active_users; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.application_monthly_active_users (
    applications_id uuid NOT NULL,
    count integer NOT NULL,
    month integer NOT NULL
);


ALTER TABLE public.application_monthly_active_users OWNER TO fusionauth;

--
-- Name: application_registration_counts; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.application_registration_counts (
    applications_id uuid NOT NULL,
    count integer NOT NULL,
    decremented_count integer NOT NULL,
    hour integer NOT NULL
);


ALTER TABLE public.application_registration_counts OWNER TO fusionauth;

--
-- Name: application_roles; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.application_roles (
    id uuid NOT NULL,
    applications_id uuid NOT NULL,
    description character varying(255),
    insert_instant bigint NOT NULL,
    is_default boolean NOT NULL,
    is_super_role boolean NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL
);


ALTER TABLE public.application_roles OWNER TO fusionauth;

--
-- Name: applications; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.applications (
    id uuid NOT NULL,
    access_token_populate_lambdas_id uuid,
    access_token_signing_keys_id uuid,
    active boolean NOT NULL,
    admin_registration_forms_id uuid NOT NULL,
    data text NOT NULL,
    email_update_email_templates_id uuid,
    email_verification_email_templates_id uuid,
    email_verified_email_templates_id uuid,
    forgot_password_email_templates_id uuid,
    forms_id uuid,
    id_token_populate_lambdas_id uuid,
    id_token_signing_keys_id uuid,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    login_id_in_use_on_create_email_templates_id uuid,
    login_id_in_use_on_update_email_templates_id uuid,
    login_new_device_email_templates_id uuid,
    login_suspicious_email_templates_id uuid,
    multi_factor_email_message_templates_id uuid,
    multi_factor_sms_message_templates_id uuid,
    name character varying(191) NOT NULL,
    passwordless_email_templates_id uuid,
    password_update_email_templates_id uuid,
    password_reset_success_email_templates_id uuid,
    samlv2_default_verification_keys_id uuid,
    samlv2_issuer character varying(191),
    samlv2_keys_id uuid,
    samlv2_logout_keys_id uuid,
    samlv2_logout_default_verification_keys_id uuid,
    samlv2_populate_lambdas_id uuid,
    samlv2_single_logout_keys_id uuid,
    self_service_user_forms_id uuid,
    set_password_email_templates_id uuid,
    tenants_id uuid NOT NULL,
    themes_id uuid,
    two_factor_method_add_email_templates_id uuid,
    two_factor_method_remove_email_templates_id uuid,
    ui_ip_access_control_lists_id uuid,
    verification_email_templates_id uuid
);


ALTER TABLE public.applications OWNER TO fusionauth;

--
-- Name: asynchronous_tasks; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.asynchronous_tasks (
    id uuid NOT NULL,
    data text,
    entity_id uuid,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    nodes_id uuid,
    status smallint NOT NULL,
    type smallint NOT NULL
);


ALTER TABLE public.asynchronous_tasks OWNER TO fusionauth;

--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    insert_instant bigint NOT NULL,
    insert_user character varying(255) NOT NULL,
    message text NOT NULL,
    data text
);


ALTER TABLE public.audit_logs OWNER TO fusionauth;

--
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: fusionauth
--

CREATE SEQUENCE public.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.audit_logs_id_seq OWNER TO fusionauth;

--
-- Name: audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: fusionauth
--

ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;


--
-- Name: authentication_keys; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.authentication_keys (
    id uuid NOT NULL,
    insert_instant bigint NOT NULL,
    ip_access_control_lists_id uuid,
    last_update_instant bigint NOT NULL,
    key_manager boolean NOT NULL,
    key_value character varying(191) NOT NULL,
    permissions text,
    meta_data text,
    tenants_id uuid
);


ALTER TABLE public.authentication_keys OWNER TO fusionauth;

--
-- Name: breached_password_metrics; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.breached_password_metrics (
    tenants_id uuid NOT NULL,
    matched_exact_count integer NOT NULL,
    matched_sub_address_count integer NOT NULL,
    matched_common_password_count integer NOT NULL,
    matched_password_count integer NOT NULL,
    passwords_checked_count integer NOT NULL
);


ALTER TABLE public.breached_password_metrics OWNER TO fusionauth;

--
-- Name: clean_speak_applications; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.clean_speak_applications (
    applications_id uuid NOT NULL,
    clean_speak_application_id uuid NOT NULL
);


ALTER TABLE public.clean_speak_applications OWNER TO fusionauth;

--
-- Name: common_breached_passwords; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.common_breached_passwords (
    password character varying(191) NOT NULL
);


ALTER TABLE public.common_breached_passwords OWNER TO fusionauth;

--
-- Name: connectors; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.connectors (
    id uuid NOT NULL,
    data text NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL,
    reconcile_lambdas_id uuid,
    ssl_certificate_keys_id uuid,
    type smallint NOT NULL
);


ALTER TABLE public.connectors OWNER TO fusionauth;

--
-- Name: connectors_tenants; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.connectors_tenants (
    connectors_id uuid NOT NULL,
    data text NOT NULL,
    sequence smallint NOT NULL,
    tenants_id uuid NOT NULL
);


ALTER TABLE public.connectors_tenants OWNER TO fusionauth;

--
-- Name: consents; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.consents (
    id uuid NOT NULL,
    consent_email_templates_id uuid,
    data text,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL,
    email_plus_email_templates_id uuid
);


ALTER TABLE public.consents OWNER TO fusionauth;

--
-- Name: data_sets; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.data_sets (
    name character varying(191) NOT NULL,
    last_update_instant bigint NOT NULL
);


ALTER TABLE public.data_sets OWNER TO fusionauth;

--
-- Name: email_templates; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.email_templates (
    id uuid NOT NULL,
    default_from_name character varying(255),
    default_html_template text NOT NULL,
    default_subject character varying(255) NOT NULL,
    default_text_template text NOT NULL,
    from_email character varying(255),
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    localized_from_names text,
    localized_html_templates text NOT NULL,
    localized_subjects text NOT NULL,
    localized_text_templates text NOT NULL,
    name character varying(191) NOT NULL
);


ALTER TABLE public.email_templates OWNER TO fusionauth;

--
-- Name: entities; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.entities (
    id uuid NOT NULL,
    client_id character varying(191) NOT NULL,
    client_secret character varying(255) NOT NULL,
    data text,
    entity_types_id uuid NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(255) NOT NULL,
    parent_id uuid,
    tenants_id uuid NOT NULL
);


ALTER TABLE public.entities OWNER TO fusionauth;

--
-- Name: entity_entity_grant_permissions; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.entity_entity_grant_permissions (
    entity_entity_grants_id uuid NOT NULL,
    entity_type_permissions_id uuid NOT NULL
);


ALTER TABLE public.entity_entity_grant_permissions OWNER TO fusionauth;

--
-- Name: entity_entity_grants; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.entity_entity_grants (
    id uuid NOT NULL,
    data text,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    recipient_id uuid NOT NULL,
    target_id uuid NOT NULL
);


ALTER TABLE public.entity_entity_grants OWNER TO fusionauth;

--
-- Name: entity_type_permissions; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.entity_type_permissions (
    id uuid NOT NULL,
    data text,
    description text,
    entity_types_id uuid NOT NULL,
    insert_instant bigint NOT NULL,
    is_default boolean NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL
);


ALTER TABLE public.entity_type_permissions OWNER TO fusionauth;

--
-- Name: entity_types; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.entity_types (
    id uuid NOT NULL,
    access_token_signing_keys_id uuid,
    data text,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL
);


ALTER TABLE public.entity_types OWNER TO fusionauth;

--
-- Name: entity_user_grant_permissions; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.entity_user_grant_permissions (
    entity_user_grants_id uuid NOT NULL,
    entity_type_permissions_id uuid NOT NULL
);


ALTER TABLE public.entity_user_grant_permissions OWNER TO fusionauth;

--
-- Name: entity_user_grants; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.entity_user_grants (
    id uuid NOT NULL,
    data text,
    entities_id uuid NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.entity_user_grants OWNER TO fusionauth;

--
-- Name: event_logs; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.event_logs (
    id bigint NOT NULL,
    insert_instant bigint NOT NULL,
    message text NOT NULL,
    type smallint NOT NULL
);


ALTER TABLE public.event_logs OWNER TO fusionauth;

--
-- Name: event_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: fusionauth
--

CREATE SEQUENCE public.event_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.event_logs_id_seq OWNER TO fusionauth;

--
-- Name: event_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: fusionauth
--

ALTER SEQUENCE public.event_logs_id_seq OWNED BY public.event_logs.id;


--
-- Name: external_identifiers; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.external_identifiers (
    id character varying(191) NOT NULL,
    applications_id uuid,
    data text,
    expiration_instant bigint,
    insert_instant bigint NOT NULL,
    tenants_id uuid NOT NULL,
    type smallint NOT NULL,
    users_id uuid
);


ALTER TABLE public.external_identifiers OWNER TO fusionauth;

--
-- Name: families; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.families (
    data text,
    family_id uuid NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    owner boolean NOT NULL,
    role smallint NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.families OWNER TO fusionauth;

--
-- Name: federated_domains; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.federated_domains (
    identity_providers_id uuid NOT NULL,
    domain character varying(191) NOT NULL
);


ALTER TABLE public.federated_domains OWNER TO fusionauth;

--
-- Name: form_fields; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.form_fields (
    id uuid NOT NULL,
    consents_id uuid,
    data text,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL
);


ALTER TABLE public.form_fields OWNER TO fusionauth;

--
-- Name: form_steps; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.form_steps (
    form_fields_id uuid NOT NULL,
    forms_id uuid NOT NULL,
    sequence smallint NOT NULL,
    step smallint NOT NULL
);


ALTER TABLE public.form_steps OWNER TO fusionauth;

--
-- Name: forms; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.forms (
    id uuid NOT NULL,
    data text,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL,
    type smallint NOT NULL
);


ALTER TABLE public.forms OWNER TO fusionauth;

--
-- Name: global_daily_active_users; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.global_daily_active_users (
    count integer NOT NULL,
    day integer NOT NULL
);


ALTER TABLE public.global_daily_active_users OWNER TO fusionauth;

--
-- Name: global_monthly_active_users; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.global_monthly_active_users (
    count integer NOT NULL,
    month integer NOT NULL
);


ALTER TABLE public.global_monthly_active_users OWNER TO fusionauth;

--
-- Name: global_registration_counts; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.global_registration_counts (
    count integer NOT NULL,
    decremented_count integer NOT NULL,
    hour integer NOT NULL
);


ALTER TABLE public.global_registration_counts OWNER TO fusionauth;

--
-- Name: group_application_roles; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.group_application_roles (
    application_roles_id uuid NOT NULL,
    groups_id uuid NOT NULL
);


ALTER TABLE public.group_application_roles OWNER TO fusionauth;

--
-- Name: group_members; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.group_members (
    id uuid NOT NULL,
    groups_id uuid NOT NULL,
    data text,
    insert_instant bigint NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.group_members OWNER TO fusionauth;

--
-- Name: groups; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.groups (
    id uuid NOT NULL,
    data text,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL,
    tenants_id uuid NOT NULL
);


ALTER TABLE public.groups OWNER TO fusionauth;

--
-- Name: hourly_logins; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.hourly_logins (
    applications_id uuid NOT NULL,
    count integer NOT NULL,
    data text,
    hour integer NOT NULL
);


ALTER TABLE public.hourly_logins OWNER TO fusionauth;

--
-- Name: identities; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.identities (
    id bigint NOT NULL,
    breached_password_last_checked_instant bigint,
    breached_password_status smallint,
    connectors_id uuid NOT NULL,
    email character varying(191),
    encryption_scheme character varying(255) NOT NULL,
    factor integer NOT NULL,
    insert_instant bigint NOT NULL,
    last_login_instant bigint,
    last_update_instant bigint NOT NULL,
    password character varying(255) NOT NULL,
    password_change_reason smallint,
    password_change_required boolean NOT NULL,
    password_last_update_instant bigint NOT NULL,
    salt character varying(255) NOT NULL,
    status smallint NOT NULL,
    tenants_id uuid NOT NULL,
    username character varying(191),
    username_index character varying(191),
    username_status smallint NOT NULL,
    users_id uuid NOT NULL,
    verified boolean NOT NULL
);


ALTER TABLE public.identities OWNER TO fusionauth;

--
-- Name: identities_id_seq; Type: SEQUENCE; Schema: public; Owner: fusionauth
--

CREATE SEQUENCE public.identities_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.identities_id_seq OWNER TO fusionauth;

--
-- Name: identities_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: fusionauth
--

ALTER SEQUENCE public.identities_id_seq OWNED BY public.identities.id;


--
-- Name: identity_provider_links; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.identity_provider_links (
    data text NOT NULL,
    identity_providers_id uuid NOT NULL,
    identity_providers_user_id character varying(191) NOT NULL,
    insert_instant bigint NOT NULL,
    last_login_instant bigint NOT NULL,
    tenants_id uuid NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.identity_provider_links OWNER TO fusionauth;

--
-- Name: identity_providers; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.identity_providers (
    id uuid NOT NULL,
    data text NOT NULL,
    enabled boolean NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL,
    type character varying(255) NOT NULL,
    keys_id uuid,
    request_signing_keys_id uuid,
    reconcile_lambdas_id uuid
);


ALTER TABLE public.identity_providers OWNER TO fusionauth;

--
-- Name: identity_providers_applications; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.identity_providers_applications (
    applications_id uuid NOT NULL,
    data text NOT NULL,
    enabled boolean NOT NULL,
    identity_providers_id uuid NOT NULL,
    keys_id uuid
);


ALTER TABLE public.identity_providers_applications OWNER TO fusionauth;

--
-- Name: identity_providers_tenants; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.identity_providers_tenants (
    tenants_id uuid NOT NULL,
    data text NOT NULL,
    identity_providers_id uuid NOT NULL
);


ALTER TABLE public.identity_providers_tenants OWNER TO fusionauth;

--
-- Name: instance; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.instance (
    id uuid NOT NULL,
    activate_instant bigint,
    license text,
    license_id character varying(255),
    setup_complete boolean NOT NULL
);


ALTER TABLE public.instance OWNER TO fusionauth;

--
-- Name: integrations; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.integrations (
    data text NOT NULL
);


ALTER TABLE public.integrations OWNER TO fusionauth;

--
-- Name: ip_access_control_lists; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.ip_access_control_lists (
    id uuid NOT NULL,
    data text NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL
);


ALTER TABLE public.ip_access_control_lists OWNER TO fusionauth;

--
-- Name: ip_location_database; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.ip_location_database (
    data bytea,
    last_modified bigint NOT NULL
);


ALTER TABLE public.ip_location_database OWNER TO fusionauth;

--
-- Name: keys; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.keys (
    id uuid NOT NULL,
    algorithm character varying(255),
    certificate text,
    expiration_instant bigint,
    insert_instant bigint NOT NULL,
    issuer character varying(255),
    kid character varying(191) NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL,
    private_key text,
    public_key text,
    secret text,
    type character varying(255) NOT NULL
);


ALTER TABLE public.keys OWNER TO fusionauth;

--
-- Name: kickstart_files; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.kickstart_files (
    id uuid NOT NULL,
    kickstart bytea NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.kickstart_files OWNER TO fusionauth;

--
-- Name: lambdas; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.lambdas (
    id uuid NOT NULL,
    body text NOT NULL,
    debug boolean NOT NULL,
    engine_type character varying(255) NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(255) NOT NULL,
    type smallint NOT NULL
);


ALTER TABLE public.lambdas OWNER TO fusionauth;

--
-- Name: last_login_instants; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.last_login_instants (
    applications_id uuid,
    registration_last_login_instant bigint,
    users_id uuid,
    user_last_login_instant bigint
);


ALTER TABLE public.last_login_instants OWNER TO fusionauth;

--
-- Name: locks; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.locks (
    type character varying(191) NOT NULL,
    update_instant bigint
);


ALTER TABLE public.locks OWNER TO fusionauth;

--
-- Name: master_record; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.master_record (
    id uuid NOT NULL,
    instant bigint NOT NULL
);


ALTER TABLE public.master_record OWNER TO fusionauth;

--
-- Name: message_templates; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.message_templates (
    id uuid NOT NULL,
    data text NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL,
    type smallint NOT NULL
);


ALTER TABLE public.message_templates OWNER TO fusionauth;

--
-- Name: messengers; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.messengers (
    id uuid NOT NULL,
    data text NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL,
    type smallint NOT NULL
);


ALTER TABLE public.messengers OWNER TO fusionauth;

--
-- Name: migrations; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.migrations (
    name character varying(191) NOT NULL,
    run_instant bigint NOT NULL
);


ALTER TABLE public.migrations OWNER TO fusionauth;

--
-- Name: nodes; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.nodes (
    id uuid NOT NULL,
    data text NOT NULL,
    insert_instant bigint NOT NULL,
    last_checkin_instant bigint NOT NULL,
    runtime_mode character varying(255) NOT NULL,
    url character varying(255) NOT NULL
);


ALTER TABLE public.nodes OWNER TO fusionauth;

--
-- Name: previous_passwords; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.previous_passwords (
    insert_instant bigint NOT NULL,
    encryption_scheme character varying(255) NOT NULL,
    factor integer NOT NULL,
    password character varying(255) NOT NULL,
    salt character varying(255) NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.previous_passwords OWNER TO fusionauth;

--
-- Name: raw_application_daily_active_users; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.raw_application_daily_active_users (
    applications_id uuid NOT NULL,
    day integer NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.raw_application_daily_active_users OWNER TO fusionauth;

--
-- Name: raw_application_monthly_active_users; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.raw_application_monthly_active_users (
    applications_id uuid NOT NULL,
    month integer NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.raw_application_monthly_active_users OWNER TO fusionauth;

--
-- Name: raw_application_registration_counts; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.raw_application_registration_counts (
    id bigint NOT NULL,
    applications_id uuid NOT NULL,
    count integer NOT NULL,
    decremented_count integer NOT NULL,
    hour integer NOT NULL
);


ALTER TABLE public.raw_application_registration_counts OWNER TO fusionauth;

--
-- Name: raw_application_registration_counts_id_seq; Type: SEQUENCE; Schema: public; Owner: fusionauth
--

CREATE SEQUENCE public.raw_application_registration_counts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.raw_application_registration_counts_id_seq OWNER TO fusionauth;

--
-- Name: raw_application_registration_counts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: fusionauth
--

ALTER SEQUENCE public.raw_application_registration_counts_id_seq OWNED BY public.raw_application_registration_counts.id;


--
-- Name: raw_global_daily_active_users; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.raw_global_daily_active_users (
    day integer NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.raw_global_daily_active_users OWNER TO fusionauth;

--
-- Name: raw_global_monthly_active_users; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.raw_global_monthly_active_users (
    month integer NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.raw_global_monthly_active_users OWNER TO fusionauth;

--
-- Name: raw_global_registration_counts; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.raw_global_registration_counts (
    id bigint NOT NULL,
    count integer NOT NULL,
    decremented_count integer NOT NULL,
    hour integer NOT NULL
);


ALTER TABLE public.raw_global_registration_counts OWNER TO fusionauth;

--
-- Name: raw_global_registration_counts_id_seq; Type: SEQUENCE; Schema: public; Owner: fusionauth
--

CREATE SEQUENCE public.raw_global_registration_counts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.raw_global_registration_counts_id_seq OWNER TO fusionauth;

--
-- Name: raw_global_registration_counts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: fusionauth
--

ALTER SEQUENCE public.raw_global_registration_counts_id_seq OWNED BY public.raw_global_registration_counts.id;


--
-- Name: raw_logins; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.raw_logins (
    applications_id uuid,
    instant bigint NOT NULL,
    ip_address character varying(255),
    users_id uuid NOT NULL
);


ALTER TABLE public.raw_logins OWNER TO fusionauth;

--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.refresh_tokens (
    id uuid NOT NULL,
    applications_id uuid,
    data text NOT NULL,
    insert_instant bigint NOT NULL,
    start_instant bigint NOT NULL,
    tenants_id uuid,
    token character varying(191),
    token_hash character(64),
    token_text text,
    users_id uuid NOT NULL
);


ALTER TABLE public.refresh_tokens OWNER TO fusionauth;

--
-- Name: request_frequencies; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.request_frequencies (
    count integer NOT NULL,
    last_update_instant bigint NOT NULL,
    request_id character varying(191) NOT NULL,
    tenants_id uuid NOT NULL,
    type smallint NOT NULL
);


ALTER TABLE public.request_frequencies OWNER TO fusionauth;

--
-- Name: scim_external_id_groups; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.scim_external_id_groups (
    entities_id uuid NOT NULL,
    external_id character varying(255) NOT NULL,
    groups_id uuid NOT NULL
);


ALTER TABLE public.scim_external_id_groups OWNER TO fusionauth;

--
-- Name: scim_external_id_users; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.scim_external_id_users (
    entities_id uuid NOT NULL,
    external_id character varying(255) NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.scim_external_id_users OWNER TO fusionauth;

--
-- Name: system_configuration; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.system_configuration (
    data text NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    report_timezone character varying(255) NOT NULL
);


ALTER TABLE public.system_configuration OWNER TO fusionauth;

--
-- Name: tenants; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.tenants (
    id uuid NOT NULL,
    access_token_signing_keys_id uuid NOT NULL,
    admin_user_forms_id uuid NOT NULL,
    client_credentials_access_token_populate_lambdas_id uuid,
    confirm_child_email_templates_id uuid,
    data text,
    email_update_email_templates_id uuid,
    email_verified_email_templates_id uuid,
    failed_authentication_user_actions_id uuid,
    family_request_email_templates_id uuid,
    forgot_password_email_templates_id uuid,
    id_token_signing_keys_id uuid NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    login_id_in_use_on_create_email_templates_id uuid,
    login_id_in_use_on_update_email_templates_id uuid,
    login_new_device_email_templates_id uuid,
    login_suspicious_email_templates_id uuid,
    multi_factor_email_message_templates_id uuid,
    multi_factor_sms_message_templates_id uuid,
    multi_factor_sms_messengers_id uuid,
    name character varying(191) NOT NULL,
    password_reset_success_email_templates_id uuid,
    password_update_email_templates_id uuid,
    parent_registration_email_templates_id uuid,
    passwordless_email_templates_id uuid,
    scim_client_entity_types_id uuid,
    scim_enterprise_user_request_converter_lambdas_id uuid,
    scim_enterprise_user_response_converter_lambdas_id uuid,
    scim_group_request_converter_lambdas_id uuid,
    scim_group_response_converter_lambdas_id uuid,
    scim_server_entity_types_id uuid,
    scim_user_request_converter_lambdas_id uuid,
    scim_user_response_converter_lambdas_id uuid,
    set_password_email_templates_id uuid,
    themes_id uuid NOT NULL,
    two_factor_method_add_email_templates_id uuid,
    two_factor_method_remove_email_templates_id uuid,
    ui_ip_access_control_lists_id uuid,
    verification_email_templates_id uuid
);


ALTER TABLE public.tenants OWNER TO fusionauth;

--
-- Name: themes; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.themes (
    id uuid NOT NULL,
    data text NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    name character varying(191) NOT NULL
);


ALTER TABLE public.themes OWNER TO fusionauth;

--
-- Name: user_action_logs; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.user_action_logs (
    id uuid NOT NULL,
    actioner_users_id uuid,
    actionee_users_id uuid NOT NULL,
    comment text,
    email_user_on_end boolean NOT NULL,
    end_event_sent boolean,
    expiry bigint,
    history text,
    insert_instant bigint NOT NULL,
    localized_name character varying(191),
    localized_option character varying(255),
    localized_reason character varying(255),
    name character varying(191),
    notify_user_on_end boolean NOT NULL,
    option_name character varying(255),
    reason character varying(255),
    reason_code character varying(255),
    user_actions_id uuid NOT NULL
);


ALTER TABLE public.user_action_logs OWNER TO fusionauth;

--
-- Name: user_action_logs_applications; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.user_action_logs_applications (
    applications_id uuid NOT NULL,
    user_action_logs_id uuid NOT NULL
);


ALTER TABLE public.user_action_logs_applications OWNER TO fusionauth;

--
-- Name: user_action_reasons; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.user_action_reasons (
    id uuid NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    localized_texts text,
    text character varying(191) NOT NULL,
    code character varying(191) NOT NULL
);


ALTER TABLE public.user_action_reasons OWNER TO fusionauth;

--
-- Name: user_actions; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.user_actions (
    id uuid NOT NULL,
    active boolean NOT NULL,
    cancel_email_templates_id uuid,
    end_email_templates_id uuid,
    include_email_in_event_json boolean NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    localized_names text,
    modify_email_templates_id uuid,
    name character varying(191) NOT NULL,
    options text,
    prevent_login boolean NOT NULL,
    send_end_event boolean NOT NULL,
    start_email_templates_id uuid,
    temporal boolean NOT NULL,
    transaction_type smallint NOT NULL,
    user_notifications_enabled boolean NOT NULL,
    user_emailing_enabled boolean NOT NULL
);


ALTER TABLE public.user_actions OWNER TO fusionauth;

--
-- Name: user_comments; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.user_comments (
    id uuid NOT NULL,
    comment text,
    commenter_id uuid NOT NULL,
    insert_instant bigint NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.user_comments OWNER TO fusionauth;

--
-- Name: user_consents; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.user_consents (
    id uuid NOT NULL,
    consents_id uuid NOT NULL,
    data text,
    giver_users_id uuid NOT NULL,
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.user_consents OWNER TO fusionauth;

--
-- Name: user_consents_email_plus; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.user_consents_email_plus (
    id bigint NOT NULL,
    next_email_instant bigint NOT NULL,
    user_consents_id uuid NOT NULL
);


ALTER TABLE public.user_consents_email_plus OWNER TO fusionauth;

--
-- Name: user_consents_email_plus_id_seq; Type: SEQUENCE; Schema: public; Owner: fusionauth
--

CREATE SEQUENCE public.user_consents_email_plus_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_consents_email_plus_id_seq OWNER TO fusionauth;

--
-- Name: user_consents_email_plus_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: fusionauth
--

ALTER SEQUENCE public.user_consents_email_plus_id_seq OWNED BY public.user_consents_email_plus.id;


--
-- Name: user_registrations; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.user_registrations (
    id uuid NOT NULL,
    applications_id uuid NOT NULL,
    authentication_token character varying(255),
    clean_speak_id uuid,
    data text,
    insert_instant bigint NOT NULL,
    last_login_instant bigint,
    last_update_instant bigint NOT NULL,
    timezone character varying(255),
    username character varying(191),
    username_status smallint NOT NULL,
    users_id uuid NOT NULL,
    verified boolean NOT NULL
);


ALTER TABLE public.user_registrations OWNER TO fusionauth;

--
-- Name: user_registrations_application_roles; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.user_registrations_application_roles (
    application_roles_id uuid NOT NULL,
    user_registrations_id uuid NOT NULL
);


ALTER TABLE public.user_registrations_application_roles OWNER TO fusionauth;

--
-- Name: users; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    active boolean NOT NULL,
    birth_date character(10),
    clean_speak_id uuid,
    data text,
    expiry bigint,
    first_name character varying(255),
    full_name character varying(255),
    image_url text,
    insert_instant bigint NOT NULL,
    last_name character varying(255),
    last_update_instant bigint NOT NULL,
    middle_name character varying(255),
    mobile_phone character varying(255),
    parent_email character varying(255),
    tenants_id uuid NOT NULL,
    timezone character varying(255)
);


ALTER TABLE public.users OWNER TO fusionauth;

--
-- Name: version; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.version (
    version character varying(255) NOT NULL
);


ALTER TABLE public.version OWNER TO fusionauth;

--
-- Name: webauthn_credentials; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.webauthn_credentials (
    id uuid NOT NULL,
    credential_id text NOT NULL,
    data text NOT NULL,
    insert_instant bigint NOT NULL,
    last_use_instant bigint NOT NULL,
    tenants_id uuid NOT NULL,
    users_id uuid NOT NULL
);


ALTER TABLE public.webauthn_credentials OWNER TO fusionauth;

--
-- Name: webhooks; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.webhooks (
    id uuid NOT NULL,
    connect_timeout integer NOT NULL,
    description character varying(255),
    data text,
    global boolean NOT NULL,
    headers text,
    http_authentication_username character varying(255),
    http_authentication_password character varying(255),
    insert_instant bigint NOT NULL,
    last_update_instant bigint NOT NULL,
    read_timeout integer NOT NULL,
    ssl_certificate text,
    url text NOT NULL
);


ALTER TABLE public.webhooks OWNER TO fusionauth;

--
-- Name: webhooks_tenants; Type: TABLE; Schema: public; Owner: fusionauth
--

CREATE TABLE public.webhooks_tenants (
    webhooks_id uuid NOT NULL,
    tenants_id uuid NOT NULL
);


ALTER TABLE public.webhooks_tenants OWNER TO fusionauth;

--
-- Name: audit_logs id; Type: DEFAULT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);


--
-- Name: event_logs id; Type: DEFAULT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.event_logs ALTER COLUMN id SET DEFAULT nextval('public.event_logs_id_seq'::regclass);


--
-- Name: identities id; Type: DEFAULT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identities ALTER COLUMN id SET DEFAULT nextval('public.identities_id_seq'::regclass);


--
-- Name: raw_application_registration_counts id; Type: DEFAULT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.raw_application_registration_counts ALTER COLUMN id SET DEFAULT nextval('public.raw_application_registration_counts_id_seq'::regclass);


--
-- Name: raw_global_registration_counts id; Type: DEFAULT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.raw_global_registration_counts ALTER COLUMN id SET DEFAULT nextval('public.raw_global_registration_counts_id_seq'::regclass);


--
-- Name: user_consents_email_plus id; Type: DEFAULT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_consents_email_plus ALTER COLUMN id SET DEFAULT nextval('public.user_consents_email_plus_id_seq'::regclass);


--
-- Data for Name: application_daily_active_users; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.application_daily_active_users (applications_id, count, day) FROM stdin;
056867c7-e20a-4af9-8570-47100496229f	1	19344
3c219e58-ed0e-4b18-ad48-f4f92793ae32	1	19344
\.


--
-- Data for Name: application_monthly_active_users; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.application_monthly_active_users (applications_id, count, month) FROM stdin;
3c219e58-ed0e-4b18-ad48-f4f92793ae32	1	635
056867c7-e20a-4af9-8570-47100496229f	1	635
\.


--
-- Data for Name: application_registration_counts; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.application_registration_counts (applications_id, count, decremented_count, hour) FROM stdin;
3c219e58-ed0e-4b18-ad48-f4f92793ae32	1	0	464265
056867c7-e20a-4af9-8570-47100496229f	1	0	464265
\.


--
-- Data for Name: application_roles; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.application_roles (id, applications_id, description, insert_instant, is_default, is_super_role, last_update_instant, name) FROM stdin;
631ecd9d-8d40-4c13-8277-80cedb8236e2	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Global admin	1671355209652	f	t	1671355209652	admin
631ecd9d-8d40-4c13-8277-80cedb8236e3	3c219e58-ed0e-4b18-ad48-f4f92793ae32	API key manager	1671355209652	f	f	1671355209652	api_key_manager
631ecd9d-8d40-4c13-8277-80cedb8236e4	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Application deleter	1671355209652	f	f	1671355209652	application_deleter
631ecd9d-8d40-4c13-8277-80cedb8236e5	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Application manager	1671355209652	f	f	1671355209652	application_manager
631ecd9d-8d40-4c13-8277-80cedb8236e6	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Audit log viewer	1671355209652	f	f	1671355209652	audit_log_viewer
631ecd9d-8d40-4c13-8277-80cedb8236e7	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Email template manager	1671355209652	f	f	1671355209652	email_template_manager
631ecd9d-8d40-4c13-8277-80cedb8236e8	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Report viewer	1671355209652	f	f	1671355209652	report_viewer
631ecd9d-8d40-4c13-8277-80cedb8236e9	3c219e58-ed0e-4b18-ad48-f4f92793ae32	System configuration manager	1671355209652	f	f	1671355209652	system_manager
631ecd9d-8d40-4c13-8277-80cedb8236f0	3c219e58-ed0e-4b18-ad48-f4f92793ae32	User action deleter	1671355209652	f	f	1671355209652	user_action_deleter
631ecd9d-8d40-4c13-8277-80cedb8236f1	3c219e58-ed0e-4b18-ad48-f4f92793ae32	User action manager	1671355209652	f	f	1671355209652	user_action_manager
631ecd9d-8d40-4c13-8277-80cedb8236f2	3c219e58-ed0e-4b18-ad48-f4f92793ae32	User deleter	1671355209652	f	f	1671355209652	user_deleter
631ecd9d-8d40-4c13-8277-80cedb8236f3	3c219e58-ed0e-4b18-ad48-f4f92793ae32	User manager	1671355209652	f	f	1671355209652	user_manager
631ecd9d-8d40-4c13-8277-80cedb8236f4	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Webhook manager	1671355209652	f	f	1671355209652	webhook_manager
631ecd9d-8d40-4c13-8277-80cedb8236f5	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Group manager	1671355209652	f	f	1671355209652	group_manager
631ecd9d-8d40-4c13-8277-80cedb8236f6	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Group deleter	1671355209652	f	f	1671355209652	group_deleter
631ecd9d-8d40-4c13-8277-80cedb8236f7	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Tenant manager	1671355209652	f	f	1671355209652	tenant_manager
631ecd9d-8d40-4c13-8277-80cedb8236f8	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Tenant deleter	1671355209652	f	f	1671355209652	tenant_deleter
631ecd9d-8d40-4c13-8277-80cedb8236f9	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Lambda manager	1671355209652	f	f	1671355209652	lambda_manager
631ecd9d-8d40-4c13-8277-80cedb8236fa	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Event log viewer	1671355209652	f	f	1671355209652	event_log_viewer
631ecd9d-8d40-4c13-8277-80cedb8236fb	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Key manager	1671355209652	f	f	1671355209652	key_manager
631ecd9d-8d40-4c13-8277-80cedb8236fc	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Consent deleter	1671355209652	f	f	1671355209652	consent_deleter
631ecd9d-8d40-4c13-8277-80cedb8236fd	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Consent manager	1671355209652	f	f	1671355209652	consent_manager
631ecd9d-8d40-4c13-8277-80cedb8236fe	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Theme manager	1671355209652	f	f	1671355209652	theme_manager
631ecd9d-8d40-4c13-8277-80cedb8236ff	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Reactor manager	1671355209652	f	f	1671355209652	reactor_manager
631ecd9d-8d40-4c13-8277-80cedb823700	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Connector deleter	1671355209652	f	f	1671355209652	connector_deleter
631ecd9d-8d40-4c13-8277-80cedb823701	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Connector manager	1671355209652	f	f	1671355209652	connector_manager
631ecd9d-8d40-4c13-8277-80cedb823702	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Form deleter	1671355209652	f	f	1671355209652	form_deleter
631ecd9d-8d40-4c13-8277-80cedb823703	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Form manager	1671355209652	f	f	1671355209652	form_manager
631ecd9d-8d40-4c13-8277-80cedb823704	3c219e58-ed0e-4b18-ad48-f4f92793ae32	User support manager	1671355209652	f	f	1671355209652	user_support_manager
631ecd9d-8d40-4c13-8277-80cedb823705	3c219e58-ed0e-4b18-ad48-f4f92793ae32	User support viewer	1671355209652	f	f	1671355209652	user_support_viewer
631ecd9d-8d40-4c13-8277-80cedb823706	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Entity manager	1671355209652	f	f	1671355209652	entity_manager
631ecd9d-8d40-4c13-8277-80cedb823707	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Messenger deleter	1671355209652	f	f	1671355209652	messenger_deleter
631ecd9d-8d40-4c13-8277-80cedb823708	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Messenger manager	1671355209652	f	f	1671355209652	messenger_manager
631ecd9d-8d40-4c13-8277-80cedb823709	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Message template deleter	1671355209652	f	f	1671355209652	message_template_deleter
631ecd9d-8d40-4c13-8277-80cedb823710	3c219e58-ed0e-4b18-ad48-f4f92793ae32	Message template manager	1671355209652	f	f	1671355209652	message_template_manager
631ecd9d-8d40-4c13-8277-80cedb823711	3c219e58-ed0e-4b18-ad48-f4f92793ae32	ACL deleter	1671355209652	f	f	1671355209652	acl_deleter
631ecd9d-8d40-4c13-8277-80cedb823712	3c219e58-ed0e-4b18-ad48-f4f92793ae32	ACL manager	1671355209652	f	f	1671355209652	acl_manager
ff345e71-6387-466e-9601-7362e3540eb0	056867c7-e20a-4af9-8570-47100496229f	\N	1671356177125	f	f	1671356177125	admin
\.


--
-- Data for Name: applications; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.applications (id, access_token_populate_lambdas_id, access_token_signing_keys_id, active, admin_registration_forms_id, data, email_update_email_templates_id, email_verification_email_templates_id, email_verified_email_templates_id, forgot_password_email_templates_id, forms_id, id_token_populate_lambdas_id, id_token_signing_keys_id, insert_instant, last_update_instant, login_id_in_use_on_create_email_templates_id, login_id_in_use_on_update_email_templates_id, login_new_device_email_templates_id, login_suspicious_email_templates_id, multi_factor_email_message_templates_id, multi_factor_sms_message_templates_id, name, passwordless_email_templates_id, password_update_email_templates_id, password_reset_success_email_templates_id, samlv2_default_verification_keys_id, samlv2_issuer, samlv2_keys_id, samlv2_logout_keys_id, samlv2_logout_default_verification_keys_id, samlv2_populate_lambdas_id, samlv2_single_logout_keys_id, self_service_user_forms_id, set_password_email_templates_id, tenants_id, themes_id, two_factor_method_add_email_templates_id, two_factor_method_remove_email_templates_id, ui_ip_access_control_lists_id, verification_email_templates_id) FROM stdin;
3c219e58-ed0e-4b18-ad48-f4f92793ae32	\N	a06e2c22-5bf5-04c0-e2a8-137ed19b3cf6	t	2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66	{"jwtConfiguration": {"enabled": true, "timeToLiveInSeconds": 60, "refreshTokenExpirationPolicy": "SlidingWindow", "refreshTokenTimeToLiveInMinutes": 60, "refreshTokenUsagePolicy": "OneTimeUse"},"registrationConfiguration": {"type":"basic"}, "oauthConfiguration": {"authorizedRedirectURLs": ["/admin/login"], "clientId": "3c219e58-ed0e-4b18-ad48-f4f92793ae32", "clientSecret": "MzhjYmE2OWVhZDMxZWE2ZDJiZjkxMzk1YjE3ZTM0OWRmYzkyNWJhYjkwNzM4YmQ1YjU0ZjM5NjNkNGIwODNhNA==", "enabledGrants": ["authorization_code", "refresh_token"], "logoutURL": "/admin/single-logout", "generateRefreshTokens": true, "clientAuthenticationPolicy": "Required", "proofKeyForCodeExchangePolicy": "Required" },"loginConfiguration": {"allowTokenRefresh": false, "generateRefreshTokens": false, "requireAuthentication": true},"unverified":{ "behavior": "Allow" },"verificationStrategy":"ClickableLink","state": "Active"}	\N	\N	\N	\N	\N	\N	092dbedc-30af-4149-9c61-b578f2c72f59	1671355209652	1671355209652	\N	\N	\N	\N	\N	\N	FusionAuth	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	4544ae7e-7b90-67da-4ee5-91f1afd726d9	\N	\N	\N	\N	\N
056867c7-e20a-4af9-8570-47100496229f	\N	e71e1cb5-691f-4ccd-95ca-dd9529f53897	t	2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66	{"accessControlConfiguration":{},"authenticationTokenConfiguration":{"enabled":false},"data":{},"externalIdentifierConfiguration":{},"formConfiguration":{"adminRegistrationFormId":"2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66"},"jwtConfiguration":{"enabled":true,"refreshTokenExpirationPolicy":"Fixed","refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"loginConfiguration":{"allowTokenRefresh":false,"generateRefreshTokens":false,"requireAuthentication":true},"multiFactorConfiguration":{"email":{},"sms":{}},"oauthConfiguration":{"authorizedRedirectURLs":["http://localhost:4200/json/oauth-redirect"],"clientAuthenticationPolicy":"Required","clientId":"056867c7-e20a-4af9-8570-47100496229f","clientSecret":"Tjm7lugRNeFRQkP3P_FqLdzwrZxhOTF_30YvZCp66rg","debug":false,"enabledGrants":["authorization_code","refresh_token"],"generateRefreshTokens":true,"logoutBehavior":"AllApplications","logoutURL":"http://localhost:4200/viewer","proofKeyForCodeExchangePolicy":"NotRequired","requireClientAuthentication":true,"requireRegistration":false},"passwordlessConfiguration":{"enabled":false},"registrationConfiguration":{"birthDate":{"enabled":false,"required":false},"confirmPassword":false,"enabled":false,"firstName":{"enabled":false,"required":false},"fullName":{"enabled":false,"required":false},"lastName":{"enabled":false,"required":false},"loginIdType":"email","middleName":{"enabled":false,"required":false},"mobilePhone":{"enabled":false,"required":false},"type":"basic"},"registrationDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"samlv2Configuration":{"debug":false,"enabled":false,"initiatedLogin":{"enabled":false,"nameIdFormat":"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"},"logout":{"behavior":"AllParticipants","requireSignedRequests":false,"singleLogout":{"enabled":false,"xmlSignatureC14nMethod":"exclusive_with_comments"},"xmlSignatureC14nMethod":"exclusive_with_comments"},"requireSignedRequests":false,"xmlSignatureC14nMethod":"exclusive_with_comments","xmlSignatureLocation":"Assertion"},"state":"Active","unverified":{"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyRegistration":false,"webAuthnConfiguration":{"bootstrapWorkflow":{"enabled":false},"enabled":false,"reauthenticationWorkflow":{"enabled":false}}}	\N	\N	\N	\N	\N	\N	720146f5-7f56-4c86-a0f0-b429b7e8929d	1671355252302	1671356262885	\N	\N	\N	\N	\N	\N	Panomicon	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	4544ae7e-7b90-67da-4ee5-91f1afd726d9	\N	\N	\N	\N	\N
\.


--
-- Data for Name: asynchronous_tasks; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.asynchronous_tasks (id, data, entity_id, insert_instant, last_update_instant, nodes_id, status, type) FROM stdin;
\.


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.audit_logs (id, insert_instant, insert_user, message, data) FROM stdin;
1	1671355252334	yuji.kosugi@gmail.com	Created the application with Id [056867c7-e20a-4af9-8570-47100496229f] and name [panomicon]	{"data":{},"reason":"FusionAuth User Interface"}
2	1671355487069	yuji.kosugi@gmail.com	Updated the tenant with Id [4544ae7e-7b90-67da-4ee5-91f1afd726d9] and name [Default]	{"data":{},"newValue":{"accessControlConfiguration":{},"captchaConfiguration":{"captchaMethod":"GoogleRecaptchaV3","enabled":false,"threshold":0.5},"configured":true,"connectorPolicies":[{"connectorId":"e3306678-a53a-4964-9040-1c96f36dda72","domains":["*"],"migrate":false}],"data":{},"emailConfiguration":{"additionalHeaders":[],"debug":false,"defaultFromEmail":"change-me@example.com","defaultFromName":"FusionAuth","forgotPasswordEmailTemplateId":"4e0def6a-f6c6-45fa-a1a5-704edd8a163e","host":"localhost","implicitEmailVerificationAllowed":true,"passwordlessEmailTemplateId":"9c4cb0b7-c8f8-4169-9dc7-83072b90f654","port":25,"security":"NONE","setPasswordEmailTemplateId":"1e93cf8f-cc01-4aa8-9600-fdd95748337e","unverified":{"allowEmailChangeWhenGated":false,"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyEmail":false,"verifyEmailWhenChanged":false},"eventConfiguration":{"events":{"group.create":{"enabled":false,"transactionType":"None"},"group.create.complete":{"enabled":false,"transactionType":"None"},"group.delete":{"enabled":false,"transactionType":"None"},"group.delete.complete":{"enabled":false,"transactionType":"None"},"group.member.add":{"enabled":false,"transactionType":"None"},"group.member.add.complete":{"enabled":false,"transactionType":"None"},"group.member.remove":{"enabled":false,"transactionType":"None"},"group.member.remove.complete":{"enabled":false,"transactionType":"None"},"group.member.update":{"enabled":false,"transactionType":"None"},"group.member.update.complete":{"enabled":false,"transactionType":"None"},"group.update":{"enabled":false,"transactionType":"None"},"group.update.complete":{"enabled":false,"transactionType":"None"},"jwt.public-key.update":{"enabled":false,"transactionType":"None"},"jwt.refresh":{"enabled":false,"transactionType":"None"},"jwt.refresh-token.revoke":{"enabled":false,"transactionType":"None"},"user.action":{"enabled":false,"transactionType":"None"},"user.bulk.create":{"enabled":false,"transactionType":"None"},"user.create":{"enabled":false,"transactionType":"None"},"user.create.complete":{"enabled":false,"transactionType":"None"},"user.deactivate":{"enabled":false,"transactionType":"None"},"user.delete":{"enabled":false,"transactionType":"None"},"user.delete.complete":{"enabled":false,"transactionType":"None"},"user.email.update":{"enabled":false,"transactionType":"None"},"user.email.verified":{"enabled":false,"transactionType":"None"},"user.identity-provider.link":{"enabled":false,"transactionType":"None"},"user.identity-provider.unlink":{"enabled":false,"transactionType":"None"},"user.login.failed":{"enabled":false,"transactionType":"None"},"user.login.new-device":{"enabled":false,"transactionType":"None"},"user.login.success":{"enabled":false,"transactionType":"None"},"user.login.suspicious":{"enabled":false,"transactionType":"None"},"user.loginId.duplicate.create":{"enabled":false,"transactionType":"None"},"user.loginId.duplicate.update":{"enabled":false,"transactionType":"None"},"user.password.breach":{"enabled":false,"transactionType":"None"},"user.password.reset.send":{"enabled":false,"transactionType":"None"},"user.password.reset.start":{"enabled":false,"transactionType":"None"},"user.password.reset.success":{"enabled":false,"transactionType":"None"},"user.password.update":{"enabled":false,"transactionType":"None"},"user.reactivate":{"enabled":false,"transactionType":"None"},"user.registration.create":{"enabled":false,"transactionType":"None"},"user.registration.create.complete":{"enabled":false,"transactionType":"None"},"user.registration.delete":{"enabled":false,"transactionType":"None"},"user.registration.delete.complete":{"enabled":false,"transactionType":"None"},"user.registration.update":{"enabled":false,"transactionType":"None"},"user.registration.update.complete":{"enabled":false,"transactionType":"None"},"user.registration.verified":{"enabled":false,"transactionType":"None"},"user.two-factor.method.add":{"enabled":false,"transactionType":"None"},"user.two-factor.method.remove":{"enabled":false,"transactionType":"None"},"user.update":{"enabled":false,"transactionType":"None"},"user.update.complete":{"enabled":false,"transactionType":"None"}}},"externalIdentifierConfiguration":{"authorizationGrantIdTimeToLiveInSeconds":30,"changePasswordIdGenerator":{"length":32,"type":"randomBytes"},"changePasswordIdTimeToLiveInSeconds":600,"deviceCodeTimeToLiveInSeconds":300,"deviceUserCodeIdGenerator":{"length":6,"type":"randomAlphaNumeric"},"emailVerificationIdGenerator":{"length":32,"type":"randomBytes"},"emailVerificationIdTimeToLiveInSeconds":86400,"emailVerificationOneTimeCodeGenerator":{"length":6,"type":"randomAlphaNumeric"},"externalAuthenticationIdTimeToLiveInSeconds":300,"oneTimePasswordTimeToLiveInSeconds":60,"passwordlessLoginGenerator":{"length":32,"type":"randomBytes"},"passwordlessLoginTimeToLiveInSeconds":180,"pendingAccountLinkTimeToLiveInSeconds":3600,"registrationVerificationIdGenerator":{"length":32,"type":"randomBytes"},"registrationVerificationIdTimeToLiveInSeconds":86400,"registrationVerificationOneTimeCodeGenerator":{"length":6,"type":"randomAlphaNumeric"},"samlv2AuthNRequestIdTimeToLiveInSeconds":300,"setupPasswordIdGenerator":{"length":32,"type":"randomBytes"},"setupPasswordIdTimeToLiveInSeconds":86400,"trustTokenTimeToLiveInSeconds":180,"twoFactorIdTimeToLiveInSeconds":300,"twoFactorOneTimeCodeIdGenerator":{"length":6,"type":"randomDigits"},"twoFactorOneTimeCodeIdTimeToLiveInSeconds":60,"twoFactorTrustIdTimeToLiveInSeconds":2592000,"webAuthnAuthenticationChallengeTimeToLiveInSeconds":180,"webAuthnRegistrationChallengeTimeToLiveInSeconds":180},"failedAuthenticationConfiguration":{"actionCancelPolicy":{"onPasswordReset":false},"actionDuration":3,"actionDurationUnit":"MINUTES","emailUser":false,"resetCountInSeconds":60,"tooManyAttempts":5},"familyConfiguration":{"allowChildRegistrations":true,"deleteOrphanedAccounts":false,"deleteOrphanedAccountsDays":30,"enabled":false,"maximumChildAge":12,"minimumOwnerAge":21,"parentEmailRequired":false},"formConfiguration":{"adminUserFormId":"4efdc20f-b73f-8831-c724-d4b140a6c13d"},"httpSessionMaxInactiveInterval":3600,"id":"4544ae7e-7b90-67da-4ee5-91f1afd726d9","insertInstant":1671355208652,"issuer":"toxygates.nibiohn.go.jp","jwtConfiguration":{"accessTokenKeyId":"a06e2c22-5bf5-04c0-e2a8-137ed19b3cf6","idTokenKeyId":"092dbedc-30af-4149-9c61-b578f2c72f59","refreshTokenExpirationPolicy":"Fixed","refreshTokenRevocationPolicy":{"onLoginPrevented":true,"onMultiFactorEnable":false,"onPasswordChanged":true},"refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"lambdaConfiguration":{},"lastUpdateInstant":1671355486998,"loginConfiguration":{"requireAuthentication":true},"maximumPasswordAge":{"days":180,"enabled":false},"minimumPasswordAge":{"enabled":false,"seconds":30},"multiFactorConfiguration":{"authenticator":{"algorithm":"HmacSHA1","codeLength":6,"enabled":true,"timeStep":30},"email":{"enabled":false,"templateId":"6b34ee69-542b-4926-a469-ada36ddf39cf"},"loginPolicy":"Enabled","sms":{"enabled":false}},"name":"Default","oauthConfiguration":{},"passwordEncryptionConfiguration":{"encryptionScheme":"salted-pbkdf2-hmac-sha256","encryptionSchemeFactor":24000,"modifyEncryptionSchemeOnLogin":false},"passwordValidationRules":{"breachDetection":{"enabled":false,"matchMode":"High","notifyUserEmailTemplateId":"421c7d2d-f85c-4719-adb9-5bbb32501b14","onLogin":"Off"},"maxLength":256,"minLength":8,"rememberPreviousPasswords":{"count":1,"enabled":false},"requireMixedCase":false,"requireNonAlpha":false,"requireNumber":false,"validateOnLogin":false},"rateLimitConfiguration":{"failedLogin":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"forgotPassword":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendEmailVerification":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendPasswordless":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendRegistrationVerification":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendTwoFactor":{"enabled":false,"limit":5,"timePeriodInSeconds":60}},"registrationConfiguration":{"blockedDomains":[]},"scimServerConfiguration":{"enabled":false},"ssoConfiguration":{"deviceTrustTimeToLiveInSeconds":31536000},"state":"Active","themeId":"75a068fd-e94b-451a-9aeb-3ddb9a3b5987","userDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"usernameConfiguration":{"unique":{"enabled":false,"numberOfDigits":5,"separator":"#","strategy":"OnCollision"}},"webAuthnConfiguration":{"bootstrapWorkflow":{"authenticatorAttachmentPreference":"any","enabled":false,"userVerificationRequirement":"required"},"debug":false,"enabled":false,"reauthenticationWorkflow":{"authenticatorAttachmentPreference":"platform","enabled":false,"userVerificationRequirement":"required"}}},"oldValue":{"accessControlConfiguration":{},"captchaConfiguration":{"captchaMethod":"GoogleRecaptchaV3","enabled":false,"threshold":0.5},"configured":false,"connectorPolicies":[{"connectorId":"e3306678-a53a-4964-9040-1c96f36dda72","domains":["*"],"migrate":false}],"data":{},"emailConfiguration":{"additionalHeaders":[],"debug":false,"defaultFromEmail":"change-me@example.com","defaultFromName":"FusionAuth","forgotPasswordEmailTemplateId":"4e0def6a-f6c6-45fa-a1a5-704edd8a163e","host":"localhost","implicitEmailVerificationAllowed":true,"passwordlessEmailTemplateId":"9c4cb0b7-c8f8-4169-9dc7-83072b90f654","port":25,"setPasswordEmailTemplateId":"1e93cf8f-cc01-4aa8-9600-fdd95748337e","unverified":{"allowEmailChangeWhenGated":false,"behavior":"Allow"},"verifyEmail":false,"verifyEmailWhenChanged":false},"eventConfiguration":{"events":{}},"externalIdentifierConfiguration":{"authorizationGrantIdTimeToLiveInSeconds":30,"changePasswordIdGenerator":{"length":32,"type":"randomBytes"},"changePasswordIdTimeToLiveInSeconds":600,"deviceCodeTimeToLiveInSeconds":300,"deviceUserCodeIdGenerator":{"length":6,"type":"randomAlphaNumeric"},"emailVerificationIdGenerator":{"length":32,"type":"randomBytes"},"emailVerificationIdTimeToLiveInSeconds":86400,"emailVerificationOneTimeCodeGenerator":{"length":6,"type":"randomAlphaNumeric"},"externalAuthenticationIdTimeToLiveInSeconds":300,"oneTimePasswordTimeToLiveInSeconds":60,"passwordlessLoginGenerator":{"length":32,"type":"randomBytes"},"passwordlessLoginTimeToLiveInSeconds":180,"pendingAccountLinkTimeToLiveInSeconds":3600,"registrationVerificationIdGenerator":{"length":32,"type":"randomBytes"},"registrationVerificationIdTimeToLiveInSeconds":86400,"registrationVerificationOneTimeCodeGenerator":{"length":6,"type":"randomAlphaNumeric"},"samlv2AuthNRequestIdTimeToLiveInSeconds":300,"setupPasswordIdGenerator":{"length":32,"type":"randomBytes"},"setupPasswordIdTimeToLiveInSeconds":86400,"trustTokenTimeToLiveInSeconds":180,"twoFactorIdTimeToLiveInSeconds":300,"twoFactorOneTimeCodeIdGenerator":{"length":6,"type":"randomDigits"},"twoFactorOneTimeCodeIdTimeToLiveInSeconds":60,"twoFactorTrustIdTimeToLiveInSeconds":2592000,"webAuthnAuthenticationChallengeTimeToLiveInSeconds":180,"webAuthnRegistrationChallengeTimeToLiveInSeconds":180},"failedAuthenticationConfiguration":{"actionCancelPolicy":{"onPasswordReset":false},"actionDuration":3,"actionDurationUnit":"MINUTES","emailUser":false,"resetCountInSeconds":60,"tooManyAttempts":5},"familyConfiguration":{"allowChildRegistrations":true,"deleteOrphanedAccounts":false,"deleteOrphanedAccountsDays":30,"enabled":false,"maximumChildAge":12,"minimumOwnerAge":21,"parentEmailRequired":false},"formConfiguration":{"adminUserFormId":"4efdc20f-b73f-8831-c724-d4b140a6c13d"},"httpSessionMaxInactiveInterval":3600,"id":"4544ae7e-7b90-67da-4ee5-91f1afd726d9","insertInstant":1671355208652,"issuer":"acme.com","jwtConfiguration":{"accessTokenKeyId":"a06e2c22-5bf5-04c0-e2a8-137ed19b3cf6","idTokenKeyId":"092dbedc-30af-4149-9c61-b578f2c72f59","refreshTokenExpirationPolicy":"Fixed","refreshTokenRevocationPolicy":{"onLoginPrevented":true,"onMultiFactorEnable":false,"onPasswordChanged":true},"refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"lambdaConfiguration":{},"lastUpdateInstant":1671355235755,"loginConfiguration":{"requireAuthentication":true},"maximumPasswordAge":{"days":180,"enabled":false},"minimumPasswordAge":{"enabled":false,"seconds":30},"multiFactorConfiguration":{"authenticator":{"algorithm":"HmacSHA1","codeLength":6,"enabled":true,"timeStep":30},"email":{"enabled":false,"templateId":"6b34ee69-542b-4926-a469-ada36ddf39cf"},"loginPolicy":"Enabled","sms":{"enabled":false}},"name":"Default","oauthConfiguration":{},"passwordEncryptionConfiguration":{"encryptionScheme":"salted-pbkdf2-hmac-sha256","encryptionSchemeFactor":24000,"modifyEncryptionSchemeOnLogin":false},"passwordValidationRules":{"breachDetection":{"enabled":false,"notifyUserEmailTemplateId":"421c7d2d-f85c-4719-adb9-5bbb32501b14"},"maxLength":256,"minLength":8,"rememberPreviousPasswords":{"count":1,"enabled":false},"requireMixedCase":false,"requireNonAlpha":false,"requireNumber":false,"validateOnLogin":false},"rateLimitConfiguration":{"failedLogin":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"forgotPassword":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendEmailVerification":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendPasswordless":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendRegistrationVerification":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendTwoFactor":{"enabled":false,"limit":5,"timePeriodInSeconds":60}},"registrationConfiguration":{"blockedDomains":[]},"scimServerConfiguration":{"enabled":false},"ssoConfiguration":{"deviceTrustTimeToLiveInSeconds":31536000},"state":"Active","themeId":"75a068fd-e94b-451a-9aeb-3ddb9a3b5987","userDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"usernameConfiguration":{"unique":{"enabled":false,"numberOfDigits":5,"separator":"#","strategy":"OnCollision"}},"webAuthnConfiguration":{"bootstrapWorkflow":{"authenticatorAttachmentPreference":"any","enabled":false,"userVerificationRequirement":"required"},"debug":false,"enabled":false,"reauthenticationWorkflow":{"authenticatorAttachmentPreference":"platform","enabled":false,"userVerificationRequirement":"required"}}},"reason":"FusionAuth User Interface"}
3	1671355494839	yuji.kosugi@gmail.com	Updated the application with Id [056867c7-e20a-4af9-8570-47100496229f] and name [panomicona]	{"data":{},"newValue":{"accessControlConfiguration":{},"active":true,"authenticationTokenConfiguration":{"enabled":false},"data":{},"emailConfiguration":{},"externalIdentifierConfiguration":{},"formConfiguration":{"adminRegistrationFormId":"2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66"},"id":"056867c7-e20a-4af9-8570-47100496229f","insertInstant":1671355252302,"jwtConfiguration":{"accessTokenKeyId":"a06e2c22-5bf5-04c0-e2a8-137ed19b3cf6","enabled":false,"idTokenKeyId":"092dbedc-30af-4149-9c61-b578f2c72f59","refreshTokenExpirationPolicy":"Fixed","refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"lambdaConfiguration":{},"lastUpdateInstant":1671355494830,"loginConfiguration":{"allowTokenRefresh":false,"generateRefreshTokens":false,"requireAuthentication":true},"multiFactorConfiguration":{"email":{},"sms":{}},"name":"panomicona","oauthConfiguration":{"authorizedOriginURLs":[],"authorizedRedirectURLs":[],"clientAuthenticationPolicy":"Required","clientId":"056867c7-e20a-4af9-8570-47100496229f","clientSecret":"Tjm7lugRNeFRQkP3P_FqLdzwrZxhOTF_30YvZCp66rg","debug":false,"enabledGrants":["authorization_code","refresh_token"],"generateRefreshTokens":true,"logoutBehavior":"AllApplications","proofKeyForCodeExchangePolicy":"NotRequired","requireClientAuthentication":true,"requireRegistration":false},"passwordlessConfiguration":{"enabled":false},"registrationConfiguration":{"birthDate":{"enabled":false,"required":false},"confirmPassword":false,"enabled":false,"firstName":{"enabled":false,"required":false},"fullName":{"enabled":false,"required":false},"lastName":{"enabled":false,"required":false},"loginIdType":"email","middleName":{"enabled":false,"required":false},"mobilePhone":{"enabled":false,"required":false},"type":"basic"},"registrationDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"roles":[],"samlv2Configuration":{"authorizedRedirectURLs":[],"debug":false,"enabled":false,"initiatedLogin":{"enabled":false,"nameIdFormat":"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"},"logout":{"behavior":"AllParticipants","requireSignedRequests":false,"singleLogout":{"enabled":false,"xmlSignatureC14nMethod":"exclusive_with_comments"},"xmlSignatureC14nMethod":"exclusive_with_comments"},"requireSignedRequests":false,"xmlSignatureC14nMethod":"exclusive_with_comments","xmlSignatureLocation":"Assertion"},"state":"Active","tenantId":"4544ae7e-7b90-67da-4ee5-91f1afd726d9","unverified":{"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyRegistration":false,"webAuthnConfiguration":{"bootstrapWorkflow":{"enabled":false},"enabled":false,"reauthenticationWorkflow":{"enabled":false}}},"oldValue":{"accessControlConfiguration":{},"active":true,"authenticationTokenConfiguration":{"enabled":false},"data":{},"emailConfiguration":{},"externalIdentifierConfiguration":{},"formConfiguration":{"adminRegistrationFormId":"2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66"},"id":"056867c7-e20a-4af9-8570-47100496229f","insertInstant":1671355252302,"jwtConfiguration":{"accessTokenKeyId":"a06e2c22-5bf5-04c0-e2a8-137ed19b3cf6","enabled":false,"idTokenKeyId":"092dbedc-30af-4149-9c61-b578f2c72f59","refreshTokenExpirationPolicy":"Fixed","refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"lambdaConfiguration":{},"lastUpdateInstant":1671355252302,"loginConfiguration":{"allowTokenRefresh":false,"generateRefreshTokens":false,"requireAuthentication":true},"multiFactorConfiguration":{"email":{},"sms":{}},"name":"panomicon","oauthConfiguration":{"authorizedOriginURLs":[],"authorizedRedirectURLs":[],"clientAuthenticationPolicy":"Required","clientId":"056867c7-e20a-4af9-8570-47100496229f","clientSecret":"Tjm7lugRNeFRQkP3P_FqLdzwrZxhOTF_30YvZCp66rg","debug":false,"enabledGrants":["authorization_code","refresh_token"],"generateRefreshTokens":true,"logoutBehavior":"AllApplications","proofKeyForCodeExchangePolicy":"NotRequired","requireClientAuthentication":true,"requireRegistration":false},"passwordlessConfiguration":{"enabled":false},"registrationConfiguration":{"birthDate":{"enabled":false,"required":false},"confirmPassword":false,"enabled":false,"firstName":{"enabled":false,"required":false},"fullName":{"enabled":false,"required":false},"lastName":{"enabled":false,"required":false},"loginIdType":"email","middleName":{"enabled":false,"required":false},"mobilePhone":{"enabled":false,"required":false},"type":"basic"},"registrationDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"roles":[],"samlv2Configuration":{"authorizedRedirectURLs":[],"debug":false,"enabled":false,"initiatedLogin":{"enabled":false,"nameIdFormat":"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"},"logout":{"behavior":"AllParticipants","requireSignedRequests":false,"singleLogout":{"enabled":false,"xmlSignatureC14nMethod":"exclusive_with_comments"},"xmlSignatureC14nMethod":"exclusive_with_comments"},"requireSignedRequests":false,"xmlSignatureC14nMethod":"exclusive_with_comments","xmlSignatureLocation":"Assertion"},"state":"Active","tenantId":"4544ae7e-7b90-67da-4ee5-91f1afd726d9","unverified":{"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyRegistration":false,"webAuthnConfiguration":{"bootstrapWorkflow":{"enabled":false},"enabled":false,"reauthenticationWorkflow":{"enabled":false}}},"reason":"FusionAuth User Interface"}
4	1671355500162	yuji.kosugi@gmail.com	Updated the application with Id [056867c7-e20a-4af9-8570-47100496229f] and name [Panomicon]	{"data":{},"newValue":{"accessControlConfiguration":{},"active":true,"authenticationTokenConfiguration":{"enabled":false},"data":{},"emailConfiguration":{},"externalIdentifierConfiguration":{},"formConfiguration":{"adminRegistrationFormId":"2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66"},"id":"056867c7-e20a-4af9-8570-47100496229f","insertInstant":1671355252302,"jwtConfiguration":{"accessTokenKeyId":"a06e2c22-5bf5-04c0-e2a8-137ed19b3cf6","enabled":false,"idTokenKeyId":"092dbedc-30af-4149-9c61-b578f2c72f59","refreshTokenExpirationPolicy":"Fixed","refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"lambdaConfiguration":{},"lastUpdateInstant":1671355500115,"loginConfiguration":{"allowTokenRefresh":false,"generateRefreshTokens":false,"requireAuthentication":true},"multiFactorConfiguration":{"email":{},"sms":{}},"name":"Panomicon","oauthConfiguration":{"authorizedOriginURLs":[],"authorizedRedirectURLs":[],"clientAuthenticationPolicy":"Required","clientId":"056867c7-e20a-4af9-8570-47100496229f","clientSecret":"Tjm7lugRNeFRQkP3P_FqLdzwrZxhOTF_30YvZCp66rg","debug":false,"enabledGrants":["authorization_code","refresh_token"],"generateRefreshTokens":true,"logoutBehavior":"AllApplications","proofKeyForCodeExchangePolicy":"NotRequired","requireClientAuthentication":true,"requireRegistration":false},"passwordlessConfiguration":{"enabled":false},"registrationConfiguration":{"birthDate":{"enabled":false,"required":false},"confirmPassword":false,"enabled":false,"firstName":{"enabled":false,"required":false},"fullName":{"enabled":false,"required":false},"lastName":{"enabled":false,"required":false},"loginIdType":"email","middleName":{"enabled":false,"required":false},"mobilePhone":{"enabled":false,"required":false},"type":"basic"},"registrationDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"roles":[],"samlv2Configuration":{"authorizedRedirectURLs":[],"debug":false,"enabled":false,"initiatedLogin":{"enabled":false,"nameIdFormat":"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"},"logout":{"behavior":"AllParticipants","requireSignedRequests":false,"singleLogout":{"enabled":false,"xmlSignatureC14nMethod":"exclusive_with_comments"},"xmlSignatureC14nMethod":"exclusive_with_comments"},"requireSignedRequests":false,"xmlSignatureC14nMethod":"exclusive_with_comments","xmlSignatureLocation":"Assertion"},"state":"Active","tenantId":"4544ae7e-7b90-67da-4ee5-91f1afd726d9","unverified":{"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyRegistration":false,"webAuthnConfiguration":{"bootstrapWorkflow":{"enabled":false},"enabled":false,"reauthenticationWorkflow":{"enabled":false}}},"oldValue":{"accessControlConfiguration":{},"active":true,"authenticationTokenConfiguration":{"enabled":false},"data":{},"emailConfiguration":{},"externalIdentifierConfiguration":{},"formConfiguration":{"adminRegistrationFormId":"2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66"},"id":"056867c7-e20a-4af9-8570-47100496229f","insertInstant":1671355252302,"jwtConfiguration":{"accessTokenKeyId":"a06e2c22-5bf5-04c0-e2a8-137ed19b3cf6","enabled":false,"idTokenKeyId":"092dbedc-30af-4149-9c61-b578f2c72f59","refreshTokenExpirationPolicy":"Fixed","refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"lambdaConfiguration":{},"lastUpdateInstant":1671355494830,"loginConfiguration":{"allowTokenRefresh":false,"generateRefreshTokens":false,"requireAuthentication":true},"multiFactorConfiguration":{"email":{},"sms":{}},"name":"panomicona","oauthConfiguration":{"authorizedOriginURLs":[],"authorizedRedirectURLs":[],"clientAuthenticationPolicy":"Required","clientId":"056867c7-e20a-4af9-8570-47100496229f","clientSecret":"Tjm7lugRNeFRQkP3P_FqLdzwrZxhOTF_30YvZCp66rg","debug":false,"enabledGrants":["authorization_code","refresh_token"],"generateRefreshTokens":true,"logoutBehavior":"AllApplications","proofKeyForCodeExchangePolicy":"NotRequired","requireClientAuthentication":true,"requireRegistration":false},"passwordlessConfiguration":{"enabled":false},"registrationConfiguration":{"birthDate":{"enabled":false,"required":false},"confirmPassword":false,"enabled":false,"firstName":{"enabled":false,"required":false},"fullName":{"enabled":false,"required":false},"lastName":{"enabled":false,"required":false},"loginIdType":"email","middleName":{"enabled":false,"required":false},"mobilePhone":{"enabled":false,"required":false},"type":"basic"},"registrationDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"roles":[],"samlv2Configuration":{"authorizedRedirectURLs":[],"debug":false,"enabled":false,"initiatedLogin":{"enabled":false,"nameIdFormat":"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"},"logout":{"behavior":"AllParticipants","requireSignedRequests":false,"singleLogout":{"enabled":false,"xmlSignatureC14nMethod":"exclusive_with_comments"},"xmlSignatureC14nMethod":"exclusive_with_comments"},"requireSignedRequests":false,"xmlSignatureC14nMethod":"exclusive_with_comments","xmlSignatureLocation":"Assertion"},"state":"Active","tenantId":"4544ae7e-7b90-67da-4ee5-91f1afd726d9","unverified":{"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyRegistration":false,"webAuthnConfiguration":{"bootstrapWorkflow":{"enabled":false},"enabled":false,"reauthenticationWorkflow":{"enabled":false}}},"reason":"FusionAuth User Interface"}
5	1671355521472	yuji.kosugi@gmail.com	Updated the application with Id [056867c7-e20a-4af9-8570-47100496229f] and name [Panomicon]	{"data":{},"newValue":{"accessControlConfiguration":{},"active":true,"authenticationTokenConfiguration":{"enabled":false},"data":{},"emailConfiguration":{},"externalIdentifierConfiguration":{},"formConfiguration":{"adminRegistrationFormId":"2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66"},"id":"056867c7-e20a-4af9-8570-47100496229f","insertInstant":1671355252302,"jwtConfiguration":{"accessTokenKeyId":"e71e1cb5-691f-4ccd-95ca-dd9529f53897","enabled":true,"idTokenKeyId":"720146f5-7f56-4c86-a0f0-b429b7e8929d","refreshTokenExpirationPolicy":"Fixed","refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"lambdaConfiguration":{},"lastUpdateInstant":1671355521465,"loginConfiguration":{"allowTokenRefresh":false,"generateRefreshTokens":false,"requireAuthentication":true},"multiFactorConfiguration":{"email":{},"sms":{}},"name":"Panomicon","oauthConfiguration":{"authorizedOriginURLs":[],"authorizedRedirectURLs":[],"clientAuthenticationPolicy":"Required","clientId":"056867c7-e20a-4af9-8570-47100496229f","clientSecret":"Tjm7lugRNeFRQkP3P_FqLdzwrZxhOTF_30YvZCp66rg","debug":false,"enabledGrants":["authorization_code","refresh_token"],"generateRefreshTokens":true,"logoutBehavior":"AllApplications","proofKeyForCodeExchangePolicy":"NotRequired","requireClientAuthentication":true,"requireRegistration":false},"passwordlessConfiguration":{"enabled":false},"registrationConfiguration":{"birthDate":{"enabled":false,"required":false},"confirmPassword":false,"enabled":false,"firstName":{"enabled":false,"required":false},"fullName":{"enabled":false,"required":false},"lastName":{"enabled":false,"required":false},"loginIdType":"email","middleName":{"enabled":false,"required":false},"mobilePhone":{"enabled":false,"required":false},"type":"basic"},"registrationDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"roles":[],"samlv2Configuration":{"authorizedRedirectURLs":[],"debug":false,"enabled":false,"initiatedLogin":{"enabled":false,"nameIdFormat":"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"},"logout":{"behavior":"AllParticipants","requireSignedRequests":false,"singleLogout":{"enabled":false,"xmlSignatureC14nMethod":"exclusive_with_comments"},"xmlSignatureC14nMethod":"exclusive_with_comments"},"requireSignedRequests":false,"xmlSignatureC14nMethod":"exclusive_with_comments","xmlSignatureLocation":"Assertion"},"state":"Active","tenantId":"4544ae7e-7b90-67da-4ee5-91f1afd726d9","unverified":{"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyRegistration":false,"webAuthnConfiguration":{"bootstrapWorkflow":{"enabled":false},"enabled":false,"reauthenticationWorkflow":{"enabled":false}}},"oldValue":{"accessControlConfiguration":{},"active":true,"authenticationTokenConfiguration":{"enabled":false},"data":{},"emailConfiguration":{},"externalIdentifierConfiguration":{},"formConfiguration":{"adminRegistrationFormId":"2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66"},"id":"056867c7-e20a-4af9-8570-47100496229f","insertInstant":1671355252302,"jwtConfiguration":{"accessTokenKeyId":"a06e2c22-5bf5-04c0-e2a8-137ed19b3cf6","enabled":false,"idTokenKeyId":"092dbedc-30af-4149-9c61-b578f2c72f59","refreshTokenExpirationPolicy":"Fixed","refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"lambdaConfiguration":{},"lastUpdateInstant":1671355500115,"loginConfiguration":{"allowTokenRefresh":false,"generateRefreshTokens":false,"requireAuthentication":true},"multiFactorConfiguration":{"email":{},"sms":{}},"name":"Panomicon","oauthConfiguration":{"authorizedOriginURLs":[],"authorizedRedirectURLs":[],"clientAuthenticationPolicy":"Required","clientId":"056867c7-e20a-4af9-8570-47100496229f","clientSecret":"Tjm7lugRNeFRQkP3P_FqLdzwrZxhOTF_30YvZCp66rg","debug":false,"enabledGrants":["authorization_code","refresh_token"],"generateRefreshTokens":true,"logoutBehavior":"AllApplications","proofKeyForCodeExchangePolicy":"NotRequired","requireClientAuthentication":true,"requireRegistration":false},"passwordlessConfiguration":{"enabled":false},"registrationConfiguration":{"birthDate":{"enabled":false,"required":false},"confirmPassword":false,"enabled":false,"firstName":{"enabled":false,"required":false},"fullName":{"enabled":false,"required":false},"lastName":{"enabled":false,"required":false},"loginIdType":"email","middleName":{"enabled":false,"required":false},"mobilePhone":{"enabled":false,"required":false},"type":"basic"},"registrationDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"roles":[],"samlv2Configuration":{"authorizedRedirectURLs":[],"debug":false,"enabled":false,"initiatedLogin":{"enabled":false,"nameIdFormat":"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"},"logout":{"behavior":"AllParticipants","requireSignedRequests":false,"singleLogout":{"enabled":false,"xmlSignatureC14nMethod":"exclusive_with_comments"},"xmlSignatureC14nMethod":"exclusive_with_comments"},"requireSignedRequests":false,"xmlSignatureC14nMethod":"exclusive_with_comments","xmlSignatureLocation":"Assertion"},"state":"Active","tenantId":"4544ae7e-7b90-67da-4ee5-91f1afd726d9","unverified":{"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyRegistration":false,"webAuthnConfiguration":{"bootstrapWorkflow":{"enabled":false},"enabled":false,"reauthenticationWorkflow":{"enabled":false}}},"reason":"FusionAuth User Interface"}
6	1671356177132	yuji.kosugi@gmail.com	Added role with Id [ff345e71-6387-466e-9601-7362e3540eb0] and name [admin] to application with Id [056867c7-e20a-4af9-8570-47100496229f] and name [Panomicon]	{"data":{},"reason":"FusionAuth User Interface"}
7	1671356262889	yuji.kosugi@gmail.com	Updated the application with Id [056867c7-e20a-4af9-8570-47100496229f] and name [Panomicon]	{"data":{},"newValue":{"accessControlConfiguration":{},"active":true,"authenticationTokenConfiguration":{"enabled":false},"data":{},"emailConfiguration":{},"externalIdentifierConfiguration":{},"formConfiguration":{"adminRegistrationFormId":"2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66"},"id":"056867c7-e20a-4af9-8570-47100496229f","insertInstant":1671355252302,"jwtConfiguration":{"accessTokenKeyId":"e71e1cb5-691f-4ccd-95ca-dd9529f53897","enabled":true,"idTokenKeyId":"720146f5-7f56-4c86-a0f0-b429b7e8929d","refreshTokenExpirationPolicy":"Fixed","refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"lambdaConfiguration":{},"lastUpdateInstant":1671356262885,"loginConfiguration":{"allowTokenRefresh":false,"generateRefreshTokens":false,"requireAuthentication":true},"multiFactorConfiguration":{"email":{},"sms":{}},"name":"Panomicon","oauthConfiguration":{"authorizedOriginURLs":[],"authorizedRedirectURLs":["http://localhost:4200/json/oauth-redirect"],"clientAuthenticationPolicy":"Required","clientId":"056867c7-e20a-4af9-8570-47100496229f","clientSecret":"Tjm7lugRNeFRQkP3P_FqLdzwrZxhOTF_30YvZCp66rg","debug":false,"enabledGrants":["authorization_code","refresh_token"],"generateRefreshTokens":true,"logoutBehavior":"AllApplications","logoutURL":"http://localhost:4200/viewer","proofKeyForCodeExchangePolicy":"NotRequired","requireClientAuthentication":true,"requireRegistration":false},"passwordlessConfiguration":{"enabled":false},"registrationConfiguration":{"birthDate":{"enabled":false,"required":false},"confirmPassword":false,"enabled":false,"firstName":{"enabled":false,"required":false},"fullName":{"enabled":false,"required":false},"lastName":{"enabled":false,"required":false},"loginIdType":"email","middleName":{"enabled":false,"required":false},"mobilePhone":{"enabled":false,"required":false},"type":"basic"},"registrationDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"roles":[{"id":"ff345e71-6387-466e-9601-7362e3540eb0","insertInstant":1671356177125,"isDefault":false,"isSuperRole":false,"lastUpdateInstant":1671356177125,"name":"admin"}],"samlv2Configuration":{"authorizedRedirectURLs":[],"debug":false,"enabled":false,"initiatedLogin":{"enabled":false,"nameIdFormat":"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"},"logout":{"behavior":"AllParticipants","requireSignedRequests":false,"singleLogout":{"enabled":false,"xmlSignatureC14nMethod":"exclusive_with_comments"},"xmlSignatureC14nMethod":"exclusive_with_comments"},"requireSignedRequests":false,"xmlSignatureC14nMethod":"exclusive_with_comments","xmlSignatureLocation":"Assertion"},"state":"Active","tenantId":"4544ae7e-7b90-67da-4ee5-91f1afd726d9","unverified":{"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyRegistration":false,"webAuthnConfiguration":{"bootstrapWorkflow":{"enabled":false},"enabled":false,"reauthenticationWorkflow":{"enabled":false}}},"oldValue":{"accessControlConfiguration":{},"active":true,"authenticationTokenConfiguration":{"enabled":false},"data":{},"emailConfiguration":{},"externalIdentifierConfiguration":{},"formConfiguration":{"adminRegistrationFormId":"2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66"},"id":"056867c7-e20a-4af9-8570-47100496229f","insertInstant":1671355252302,"jwtConfiguration":{"accessTokenKeyId":"e71e1cb5-691f-4ccd-95ca-dd9529f53897","enabled":true,"idTokenKeyId":"720146f5-7f56-4c86-a0f0-b429b7e8929d","refreshTokenExpirationPolicy":"Fixed","refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"lambdaConfiguration":{},"lastUpdateInstant":1671355521465,"loginConfiguration":{"allowTokenRefresh":false,"generateRefreshTokens":false,"requireAuthentication":true},"multiFactorConfiguration":{"email":{},"sms":{}},"name":"Panomicon","oauthConfiguration":{"authorizedOriginURLs":[],"authorizedRedirectURLs":[],"clientAuthenticationPolicy":"Required","clientId":"056867c7-e20a-4af9-8570-47100496229f","clientSecret":"Tjm7lugRNeFRQkP3P_FqLdzwrZxhOTF_30YvZCp66rg","debug":false,"enabledGrants":["authorization_code","refresh_token"],"generateRefreshTokens":true,"logoutBehavior":"AllApplications","proofKeyForCodeExchangePolicy":"NotRequired","requireClientAuthentication":true,"requireRegistration":false},"passwordlessConfiguration":{"enabled":false},"registrationConfiguration":{"birthDate":{"enabled":false,"required":false},"confirmPassword":false,"enabled":false,"firstName":{"enabled":false,"required":false},"fullName":{"enabled":false,"required":false},"lastName":{"enabled":false,"required":false},"loginIdType":"email","middleName":{"enabled":false,"required":false},"mobilePhone":{"enabled":false,"required":false},"type":"basic"},"registrationDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"roles":[{"id":"ff345e71-6387-466e-9601-7362e3540eb0","insertInstant":1671356177125,"isDefault":false,"isSuperRole":false,"lastUpdateInstant":1671356177125,"name":"admin"}],"samlv2Configuration":{"authorizedRedirectURLs":[],"debug":false,"enabled":false,"initiatedLogin":{"enabled":false,"nameIdFormat":"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"},"logout":{"behavior":"AllParticipants","requireSignedRequests":false,"singleLogout":{"enabled":false,"xmlSignatureC14nMethod":"exclusive_with_comments"},"xmlSignatureC14nMethod":"exclusive_with_comments"},"requireSignedRequests":false,"xmlSignatureC14nMethod":"exclusive_with_comments","xmlSignatureLocation":"Assertion"},"state":"Active","tenantId":"4544ae7e-7b90-67da-4ee5-91f1afd726d9","unverified":{"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyRegistration":false,"webAuthnConfiguration":{"bootstrapWorkflow":{"enabled":false},"enabled":false,"reauthenticationWorkflow":{"enabled":false}}},"reason":"FusionAuth User Interface"}
8	1671356313137	yuji.kosugi@gmail.com	Created user registration with Id [c78bf303-fd85-4bf5-bc33-13d756ffe30b] for user with Id [ddf28f6c-75ec-4949-90d0-759c6b09a80a] and application with Id [056867c7-e20a-4af9-8570-47100496229f]	{"data":{},"reason":"FusionAuth User Interface"}
\.


--
-- Data for Name: authentication_keys; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.authentication_keys (id, insert_instant, ip_access_control_lists_id, last_update_instant, key_manager, key_value, permissions, meta_data, tenants_id) FROM stdin;
f706cafc-2378-5f21-4758-4193172b0778	1671355209652	\N	1671355209652	f	__internal_YzcwZWI0MjRhZTM0MmRlZWVlNmU2YTBlOGIzNjUyMDg3OTdmNWZjOTI4MzI4YzM2NmFkNzY0ZDJkYTU3ZWZmYg==	{"endpoints": {"/api/cache/reload": ["POST"], "/api/system/log/export": ["POST"]}}	{"attributes": {"description": "Internal Use Only. [DistributedCacheNotifier][DistributedLogDownloader]", "internalCacheReloader": "true", "internalLogDownloader": "true"}}	\N
\.


--
-- Data for Name: breached_password_metrics; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.breached_password_metrics (tenants_id, matched_exact_count, matched_sub_address_count, matched_common_password_count, matched_password_count, passwords_checked_count) FROM stdin;
\.


--
-- Data for Name: clean_speak_applications; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.clean_speak_applications (applications_id, clean_speak_application_id) FROM stdin;
\.


--
-- Data for Name: common_breached_passwords; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.common_breached_passwords (password) FROM stdin;
\.


--
-- Data for Name: connectors; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.connectors (id, data, insert_instant, last_update_instant, name, reconcile_lambdas_id, ssl_certificate_keys_id, type) FROM stdin;
e3306678-a53a-4964-9040-1c96f36dda72	{}	1671355209652	1671355209652	Default	\N	\N	0
\.


--
-- Data for Name: connectors_tenants; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.connectors_tenants (connectors_id, data, sequence, tenants_id) FROM stdin;
e3306678-a53a-4964-9040-1c96f36dda72	{"data":{},"domains":["*"],"migrate":false}	0	4544ae7e-7b90-67da-4ee5-91f1afd726d9
\.


--
-- Data for Name: consents; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.consents (id, consent_email_templates_id, data, insert_instant, last_update_instant, name, email_plus_email_templates_id) FROM stdin;
03182100-0bea-4481-9e23-ecf52b22d0b7	\N	{"countryMinimumAgeForSelfConsent":{},"data":{},"defaultMinimumAgeForSelfConsent":13,"emailPlus":{"enabled":true,"maximumTimeToSendEmailInHours":48,"minimumTimeToSendEmailInHours":24},"multipleValuesAllowed":false,"values":[]}	1671355235773	1671355235773	COPPA Email+	\N
2960dc6f-8bb2-4c23-8377-a45544f5ec26	\N	{"countryMinimumAgeForSelfConsent":{},"data":{},"defaultMinimumAgeForSelfConsent":13,"emailPlus":{"enabled":false,"maximumTimeToSendEmailInHours":48,"minimumTimeToSendEmailInHours":24},"multipleValuesAllowed":false,"values":[]}	1671355235776	1671355235776	COPPA VPC	\N
\.


--
-- Data for Name: data_sets; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.data_sets (name, last_update_instant) FROM stdin;
BreachPasswords	1581476456155
\.


--
-- Data for Name: email_templates; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.email_templates (id, default_from_name, default_html_template, default_subject, default_text_template, from_email, insert_instant, last_update_instant, localized_from_names, localized_html_templates, localized_subjects, localized_text_templates, name) FROM stdin;
bef9069a-3045-43b0-b847-5d1e653351d7	\N	<p>\n The following two factor method was added to ${user.email}:\n\n  <br>\n  <strong>Method: ${method.method}</strong>\n  <br>\n  <strong>Identifier: ${method.id}</strong>\n\n</p>\n\n- FusionAuth Admin	A second factor was added	The following two factor method was added to ${user.email}:\n\n- Method: ${method.method}\n- Identifier: ${method.id}\n\n- FusionAuth Admin	\N	1671355235728	1671355235728	{}	{}	{}	{}	[FusionAuth Default] Two Factor Authentication Method Added
09f3392b-0f8d-40eb-af06-497d5913ffd4	\N	<p>\n The following two factor method was removed from ${user.email}:\n\n  <br>\n  <strong>Method: ${method.method}</strong>\n  <br>\n  <strong>Identifier: ${method.id}</strong>\n\n</p>\n\n- FusionAuth Admin	A second factor was removed	The following two factor method was removed from ${user.email}:\n\n- Method: ${method.method}\n- Identifier: ${method.id}\n\n- FusionAuth Admin	\N	1671355235730	1671355235730	{}	{}	{}	{}	[FusionAuth Default] Two Factor Authentication Method Removed
c5f66b4c-4e17-49e0-861b-a9ccfd4aa2f6	\N	[#-- @ftlvariable name="event" type="io.fusionauth.domain.event.UserLoginSuspiciousEvent" --]\n[#setting url_escaping_charset="UTF-8"]\n[#if event.type == "UserLoginSuspicious"]\n  <p>A suspicious login was made on your account. If this was you, you can safely ignore this email. If this wasn't you, we recommend that you change your password as soon as possible.</p>\n[#elseif event.type == "UserLoginNewDevice"]\n  <p>A login from a new device was detected on your account. If this was you, you can safely ignore this email. If this wasn't you, we recommend that you change your password as soon as possible.</p>\n[#else]\n  <p>Suspicious activity has been observed on your account. In order to secure your account, it is recommended to change your password at your earliest convenience.</p>\n[/#if]\n\n<p>Device details</p>\n<ul>\n  <li><strong>Device name:</strong> ${(event.info.deviceName)!'&mdash;'}</li>\n  <li><strong>Device description:</strong> ${(event.info.deviceDescription)!'&mdash;'}</li>\n  <li><strong>Device type:</strong> ${(event.info.deviceType)!'&mdash;'}</li>\n  <li><strong>User agent:</strong> ${(event.info.userAgent)!'&mdash;'}</li>\n</ul>\n\n<p>Event details</p>\n<ul>\n  <li><strong>IP address:</strong> ${(event.info.ipAddress)!'&mdash;'}</li>\n  <li><strong>City:</strong> ${(event.info.location.city)!'&mdash;'}</li>\n  <li><strong>Country:</strong> ${(event.info.location.country)!'&mdash;'}</li>\n  <li><strong>Zipcode:</strong> ${(event.info.location.zipcode)!'&mdash;'}</li>\n  <li><strong>Lat/long:</strong> ${(event.info.location.latitude)!'&mdash;'}/${(event.info.location.longitude)!'&mdash;'}</li>\n</ul>\n\n- FusionAuth Admin\n	Threat Detected	[#setting url_escaping_charset="UTF-8"]\n[#if event.type == "UserLoginSuspicious"]\nA suspicious login was made on your account. If this was you, you can safely ignore this email. If this wasn't you, we recommend that you change your password as soon as possible.\n[#elseif event.type == "UserLoginNewDevice"]\nA login from a new device was detected on your account. If this was you, you can safely ignore this email. If this wasn't you, we recommend that you change your password as soon as possible.\n[#else]\nSuspicious activity has been observed on your account. In order to secure your account, it is recommended to change your password at your earliest convenience.\n[/#if]\n\nDevice details\n\n* Device name: ${(event.info.deviceName)!'&mdash;'}\n* Device description: ${(event.info.deviceDescription)!'&mdash;'}\n* Device type: ${(event.info.deviceType)!'&mdash;'}\n* User agent: ${(event.info.userAgent)!'&mdash;'}\n\nEvent details\n\n* IP address: ${(event.info.ipAddress)!'-'}\n* City: ${(event.info.location.city)!'-'}\n* Country: ${(event.info.location.country)!'-'}\n* Zipcode: ${(event.info.location.zipcode)!'-'}\n* Lat/long: ${(event.info.location.latitude)!'-'}/${(event.info.location.longitude)!'-'}\n\n- FusionAuth Admin\n	\N	1671355235731	1671355235731	{}	{}	{}	{}	[FusionAuth Default] Threat Detected
421c7d2d-f85c-4719-adb9-5bbb32501b14	\N	[#setting url_escaping_charset="UTF-8"]\n<p>This password was found in the list of vulnerable passwords, and is no longer secure.</p>\n\n<p>In order to secure your account, it is recommended to change your password at your earliest convenience.</p>\n\n<p>Follow this link to change your password.</p>\n\n<a href="http://localhost:9011/password/forgot?client_id=${(application.oauthConfiguration.clientId)!''}&email=${user.email?url}&tenantId=${user.tenantId}">\n  http://localhost:9011/password/forgot?client_id=${(application.oauthConfiguration.clientId)!''}&email=${user.email?url}&tenantId=${user.tenantId}\n</a>\n\n- FusionAuth Admin	Your password is not secure	[#setting url_escaping_charset="UTF-8"]\nThis password was found in the list of vulnerable passwords, and is no longer secure.\n\nIn order to secure your account, it is recommended to change your password at your earliest convenience.\n\nFollow this link to change your password.\n\nhttp://localhost:9011/password/forgot?client_id=${(application.oauthConfiguration.clientId)!''}&email=${user.email?url}&tenantId=${user.tenantId}\n\n- FusionAuth Admin\n\n\n	\N	1671355235732	1671355235732	{}	{}	{}	{}	[FusionAuth Default] Breached Password Notification
4e0def6a-f6c6-45fa-a1a5-704edd8a163e	\N	[#setting url_escaping_charset="UTF-8"]\nTo change your password click on the following link.\n<p>\n  [#-- The optional 'state' map provided on the Forgot Password API call is exposed in the template as 'state'.\n       If we have an application context, append the client_id to ensure the correct application theme when applicable.\n  --]\n  [#assign url = "http://localhost:9011/password/change/${changePasswordId}?client_id=${(application.oauthConfiguration.clientId)!''}&tenantId=${user.tenantId}" /]\n  [#list state!{} as key, value][#if key != "tenantId" && key != "client_id" && value??][#assign url = url + "&" + key?url + "=" + value?url/][/#if][/#list]\n  <a href="${url}">${url}</a>\n</p>\n- FusionAuth Admin\n	Reset your password	[#setting url_escaping_charset="UTF-8"]\nTo change your password click on the following link.\n\n  [#-- The optional 'state' map provided on the Forgot Password API call is exposed in the template as 'state'.\n       If we have an application context, append the client_id to ensure the correct application theme when applicable.\n  --]\n[#assign url = "http://localhost:9011/password/change/${changePasswordId}?client_id=${(application.oauthConfiguration.clientId)!''}&tenantId=${user.tenantId}" /]\n[#list state!{} as key, value][#if key != "tenantId" && key != "client_id" && value??][#assign url = url + "&" + key?url + "=" + value?url/][/#if][/#list]\n\n${url}\n\n- FusionAuth Admin\n	\N	1671355235732	1671355235732	{}	{}	{}	{}	[FusionAuth Default] Forgot Password
adb608e5-b06a-4ec4-9660-2aa29b4a1c2e	\N	Your child has created an account with us and you need to confirm them before they are added to your family. Click the link below to confirm your child's account.\n<p>\n  <a href="http://example.com/family/confirm-child">http://example.com/family/confirm-child</a>\n</p>\n- FusionAuth Admin	Confirm your child's account	Your child has created an account with us and you need to confirm them before they are added to your family. Click the link below to confirm your child's account.\n\nhttp://example.com/family/confirm-child\n\n- FusionAuth Admin	\N	1671355235733	1671355235733	{}	{}	{}	{}	[FusionAuth Default] Confirm Child Account
9abaf7d7-da9e-4399-9ac5-e15b9cc63c68	\N	A while ago, you granted your child consent in our system. This email is a second notice of this consent as required by law and also to remind to that you can revoke this consent at anytime on our website or by clicking the link below:\n<p>\n  <a href="http://example.com/consent/manage">http://example.com/consent/manage</a>\n</p>\n- FusionAuth Admin	Reminder: Notice of your consent	A while ago, you granted your child consent in our system. This email is a second notice of this consent as required by law and also to remind to that you can revoke this consent at anytime on our website or by clicking the link below:\n\nhttp://example.com/consent/manage\n\n- FusionAuth Admin	\N	1671355235734	1671355235734	{}	{}	{}	{}	[FusionAuth Default] COPPA Notice Reminder
a78735dd-88ce-42af-a7fb-dc97433a87b9	\N	You recently granted your child consent in our system. This email is to notify you of this consent. If you did not grant this consent or wish to revoke this consent, click the link below:\n<p>\n  <a href="http://example.com/consent/manage">http://example.com/consent/manage</a>\n</p>\n- FusionAuth Admin	Notice of your consent	You recently granted your child consent in our system. This email is to notify you of this consent. If you did not grant this consent or wish to revoke this consent, click the link below:\n\nhttp://example.com/consent/manage\n\n- FusionAuth Admin	\N	1671355235735	1671355235735	{}	{}	{}	{}	[FusionAuth Default] COPPA Notice
c501c9a0-0157-45ef-b02c-33028f58eec0	\N	[#if user.verified]\nPro tip, your email has already been verified, but feel free to complete the verification process to verify your verification of your email address.\n[/#if]\n\n[#-- When a one-time code is provided, you will want the user to enter this value interactively using a form. In this workflow the verificationId\n     is not shown to the user and instead the one-time code must be paired with the verificationId which is usually in a hidden form field. When the two\n     values are presented together, the email address can be verified --]\n[#if verificationOneTimeCode??]\n<p>To complete your email verification enter this code into the email verification form.</p>\n<p> ${verificationOneTimeCode} </p>\n[#else]\nTo complete your email verification click on the following link.\n<p>\n  <a href="http://localhost:9011/email/verify/${verificationId}?client_id=${(application.oauthConfiguration.clientId)!''}&postMethod=true&tenantId=${user.tenantId}">\n    http://localhost:9011/email/verify/${verificationId}?client_id=${(application.oauthConfiguration.clientId)!''}&postMethod=true&tenantId=${user.tenantId}\n  </a>\n</p>\n[/#if]\n\n- FusionAuth Admin	Verify your FusionAuth email address	[#if user.verified]\nPro tip, your email has already been verified, but feel free to complete the verification process to verify your verification of your email address.\n[/#if]\n\n[#-- When a one-time code is provided, you will want the user to enter this value interactively using a form. In this workflow the verificationId\n     is not shown to the user and instead the one-time code must be paired with the verificationId which is usually in a hidden form field. When the two\n     values are presented together, the email address can be verified --]\n[#if verificationOneTimeCode??]\nTo complete your email verification enter this code into the email verification form.\n\n${verificationOneTimeCode}\n[#else]\nTo complete your email verification click on the following link.\n\nhttp://localhost:9011/email/verify/${verificationId}?client_id=${(application.oauthConfiguration.clientId)!''}&postMethod=true&tenantId=${user.tenantId}\n[/#if]\n\n- FusionAuth Admin	\N	1671355235735	1671355235735	{}	{}	{}	{}	[FusionAuth Default] Email Verification
4094510e-1711-4049-957b-cb63fd487527	\N	Your child has created an account with us and needs you to create an account and verify them. You can sign up using the link below:\n<p>\n  <a href="http://example.com/family/confirm-child">http://example.com/family/confirm-child</a>\n</p>\n- FusionAuth Admin	Create your parental account	Your child has created an account with us and needs you to create an account and verify them. You can sign up using the link below:\n\nhttp://example.com/family/confirm-child\n\n- FusionAuth Admin	\N	1671355235736	1671355235736	{}	{}	{}	{}	[FusionAuth Default] Parent Registration
9c4cb0b7-c8f8-4169-9dc7-83072b90f654	\N	[#setting url_escaping_charset="UTF-8"]\nYou have requested to log into FusionAuth using this email address. If you do not recognize this request please ignore this email.\n<p>\n  [#-- The optional 'state' map provided on the Start Passwordless API call is exposed in the template as 'state' --]\n  [#assign url = "http://localhost:9011/oauth2/passwordless/${code}?postMethod=true&tenantId=${user.tenantId}" /]\n  [#list state!{} as key, value][#if key != "tenantId" && value??][#assign url = url + "&" + key?url + "=" + value?url/][/#if][/#list]\n  <a href="${url}">${url}</a>\n</p>\n- FusionAuth Admin\n	Log into FusionAuth	[#setting url_escaping_charset="UTF-8"]\nYou have requested to log into FusionAuth using this email address. If you do not recognize this request please ignore this email.\n\n[#-- The optional 'state' map provided on the Start Passwordless API call is exposed in the template as 'state' --]\n[#assign url = "http://localhost:9011/oauth2/passwordless/${code}?postMethod=true&tenantId=${user.tenantId}" /]\n[#list state!{} as key, value][#if key != "tenantId" && value??][#assign url = url + "&" + key?url + "=" + value?url/][/#if][/#list]\n\n${url}\n\n- FusionAuth Admin\n	\N	1671355235737	1671355235737	{}	{}	{}	{}	[FusionAuth Default] Passwordless Login
0653fc4b-4a16-4429-80b3-62606b30663a	\N	[#if registration.verified]\nPro tip, your registration has already been verified, but feel free to complete the verification process to verify your verification of your registration.\n[/#if]\n\n[#-- When a one-time code is provided, you will want the user to enter this value interactively using a form. In this workflow the verificationId\n     is not shown to the user and instead the one-time code must be paired with the verificationId which is usually in a hidden form field. When the two\n     values are presented together, the registration can be verified --]\n[#if verificationOneTimeCode??]\n<p>To complete your registration verification enter this code into the registration verification form.</p>\n<p> ${verificationOneTimeCode} </p>\n[#else]\nTo complete your registration verification click on the following link.\n<p>\n  <a href="http://localhost:9011/registration/verify/${verificationId}?client_id=${(application.oauthConfiguration.clientId)!''}&postMethod=true&tenantId=${user.tenantId}">\n    http://localhost:9011/registration/verify/${verificationId}?client_id=${(application.oauthConfiguration.clientId)!''}&postMethod=true&tenantId=${user.tenantId}\n  </a>\n</p>\n[/#if]\n\n- FusionAuth Admin	Verify your registration	[#if registration.verified]\nPro tip, your registration has already been verified, but feel free to complete the verification process to verify your verification of your registration.\n[/#if]\n\n[#-- When a one-time code is provided, you will want the user to enter this value interactively using a form. In this workflow the verificationId\n     is not shown to the user and instead the one-time code must be paired with the verificationId which is usually in a hidden form field. When the two\n     values are presented together, the registration can be verified --]\n[#if verificationOneTimeCode??]\nTo complete your registration verification enter this code into the registration verification form.\n\n${verificationOneTimeCode}\n[#else]\nTo complete your registration verification click on the following link.\n\nhttp://localhost:9011/registration/verify/${verificationId}?client_id=${(application.oauthConfiguration.clientId)!''}&postMethod=true&tenantId=${user.tenantId}\n[/#if]\n\n- FusionAuth Admin	\N	1671355235737	1671355235737	{}	{}	{}	{}	[FusionAuth Default] Registration Verification
1e93cf8f-cc01-4aa8-9600-fdd95748337e	\N	Your account has been created and you must setup a password. Click on the following link to setup your password.\n<p>\n  <a href="http://localhost:9011/password/change/${changePasswordId}?client_id=${(application.oauthConfiguration.clientId)!''}&tenantId=${user.tenantId}">\n    http://localhost:9011/password/change/${changePasswordId}?client_id=${(application.oauthConfiguration.clientId)!''}&tenantId=${user.tenantId}\n  </a>\n</p>\n- FusionAuth Admin	Setup your password	Your account has been created and you must setup a password. Click on the following link to setup your password.\n\nhttp://localhost:9011/password/change/${changePasswordId}?client_id=${(application.oauthConfiguration.clientId)!''}&tenantId=${user.tenantId}\n\n- FusionAuth Admin	\N	1671355235738	1671355235738	{}	{}	{}	{}	[FusionAuth Default] Setup Password
6b34ee69-542b-4926-a469-ada36ddf39cf	\N	<p>\n  To complete your login request, enter this one-time code code on the login form when prompted.\n</p>\n<p>\n  <strong>${code}</strong>\n</p>\n\n- FusionAuth Admin	Your second factor code	To complete your login request, enter this one-time code code on the login form when prompted.\n\n${code}\n\n- FusionAuth Admin	\N	1671355235739	1671355235739	{}	{}	{}	{}	[FusionAuth Default] Two Factor Authentication
\.


--
-- Data for Name: entities; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.entities (id, client_id, client_secret, data, entity_types_id, insert_instant, last_update_instant, name, parent_id, tenants_id) FROM stdin;
\.


--
-- Data for Name: entity_entity_grant_permissions; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.entity_entity_grant_permissions (entity_entity_grants_id, entity_type_permissions_id) FROM stdin;
\.


--
-- Data for Name: entity_entity_grants; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.entity_entity_grants (id, data, insert_instant, last_update_instant, recipient_id, target_id) FROM stdin;
\.


--
-- Data for Name: entity_type_permissions; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.entity_type_permissions (id, data, description, entity_types_id, insert_instant, is_default, last_update_instant, name) FROM stdin;
4bf67ae0-fa1c-4c50-9e23-d2876de54fd5	{"data":{}}	Create SCIM User	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235718	f	1671355235718	scim:user:create
4ed231f6-e0eb-483f-9fe4-7451fe7b98ea	{"data":{}}	Read SCIM User	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235719	f	1671355235719	scim:user:read
ffa530cb-28ed-4241-8b0c-051c9dcc6b23	{"data":{}}	Update SCIM User	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235720	f	1671355235720	scim:user:update
215b9396-6a9c-42b9-8bf7-cb2e315c3c18	{"data":{}}	Delete SCIM User	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235720	f	1671355235720	scim:user:delete
5a9f37c9-4911-4d22-a7c5-991e2b2171dc	{"data":{}}	Create SCIM Enterprise User	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235721	f	1671355235721	scim:enterprise:user:create
7c0f38e0-f431-4c95-bb19-f7f55248e9b3	{"data":{}}	Read SCIM Enterprise User	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235721	f	1671355235721	scim:enterprise:user:read
737c8d9d-6497-496f-9b5a-e83a35e9d9cc	{"data":{}}	Update SCIM Enterprise User	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235722	f	1671355235722	scim:enterprise:user:update
4c6db85c-5f10-4c6f-84f7-4043f2b430da	{"data":{}}	Delete SCIM Enterprise User	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235722	f	1671355235722	scim:enterprise:user:delete
130404e8-8ae1-4a71-832a-dee8b3208609	{"data":{}}	Create SCIM Group	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235723	f	1671355235723	scim:group:create
de118ae7-e840-4de1-9f1e-810e64641fef	{"data":{}}	Read SCIM Group	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235723	f	1671355235723	scim:group:read
dcc3aedd-3b13-4e63-9fcb-b28b83e6b898	{"data":{}}	Update SCIM Group	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235724	f	1671355235724	scim:group:update
70be861e-08d1-448e-b31b-a049ca148937	{"data":{}}	Delete SCIM Group	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235724	f	1671355235724	scim:group:delete
895b6cce-65bb-4d2a-88d2-1f0c93ce00fa	{"data":{}}	Read SCIM Resource Types	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235725	f	1671355235725	scim:resource-types:read
2bea1b20-7657-4cfa-a361-a784152423c1	{"data":{}}	Read SCIM Schemas	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235725	f	1671355235725	scim:schemas:read
d80b2110-bce4-4b18-a8b6-6259889f5246	{"data":{}}	Read SCIM Service Provider Configuration	8466232e-22d0-424f-a27b-da24cc6878dd	1671355235725	f	1671355235725	scim:service-provider-config:read
\.


--
-- Data for Name: entity_types; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.entity_types (id, access_token_signing_keys_id, data, insert_instant, last_update_instant, name) FROM stdin;
8466232e-22d0-424f-a27b-da24cc6878dd	\N	{"data":{},"jwtConfiguration":{"enabled":false,"timeToLiveInSeconds":0}}	1671355235716	1671355235716	[FusionAuth Default] SCIM Server
0bcf007a-3417-4787-bbf1-ed65519368eb	\N	{"data":{},"jwtConfiguration":{"enabled":false,"timeToLiveInSeconds":0}}	1671355235726	1671355235726	[FusionAuth Default] SCIM Client
\.


--
-- Data for Name: entity_user_grant_permissions; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.entity_user_grant_permissions (entity_user_grants_id, entity_type_permissions_id) FROM stdin;
\.


--
-- Data for Name: entity_user_grants; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.entity_user_grants (id, data, entities_id, insert_instant, last_update_instant, users_id) FROM stdin;
\.


--
-- Data for Name: event_logs; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.event_logs (id, insert_instant, message, type) FROM stdin;
\.


--
-- Data for Name: external_identifiers; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.external_identifiers (id, applications_id, data, expiration_instant, insert_instant, tenants_id, type, users_id) FROM stdin;
\.


--
-- Data for Name: families; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.families (data, family_id, insert_instant, last_update_instant, owner, role, users_id) FROM stdin;
\.


--
-- Data for Name: federated_domains; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.federated_domains (identity_providers_id, domain) FROM stdin;
\.


--
-- Data for Name: form_fields; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.form_fields (id, consents_id, data, insert_instant, last_update_instant, name) FROM stdin;
0b9805e0-30f5-3b14-f862-93d99fce8d2c	\N	{"key": "user.email", "control": "text", "required": true, "type": "email", "data": {"leftAddon": "user"}}	1671355209652	1671355209652	Email
33d88309-30f1-99ca-1650-910a726e5f49	\N	{"key": "user.password", "control": "password", "required": true, "type": "string", "data": {"leftAddon": "lock"}}	1671355209652	1671355209652	Password
44581c9e-0c76-101f-c62b-b6238af9a051	\N	{"key": "user.firstName", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	First name
178c5526-e10d-1bc2-f597-bb4734d41457	\N	{"key": "user.middleName", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	Middle name
10ccebbf-6389-cc45-0b2b-e3b946723cde	\N	{"key": "user.lastName", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	Last name
a2b50048-8518-7681-d577-a46022d8a10b	\N	{"key": "user.fullName", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	Full name
3c88c657-fe46-4a22-e405-c1610e415302	\N	{"key": "user.birthDate", "control": "text", "required": false, "type": "date", "data": {"leftAddon": "calendar"}}	1671355209652	1671355209652	Birthdate
50b221eb-d823-eb13-a088-ac97e29ad499	\N	{"key": "user.mobilePhone", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "mobile"}}	1671355209652	1671355209652	Mobile phone
78d97457-09dc-3350-3acf-22ca19519159	\N	{"key": "user.username", "control": "text", "required": true, "type": "string", "data": {"leftAddon": "user"}}	1671355209652	1671355209652	Username
0fc75efc-0780-6ee1-8f2d-f14dcc28d2fa	\N	{"key": "user.parentEmail", "control": "text", "required": false, "type": "email", "data": {"leftAddon": "user"}}	1671355209652	1671355209652	[Registration] parent email
e88c8b3a-b940-af6c-2ecb-3c1048d9b170	\N	{"key": "registration.preferredLanguages", "control": "select", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Admin Registration] preferred languages
2bb8cf5d-a211-63a1-1229-2acd87b405a9	\N	{"key": "registration.roles", "control": "checkbox", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Admin Registration] roles
1cf9b0a8-d542-315c-1017-5705c91bfe4d	\N	{"key": "registration.timezone", "control": "select", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Admin Registration] timezone
568301dd-897e-9fd2-ab49-799ebb8c79cd	\N	{"key": "registration.username", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "user"}}	1671355209652	1671355209652	[Admin Registration] username
29acc45e-e68a-267e-e764-18ba4b900f52	\N	{"key": "user.birthDate", "control": "text", "required": false, "type": "date", "data": {"leftAddon": "calendar"}}	1671355209652	1671355209652	[Admin User] birthdate
fc9772e0-a40a-120a-aeb8-36bfba26e9c7	\N	{"key": "user.email", "control": "text", "required": false, "type": "email", "data": {"leftAddon": "user"}}	1671355209652	1671355209652	[Admin User] email
1d7ed596-085d-3257-d37c-16b99b505797	\N	{"key": "user.firstName", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Admin User] first name
d02c9610-27ef-5b2d-e3fb-a051a2534946	\N	{"key": "user.fullName", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Admin User] full name
d2eb348e-517b-944f-32b2-853e002bfda4	\N	{"key": "user.imageUrl", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Admin User] image URL
b81837a4-ef68-6d8a-a798-1bf594b51c18	\N	{"key": "user.lastName", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Admin User] last name
6e89c7b5-8dce-0b89-ca06-54a3ede265db	\N	{"key": "user.middleName", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Admin User] middle name
d26b8840-b65d-d463-5f37-96f9f46113d4	\N	{"key": "user.mobilePhone", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "mobile"}}	1671355209652	1671355209652	[Admin User] mobile phone
e7109ab9-c6c5-ea32-f9b4-9f88f90fbeed	\N	{"key": "user.password", "control": "password", "required": true, "confirm": true, "type": "string", "data": {"leftAddon": "lock"}}	1671355209652	1671355209652	[Admin User] password
527bb80f-4876-ac17-981d-220b3f027554	\N	{"key": "user.preferredLanguages", "control": "select", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Admin User] preferred languages
f23f879f-bcc1-a17d-ba31-8044b09b0840	\N	{"key": "user.timezone", "control": "select", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Admin User] timezone
7011bb8f-f1d8-936a-0af4-61cb231de635	\N	{"key": "user.username", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "user"}}	1671355209652	1671355209652	[Admin User] username
76db6281-dadb-5f07-8916-846806e91c9d	\N	{"key": "user.email", "control": "text", "required": false, "type": "email", "data": {"leftAddon": "user"}}	1671355209652	1671355209652	[Self Service User] email
5b693301-961b-8b87-767b-9385860f3ac3	\N	{"key": "user.firstName", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Self Service User] first name
f9904acb-38fb-a188-ddf7-115f321f8bce	\N	{"key": "user.lastName", "control": "text", "required": false, "type": "string", "data": {"leftAddon": "info"}}	1671355209652	1671355209652	[Self Service User] last name
5cc644f0-601c-a552-3ef1-f29793bc18a7	\N	{"key": "user.password", "control": "password", "required": true, "confirm": true, "type": "string", "data": {"leftAddon": "lock"}}	1671355209652	1671355209652	[Self Service User] password
\.


--
-- Data for Name: form_steps; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.form_steps (form_fields_id, forms_id, sequence, step) FROM stdin;
568301dd-897e-9fd2-ab49-799ebb8c79cd	2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66	0	0
e88c8b3a-b940-af6c-2ecb-3c1048d9b170	2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66	1	0
1cf9b0a8-d542-315c-1017-5705c91bfe4d	2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66	2	0
2bb8cf5d-a211-63a1-1229-2acd87b405a9	2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66	3	0
fc9772e0-a40a-120a-aeb8-36bfba26e9c7	4efdc20f-b73f-8831-c724-d4b140a6c13d	0	0
7011bb8f-f1d8-936a-0af4-61cb231de635	4efdc20f-b73f-8831-c724-d4b140a6c13d	1	0
d26b8840-b65d-d463-5f37-96f9f46113d4	4efdc20f-b73f-8831-c724-d4b140a6c13d	2	0
e7109ab9-c6c5-ea32-f9b4-9f88f90fbeed	4efdc20f-b73f-8831-c724-d4b140a6c13d	3	0
29acc45e-e68a-267e-e764-18ba4b900f52	4efdc20f-b73f-8831-c724-d4b140a6c13d	0	1
1d7ed596-085d-3257-d37c-16b99b505797	4efdc20f-b73f-8831-c724-d4b140a6c13d	1	1
6e89c7b5-8dce-0b89-ca06-54a3ede265db	4efdc20f-b73f-8831-c724-d4b140a6c13d	2	1
b81837a4-ef68-6d8a-a798-1bf594b51c18	4efdc20f-b73f-8831-c724-d4b140a6c13d	3	1
d02c9610-27ef-5b2d-e3fb-a051a2534946	4efdc20f-b73f-8831-c724-d4b140a6c13d	4	1
527bb80f-4876-ac17-981d-220b3f027554	4efdc20f-b73f-8831-c724-d4b140a6c13d	5	1
f23f879f-bcc1-a17d-ba31-8044b09b0840	4efdc20f-b73f-8831-c724-d4b140a6c13d	6	1
d2eb348e-517b-944f-32b2-853e002bfda4	4efdc20f-b73f-8831-c724-d4b140a6c13d	7	1
76db6281-dadb-5f07-8916-846806e91c9d	1e7c3a25-72b8-43a6-64cd-0bb1eacb45b4	0	0
5b693301-961b-8b87-767b-9385860f3ac3	1e7c3a25-72b8-43a6-64cd-0bb1eacb45b4	1	0
f9904acb-38fb-a188-ddf7-115f321f8bce	1e7c3a25-72b8-43a6-64cd-0bb1eacb45b4	2	0
5cc644f0-601c-a552-3ef1-f29793bc18a7	1e7c3a25-72b8-43a6-64cd-0bb1eacb45b4	3	0
\.


--
-- Data for Name: forms; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.forms (id, data, insert_instant, last_update_instant, name, type) FROM stdin;
2f9d03d9-44e5-5e48-a4a1-a4d81ed2ad66	\N	1671355206652	1671355206652	Default Admin Registration provided by FusionAuth	1
4efdc20f-b73f-8831-c724-d4b140a6c13d	\N	1671355207652	1671355207652	Default Admin User provided by FusionAuth	2
1e7c3a25-72b8-43a6-64cd-0bb1eacb45b4	\N	1671355208652	1671355208652	Default User Self Service provided by FusionAuth	3
\.


--
-- Data for Name: global_daily_active_users; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.global_daily_active_users (count, day) FROM stdin;
1	19344
\.


--
-- Data for Name: global_monthly_active_users; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.global_monthly_active_users (count, month) FROM stdin;
1	635
\.


--
-- Data for Name: global_registration_counts; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.global_registration_counts (count, decremented_count, hour) FROM stdin;
1	0	464265
\.


--
-- Data for Name: group_application_roles; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.group_application_roles (application_roles_id, groups_id) FROM stdin;
\.


--
-- Data for Name: group_members; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.group_members (id, groups_id, data, insert_instant, users_id) FROM stdin;
\.


--
-- Data for Name: groups; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.groups (id, data, insert_instant, last_update_instant, name, tenants_id) FROM stdin;
\.


--
-- Data for Name: hourly_logins; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.hourly_logins (applications_id, count, data, hour) FROM stdin;
056867c7-e20a-4af9-8570-47100496229f	12	\N	464267
3c219e58-ed0e-4b18-ad48-f4f92793ae32	1	\N	464267
056867c7-e20a-4af9-8570-47100496229f	121	\N	464265
\.


--
-- Data for Name: identities; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.identities (id, breached_password_last_checked_instant, breached_password_status, connectors_id, email, encryption_scheme, factor, insert_instant, last_login_instant, last_update_instant, password, password_change_reason, password_change_required, password_last_update_instant, salt, status, tenants_id, username, username_index, username_status, users_id, verified) FROM stdin;
1	\N	\N	e3306678-a53a-4964-9040-1c96f36dda72	yuji.kosugi@gmail.com	salted-pbkdf2-hmac-sha256	24000	1671355235602	1671364072248	1671355235602	iPvYJ+9mO6fjYYMz+pihW3NMbrIkTNfJJ3Sn1Bj7hZ0=	\N	f	1671355235681	nG4J0T7Juu/Pto4zqOBDrLka2BSfarw3ZsqoT1CfTrQ=	0	4544ae7e-7b90-67da-4ee5-91f1afd726d9	\N	\N	0	ddf28f6c-75ec-4949-90d0-759c6b09a80a	t
\.


--
-- Data for Name: identity_provider_links; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.identity_provider_links (data, identity_providers_id, identity_providers_user_id, insert_instant, last_login_instant, tenants_id, users_id) FROM stdin;
\.


--
-- Data for Name: identity_providers; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.identity_providers (id, data, enabled, insert_instant, last_update_instant, name, type, keys_id, request_signing_keys_id, reconcile_lambdas_id) FROM stdin;
\.


--
-- Data for Name: identity_providers_applications; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.identity_providers_applications (applications_id, data, enabled, identity_providers_id, keys_id) FROM stdin;
\.


--
-- Data for Name: identity_providers_tenants; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.identity_providers_tenants (tenants_id, data, identity_providers_id) FROM stdin;
\.


--
-- Data for Name: instance; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.instance (id, activate_instant, license, license_id, setup_complete) FROM stdin;
9d782aae-7223-45bf-9315-e4bcd550904d	\N	\N	\N	t
\.


--
-- Data for Name: integrations; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.integrations (data) FROM stdin;
{}
\.


--
-- Data for Name: ip_access_control_lists; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.ip_access_control_lists (id, data, insert_instant, last_update_instant, name) FROM stdin;
\.


--
-- Data for Name: ip_location_database; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.ip_location_database (data, last_modified) FROM stdin;
\N	0
\.


--
-- Data for Name: keys; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.keys (id, algorithm, certificate, expiration_instant, insert_instant, issuer, kid, last_update_instant, name, private_key, public_key, secret, type) FROM stdin;
a06e2c22-5bf5-04c0-e2a8-137ed19b3cf6	HS256	\N	\N	1671355205652	\N	91deea160	1671355205652	Default signing key	\N	\N	/38RYC8sUAAnNfu7GPsrqIeUFKhU5hDMf4sHtCWSIcg=	HMAC
092dbedc-30af-4149-9c61-b578f2c72f59	HS256	\N	\N	1671355206652	\N	4927e2032	1671355206652	OpenID Connect compliant HMAC using SHA-256	\N	\N	\N	HMAC
4b8f1c06-518e-45bd-9ac5-d549686ae02a	HS384	\N	\N	1671355207652	\N	e13fc73b2	1671355207652	OpenID Connect compliant HMAC using SHA-384	\N	\N	\N	HMAC
c753a44d-7f2e-48d3-bc4e-c2c16488a23b	HS512	\N	\N	1671355208652	\N	83d5cf2fe	1671355208652	OpenID Connect compliant HMAC using SHA-512	\N	\N	\N	HMAC
e71e1cb5-691f-4ccd-95ca-dd9529f53897	RS256	-----BEGIN CERTIFICATE-----\nMIICzTCCAbWgAwIBAQIRAOceHLVpH0zNlcrdlSn1OJcwDQYJKoZIhvcNAQELBQAw\nIjEgMB4GA1UEAxMXdG94eWdhdGVzLm5pYmlvaG4uZ28uanAwHhcNMjIxMjE4MDky\nNTIxWhcNMzIxMjE4MDkyNTIxWjAiMSAwHgYDVQQDExd0b3h5Z2F0ZXMubmliaW9o\nbi5nby5qcDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALNN6b3hfzWW\ndOUfcRqjXs6SgHt6wvd2lUGJdT0swK7/IDn4qpUQXmkA4nU/vXoN2SnKp5Kzyljg\nH5gbgqDGYaRJ55k+PljW2LA9V6cBnN7prtt9yGdorQeJC2wIr458GEWOf/E+Ydim\nM73wYowERWWRvNdkXDB5b17K5jn6wt7f0tr/XFj+BKXBzVA/JroZWFs8wrh6Y5TZ\nbxe8/sVQSumYAVWz9ohSsTboT661YwlL4M9U7IMY5JdRMK++IHBw4okEUhtdOXGL\nSy64/oAt7VHqjjZNEtproGpkgjvg7BgtuOgcQcybAeZxrXjKevoXudIiOowU8VrE\n1KHwqAbokd0CAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAejqny8NfccpBpNthBqa1\nsX2p77GpqoWg30dpoUM9ttmwggyIKIfii0+5JxJwqlrbLEb/CZMLFBgVBIbd564O\nI8bbnRx1kiJPEyGLWcXEJMy7aSl/cd94w8eAytoB81ceueAEPpOqhNFvynMmWIqd\nWAN30XiqkL93Txe2gewMmb13oZxpSvnQ9LwJfR7rxW70hOPj92K1ew+BXVHKo14f\ndm5Myj4g6NSHWNZJ+PAkYeVALS556W3ofZyZmIolpTbGu8VVjK0lYk0Yn4782t6V\nBnGc37jleAwG2QA+8qqYRv2SMFOQhgrlsZYOJA82awizC/Nret9ItL8ogH2zi7C2\nVQ==\n-----END CERTIFICATE-----	1986974721161	1671355521161	toxygates.nibiohn.go.jp	a_pfn5MGQvY4RaOYrJFQ3aYUCG0	1671355521161	Access token signing key generated for application Panomicon	-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCzTem94X81lnTl\nH3Eao17OkoB7esL3dpVBiXU9LMCu/yA5+KqVEF5pAOJ1P716DdkpyqeSs8pY4B+Y\nG4KgxmGkSeeZPj5Y1tiwPVenAZze6a7bfchnaK0HiQtsCK+OfBhFjn/xPmHYpjO9\n8GKMBEVlkbzXZFwweW9eyuY5+sLe39La/1xY/gSlwc1QPya6GVhbPMK4emOU2W8X\nvP7FUErpmAFVs/aIUrE26E+utWMJS+DPVOyDGOSXUTCvviBwcOKJBFIbXTlxi0su\nuP6ALe1R6o42TRLaa6BqZII74OwYLbjoHEHMmwHmca14ynr6F7nSIjqMFPFaxNSh\n8KgG6JHdAgMBAAECggEAJt+W9AhN/I+8kyowas6NkOqZOWwhleYbMOS8ysEHm0kL\nsxXS4dN1LH25GyNxEGOtN87MatVY4tDgeKlKNlIcPDKbqBEjEj6uJvN9q+MBICD4\nvHR2PNkoXlBbbhYp/ZGw72+YFHTqYL6TzDOwIdwRMZaZovcKt6W0PBBwPpS7p/bw\nyNqLv5CT3bMUOPbCva+waWWT9oyKhFWXrwOlp2I9jSJeKACe/Zt0mqnJ1LO9N/ok\n+T8+zfFcGWqAo8+VyV6dmmHVNfYccgzYaYcF9dumkjagUByqgJncZQOvt4FIx0ec\neNNlBH/+1fxKRKV1WLlzEwEZFJaVHj26xTHnhtXx4QKBgQDVKZvYG6ZBXSRxJ6k/\nipv2JF6rtZBAY3YDLB8KUJl2ot+R+T0Dvlk5mouRQJZuXRU3Hm3CSPYXzdX3R3VW\nw7XLQIwZep0qKVyOo5KBlfl0mTNMdjMS34YyiCa3nDZ6uwMcyijCRjkdPGiYKgEM\nn/Ic/y0G3yVoSYICIhLYI+4LUQKBgQDXVm5X2g+ok69jRk43m2f6K+CK83JUZpCQ\nVOL5+km3k6pG8QfvNVmc+gleV/egDyZCSD68zduFvVFZ0KMP6EViRX92Z3w1OUJJ\nOb+8KsJeZo2SuaklXs4Yx+hSZjVVb5K8nMvDfQf4ro46JTwTZ/X/C/GWfQ+A6KJz\nBtcte4zizQKBgQCw5LCxrHBRuevuA03Or0YyA4iU0j19UYyzoT+5HZ3c99i0mLLr\nkmEVDo1X4tNzjsd2UEghfM7MfkJbzO8xK4SHwW8CDeCzBlQLAu8nnr++5QvPHh/Q\nP7Jr6NTIdlg3jU5N6+3bGs921ZSvAdyRD4KqUZCaLUArxSSghIpC175JoQKBgQCF\nbxdc9NCzIyCRIaWiK3hYuwuezo/P4OY/qXokcIVxbd0brIvAHIz1CrL3dQcKdZi9\nhcrqb51R2CgqwW/pkYI9DNeY5TNAP8TlXxWCYfSjTmT3wHghogePr4A0XwU5dbO1\nkGtrLXTdEj0rppr9Y3Q5oIsJN2F9/al8IYcAtghtGQKBgFb41YAnR0ptNMLhOWAz\ntA9zjuPfE/MM6xlPUGnEx5IW5mJAdDak/hjtFsF/gzYOmu/1T5G7npp+MeUFkabS\nH45JKHLNCsvKF+9Avmp9NTVE7q6MsDEoRgTc7aHP+idUjTc5mpHSED9LUzZQF4V+\nULCBdifT95bOblVJXr/40KCS\n-----END PRIVATE KEY-----	-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs03pveF/NZZ05R9xGqNe\nzpKAe3rC93aVQYl1PSzArv8gOfiqlRBeaQDidT+9eg3ZKcqnkrPKWOAfmBuCoMZh\npEnnmT4+WNbYsD1XpwGc3umu233IZ2itB4kLbAivjnwYRY5/8T5h2KYzvfBijARF\nZZG812RcMHlvXsrmOfrC3t/S2v9cWP4EpcHNUD8muhlYWzzCuHpjlNlvF7z+xVBK\n6ZgBVbP2iFKxNuhPrrVjCUvgz1Tsgxjkl1Ewr74gcHDiiQRSG105cYtLLrj+gC3t\nUeqONk0S2mugamSCO+DsGC246BxBzJsB5nGteMp6+he50iI6jBTxWsTUofCoBuiR\n3QIDAQAB\n-----END PUBLIC KEY-----	\N	RSA
720146f5-7f56-4c86-a0f0-b429b7e8929d	RS256	-----BEGIN CERTIFICATE-----\nMIICzDCCAbSgAwIBAQIQcgFG9X9WTIag8LQpt+iSnTANBgkqhkiG9w0BAQsFADAi\nMSAwHgYDVQQDExd0b3h5Z2F0ZXMubmliaW9obi5nby5qcDAeFw0yMjEyMTgwOTI1\nMjFaFw0zMjEyMTgwOTI1MjFaMCIxIDAeBgNVBAMTF3RveHlnYXRlcy5uaWJpb2hu\nLmdvLmpwMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkvXKHI1mhYLg\n/of7YApJMA0/vuDdG+tz5tbRYbwvNiVysHvm2a0AKm/v4IgQnQuGHbXBxH2EvZmS\nE9THtFML9yRcnlEW2Eb1XrrHUurIeS0P3pWgpkIsK2ezpuCXCCmTCvMvc04x2+ha\njtx+ELOc1HLcFDkY7PINsGdsuNVK7wP0cT0zIjNaWDQRFMg3hpMIA8BQLbevIN3Q\nIgqdwNrEc8ltNf9DiFLNG3rGyeQ3ogzyEl0sKz7I9TIpVBDXa2nkKQV+EN191bLh\nfYXkSjENd7Gq0zD83eaF9dDihEBSDULCF93s1nuORYikz3Lyexwk8JmU/tycjIDw\ncye+JypZywIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBjsQmAcPO7ni6eRp7Td4CL\nYDDjtdpWbwTeqTIfbXBing7yvu5lY6XpWrv1TpsEutrl7Kl0i5rJO1xWDShmcYq8\nxJiddj/fPZyM+e+BUdM6nh+MFmvebMUnNq/toasTv+E0hxFfRZCh2HDNtCACDXiz\nH2kwxI1nwhEZwbBlb0Gx6mP0GCzNZXGV3MpxP1m00/Q/ImBH35TvR2WpP+4vLzPi\ny4s0hBNYA14O4/uIN1+AaEcJGdbheQLtVtvb7+qYiS/tjQ1cmdOK+NHSXhg+np3h\nD/gimj1Z6RBwQwWo4b3i0nfUSqcX7gVf2VsLR0fZx0JCSFgCvvqG9vIVBJFzaMFf\n-----END CERTIFICATE-----	1986974721332	1671355521332	toxygates.nibiohn.go.jp	U7SysRJOv014R9RlQHtxWOT4d90	1671355521332	Id token signing key generated for application Panomicon	-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCS9cocjWaFguD+\nh/tgCkkwDT++4N0b63Pm1tFhvC82JXKwe+bZrQAqb+/giBCdC4YdtcHEfYS9mZIT\n1Me0Uwv3JFyeURbYRvVeusdS6sh5LQ/elaCmQiwrZ7Om4JcIKZMK8y9zTjHb6FqO\n3H4Qs5zUctwUORjs8g2wZ2y41UrvA/RxPTMiM1pYNBEUyDeGkwgDwFAtt68g3dAi\nCp3A2sRzyW01/0OIUs0besbJ5DeiDPISXSwrPsj1MilUENdraeQpBX4Q3X3VsuF9\nheRKMQ13sarTMPzd5oX10OKEQFINQsIX3ezWe45FiKTPcvJ7HCTwmZT+3JyMgPBz\nJ74nKlnLAgMBAAECggEACIUecBRUjsX9NFR+4t0FzDbgbLkNqJo4cHgg0HlABXQb\nV5b/VbxdZ5hwwEQzLl+mKV/JuZILWZCnMuYho9cyuOPPSNBrsiM/A3xxpOF+uOO/\naUikdS8d73BGLpUTvIHoh/h8VkOAyX6JjYHD/0TigUCdeMCsrMQFviXo3DhlbUDn\nb6RSiHZmovLDCZHF0YZIzBVTBdOsqfD0UxLGfdbF8rCrD7cS8YPJTzPsjjrpBoX8\ne/vzMXFTx3KL0fSwM/VKWr2tQbfFAm7Q1/arEddHa9Pio0517bndj5yJChz4jUPK\nZ2BPAMeTLHMBdVG01d4W4hh17MR3o3L3PP7nj/YE7QKBgQC6iY1rgo//Il7U4rSd\nCjozS3Wmx6GRx1gumlOjwav3zrd6Ph/pe0sWs29h9qRExkfwhM7ySzoi6aR2mcoY\nI0rKTeN90VcfJZLrXQyJ+ANh6hx7DnGbOEq2dETxskCsbE91MOoaQZviYeT+hgJ1\nsKR9khbWxcLVs5O8+Emo9AZOnQKBgQDJr2AgxzoJ9qPQmv7tGpv8An/h86qdZHZ8\no6LK7RL8p1HFqjYFnwVu/Lr+zjuEqtpUmPJe1rf99DgTyNRL0iTxFzKTcOCBX6eK\nIsw5gfWG9cLWR1FwhlNk806EIXgsgI1ywceQV/467fg220zVn3O6oGqNoO+0un+q\nrEd0/FLphwKBgH9E3jId8HUYVC9XRfdnRPI+Z9UUm03E/os9TBeMSfJXHaPPwyIf\ntsmUP0hNaSJrTiXx1uhFHzM2ybkJkHMLkTmaDr6HF7PuhOEEqUtw5Y3mluv2nNhU\nfdUiWu+0uJ9rfoLsjqy/WsYIGJ73e7IkhPYPuHlskFQRo5jopgkaBTuZAoGBAMXk\nx+zBr0gxcqFwRC0ATAelqDdb8JAJEF3R7h0xK80unATYs7MmSp2RbCYy06GkRyB1\nBnRaOST86b039F8FLmxFd8HGIvOumOToqfzPcMvg/zFjvxwrIU7Pkb4CXTlFVgVA\n8IDIZlMKRuAEpEqD73kyh/q57BooWLZt3puCLhSnAoGAJ2Gv4uISXwl9Tmzsf35E\nSGpovoIUs+gvx3T6C38B1nw8f3zuCrr11i6tb9w6LwdeNvm4At08vRDHMSOKlG2Z\nxM8bfnWorSCg84Xm5uqKbk6/LaOs7lA9bvtAtZHiKxJAhMl5+3dS08JXhzMmO8P6\nR8Qa+/nNldFBWWkewVEs2Qw=\n-----END PRIVATE KEY-----	-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkvXKHI1mhYLg/of7YApJ\nMA0/vuDdG+tz5tbRYbwvNiVysHvm2a0AKm/v4IgQnQuGHbXBxH2EvZmSE9THtFML\n9yRcnlEW2Eb1XrrHUurIeS0P3pWgpkIsK2ezpuCXCCmTCvMvc04x2+hajtx+ELOc\n1HLcFDkY7PINsGdsuNVK7wP0cT0zIjNaWDQRFMg3hpMIA8BQLbevIN3QIgqdwNrE\nc8ltNf9DiFLNG3rGyeQ3ogzyEl0sKz7I9TIpVBDXa2nkKQV+EN191bLhfYXkSjEN\nd7Gq0zD83eaF9dDihEBSDULCF93s1nuORYikz3Lyexwk8JmU/tycjIDwcye+JypZ\nywIDAQAB\n-----END PUBLIC KEY-----	\N	RSA
\.


--
-- Data for Name: kickstart_files; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.kickstart_files (id, kickstart, name) FROM stdin;
\.


--
-- Data for Name: lambdas; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.lambdas (id, body, debug, engine_type, insert_instant, last_update_instant, name, type) FROM stdin;
d728dc22-dc33-4e43-88fe-3c8737037ae7	// This is the default SCIM Group response converter, modify this to your liking.\nfunction convert(group, members, options, scimGroup) {\n\n  // Un-comment this line to see the scimGroup object printed to the event log\n  // console.info(JSON.stringify(scimGroup, null, 2));\n\n  // Request options\n  // FusionAuth allows you to assign one or more application roles to a group.\n  // To use this feature, assign one or more application Ids here.\n  // options.roleIds = [];\n\n  // Set the name of the group using the SCIM Group displayName\n  group.name = scimGroup.displayName;\n\n  // Build a members array with a userId and a $ref in custom data\n  if (scimGroup.members) {\n    for (var i = 0; i < scimGroup.members.length; i++) {\n      members.push({\n        userId: scimGroup.members[i].value,\n        data: {\n          $ref: scimGroup.members[i]['$ref']\n        }\n      });\n    }\n  }\n}	f	GraalJS	1671355235700	1671355235700	[FusionAuth Default] SCIM Group Request Converter	19
921dd77f-4dab-497c-a76a-8a399b10bc9b	// This is the default SCIM Group request converter, modify this to your liking.\nfunction convert(scimGroup, group, members) {\n\n  // Un-comment this line to see the group object printed to the event log\n  // console.info(JSON.stringify(group, null, 2));\n\n  // Set the outgoing displayName on the SCIM group using the FusionAuth group name.\n  scimGroup.displayName = group.name;\n}	f	GraalJS	1671355235704	1671355235704	[FusionAuth Default] SCIM Group Response Converter	20
4267e335-a883-4784-b866-acbe22e04f0d	// This is the default SCIM User request converter, modify this to your liking.\nfunction convert(user, options, scimUser) {\n\n  // Un-comment this line to see the scimUser object printed to the event log\n  // console.info(JSON.stringify(scimUser, null, 2));\n\n  // Request options\n  // Note, sendSetPasswordEmail is only utilized during a user create request.\n  // options.applicationId = null;\n  // options.disableDomainBlock = false;\n  // options.sendSetPasswordEmail = false;\n  // options.skipVerification = false;\n\n  user.active = scimUser.active;\n  user.data.honorificPrefix = scimUser.name && scimUser.name.honorificPrefix;\n  user.data.honorificSuffix = scimUser.name && scimUser.name.honorificSuffix;\n  user.firstName = scimUser.name && scimUser.name.givenName;\n  user.fullName = scimUser.name && scimUser.name.formatted;\n  user.lastName = scimUser.name && scimUser.name.familyName;\n  user.middleName = scimUser.name && scimUser.name.middleName;\n  user.password = scimUser.password;\n  user.username = scimUser.userName;\n\n  // user.email\n  if (scimUser.emails) {\n    for (var i = 0; i < scimUser.emails.length; i++) {\n      if (scimUser.emails[i].primary) {\n        user.email = scimUser.emails[i].value;\n      }\n    }\n  }\n\n  // user.mobilePhone\n  if (scimUser.phoneNumbers) {\n    for (var j = 0; j < scimUser.phoneNumbers.length; j++) {\n      if (scimUser.phoneNumbers[j].primary) {\n        user.mobilePhone = scimUser.phoneNumbers[j].value;\n      }\n    }\n  }\n\n  // Handle the Enterprise User extension and other custom extensions\n  if (scimUser.schemas) {\n    for (var k = 0; k < scimUser.schemas.length; k++) {\n      var schema = scimUser.schemas[k];\n      if (schema !== 'urn:ietf:params:scim:schemas:core:2.0:User') {\n        user.data = user.data || {};\n        user.data.extensions = user.data.extensions || {};\n        user.data.extensions[schema] = scimUser[schema];\n      }\n    }\n  }\n}	f	GraalJS	1671355235707	1671355235707	[FusionAuth Default] SCIM User Request Converter	21
426d0116-c3c8-4faf-bca0-98bc3a3456b8	// This is the default SCIM User response converter, modify this to your liking.\nfunction convert(scimUser, user) {\n\n  // Un-comment this line to see the user object printed to the event log\n  // console.info(JSON.stringify(user, null, 2));\n\n  scimUser.active = user.active;\n  scimUser.userName = user.username;\n  scimUser.name = {\n    formatted: user.fullName,\n    familyName: user.lastName,\n    givenName: user.firstName,\n    middleName: user.middleName,\n    honorificPrefix: user.data.honorificPrefix,\n    honorificSuffix: user.data.honorificSuffix\n  };\n\n  scimUser.phoneNumbers = [{\n    primary: true,\n    value: user.mobilePhone,\n    type: "mobile"\n  }];\n\n  scimUser.emails = [{\n    primary: true,\n    value: user.email,\n    type: "work"\n  }];\n\n  // Optionally return any custom extensions stored in user.data\n  if (user.data && user.data.extensions) {\n    for (var extension in user.data.extensions) {\n      if (scimUser.schemas.indexOf(extension) === -1) {\n        scimUser.schemas.push(extension);\n      }\n      scimUser[extension] = user.data.extensions[extension];\n    }\n  }\n}	f	GraalJS	1671355235709	1671355235709	[FusionAuth Default] SCIM User Response Converter	22
effe753d-64a8-4d7d-9ecf-26046a9f7380	// This is the default Apple reconcile, modify this to your liking.\nfunction reconcile(user, registration, idToken) {\n\n  // Un-comment this line to see the idToken object printed to the event log\n  // console.info(JSON.stringify(idToken, null, 2));\n\n  // During the first login attempt, the user object will be available which may contain first and last name.\n  if (idToken.user && idToken.user.name) {\n    user.firstName = idToken.user.name.firstName || user.firstName;\n    user.lastName = idToken.user.name.lastName || user.lastName;\n  }\n}	f	GraalJS	1671355235739	1671355235739	[FusionAuth Default] Apple Reconcile	4
911c9f18-3bd0-4761-8fe3-34688445353f	// This is the default Facebook reconcile, modify this to your liking.\nfunction reconcile(user, registration, facebookUser) {\n\n  // Un-comment this line to see the facebookUser object printed to the event log\n  // console.info(JSON.stringify(facebookUser, null, 2));\n\n  user.firstName = facebookUser.first_name;\n  user.middleName = facebookUser.middle_name;\n  user.lastName = facebookUser.last_name;\n  user.fullName = facebookUser.name;\n\n  if (facebookUser.picture && !facebookUser.picture.data.is_silhouette) {\n    user.imageUrl = facebookUser.picture.data.url;\n  }\n\n  if (facebookUser.birthday) {\n    // Convert MM/dd/yyyy -> YYYY-MM-DD\n    var parts = facebookUser.birthday.split('/');\n    user.birthDate = parts[2] + '-' + parts[0] + '-' + parts[1];\n  }\n}	f	GraalJS	1671355235740	1671355235740	[FusionAuth Default] Facebook Reconcile	6
1fedb68a-620f-4abe-8068-1a7afc94f897	// This is the default Google reconcile, modify this to your liking.\nfunction reconcile(user, registration, idToken) {\n\n  // Un-comment this line to see the idToken object printed to the event log\n  // console.info(JSON.stringify(idToken, null, 2));\n\n  // The idToken is the response from the tokeninfo endpoint\n  // https://developers.google.com/identity/sign-in/web/backend-auth#calling-the-tokeninfo-endpoint\n  user.firstName = idToken.given_name;\n  user.lastName = idToken.family_name;\n  user.fullName = idToken.name;\n  user.imageUrl = idToken.picture;\n}	f	GraalJS	1671355235743	1671355235743	[FusionAuth Default] Google Reconcile	7
0c4e4efe-f71e-4dea-9764-420ef2b4c629	// This is the default LinkedIn reconcile, modify this to your liking.\nfunction reconcile(user, registration, linkedInUser) {\n\n  // Un-comment this line to see the linkedInUser object printed to the event log\n  // console.info(JSON.stringify(linkedInUser, null, ' '));\n\n  user.firstName = linkedInUser.localizedFirstName || user.firstName;\n  user.lastName = linkedInUser.localizedLastName || user.lastName;\n\n  // LinkedIn returns several images sizes.\n  // See https://docs.microsoft.com/en-us/linkedin/shared/references/v2/profile/profile-picture\n  var images = linkedInUser.profilePicture['displayImage~'].elements || [];\n  var image100 = images.length >= 1 ? images[0].identifiers[0].identifier : null;\n  var image200 = images.length >= 2 ? images[1].identifiers[0].identifier : null;\n  var image400 = images.length >= 3 ? images[2].identifiers[0].identifier : null;\n  var image800 = images.length >= 4 ? images[3].identifiers[0].identifier : null;\n\n  // Use the largest image.\n  user.imageUrl = image800;\n\n  // Record the LinkedIn Id\n  registration.data.linkedIn = registration.data.linkedIn || {};\n  registration.data.linkedIn.id = linkedInUser.id;\n}	f	GraalJS	1671355235745	1671355235745	[FusionAuth Default] LinkedIn Reconcile	11
09fbc41a-8e72-4384-9d70-23969b5e9931	// This is the default OpenID Connect reconcile, modify this to your liking.\nfunction reconcile(user, registration, jwt, idToken) {\n  // When the openid scope was requested, and the IdP returns an id_token, this value will optionally\n  // be available to this lambda if signed using the client secret using HS256, HS384, or the HS512 algorithm.\n\n  // Un-comment this line to see the jwt object printed to the event log\n  // console.info(JSON.stringify(jwt, null, 2));\n\n  user.firstName = jwt.given_name;\n  user.middleName = jwt.middle_name;\n  user.lastName = jwt.family_name;\n  user.fullName = jwt.name;\n  user.imageUrl = jwt.picture;\n  user.mobilePhone = jwt.phone_number;\n\n  // https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims\n  if (jwt.birthdate && jwt.birthdate != '0000') {\n    if (jwt.birthdate.length == 4) {\n      // Only a year was provided, set to January 1.\n      user.birthDate = jwt.birthdate + '-01-01';\n    } else {\n      user.birthDate = jwt.birthdate;\n    }\n  }\n\n  // https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims\n  if (jwt.locale) {\n    user.preferredLanguages = user.preferredLanguages || [];\n    // Replace the dash with an under_score.\n    user.preferredLanguages.push(jwt.locale.replace('-', '_'));\n  }\n\n  // Set preferred_username in registration.\n  // - This is just for display purposes, this value cannot be used to uniquely identify\n  //   the user in FusionAuth.\n  registration.username = jwt.preferred_username;\n}	f	GraalJS	1671355235746	1671355235746	[FusionAuth Default] OpenID Connect Reconcile	1
5a003c3b-8c45-43b4-bbb4-2906eb9a8e69	// This is the default SAML v2 reconcile, modify this to your liking.\nfunction reconcile(user, registration, samlResponse) {\n\n  // Un-comment this line to see the samlResponse object printed to the event log\n  // console.info(JSON.stringify(samlResponse, null, 2));\n\n  var getAttribute = function(samlResponse, attribute) {\n    var values = samlResponse.assertion.attributes[attribute];\n    if (values && values.length > 0) {\n      return values[0];\n    }\n\n    return null;\n  };\n\n  // Retrieve an attribute from the samlResponse\n  // - Arguments [2 .. ] provide a preferred order of attribute names to lookup the value in the response.\n  var defaultIfNull = function(samlResponse) {\n    for (var i = 1; i < arguments.length; i++) {\n      var value = getAttribute(samlResponse, arguments[i]);\n      if (value !== null) {\n        return value;\n      }\n    }\n  };\n\n  user.birthDate = defaultIfNull(samlResponse, 'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/dateofbirth', 'birthdate', 'date_of_birth');\n  user.firstName = defaultIfNull(samlResponse, 'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname', 'first_name');\n  user.lastName = defaultIfNull(samlResponse, 'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname', 'last_name');\n  user.fullName = defaultIfNull(samlResponse, 'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name', 'name', 'full_name');\n  user.mobilePhone = defaultIfNull(samlResponse, 'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mobilephone', 'mobile_phone');\n}	f	GraalJS	1671355235748	1671355235748	[FusionAuth Default] SAML v2 Reconcile	2
50af6916-7621-4b1f-9f34-79b429fb1b3c	// This is the default Twitter reconcile, modify this to your liking.\nfunction reconcile(user, registration, twitterUser) {\n\n  // Un-comment this line to see the twitterUser object printed to the event log\n  // console.info(JSON.stringify(twitterUser, null, 2));\n\n  // Set name if available in the response\n  if (twitterUser.name) {\n    user.fullName = twitterUser.name;\n  }\n\n  // https://developer.twitter.com/en/docs/accounts-and-users/user-profile-images-and-banners.html\n  if (twitterUser.profile_image_url_https) {\n    // Remove the _normal suffix to get the original size.\n    user.imageUrl = twitterUser.profile_image_url_https.replace('_normal.png', '.png');\n  }\n\n  // Set twitter screen_name in registration.\n  // - This is just for display purposes, this value cannot be used to uniquely identify\n  //   the user in FusionAuth.\n  registration.username = twitterUser.screen_name;\n}	f	GraalJS	1671355235750	1671355235750	[FusionAuth Default] Twitter Reconcile	9
\.


--
-- Data for Name: last_login_instants; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.last_login_instants (applications_id, registration_last_login_instant, users_id, user_last_login_instant) FROM stdin;
\.


--
-- Data for Name: locks; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.locks (type, update_instant) FROM stdin;
UserActionEndEvent	\N
EmailPlus	\N
Family	\N
com.inversoft.migration.Migrator	\N
Reindex	\N
Reset	\N
AsyncTaskManager	\N
\.


--
-- Data for Name: master_record; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.master_record (id, instant) FROM stdin;
cd96d470-cd68-4d9a-882d-6b9d0d686da0	1671364837242
\.


--
-- Data for Name: message_templates; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.message_templates (id, data, insert_instant, last_update_instant, name, type) FROM stdin;
b22616fd-a20f-e17b-9f5c-164d1107e9a7	{ "defaultTemplate": "Two Factor Code: ${code}" }	1671355209652	1671355209652	Default Two Factor Request	0
\.


--
-- Data for Name: messengers; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.messengers (id, data, insert_instant, last_update_instant, name, type) FROM stdin;
\.


--
-- Data for Name: migrations; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.migrations (name, run_instant) FROM stdin;
io.fusionauth.api.migration.guice.Migration_1_8_0	0
io.fusionauth.api.migration.guice.Migration_1_9_2	0
io.fusionauth.api.migration.guice.Migration_1_10_0	0
io.fusionauth.api.migration.guice.Migration_1_13_0	0
io.fusionauth.api.migration.guice.Migration_1_15_3	0
io.fusionauth.api.migration.guice.Migration_1_30_0	0
\.


--
-- Data for Name: nodes; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.nodes (id, data, insert_instant, last_checkin_instant, runtime_mode, url) FROM stdin;
cd96d470-cd68-4d9a-882d-6b9d0d686da0	{"data":{},"ipAddresses":{"eth0":["172.25.0.3"]},"platform":{"arch":"aarch64","name":"Linux","version":"5.15.49-linuxkit"}}	1671362884312	1671364867232	development	http://fusionauth:9011
\.


--
-- Data for Name: previous_passwords; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.previous_passwords (insert_instant, encryption_scheme, factor, password, salt, users_id) FROM stdin;
\.


--
-- Data for Name: raw_application_daily_active_users; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.raw_application_daily_active_users (applications_id, day, users_id) FROM stdin;
3c219e58-ed0e-4b18-ad48-f4f92793ae32	19344	ddf28f6c-75ec-4949-90d0-759c6b09a80a
056867c7-e20a-4af9-8570-47100496229f	19344	ddf28f6c-75ec-4949-90d0-759c6b09a80a
\.


--
-- Data for Name: raw_application_monthly_active_users; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.raw_application_monthly_active_users (applications_id, month, users_id) FROM stdin;
3c219e58-ed0e-4b18-ad48-f4f92793ae32	635	ddf28f6c-75ec-4949-90d0-759c6b09a80a
056867c7-e20a-4af9-8570-47100496229f	635	ddf28f6c-75ec-4949-90d0-759c6b09a80a
\.


--
-- Data for Name: raw_application_registration_counts; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.raw_application_registration_counts (id, applications_id, count, decremented_count, hour) FROM stdin;
\.


--
-- Data for Name: raw_global_daily_active_users; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.raw_global_daily_active_users (day, users_id) FROM stdin;
19344	ddf28f6c-75ec-4949-90d0-759c6b09a80a
\.


--
-- Data for Name: raw_global_monthly_active_users; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.raw_global_monthly_active_users (month, users_id) FROM stdin;
635	ddf28f6c-75ec-4949-90d0-759c6b09a80a
\.


--
-- Data for Name: raw_global_registration_counts; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.raw_global_registration_counts (id, count, decremented_count, hour) FROM stdin;
\.


--
-- Data for Name: raw_logins; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.raw_logins (applications_id, instant, ip_address, users_id) FROM stdin;
\.


--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.refresh_tokens (id, applications_id, data, insert_instant, start_instant, tenants_id, token, token_hash, token_text, users_id) FROM stdin;
\.


--
-- Data for Name: request_frequencies; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.request_frequencies (count, last_update_instant, request_id, tenants_id, type) FROM stdin;
\.


--
-- Data for Name: scim_external_id_groups; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.scim_external_id_groups (entities_id, external_id, groups_id) FROM stdin;
\.


--
-- Data for Name: scim_external_id_users; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.scim_external_id_users (entities_id, external_id, users_id) FROM stdin;
\.


--
-- Data for Name: system_configuration; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.system_configuration (data, insert_instant, last_update_instant, report_timezone) FROM stdin;
{"auditLogConfiguration":{"delete":{"enabled":false,"numberOfDaysToRetain":365}},"cookieEncryptionKey":"f2qZUZYY57f3A53PYR+2gQ==","corsConfiguration":{"allowCredentials":false,"debug":false,"enabled":false,"preflightMaxAgeInSeconds":0},"data":{},"eventLogConfiguration":{"numberToRetain":10000},"loginRecordConfiguration":{"delete":{"enabled":false,"numberOfDaysToRetain":365}},"uiConfiguration":{}}	1671355209652	1671355209652	America/Denver
\.


--
-- Data for Name: tenants; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.tenants (id, access_token_signing_keys_id, admin_user_forms_id, client_credentials_access_token_populate_lambdas_id, confirm_child_email_templates_id, data, email_update_email_templates_id, email_verified_email_templates_id, failed_authentication_user_actions_id, family_request_email_templates_id, forgot_password_email_templates_id, id_token_signing_keys_id, insert_instant, last_update_instant, login_id_in_use_on_create_email_templates_id, login_id_in_use_on_update_email_templates_id, login_new_device_email_templates_id, login_suspicious_email_templates_id, multi_factor_email_message_templates_id, multi_factor_sms_message_templates_id, multi_factor_sms_messengers_id, name, password_reset_success_email_templates_id, password_update_email_templates_id, parent_registration_email_templates_id, passwordless_email_templates_id, scim_client_entity_types_id, scim_enterprise_user_request_converter_lambdas_id, scim_enterprise_user_response_converter_lambdas_id, scim_group_request_converter_lambdas_id, scim_group_response_converter_lambdas_id, scim_server_entity_types_id, scim_user_request_converter_lambdas_id, scim_user_response_converter_lambdas_id, set_password_email_templates_id, themes_id, two_factor_method_add_email_templates_id, two_factor_method_remove_email_templates_id, ui_ip_access_control_lists_id, verification_email_templates_id) FROM stdin;
4544ae7e-7b90-67da-4ee5-91f1afd726d9	a06e2c22-5bf5-04c0-e2a8-137ed19b3cf6	4efdc20f-b73f-8831-c724-d4b140a6c13d	\N	\N	{"accessControlConfiguration":{},"captchaConfiguration":{"captchaMethod":"GoogleRecaptchaV3","enabled":false,"threshold":0.5},"configured":true,"data":{},"emailConfiguration":{"debug":false,"defaultFromEmail":"change-me@example.com","defaultFromName":"FusionAuth","host":"localhost","implicitEmailVerificationAllowed":true,"port":25,"security":"NONE","unverified":{"allowEmailChangeWhenGated":false,"behavior":"Allow"},"verificationStrategy":"ClickableLink","verifyEmail":false,"verifyEmailWhenChanged":false},"eventConfiguration":{"events":{"jwt.public-key.update":{"enabled":false,"transactionType":"None"},"jwt.refresh-token.revoke":{"enabled":false,"transactionType":"None"},"jwt.refresh":{"enabled":false,"transactionType":"None"},"group.create":{"enabled":false,"transactionType":"None"},"group.create.complete":{"enabled":false,"transactionType":"None"},"group.delete":{"enabled":false,"transactionType":"None"},"group.delete.complete":{"enabled":false,"transactionType":"None"},"group.member.add":{"enabled":false,"transactionType":"None"},"group.member.add.complete":{"enabled":false,"transactionType":"None"},"group.member.remove":{"enabled":false,"transactionType":"None"},"group.member.remove.complete":{"enabled":false,"transactionType":"None"},"group.member.update":{"enabled":false,"transactionType":"None"},"group.member.update.complete":{"enabled":false,"transactionType":"None"},"group.update":{"enabled":false,"transactionType":"None"},"group.update.complete":{"enabled":false,"transactionType":"None"},"user.action":{"enabled":false,"transactionType":"None"},"user.bulk.create":{"enabled":false,"transactionType":"None"},"user.create":{"enabled":false,"transactionType":"None"},"user.create.complete":{"enabled":false,"transactionType":"None"},"user.deactivate":{"enabled":false,"transactionType":"None"},"user.delete":{"enabled":false,"transactionType":"None"},"user.delete.complete":{"enabled":false,"transactionType":"None"},"user.email.update":{"enabled":false,"transactionType":"None"},"user.email.verified":{"enabled":false,"transactionType":"None"},"user.identity-provider.link":{"enabled":false,"transactionType":"None"},"user.identity-provider.unlink":{"enabled":false,"transactionType":"None"},"user.loginId.duplicate.create":{"enabled":false,"transactionType":"None"},"user.loginId.duplicate.update":{"enabled":false,"transactionType":"None"},"user.login.failed":{"enabled":false,"transactionType":"None"},"user.login.new-device":{"enabled":false,"transactionType":"None"},"user.login.success":{"enabled":false,"transactionType":"None"},"user.login.suspicious":{"enabled":false,"transactionType":"None"},"user.password.breach":{"enabled":false,"transactionType":"None"},"user.password.reset.send":{"enabled":false,"transactionType":"None"},"user.password.reset.start":{"enabled":false,"transactionType":"None"},"user.password.reset.success":{"enabled":false,"transactionType":"None"},"user.password.update":{"enabled":false,"transactionType":"None"},"user.reactivate":{"enabled":false,"transactionType":"None"},"user.registration.create":{"enabled":false,"transactionType":"None"},"user.registration.create.complete":{"enabled":false,"transactionType":"None"},"user.registration.delete":{"enabled":false,"transactionType":"None"},"user.registration.delete.complete":{"enabled":false,"transactionType":"None"},"user.registration.update":{"enabled":false,"transactionType":"None"},"user.registration.update.complete":{"enabled":false,"transactionType":"None"},"user.registration.verified":{"enabled":false,"transactionType":"None"},"user.two-factor.method.add":{"enabled":false,"transactionType":"None"},"user.two-factor.method.remove":{"enabled":false,"transactionType":"None"},"user.update":{"enabled":false,"transactionType":"None"},"user.update.complete":{"enabled":false,"transactionType":"None"}}},"externalIdentifierConfiguration":{"authorizationGrantIdTimeToLiveInSeconds":30,"changePasswordIdGenerator":{"length":32,"type":"randomBytes"},"changePasswordIdTimeToLiveInSeconds":600,"deviceCodeTimeToLiveInSeconds":300,"deviceUserCodeIdGenerator":{"length":6,"type":"randomAlphaNumeric"},"emailVerificationIdGenerator":{"length":32,"type":"randomBytes"},"emailVerificationIdTimeToLiveInSeconds":86400,"emailVerificationOneTimeCodeGenerator":{"length":6,"type":"randomAlphaNumeric"},"externalAuthenticationIdTimeToLiveInSeconds":300,"oneTimePasswordTimeToLiveInSeconds":60,"passwordlessLoginGenerator":{"length":32,"type":"randomBytes"},"passwordlessLoginTimeToLiveInSeconds":180,"pendingAccountLinkTimeToLiveInSeconds":3600,"registrationVerificationIdGenerator":{"length":32,"type":"randomBytes"},"registrationVerificationIdTimeToLiveInSeconds":86400,"registrationVerificationOneTimeCodeGenerator":{"length":6,"type":"randomAlphaNumeric"},"samlv2AuthNRequestIdTimeToLiveInSeconds":300,"setupPasswordIdGenerator":{"length":32,"type":"randomBytes"},"setupPasswordIdTimeToLiveInSeconds":86400,"trustTokenTimeToLiveInSeconds":180,"twoFactorIdTimeToLiveInSeconds":300,"twoFactorOneTimeCodeIdGenerator":{"length":6,"type":"randomDigits"},"twoFactorOneTimeCodeIdTimeToLiveInSeconds":60,"twoFactorTrustIdTimeToLiveInSeconds":2592000,"webAuthnAuthenticationChallengeTimeToLiveInSeconds":180,"webAuthnRegistrationChallengeTimeToLiveInSeconds":180},"failedAuthenticationConfiguration":{"actionCancelPolicy":{"onPasswordReset":false},"actionDuration":3,"actionDurationUnit":"MINUTES","emailUser":false,"resetCountInSeconds":60,"tooManyAttempts":5},"familyConfiguration":{"allowChildRegistrations":true,"deleteOrphanedAccounts":false,"deleteOrphanedAccountsDays":30,"enabled":false,"maximumChildAge":12,"minimumOwnerAge":21,"parentEmailRequired":false},"formConfiguration":{"adminUserFormId":"4efdc20f-b73f-8831-c724-d4b140a6c13d"},"httpSessionMaxInactiveInterval":3600,"issuer":"toxygates.nibiohn.go.jp","jwtConfiguration":{"enabled":false,"refreshTokenExpirationPolicy":"Fixed","refreshTokenRevocationPolicy":{"onLoginPrevented":true,"onMultiFactorEnable":false,"onPasswordChanged":true},"refreshTokenTimeToLiveInMinutes":43200,"refreshTokenUsagePolicy":"Reusable","timeToLiveInSeconds":3600},"loginConfiguration":{"requireAuthentication":true},"maximumPasswordAge":{"days":180,"enabled":false},"minimumPasswordAge":{"enabled":false,"seconds":30},"multiFactorConfiguration":{"authenticator":{"algorithm":"HmacSHA1","codeLength":6,"enabled":true,"timeStep":30},"email":{"enabled":false,"templateId":"6b34ee69-542b-4926-a469-ada36ddf39cf"},"loginPolicy":"Enabled","sms":{"enabled":false}},"passwordEncryptionConfiguration":{"encryptionScheme":"salted-pbkdf2-hmac-sha256","encryptionSchemeFactor":24000,"modifyEncryptionSchemeOnLogin":false},"passwordValidationRules":{"breachDetection":{"enabled":false,"matchMode":"High","notifyUserEmailTemplateId":"421c7d2d-f85c-4719-adb9-5bbb32501b14","onLogin":"Off"},"maxLength":256,"minLength":8,"rememberPreviousPasswords":{"count":1,"enabled":false},"requireMixedCase":false,"requireNonAlpha":false,"requireNumber":false,"validateOnLogin":false},"rateLimitConfiguration":{"failedLogin":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"forgotPassword":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendEmailVerification":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendPasswordless":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendRegistrationVerification":{"enabled":false,"limit":5,"timePeriodInSeconds":60},"sendTwoFactor":{"enabled":false,"limit":5,"timePeriodInSeconds":60}},"registrationConfiguration":{},"scimServerConfiguration":{"enabled":false},"ssoConfiguration":{"deviceTrustTimeToLiveInSeconds":31536000},"state":"Active","userDeletePolicy":{"unverified":{"enabled":false,"numberOfDaysToRetain":120}},"usernameConfiguration":{"unique":{"enabled":false,"numberOfDigits":5,"separator":"#","strategy":"OnCollision"}},"webAuthnConfiguration":{"bootstrapWorkflow":{"authenticatorAttachmentPreference":"any","enabled":false,"userVerificationRequirement":"required"},"debug":false,"enabled":false,"reauthenticationWorkflow":{"authenticatorAttachmentPreference":"platform","enabled":false,"userVerificationRequirement":"required"}}}	\N	\N	\N	\N	4e0def6a-f6c6-45fa-a1a5-704edd8a163e	092dbedc-30af-4149-9c61-b578f2c72f59	1671355208652	1671355486998	\N	\N	\N	\N	6b34ee69-542b-4926-a469-ada36ddf39cf	\N	\N	Default	\N	\N	\N	9c4cb0b7-c8f8-4169-9dc7-83072b90f654	\N	\N	\N	\N	\N	\N	\N	\N	1e93cf8f-cc01-4aa8-9600-fdd95748337e	75a068fd-e94b-451a-9aeb-3ddb9a3b5987	\N	\N	\N	\N
\.


--
-- Data for Name: themes; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.themes (id, data, insert_instant, last_update_instant, name) FROM stdin;
75a068fd-e94b-451a-9aeb-3ddb9a3b5987	{}	1671355209652	1671355209652	FusionAuth
\.


--
-- Data for Name: user_action_logs; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.user_action_logs (id, actioner_users_id, actionee_users_id, comment, email_user_on_end, end_event_sent, expiry, history, insert_instant, localized_name, localized_option, localized_reason, name, notify_user_on_end, option_name, reason, reason_code, user_actions_id) FROM stdin;
\.


--
-- Data for Name: user_action_logs_applications; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.user_action_logs_applications (applications_id, user_action_logs_id) FROM stdin;
\.


--
-- Data for Name: user_action_reasons; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.user_action_reasons (id, insert_instant, last_update_instant, localized_texts, text, code) FROM stdin;
\.


--
-- Data for Name: user_actions; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.user_actions (id, active, cancel_email_templates_id, end_email_templates_id, include_email_in_event_json, insert_instant, last_update_instant, localized_names, modify_email_templates_id, name, options, prevent_login, send_end_event, start_email_templates_id, temporal, transaction_type, user_notifications_enabled, user_emailing_enabled) FROM stdin;
\.


--
-- Data for Name: user_comments; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.user_comments (id, comment, commenter_id, insert_instant, users_id) FROM stdin;
\.


--
-- Data for Name: user_consents; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.user_consents (id, consents_id, data, giver_users_id, insert_instant, last_update_instant, users_id) FROM stdin;
\.


--
-- Data for Name: user_consents_email_plus; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.user_consents_email_plus (id, next_email_instant, user_consents_id) FROM stdin;
\.


--
-- Data for Name: user_registrations; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.user_registrations (id, applications_id, authentication_token, clean_speak_id, data, insert_instant, last_login_instant, last_update_instant, timezone, username, username_status, users_id, verified) FROM stdin;
c78bf303-fd85-4bf5-bc33-13d756ffe30b	056867c7-e20a-4af9-8570-47100496229f	\N	\N	{"data":{},"preferredLanguages":[],"tokens":{}}	1671356313126	1671363801612	1671356313126	\N	\N	0	ddf28f6c-75ec-4949-90d0-759c6b09a80a	t
4cb5abf5-a7b1-4f14-aeb4-9f1b36e1d043	3c219e58-ed0e-4b18-ad48-f4f92793ae32	\N	\N	{"data":{},"preferredLanguages":[],"tokens":{}}	1671355235687	1671364072248	1671355235687	\N	\N	0	ddf28f6c-75ec-4949-90d0-759c6b09a80a	t
\.


--
-- Data for Name: user_registrations_application_roles; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.user_registrations_application_roles (application_roles_id, user_registrations_id) FROM stdin;
631ecd9d-8d40-4c13-8277-80cedb8236e2	4cb5abf5-a7b1-4f14-aeb4-9f1b36e1d043
ff345e71-6387-466e-9601-7362e3540eb0	c78bf303-fd85-4bf5-bc33-13d756ffe30b
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.users (id, active, birth_date, clean_speak_id, data, expiry, first_name, full_name, image_url, insert_instant, last_name, last_update_instant, middle_name, mobile_phone, parent_email, tenants_id, timezone) FROM stdin;
ddf28f6c-75ec-4949-90d0-759c6b09a80a	t	\N	\N	{"data":{},"preferredLanguages":[],"twoFactor":{}}	\N	Yuji	\N	\N	1671355235602	Kosugi	1671355235602	\N	\N	\N	4544ae7e-7b90-67da-4ee5-91f1afd726d9	\N
\.


--
-- Data for Name: version; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.version (version) FROM stdin;
1.42.0
\.


--
-- Data for Name: webauthn_credentials; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.webauthn_credentials (id, credential_id, data, insert_instant, last_use_instant, tenants_id, users_id) FROM stdin;
\.


--
-- Data for Name: webhooks; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.webhooks (id, connect_timeout, description, data, global, headers, http_authentication_username, http_authentication_password, insert_instant, last_update_instant, read_timeout, ssl_certificate, url) FROM stdin;
\.


--
-- Data for Name: webhooks_tenants; Type: TABLE DATA; Schema: public; Owner: fusionauth
--

COPY public.webhooks_tenants (webhooks_id, tenants_id) FROM stdin;
\.


--
-- Name: audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: fusionauth
--

SELECT pg_catalog.setval('public.audit_logs_id_seq', 8, true);


--
-- Name: event_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: fusionauth
--

SELECT pg_catalog.setval('public.event_logs_id_seq', 1, false);


--
-- Name: identities_id_seq; Type: SEQUENCE SET; Schema: public; Owner: fusionauth
--

SELECT pg_catalog.setval('public.identities_id_seq', 1, true);


--
-- Name: raw_application_registration_counts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: fusionauth
--

SELECT pg_catalog.setval('public.raw_application_registration_counts_id_seq', 2, true);


--
-- Name: raw_global_registration_counts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: fusionauth
--

SELECT pg_catalog.setval('public.raw_global_registration_counts_id_seq', 1, true);


--
-- Name: user_consents_email_plus_id_seq; Type: SEQUENCE SET; Schema: public; Owner: fusionauth
--

SELECT pg_catalog.setval('public.user_consents_email_plus_id_seq', 1, false);


--
-- Name: application_daily_active_users application_daily_active_users_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.application_daily_active_users
    ADD CONSTRAINT application_daily_active_users_uk_1 UNIQUE (applications_id, day);


--
-- Name: application_monthly_active_users application_monthly_active_users_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.application_monthly_active_users
    ADD CONSTRAINT application_monthly_active_users_uk_1 UNIQUE (applications_id, month);


--
-- Name: application_registration_counts application_registration_counts_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.application_registration_counts
    ADD CONSTRAINT application_registration_counts_uk_1 UNIQUE (applications_id, hour);


--
-- Name: application_roles application_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.application_roles
    ADD CONSTRAINT application_roles_pkey PRIMARY KEY (id);


--
-- Name: application_roles application_roles_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.application_roles
    ADD CONSTRAINT application_roles_uk_1 UNIQUE (name, applications_id);


--
-- Name: applications applications_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_pkey PRIMARY KEY (id);


--
-- Name: applications applications_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_uk_1 UNIQUE (name, tenants_id);


--
-- Name: applications applications_uk_2; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_uk_2 UNIQUE (samlv2_issuer, tenants_id);


--
-- Name: asynchronous_tasks asynchronous_tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.asynchronous_tasks
    ADD CONSTRAINT asynchronous_tasks_pkey PRIMARY KEY (id);


--
-- Name: asynchronous_tasks asynchronous_tasks_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.asynchronous_tasks
    ADD CONSTRAINT asynchronous_tasks_uk_1 UNIQUE (entity_id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: authentication_keys authentication_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.authentication_keys
    ADD CONSTRAINT authentication_keys_pkey PRIMARY KEY (id);


--
-- Name: authentication_keys authentication_keys_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.authentication_keys
    ADD CONSTRAINT authentication_keys_uk_1 UNIQUE (key_value);


--
-- Name: breached_password_metrics breached_password_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.breached_password_metrics
    ADD CONSTRAINT breached_password_metrics_pkey PRIMARY KEY (tenants_id);


--
-- Name: clean_speak_applications clean_speak_applications_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.clean_speak_applications
    ADD CONSTRAINT clean_speak_applications_uk_1 UNIQUE (applications_id, clean_speak_application_id);


--
-- Name: common_breached_passwords common_breached_passwords_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.common_breached_passwords
    ADD CONSTRAINT common_breached_passwords_pkey PRIMARY KEY (password);


--
-- Name: connectors connectors_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.connectors
    ADD CONSTRAINT connectors_pkey PRIMARY KEY (id);


--
-- Name: connectors_tenants connectors_tenants_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.connectors_tenants
    ADD CONSTRAINT connectors_tenants_pkey PRIMARY KEY (connectors_id, tenants_id);


--
-- Name: connectors_tenants connectors_tenants_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.connectors_tenants
    ADD CONSTRAINT connectors_tenants_uk_1 UNIQUE (connectors_id, tenants_id, sequence);


--
-- Name: connectors connectors_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.connectors
    ADD CONSTRAINT connectors_uk_1 UNIQUE (name);


--
-- Name: consents consents_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.consents
    ADD CONSTRAINT consents_pkey PRIMARY KEY (id);


--
-- Name: consents consents_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.consents
    ADD CONSTRAINT consents_uk_1 UNIQUE (name);


--
-- Name: data_sets data_sets_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.data_sets
    ADD CONSTRAINT data_sets_pkey PRIMARY KEY (name);


--
-- Name: email_templates email_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.email_templates
    ADD CONSTRAINT email_templates_pkey PRIMARY KEY (id);


--
-- Name: email_templates email_templates_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.email_templates
    ADD CONSTRAINT email_templates_uk_1 UNIQUE (name);


--
-- Name: entities entities_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entities
    ADD CONSTRAINT entities_pkey PRIMARY KEY (id);


--
-- Name: entities entities_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entities
    ADD CONSTRAINT entities_uk_1 UNIQUE (client_id);


--
-- Name: entity_entity_grant_permissions entity_entity_grant_permissions_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_entity_grant_permissions
    ADD CONSTRAINT entity_entity_grant_permissions_uk_1 UNIQUE (entity_entity_grants_id, entity_type_permissions_id);


--
-- Name: entity_entity_grants entity_entity_grants_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_entity_grants
    ADD CONSTRAINT entity_entity_grants_pkey PRIMARY KEY (id);


--
-- Name: entity_entity_grants entity_entity_grants_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_entity_grants
    ADD CONSTRAINT entity_entity_grants_uk_1 UNIQUE (recipient_id, target_id);


--
-- Name: entity_type_permissions entity_type_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_type_permissions
    ADD CONSTRAINT entity_type_permissions_pkey PRIMARY KEY (id);


--
-- Name: entity_type_permissions entity_type_permissions_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_type_permissions
    ADD CONSTRAINT entity_type_permissions_uk_1 UNIQUE (entity_types_id, name);


--
-- Name: entity_types entity_types_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_types
    ADD CONSTRAINT entity_types_pkey PRIMARY KEY (id);


--
-- Name: entity_types entity_types_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_types
    ADD CONSTRAINT entity_types_uk_1 UNIQUE (name);


--
-- Name: entity_user_grant_permissions entity_user_grant_permissions_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_user_grant_permissions
    ADD CONSTRAINT entity_user_grant_permissions_uk_1 UNIQUE (entity_user_grants_id, entity_type_permissions_id);


--
-- Name: entity_user_grants entity_user_grants_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_user_grants
    ADD CONSTRAINT entity_user_grants_pkey PRIMARY KEY (id);


--
-- Name: entity_user_grants entity_user_grants_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_user_grants
    ADD CONSTRAINT entity_user_grants_uk_1 UNIQUE (entities_id, users_id);


--
-- Name: event_logs event_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.event_logs
    ADD CONSTRAINT event_logs_pkey PRIMARY KEY (id);


--
-- Name: external_identifiers external_identifiers_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.external_identifiers
    ADD CONSTRAINT external_identifiers_pkey PRIMARY KEY (id);


--
-- Name: families families_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.families
    ADD CONSTRAINT families_pkey PRIMARY KEY (family_id, users_id);


--
-- Name: federated_domains federated_domains_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.federated_domains
    ADD CONSTRAINT federated_domains_uk_1 UNIQUE (domain);


--
-- Name: form_fields form_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.form_fields
    ADD CONSTRAINT form_fields_pkey PRIMARY KEY (id);


--
-- Name: form_fields form_fields_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.form_fields
    ADD CONSTRAINT form_fields_uk_1 UNIQUE (name);


--
-- Name: form_steps form_steps_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.form_steps
    ADD CONSTRAINT form_steps_pkey PRIMARY KEY (forms_id, form_fields_id);


--
-- Name: forms forms_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.forms
    ADD CONSTRAINT forms_pkey PRIMARY KEY (id);


--
-- Name: forms forms_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.forms
    ADD CONSTRAINT forms_uk_1 UNIQUE (name);


--
-- Name: global_daily_active_users global_daily_active_users_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.global_daily_active_users
    ADD CONSTRAINT global_daily_active_users_uk_1 UNIQUE (day);


--
-- Name: global_monthly_active_users global_monthly_active_users_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.global_monthly_active_users
    ADD CONSTRAINT global_monthly_active_users_uk_1 UNIQUE (month);


--
-- Name: global_registration_counts global_registration_counts_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.global_registration_counts
    ADD CONSTRAINT global_registration_counts_uk_1 UNIQUE (hour);


--
-- Name: group_application_roles group_application_roles_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.group_application_roles
    ADD CONSTRAINT group_application_roles_uk_1 UNIQUE (groups_id, application_roles_id);


--
-- Name: group_members group_members_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_pkey PRIMARY KEY (id);


--
-- Name: group_members group_members_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_uk_1 UNIQUE (groups_id, users_id);


--
-- Name: groups groups_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);


--
-- Name: groups groups_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_uk_1 UNIQUE (name, tenants_id);


--
-- Name: hourly_logins hourly_logins_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.hourly_logins
    ADD CONSTRAINT hourly_logins_uk_1 UNIQUE (applications_id, hour);


--
-- Name: identities identities_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identities
    ADD CONSTRAINT identities_pkey PRIMARY KEY (id);


--
-- Name: identities identities_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identities
    ADD CONSTRAINT identities_uk_1 UNIQUE (email, tenants_id);


--
-- Name: identities identities_uk_2; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identities
    ADD CONSTRAINT identities_uk_2 UNIQUE (username_index, tenants_id);


--
-- Name: identity_provider_links identity_provider_links_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_provider_links
    ADD CONSTRAINT identity_provider_links_uk_1 UNIQUE (identity_providers_id, identity_providers_user_id, tenants_id);


--
-- Name: identity_providers identity_providers_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_providers
    ADD CONSTRAINT identity_providers_pkey PRIMARY KEY (id);


--
-- Name: identity_providers identity_providers_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_providers
    ADD CONSTRAINT identity_providers_uk_1 UNIQUE (name);


--
-- Name: ip_access_control_lists ip_access_control_lists_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.ip_access_control_lists
    ADD CONSTRAINT ip_access_control_lists_pkey PRIMARY KEY (id);


--
-- Name: ip_access_control_lists ip_access_control_lists_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.ip_access_control_lists
    ADD CONSTRAINT ip_access_control_lists_uk_1 UNIQUE (name);


--
-- Name: keys keys_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.keys
    ADD CONSTRAINT keys_pkey PRIMARY KEY (id);


--
-- Name: keys keys_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.keys
    ADD CONSTRAINT keys_uk_1 UNIQUE (kid);


--
-- Name: keys keys_uk_2; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.keys
    ADD CONSTRAINT keys_uk_2 UNIQUE (name);


--
-- Name: kickstart_files kickstart_files_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.kickstart_files
    ADD CONSTRAINT kickstart_files_pkey PRIMARY KEY (id);


--
-- Name: lambdas lambdas_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.lambdas
    ADD CONSTRAINT lambdas_pkey PRIMARY KEY (id);


--
-- Name: locks locks_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.locks
    ADD CONSTRAINT locks_pkey PRIMARY KEY (type);


--
-- Name: message_templates message_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.message_templates
    ADD CONSTRAINT message_templates_pkey PRIMARY KEY (id);


--
-- Name: message_templates message_templates_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.message_templates
    ADD CONSTRAINT message_templates_uk_1 UNIQUE (name);


--
-- Name: messengers messengers_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.messengers
    ADD CONSTRAINT messengers_pkey PRIMARY KEY (id);


--
-- Name: messengers messengers_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.messengers
    ADD CONSTRAINT messengers_uk_1 UNIQUE (name);


--
-- Name: migrations migrations_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.migrations
    ADD CONSTRAINT migrations_pkey PRIMARY KEY (name);


--
-- Name: nodes nodes_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.nodes
    ADD CONSTRAINT nodes_pkey PRIMARY KEY (id);


--
-- Name: previous_passwords previous_passwords_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.previous_passwords
    ADD CONSTRAINT previous_passwords_uk_1 UNIQUE (users_id, insert_instant);


--
-- Name: raw_application_daily_active_users raw_application_daily_active_users_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.raw_application_daily_active_users
    ADD CONSTRAINT raw_application_daily_active_users_uk_1 UNIQUE (applications_id, day, users_id);


--
-- Name: raw_application_monthly_active_users raw_application_monthly_active_users_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.raw_application_monthly_active_users
    ADD CONSTRAINT raw_application_monthly_active_users_uk_1 UNIQUE (applications_id, month, users_id);


--
-- Name: raw_application_registration_counts raw_application_registration_counts_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.raw_application_registration_counts
    ADD CONSTRAINT raw_application_registration_counts_pkey PRIMARY KEY (id);


--
-- Name: raw_global_daily_active_users raw_global_daily_active_users_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.raw_global_daily_active_users
    ADD CONSTRAINT raw_global_daily_active_users_uk_1 UNIQUE (day, users_id);


--
-- Name: raw_global_monthly_active_users raw_global_monthly_active_users_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.raw_global_monthly_active_users
    ADD CONSTRAINT raw_global_monthly_active_users_uk_1 UNIQUE (month, users_id);


--
-- Name: raw_global_registration_counts raw_global_registration_counts_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.raw_global_registration_counts
    ADD CONSTRAINT raw_global_registration_counts_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_uk_1 UNIQUE (token);


--
-- Name: refresh_tokens refresh_tokens_uk_2; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_uk_2 UNIQUE (token_hash);


--
-- Name: request_frequencies request_frequencies_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.request_frequencies
    ADD CONSTRAINT request_frequencies_uk_1 UNIQUE (tenants_id, type, request_id);


--
-- Name: tenants tenants_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_pkey PRIMARY KEY (id);


--
-- Name: tenants tenants_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_uk_1 UNIQUE (name);


--
-- Name: themes themes_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.themes
    ADD CONSTRAINT themes_pkey PRIMARY KEY (id);


--
-- Name: themes themes_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.themes
    ADD CONSTRAINT themes_uk_1 UNIQUE (name);


--
-- Name: user_action_logs user_action_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_action_logs
    ADD CONSTRAINT user_action_logs_pkey PRIMARY KEY (id);


--
-- Name: user_action_reasons user_action_reasons_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_action_reasons
    ADD CONSTRAINT user_action_reasons_pkey PRIMARY KEY (id);


--
-- Name: user_action_reasons user_action_reasons_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_action_reasons
    ADD CONSTRAINT user_action_reasons_uk_1 UNIQUE (text);


--
-- Name: user_action_reasons user_action_reasons_uk_2; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_action_reasons
    ADD CONSTRAINT user_action_reasons_uk_2 UNIQUE (code);


--
-- Name: user_actions user_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_actions
    ADD CONSTRAINT user_actions_pkey PRIMARY KEY (id);


--
-- Name: user_actions user_actions_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_actions
    ADD CONSTRAINT user_actions_uk_1 UNIQUE (name);


--
-- Name: user_comments user_comments_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_comments
    ADD CONSTRAINT user_comments_pkey PRIMARY KEY (id);


--
-- Name: user_consents_email_plus user_consents_email_plus_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_consents_email_plus
    ADD CONSTRAINT user_consents_email_plus_pkey PRIMARY KEY (id);


--
-- Name: user_consents user_consents_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_consents
    ADD CONSTRAINT user_consents_pkey PRIMARY KEY (id);


--
-- Name: user_registrations_application_roles user_registrations_application_roles_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_registrations_application_roles
    ADD CONSTRAINT user_registrations_application_roles_uk_1 UNIQUE (user_registrations_id, application_roles_id);


--
-- Name: user_registrations user_registrations_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_registrations
    ADD CONSTRAINT user_registrations_pkey PRIMARY KEY (id);


--
-- Name: user_registrations user_registrations_uk_1; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_registrations
    ADD CONSTRAINT user_registrations_uk_1 UNIQUE (applications_id, users_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: webauthn_credentials webauthn_credentials_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.webauthn_credentials
    ADD CONSTRAINT webauthn_credentials_pkey PRIMARY KEY (id);


--
-- Name: webhooks webhooks_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.webhooks
    ADD CONSTRAINT webhooks_pkey PRIMARY KEY (id);


--
-- Name: webhooks_tenants webhooks_tenants_pkey; Type: CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.webhooks_tenants
    ADD CONSTRAINT webhooks_tenants_pkey PRIMARY KEY (webhooks_id, tenants_id);


--
-- Name: applications_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX applications_i_1 ON public.applications USING btree (tenants_id);


--
-- Name: audit_logs_i1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX audit_logs_i1 ON public.audit_logs USING btree (insert_instant);


--
-- Name: event_logs_i1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX event_logs_i1 ON public.event_logs USING btree (insert_instant);


--
-- Name: external_identifiers_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX external_identifiers_i_1 ON public.external_identifiers USING btree (tenants_id, type, insert_instant);


--
-- Name: external_identifiers_i_2; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX external_identifiers_i_2 ON public.external_identifiers USING btree (type, users_id, applications_id);


--
-- Name: external_identifiers_i_3; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX external_identifiers_i_3 ON public.external_identifiers USING btree (expiration_instant);


--
-- Name: families_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX families_i_1 ON public.families USING btree (users_id);


--
-- Name: group_members_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX group_members_i_1 ON public.group_members USING btree (users_id);


--
-- Name: identities_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX identities_i_1 ON public.identities USING btree (users_id);


--
-- Name: raw_logins_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX raw_logins_i_1 ON public.raw_logins USING btree (instant);


--
-- Name: raw_logins_i_2; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX raw_logins_i_2 ON public.raw_logins USING btree (users_id, instant);


--
-- Name: refresh_tokens_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX refresh_tokens_i_1 ON public.refresh_tokens USING btree (start_instant);


--
-- Name: refresh_tokens_i_2; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX refresh_tokens_i_2 ON public.refresh_tokens USING btree (applications_id);


--
-- Name: refresh_tokens_i_3; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX refresh_tokens_i_3 ON public.refresh_tokens USING btree (users_id);


--
-- Name: refresh_tokens_i_4; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX refresh_tokens_i_4 ON public.refresh_tokens USING btree (tenants_id);


--
-- Name: request_frequencies_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX request_frequencies_i_1 ON public.request_frequencies USING btree (tenants_id, type, last_update_instant);


--
-- Name: scim_external_id_groups_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX scim_external_id_groups_i_1 ON public.scim_external_id_groups USING btree (entities_id, external_id);


--
-- Name: scim_external_id_groups_i_2; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX scim_external_id_groups_i_2 ON public.scim_external_id_groups USING btree (entities_id, groups_id);


--
-- Name: scim_external_id_users_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX scim_external_id_users_i_1 ON public.scim_external_id_users USING btree (entities_id, external_id);


--
-- Name: scim_external_id_users_i_2; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX scim_external_id_users_i_2 ON public.scim_external_id_users USING btree (entities_id, users_id);


--
-- Name: user_action_logs_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX user_action_logs_i_1 ON public.user_action_logs USING btree (insert_instant);


--
-- Name: user_action_logs_i_2; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX user_action_logs_i_2 ON public.user_action_logs USING btree (expiry, end_event_sent);


--
-- Name: user_comments_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX user_comments_i_1 ON public.user_comments USING btree (insert_instant);


--
-- Name: user_comments_i_2; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX user_comments_i_2 ON public.user_comments USING btree (users_id);


--
-- Name: user_comments_i_3; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX user_comments_i_3 ON public.user_comments USING btree (commenter_id);


--
-- Name: user_consents_email_plus_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX user_consents_email_plus_i_1 ON public.user_consents_email_plus USING btree (next_email_instant);


--
-- Name: user_registrations_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX user_registrations_i_1 ON public.user_registrations USING btree (clean_speak_id);


--
-- Name: user_registrations_i_2; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX user_registrations_i_2 ON public.user_registrations USING btree (users_id);


--
-- Name: users_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX users_i_1 ON public.users USING btree (clean_speak_id);


--
-- Name: users_i_2; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX users_i_2 ON public.users USING btree (parent_email);


--
-- Name: webauthn_credentials_i_1; Type: INDEX; Schema: public; Owner: fusionauth
--

CREATE INDEX webauthn_credentials_i_1 ON public.webauthn_credentials USING btree (tenants_id, users_id);


--
-- Name: application_daily_active_users application_daily_active_users_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.application_daily_active_users
    ADD CONSTRAINT application_daily_active_users_fk_1 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: application_monthly_active_users application_monthly_active_users_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.application_monthly_active_users
    ADD CONSTRAINT application_monthly_active_users_fk_1 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: application_registration_counts application_registration_counts_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.application_registration_counts
    ADD CONSTRAINT application_registration_counts_fk_1 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: application_roles application_roles_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.application_roles
    ADD CONSTRAINT application_roles_fk_1 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: applications applications_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_1 FOREIGN KEY (verification_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_10; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_10 FOREIGN KEY (email_verification_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_11; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_11 FOREIGN KEY (forgot_password_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_12; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_12 FOREIGN KEY (passwordless_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_13; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_13 FOREIGN KEY (set_password_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_14; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_14 FOREIGN KEY (samlv2_default_verification_keys_id) REFERENCES public.keys(id);


--
-- Name: applications applications_fk_15; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_15 FOREIGN KEY (admin_registration_forms_id) REFERENCES public.forms(id);


--
-- Name: applications applications_fk_16; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_16 FOREIGN KEY (samlv2_logout_keys_id) REFERENCES public.keys(id);


--
-- Name: applications applications_fk_17; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_17 FOREIGN KEY (samlv2_logout_default_verification_keys_id) REFERENCES public.keys(id);


--
-- Name: applications applications_fk_18; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_18 FOREIGN KEY (samlv2_single_logout_keys_id) REFERENCES public.keys(id);


--
-- Name: applications applications_fk_19; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_19 FOREIGN KEY (multi_factor_email_message_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_2 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: applications applications_fk_20; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_20 FOREIGN KEY (multi_factor_sms_message_templates_id) REFERENCES public.message_templates(id);


--
-- Name: applications applications_fk_21; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_21 FOREIGN KEY (self_service_user_forms_id) REFERENCES public.forms(id);


--
-- Name: applications applications_fk_22; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_22 FOREIGN KEY (themes_id) REFERENCES public.themes(id);


--
-- Name: applications applications_fk_23; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_23 FOREIGN KEY (email_verified_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_24; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_24 FOREIGN KEY (login_new_device_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_25; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_25 FOREIGN KEY (login_suspicious_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_26; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_26 FOREIGN KEY (password_reset_success_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_27; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_27 FOREIGN KEY (ui_ip_access_control_lists_id) REFERENCES public.ip_access_control_lists(id);


--
-- Name: applications applications_fk_28; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_28 FOREIGN KEY (email_update_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_29; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_29 FOREIGN KEY (login_id_in_use_on_create_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_3 FOREIGN KEY (access_token_populate_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: applications applications_fk_30; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_30 FOREIGN KEY (login_id_in_use_on_update_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_31; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_31 FOREIGN KEY (password_update_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_32; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_32 FOREIGN KEY (two_factor_method_add_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_33; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_33 FOREIGN KEY (two_factor_method_remove_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: applications applications_fk_4; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_4 FOREIGN KEY (id_token_populate_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: applications applications_fk_5; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_5 FOREIGN KEY (samlv2_keys_id) REFERENCES public.keys(id);


--
-- Name: applications applications_fk_6; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_6 FOREIGN KEY (samlv2_populate_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: applications applications_fk_7; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_7 FOREIGN KEY (access_token_signing_keys_id) REFERENCES public.keys(id);


--
-- Name: applications applications_fk_8; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_8 FOREIGN KEY (id_token_signing_keys_id) REFERENCES public.keys(id);


--
-- Name: applications applications_fk_9; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_fk_9 FOREIGN KEY (forms_id) REFERENCES public.forms(id);


--
-- Name: asynchronous_tasks asynchronous_tasks_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.asynchronous_tasks
    ADD CONSTRAINT asynchronous_tasks_fk_1 FOREIGN KEY (nodes_id) REFERENCES public.nodes(id);


--
-- Name: authentication_keys authentication_keys_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.authentication_keys
    ADD CONSTRAINT authentication_keys_fk_1 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: authentication_keys authentication_keys_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.authentication_keys
    ADD CONSTRAINT authentication_keys_fk_2 FOREIGN KEY (ip_access_control_lists_id) REFERENCES public.ip_access_control_lists(id);


--
-- Name: breached_password_metrics breached_password_metrics_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.breached_password_metrics
    ADD CONSTRAINT breached_password_metrics_fk_1 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: clean_speak_applications clean_speak_applications_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.clean_speak_applications
    ADD CONSTRAINT clean_speak_applications_fk_1 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: connectors connectors_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.connectors
    ADD CONSTRAINT connectors_fk_1 FOREIGN KEY (ssl_certificate_keys_id) REFERENCES public.keys(id);


--
-- Name: connectors connectors_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.connectors
    ADD CONSTRAINT connectors_fk_2 FOREIGN KEY (reconcile_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: connectors_tenants connectors_tenants_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.connectors_tenants
    ADD CONSTRAINT connectors_tenants_fk_1 FOREIGN KEY (connectors_id) REFERENCES public.connectors(id);


--
-- Name: connectors_tenants connectors_tenants_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.connectors_tenants
    ADD CONSTRAINT connectors_tenants_fk_2 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: consents consents_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.consents
    ADD CONSTRAINT consents_fk_1 FOREIGN KEY (consent_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: consents consents_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.consents
    ADD CONSTRAINT consents_fk_2 FOREIGN KEY (email_plus_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: entities entities_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entities
    ADD CONSTRAINT entities_fk_1 FOREIGN KEY (entity_types_id) REFERENCES public.entity_types(id) ON DELETE CASCADE;


--
-- Name: entities entities_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entities
    ADD CONSTRAINT entities_fk_2 FOREIGN KEY (parent_id) REFERENCES public.entities(id) ON DELETE CASCADE;


--
-- Name: entities entities_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entities
    ADD CONSTRAINT entities_fk_3 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id) ON DELETE CASCADE;


--
-- Name: entity_entity_grant_permissions entity_entity_grant_permissions_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_entity_grant_permissions
    ADD CONSTRAINT entity_entity_grant_permissions_fk_1 FOREIGN KEY (entity_entity_grants_id) REFERENCES public.entity_entity_grants(id) ON DELETE CASCADE;


--
-- Name: entity_entity_grant_permissions entity_entity_grant_permissions_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_entity_grant_permissions
    ADD CONSTRAINT entity_entity_grant_permissions_fk_2 FOREIGN KEY (entity_type_permissions_id) REFERENCES public.entity_type_permissions(id) ON DELETE CASCADE;


--
-- Name: entity_entity_grants entity_entity_grants_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_entity_grants
    ADD CONSTRAINT entity_entity_grants_fk_1 FOREIGN KEY (recipient_id) REFERENCES public.entities(id) ON DELETE CASCADE;


--
-- Name: entity_entity_grants entity_entity_grants_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_entity_grants
    ADD CONSTRAINT entity_entity_grants_fk_2 FOREIGN KEY (target_id) REFERENCES public.entities(id) ON DELETE CASCADE;


--
-- Name: entity_type_permissions entity_type_permissions_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_type_permissions
    ADD CONSTRAINT entity_type_permissions_fk_1 FOREIGN KEY (entity_types_id) REFERENCES public.entity_types(id) ON DELETE CASCADE;


--
-- Name: entity_types entity_types_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_types
    ADD CONSTRAINT entity_types_fk_1 FOREIGN KEY (access_token_signing_keys_id) REFERENCES public.keys(id);


--
-- Name: entity_user_grant_permissions entity_user_grant_permissions_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_user_grant_permissions
    ADD CONSTRAINT entity_user_grant_permissions_fk_1 FOREIGN KEY (entity_user_grants_id) REFERENCES public.entity_user_grants(id) ON DELETE CASCADE;


--
-- Name: entity_user_grant_permissions entity_user_grant_permissions_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_user_grant_permissions
    ADD CONSTRAINT entity_user_grant_permissions_fk_2 FOREIGN KEY (entity_type_permissions_id) REFERENCES public.entity_type_permissions(id) ON DELETE CASCADE;


--
-- Name: entity_user_grants entity_user_grants_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_user_grants
    ADD CONSTRAINT entity_user_grants_fk_1 FOREIGN KEY (entities_id) REFERENCES public.entities(id) ON DELETE CASCADE;


--
-- Name: entity_user_grants entity_user_grants_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.entity_user_grants
    ADD CONSTRAINT entity_user_grants_fk_2 FOREIGN KEY (users_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: external_identifiers external_identifiers_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.external_identifiers
    ADD CONSTRAINT external_identifiers_fk_1 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: external_identifiers external_identifiers_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.external_identifiers
    ADD CONSTRAINT external_identifiers_fk_2 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: external_identifiers external_identifiers_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.external_identifiers
    ADD CONSTRAINT external_identifiers_fk_3 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: families families_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.families
    ADD CONSTRAINT families_fk_1 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: federated_domains federated_domains_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.federated_domains
    ADD CONSTRAINT federated_domains_fk_1 FOREIGN KEY (identity_providers_id) REFERENCES public.identity_providers(id);


--
-- Name: form_fields form_fields_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.form_fields
    ADD CONSTRAINT form_fields_fk_1 FOREIGN KEY (consents_id) REFERENCES public.consents(id);


--
-- Name: form_steps form_steps_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.form_steps
    ADD CONSTRAINT form_steps_fk_1 FOREIGN KEY (forms_id) REFERENCES public.forms(id);


--
-- Name: form_steps form_steps_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.form_steps
    ADD CONSTRAINT form_steps_fk_2 FOREIGN KEY (form_fields_id) REFERENCES public.form_fields(id);


--
-- Name: group_application_roles group_application_roles_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.group_application_roles
    ADD CONSTRAINT group_application_roles_fk_1 FOREIGN KEY (groups_id) REFERENCES public.groups(id);


--
-- Name: group_application_roles group_application_roles_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.group_application_roles
    ADD CONSTRAINT group_application_roles_fk_2 FOREIGN KEY (application_roles_id) REFERENCES public.application_roles(id);


--
-- Name: group_members group_members_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_fk_1 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: group_members group_members_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_fk_2 FOREIGN KEY (groups_id) REFERENCES public.groups(id);


--
-- Name: groups groups_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_fk_1 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: hourly_logins hourly_logins_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.hourly_logins
    ADD CONSTRAINT hourly_logins_fk_1 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: identities identities_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identities
    ADD CONSTRAINT identities_fk_1 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: identities identities_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identities
    ADD CONSTRAINT identities_fk_2 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: identity_provider_links identity_provider_links_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_provider_links
    ADD CONSTRAINT identity_provider_links_fk_1 FOREIGN KEY (identity_providers_id) REFERENCES public.identity_providers(id);


--
-- Name: identity_provider_links identity_provider_links_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_provider_links
    ADD CONSTRAINT identity_provider_links_fk_2 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: identity_provider_links identity_provider_links_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_provider_links
    ADD CONSTRAINT identity_provider_links_fk_3 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: identity_providers_applications identity_providers_applications_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_providers_applications
    ADD CONSTRAINT identity_providers_applications_fk_1 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: identity_providers_applications identity_providers_applications_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_providers_applications
    ADD CONSTRAINT identity_providers_applications_fk_2 FOREIGN KEY (identity_providers_id) REFERENCES public.identity_providers(id);


--
-- Name: identity_providers_applications identity_providers_applications_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_providers_applications
    ADD CONSTRAINT identity_providers_applications_fk_3 FOREIGN KEY (keys_id) REFERENCES public.keys(id);


--
-- Name: identity_providers identity_providers_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_providers
    ADD CONSTRAINT identity_providers_fk_1 FOREIGN KEY (keys_id) REFERENCES public.keys(id);


--
-- Name: identity_providers identity_providers_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_providers
    ADD CONSTRAINT identity_providers_fk_2 FOREIGN KEY (reconcile_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: identity_providers identity_providers_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_providers
    ADD CONSTRAINT identity_providers_fk_3 FOREIGN KEY (request_signing_keys_id) REFERENCES public.keys(id);


--
-- Name: identity_providers_tenants identity_providers_tenants_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_providers_tenants
    ADD CONSTRAINT identity_providers_tenants_fk_1 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: identity_providers_tenants identity_providers_tenants_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.identity_providers_tenants
    ADD CONSTRAINT identity_providers_tenants_fk_2 FOREIGN KEY (identity_providers_id) REFERENCES public.identity_providers(id);


--
-- Name: previous_passwords previous_passwords_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.previous_passwords
    ADD CONSTRAINT previous_passwords_fk_1 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: raw_logins raw_logins_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.raw_logins
    ADD CONSTRAINT raw_logins_fk_1 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: raw_logins raw_logins_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.raw_logins
    ADD CONSTRAINT raw_logins_fk_2 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: refresh_tokens refresh_tokens_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_fk_1 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: refresh_tokens refresh_tokens_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_fk_2 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: refresh_tokens refresh_tokens_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_fk_3 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: request_frequencies request_frequencies_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.request_frequencies
    ADD CONSTRAINT request_frequencies_fk_1 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: scim_external_id_groups scim_external_id_groups_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.scim_external_id_groups
    ADD CONSTRAINT scim_external_id_groups_fk_1 FOREIGN KEY (entities_id) REFERENCES public.entities(id);


--
-- Name: scim_external_id_groups scim_external_id_groups_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.scim_external_id_groups
    ADD CONSTRAINT scim_external_id_groups_fk_2 FOREIGN KEY (groups_id) REFERENCES public.groups(id);


--
-- Name: scim_external_id_users scim_external_id_users_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.scim_external_id_users
    ADD CONSTRAINT scim_external_id_users_fk_1 FOREIGN KEY (entities_id) REFERENCES public.entities(id);


--
-- Name: scim_external_id_users scim_external_id_users_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.scim_external_id_users
    ADD CONSTRAINT scim_external_id_users_fk_2 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: tenants tenants_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_1 FOREIGN KEY (forgot_password_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_10; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_10 FOREIGN KEY (access_token_signing_keys_id) REFERENCES public.keys(id);


--
-- Name: tenants tenants_fk_11; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_11 FOREIGN KEY (id_token_signing_keys_id) REFERENCES public.keys(id);


--
-- Name: tenants tenants_fk_12; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_12 FOREIGN KEY (admin_user_forms_id) REFERENCES public.forms(id);


--
-- Name: tenants tenants_fk_13; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_13 FOREIGN KEY (multi_factor_email_message_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_14; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_14 FOREIGN KEY (multi_factor_sms_message_templates_id) REFERENCES public.message_templates(id);


--
-- Name: tenants tenants_fk_15; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_15 FOREIGN KEY (multi_factor_sms_messengers_id) REFERENCES public.messengers(id);


--
-- Name: tenants tenants_fk_16; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_16 FOREIGN KEY (client_credentials_access_token_populate_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: tenants tenants_fk_17; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_17 FOREIGN KEY (email_update_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_18; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_18 FOREIGN KEY (email_verified_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_19; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_19 FOREIGN KEY (login_id_in_use_on_create_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_2 FOREIGN KEY (set_password_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_20; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_20 FOREIGN KEY (login_id_in_use_on_update_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_21; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_21 FOREIGN KEY (login_new_device_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_22; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_22 FOREIGN KEY (login_suspicious_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_23; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_23 FOREIGN KEY (password_reset_success_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_24; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_24 FOREIGN KEY (password_update_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_25; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_25 FOREIGN KEY (two_factor_method_add_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_26; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_26 FOREIGN KEY (two_factor_method_remove_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_27; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_27 FOREIGN KEY (ui_ip_access_control_lists_id) REFERENCES public.ip_access_control_lists(id);


--
-- Name: tenants tenants_fk_28; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_28 FOREIGN KEY (scim_client_entity_types_id) REFERENCES public.entity_types(id);


--
-- Name: tenants tenants_fk_29; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_29 FOREIGN KEY (scim_enterprise_user_request_converter_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: tenants tenants_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_3 FOREIGN KEY (verification_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_30; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_30 FOREIGN KEY (scim_enterprise_user_response_converter_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: tenants tenants_fk_31; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_31 FOREIGN KEY (scim_group_request_converter_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: tenants tenants_fk_32; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_32 FOREIGN KEY (scim_group_response_converter_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: tenants tenants_fk_33; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_33 FOREIGN KEY (scim_server_entity_types_id) REFERENCES public.entity_types(id);


--
-- Name: tenants tenants_fk_34; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_34 FOREIGN KEY (scim_user_request_converter_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: tenants tenants_fk_35; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_35 FOREIGN KEY (scim_user_response_converter_lambdas_id) REFERENCES public.lambdas(id);


--
-- Name: tenants tenants_fk_4; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_4 FOREIGN KEY (passwordless_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_5; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_5 FOREIGN KEY (confirm_child_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_6; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_6 FOREIGN KEY (family_request_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_7; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_7 FOREIGN KEY (parent_registration_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: tenants tenants_fk_8; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_8 FOREIGN KEY (failed_authentication_user_actions_id) REFERENCES public.user_actions(id);


--
-- Name: tenants tenants_fk_9; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_fk_9 FOREIGN KEY (themes_id) REFERENCES public.themes(id);


--
-- Name: user_action_logs_applications user_action_logs_applications_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_action_logs_applications
    ADD CONSTRAINT user_action_logs_applications_fk_1 FOREIGN KEY (applications_id) REFERENCES public.applications(id) ON DELETE CASCADE;


--
-- Name: user_action_logs_applications user_action_logs_applications_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_action_logs_applications
    ADD CONSTRAINT user_action_logs_applications_fk_2 FOREIGN KEY (user_action_logs_id) REFERENCES public.user_action_logs(id) ON DELETE CASCADE;


--
-- Name: user_action_logs user_action_logs_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_action_logs
    ADD CONSTRAINT user_action_logs_fk_1 FOREIGN KEY (actioner_users_id) REFERENCES public.users(id);


--
-- Name: user_action_logs user_action_logs_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_action_logs
    ADD CONSTRAINT user_action_logs_fk_2 FOREIGN KEY (actionee_users_id) REFERENCES public.users(id);


--
-- Name: user_action_logs user_action_logs_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_action_logs
    ADD CONSTRAINT user_action_logs_fk_3 FOREIGN KEY (user_actions_id) REFERENCES public.user_actions(id);


--
-- Name: user_actions user_actions_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_actions
    ADD CONSTRAINT user_actions_fk_1 FOREIGN KEY (cancel_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: user_actions user_actions_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_actions
    ADD CONSTRAINT user_actions_fk_2 FOREIGN KEY (end_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: user_actions user_actions_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_actions
    ADD CONSTRAINT user_actions_fk_3 FOREIGN KEY (modify_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: user_actions user_actions_fk_4; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_actions
    ADD CONSTRAINT user_actions_fk_4 FOREIGN KEY (start_email_templates_id) REFERENCES public.email_templates(id);


--
-- Name: user_comments user_comments_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_comments
    ADD CONSTRAINT user_comments_fk_1 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: user_comments user_comments_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_comments
    ADD CONSTRAINT user_comments_fk_2 FOREIGN KEY (commenter_id) REFERENCES public.users(id);


--
-- Name: user_consents_email_plus user_consents_email_plus_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_consents_email_plus
    ADD CONSTRAINT user_consents_email_plus_fk_1 FOREIGN KEY (user_consents_id) REFERENCES public.user_consents(id);


--
-- Name: user_consents user_consents_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_consents
    ADD CONSTRAINT user_consents_fk_1 FOREIGN KEY (consents_id) REFERENCES public.consents(id);


--
-- Name: user_consents user_consents_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_consents
    ADD CONSTRAINT user_consents_fk_2 FOREIGN KEY (giver_users_id) REFERENCES public.users(id);


--
-- Name: user_consents user_consents_fk_3; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_consents
    ADD CONSTRAINT user_consents_fk_3 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: user_registrations_application_roles user_registrations_application_roles_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_registrations_application_roles
    ADD CONSTRAINT user_registrations_application_roles_fk_1 FOREIGN KEY (user_registrations_id) REFERENCES public.user_registrations(id);


--
-- Name: user_registrations_application_roles user_registrations_application_roles_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_registrations_application_roles
    ADD CONSTRAINT user_registrations_application_roles_fk_2 FOREIGN KEY (application_roles_id) REFERENCES public.application_roles(id);


--
-- Name: user_registrations user_registrations_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_registrations
    ADD CONSTRAINT user_registrations_fk_1 FOREIGN KEY (applications_id) REFERENCES public.applications(id);


--
-- Name: user_registrations user_registrations_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.user_registrations
    ADD CONSTRAINT user_registrations_fk_2 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: users users_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_fk_1 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: webauthn_credentials webauthn_credentials_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.webauthn_credentials
    ADD CONSTRAINT webauthn_credentials_fk_1 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: webauthn_credentials webauthn_credentials_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.webauthn_credentials
    ADD CONSTRAINT webauthn_credentials_fk_2 FOREIGN KEY (users_id) REFERENCES public.users(id);


--
-- Name: webhooks_tenants webhooks_tenants_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.webhooks_tenants
    ADD CONSTRAINT webhooks_tenants_fk_1 FOREIGN KEY (webhooks_id) REFERENCES public.webhooks(id);


--
-- Name: webhooks_tenants webhooks_tenants_fk_2; Type: FK CONSTRAINT; Schema: public; Owner: fusionauth
--

ALTER TABLE ONLY public.webhooks_tenants
    ADD CONSTRAINT webhooks_tenants_fk_2 FOREIGN KEY (tenants_id) REFERENCES public.tenants(id);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

--
-- Database "postgres" dump
--

\connect postgres

--
-- PostgreSQL database dump
--

-- Dumped from database version 12.9 (Debian 12.9-1.pgdg110+1)
-- Dumped by pg_dump version 15.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

--
-- PostgreSQL database cluster dump complete
--

