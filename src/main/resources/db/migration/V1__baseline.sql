-- Baseline schema migration.
-- Generated from the live skultem database (pg_dump --schema-only) on 2026-08-16 to establish
-- the Flyway baseline for the schema previously managed by Hibernate's ddl-auto=update.


CREATE TABLE public.academic_years (
    id character varying(255) NOT NULL,
    active boolean,
    created_at timestamp(6) with time zone,
    end_date date NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    start_date date NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT academic_years_status_check CHECK (((status)::text = ANY ((ARRAY['OPENED'::character varying, 'CLOSED'::character varying, 'DELETED'::character varying])::text[])))
);


--
-- Name: activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.activities (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    meta character varying(255),
    reference_id character varying(255),
    school_id character varying(255) NOT NULL,
    subject character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT activities_type_check CHECK (((type)::text = ANY (ARRAY[('STUDENT'::character varying)::text, ('TEACHER'::character varying)::text, ('PARENT'::character varying)::text, ('USER'::character varying)::text, ('CLASS'::character varying)::text, ('SUBJECT'::character varying)::text, ('GRADE'::character varying)::text, ('FEES'::character varying)::text, ('EXPENSE'::character varying)::text, ('PAYMENT'::character varying)::text, ('SCHOOL'::character varying)::text, ('SETTINGS'::character varying)::text, ('SUPPLY'::character varying)::text])))
);


--
-- Name: assessment_approval_request; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assessment_approval_request (
    id character varying(255) NOT NULL,
    approval_note text,
    created_at timestamp(6) with time zone,
    return_reason text,
    school_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    teacher_note text,
    updated_at timestamp(6) with time zone,
    class_subject_assessment_life_cycle_id character varying(255) NOT NULL,
    class_master_id character varying(255) NOT NULL,
    teacher_subject_id character varying(255) NOT NULL,
    term_id character varying(255) NOT NULL,
    CONSTRAINT assessment_approval_request_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_REVIEW'::character varying, 'RETURNED'::character varying, 'APPROVED'::character varying])::text[])))
);


--
-- Name: assessment_scores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assessment_scores (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    score integer NOT NULL,
    updated_at timestamp(6) with time zone,
    weight integer NOT NULL,
    class_subject_assessment_life_cycle_id character varying(255) NOT NULL,
    student_assessment_id character varying(255) NOT NULL
);


--
-- Name: assessment_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assessment_templates (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255),
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    pass_mark integer DEFAULT 50
);


--
-- Name: assessments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assessments (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    name character varying(255) NOT NULL,
    "position" integer NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    weight integer NOT NULL,
    template_id character varying(255) NOT NULL
);


--
-- Name: attendance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attendance (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    date date NOT NULL,
    excused boolean NOT NULL,
    holiday boolean NOT NULL,
    late boolean NOT NULL,
    present boolean NOT NULL,
    reason character varying(255),
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    enrollment_id character varying(255) NOT NULL
);


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id character varying(255) NOT NULL,
    action character varying(255) NOT NULL,
    browser character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    details text NOT NULL,
    device character varying(255) NOT NULL,
    device_type character varying(255) NOT NULL,
    ip_address character varying(255) NOT NULL,
    os character varying(255) NOT NULL,
    school_id character varying(255),
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    academic_year_id character varying(255),
    user_id character varying(255),
    CONSTRAINT audit_logs_status_check CHECK (((status)::text = ANY ((ARRAY['SUCCESS'::character varying, 'FAILURE'::character varying])::text[])))
);


--
-- Name: behaviour_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.behaviour_categories (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: behaviours; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.behaviours (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    kind character varying(255) NOT NULL,
    note character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    behaviour_category_id character varying(255) NOT NULL,
    enrollment_id character varying(255) NOT NULL,
    CONSTRAINT behaviours_kind_check CHECK (((kind)::text = ANY ((ARRAY['POSITIVE'::character varying, 'NEGATIVE'::character varying, 'NEUTRAL'::character varying])::text[])))
);


--
-- Name: class_masters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_masters (
    id character varying(255) NOT NULL,
    assigned_at timestamp(6) with time zone NOT NULL,
    created_at timestamp(6) with time zone,
    ended_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    class_session_id character varying(255) NOT NULL,
    teacher_id character varying(255) NOT NULL
);


--
-- Name: class_sections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_sections (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    class_id character varying(255) NOT NULL,
    section_id character varying(255) NOT NULL
);


--
-- Name: class_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_sessions (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    academic_year_id character varying(255) NOT NULL,
    class_id character varying(255) NOT NULL,
    section_id character varying(255) NOT NULL,
    stream_id character varying(255)
);


--
-- Name: class_streams; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_streams (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    class_id character varying(255) NOT NULL,
    stream_id character varying(255) NOT NULL
);


--
-- Name: class_subject_assessment_life_cycle; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_subject_assessment_life_cycle (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    assessment_id character varying(255) NOT NULL,
    teacher_subject_id character varying(255) NOT NULL,
    term_id character varying(255) NOT NULL,
    CONSTRAINT class_subject_assessment_life_cycle_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'RETURNED'::character varying, 'APPROVED'::character varying, 'COMPLETED'::character varying, 'LOCKED'::character varying])::text[])))
);


--
-- Name: class_subjects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.class_subjects (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    locked boolean,
    mandatory boolean,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    class_id character varying(255) NOT NULL,
    subject_group_id character varying(255),
    subject_id character varying(255) NOT NULL
);


--
-- Name: classes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.classes (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    level character varying(255) NOT NULL,
    level_order integer NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    terminal boolean NOT NULL,
    updated_at timestamp(6) with time zone,
    next_class character varying(255),
    assessment_template_id character varying(255),
    CONSTRAINT classes_level_check CHECK (((level)::text = ANY ((ARRAY['PRIMARY'::character varying, 'JSS'::character varying, 'SSS'::character varying])::text[]))),
    CONSTRAINT classes_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DELETED'::character varying])::text[])))
);


--
-- Name: enrollment_subjcts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enrollment_subjcts (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    enrollment_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    subject_id character varying(255) NOT NULL
);


--
-- Name: enrollments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enrollments (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    academic_year_id character varying(255) NOT NULL,
    class_id character varying(255) NOT NULL,
    section_id character varying(255) NOT NULL,
    stream_id character varying(255),
    student_id character varying(255) NOT NULL,
    CONSTRAINT enrollments_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'PROMOTED'::character varying, 'REPEATED'::character varying, 'LEFT'::character varying])::text[])))
);


--
-- Name: expense_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expense_categories (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    dscription character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: expenses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expenses (
    id character varying(255) NOT NULL,
    amount numeric(38,2) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    category_id character varying(255) NOT NULL
);


--
-- Name: fee_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_categories (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    dscription character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: fee_discounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_discounts (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    expiry_date date,
    kind character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    reason character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    value numeric(38,2),
    enrollment_id character varying(255) NOT NULL,
    fee_id character varying(255) NOT NULL,
    CONSTRAINT fee_discounts_kind_check CHECK (((kind)::text = ANY ((ARRAY['PERCENTAGE'::character varying, 'AMOUNT'::character varying])::text[])))
);


--
-- Name: fee_structures; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_structures (
    id character varying(255) NOT NULL,
    allow_installment boolean NOT NULL,
    amount numeric(38,2) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255),
    due_date date NOT NULL,
    has_supply boolean NOT NULL,
    school_id character varying(255) NOT NULL,
    total_supply integer NOT NULL,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    academic_year_id character varying(255) NOT NULL,
    category_id character varying(255) NOT NULL,
    class_id character varying(255),
    term_id character varying(255) NOT NULL,
    material_id character varying(255),
    CONSTRAINT fee_structures_type_check CHECK (((type)::text = ANY ((ARRAY['ALL'::character varying, 'SELECTION'::character varying, 'CLASS'::character varying])::text[])))
);


--
-- Name: holidays; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.holidays (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    date date NOT NULL,
    fixed boolean NOT NULL,
    kind character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    academic_year_id character varying(255) NOT NULL,
    CONSTRAINT holidays_kind_check CHECK (((kind)::text = ANY ((ARRAY['PUBLIC'::character varying, 'SCHOOL'::character varying])::text[])))
);


--
-- Name: houses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.houses (
    id character varying(255) NOT NULL,
    color character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    motto character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT houses_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'DELETED'::character varying])::text[])))
);


--
-- Name: houses_house_masters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.houses_house_masters (
    house_entity_id character varying(255) NOT NULL,
    house_masters_id character varying(255) NOT NULL
);


--
-- Name: lessons; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lessons (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    lesson character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    week_id character varying(255) NOT NULL,
    CONSTRAINT lessons_state_check CHECK (((state)::text = ANY ((ARRAY['NOT_STARTED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying])::text[])))
);


--
-- Name: material_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.material_categories (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: material_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.material_transactions (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    direction character varying(255) NOT NULL,
    note character varying(255),
    qty integer NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    material_id character varying(255) NOT NULL,
    CONSTRAINT material_transactions_direction_check CHECK (((direction)::text = ANY ((ARRAY['IN'::character varying, 'OUT'::character varying])::text[])))
);


--
-- Name: materials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.materials (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    name character varying(255) NOT NULL,
    reorder_level integer NOT NULL,
    school_id character varying(255) NOT NULL,
    stock_quantity numeric(38,0) NOT NULL,
    unit character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    category_id character varying(255) NOT NULL,
    last_restocked_at timestamp(6) with time zone,
    note character varying(255),
    CONSTRAINT materials_unit_check CHECK (((unit)::text = ANY ((ARRAY['PCS'::character varying, 'BOX'::character varying, 'PACK'::character varying, 'LITRE'::character varying])::text[])))
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    message text NOT NULL,
    meta jsonb NOT NULL,
    priority character varying(255) NOT NULL,
    read boolean NOT NULL,
    school_id character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id character varying(255) NOT NULL,
    CONSTRAINT notifications_priority_check CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'NORMAL'::character varying, 'HIGH'::character varying, 'URGENT'::character varying])::text[]))),
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['ATTENDANCE'::character varying, 'ANNOUNCEMENT'::character varying, 'REMINDER'::character varying, 'BEHAVIOUR'::character varying, 'FEE'::character varying, 'ASSESSMENT'::character varying])::text[])))
);


--
-- Name: parents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.parents (
    id character varying(255) NOT NULL,
    city character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    phone character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    street character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id character varying(255) NOT NULL,
    CONSTRAINT parents_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'DELETED'::character varying])::text[])))
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id character varying(255) NOT NULL,
    amount numeric(38,2) NOT NULL,
    created_at timestamp(6) with time zone,
    method character varying(255) NOT NULL,
    note character varying(255),
    paid_at timestamp(6) with time zone NOT NULL,
    reference_no character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    fee_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    CONSTRAINT payments_method_check CHECK (((method)::text = ANY ((ARRAY['CASH'::character varying, 'BANK'::character varying, 'MOBILE_MONEY'::character varying])::text[])))
);


--
-- Name: periods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.periods (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    end_time time(0) without time zone NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    start_time time(0) without time zone NOT NULL,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    session_id character varying(255) NOT NULL,
    CONSTRAINT periods_type_check CHECK (((type)::text = ANY ((ARRAY['PERIOD'::character varying, 'BREAK'::character varying, 'LUNCH'::character varying])::text[])))
);


--
-- Name: reference_sequences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reference_sequences (
    reference_type character varying(255) NOT NULL,
    year integer NOT NULL,
    last_number integer NOT NULL
);


--
-- Name: request_demos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.request_demos (
    id uuid NOT NULL,
    address character varying(255) NOT NULL,
    city character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    email character varying(255) NOT NULL,
    message character varying(255),
    name character varying(255) NOT NULL,
    phone character varying(255) NOT NULL,
    preferred character varying(255) NOT NULL,
    priority character varying(255) NOT NULL,
    school character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: rooms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rooms (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    no character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    soft_delete boolean NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: save_report; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.save_report (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    entity character varying(255),
    filters text,
    name character varying(255),
    school_id character varying(255),
    updated_at timestamp(6) with time zone
);


--
-- Name: scheme_of_works; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scheme_of_works (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    weeks bigint NOT NULL,
    session_id character varying(255) NOT NULL,
    subject_id character varying(255) NOT NULL,
    term_id character varying(255) NOT NULL,
    CONSTRAINT scheme_of_works_state_check CHECK (((state)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISH'::character varying])::text[])))
);


--
-- Name: school_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.school_users (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    role character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id character varying(255) NOT NULL,
    CONSTRAINT school_users_role_check CHECK (((role)::text = ANY (ARRAY[('SYSTEM_ADMIN'::character varying)::text, ('PROPRIETOR'::character varying)::text, ('ADMIN'::character varying)::text, ('ACCOUNTANT'::character varying)::text, ('TEACHER'::character varying)::text, ('PARENT'::character varying)::text, ('OWNER'::character varying)::text]))),
    CONSTRAINT school_users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RESET_PASSWORD'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: schools; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schools (
    id character varying(255) NOT NULL,
    address jsonb,
    created_at timestamp(6) with time zone,
    domain character varying(255) NOT NULL,
    grading_scale jsonb,
    name character varying(255) NOT NULL,
    owner jsonb,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT schools_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'DELETED'::character varying])::text[])))
);


--
-- Name: sections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sections (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255),
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: stream_subjects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stream_subjects (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    locked boolean NOT NULL,
    mandatory boolean,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    subject_group_id character varying(255),
    stream_id character varying(255) NOT NULL,
    subject_id character varying(255) NOT NULL
);


--
-- Name: streams; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.streams (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: student_assessments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_assessments (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    enrollment_id character varying(255) NOT NULL,
    teacher_subject_id character varying(255),
    term_id character varying(255)
);


--
-- Name: student_fees; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_fees (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    discount_id character varying(255),
    enrollment_id character varying(255) NOT NULL,
    fee_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL
);


--
-- Name: student_ledger_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_ledger_entries (
    id character varying(255) NOT NULL,
    academic_year_id character varying(255) NOT NULL,
    amount numeric(38,2) NOT NULL,
    balance numeric(38,2) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255) NOT NULL,
    direction character varying(255) NOT NULL,
    paid_at timestamp(6) with time zone NOT NULL,
    reference_id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    term_id character varying(255) NOT NULL,
    transaction_type character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT student_ledger_entries_direction_check CHECK (((direction)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT'::character varying])::text[]))),
    CONSTRAINT student_ledger_entries_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['FEE_ASSINMENT'::character varying, 'PAYMENT'::character varying, 'DISCOUNT'::character varying, 'REFUND'::character varying, 'ADJUSTMENT'::character varying])::text[])))
);


--
-- Name: student_parents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_parents (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    relationship character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    parent_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    CONSTRAINT student_parents_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DELETED'::character varying])::text[])))
);


--
-- Name: students; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.students (
    id character varying(255) NOT NULL,
    admission_date date,
    admission_number character varying(255) NOT NULL,
    city character varying(255),
    created_at timestamp(6) with time zone,
    date_of_birth date,
    enrollment_type character varying(255) NOT NULL,
    family jsonb,
    family_name character varying(255) NOT NULL,
    gender character varying(255) NOT NULL,
    given_names character varying(255),
    last_class character varying(255),
    nationality character varying(255),
    photo character varying(255),
    previous_school character varying(255),
    religion character varying(255),
    school_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    street character varying(255),
    updated_at timestamp(6) with time zone,
    house_id character varying(255),
    parent_id character varying(255) NOT NULL,
    session_id character varying(255) NOT NULL,
    CONSTRAINT students_enrollment_type_check CHECK (((enrollment_type)::text = ANY ((ARRAY['NEW'::character varying, 'TRANSFER'::character varying, 'RE_ENROLLMENT'::character varying])::text[]))),
    CONSTRAINT students_gender_check CHECK (((gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying])::text[]))),
    CONSTRAINT students_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'GRADUATED'::character varying, 'TRANSFERRED'::character varying, 'SUSPENDED'::character varying, 'DELETED'::character varying])::text[])))
);


--
-- Name: subject_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subject_groups (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    total_selection integer NOT NULL,
    updated_at timestamp(6) with time zone,
    class_id character varying(255),
    stream_id character varying(255)
);


--
-- Name: subjects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subjects (
    id character varying(255) NOT NULL,
    code character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: supplies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.supplies (
    id character varying(255) NOT NULL,
    collected_on timestamp(6) with time zone,
    collected_qty integer NOT NULL,
    created_at timestamp(6) with time zone,
    qty integer NOT NULL,
    school_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    material_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    CONSTRAINT supplies_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PARTIAL'::character varying, 'COLLECTED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: teacher_subjects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.teacher_subjects (
    id character varying(255) NOT NULL,
    assigned_at timestamp(6) with time zone NOT NULL,
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    class_session_id character varying(255) NOT NULL,
    subject_id character varying(255) NOT NULL,
    teacher_id character varying(255) NOT NULL
);


--
-- Name: teachers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.teachers (
    id character varying(255) NOT NULL,
    city character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    gender character varying(255) NOT NULL,
    phone character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    staff_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    street character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id character varying(255) NOT NULL,
    CONSTRAINT teachers_gender_check CHECK (((gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying])::text[]))),
    CONSTRAINT teachers_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'DELETED'::character varying])::text[]))),
    CONSTRAINT teachers_title_check CHECK (((title)::text = ANY ((ARRAY['MR'::character varying, 'MRS'::character varying, 'MISS'::character varying, 'MS'::character varying, 'DR'::character varying, 'PROF'::character varying, 'REV'::character varying, 'HON'::character varying, 'ENG'::character varying, 'SIR'::character varying, 'MADAM'::character varying])::text[])))
);


--
-- Name: terms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terms (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    end_date date NOT NULL,
    name character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    start_date date NOT NULL,
    status character varying(255) NOT NULL,
    term_number integer NOT NULL,
    updated_at timestamp(6) with time zone,
    academic_year_id character varying(255) NOT NULL,
    CONSTRAINT terms_status_check CHECK (((status)::text = ANY ((ARRAY['UPCOMING'::character varying, 'ACTIVE'::character varying, 'CLOSED'::character varying])::text[])))
);


--
-- Name: timetables; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.timetables (
    id character varying(255) NOT NULL,
    color character varying(255),
    created_at timestamp(6) with time zone,
    school_id character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    working_day_id character varying(255) NOT NULL,
    period_id character varying(255) NOT NULL,
    room_id character varying(255) NOT NULL,
    teacher_subject_id character varying(255) NOT NULL
);


--
-- Name: timings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.timings (
    id character varying(255) NOT NULL,
    break_duration integer NOT NULL,
    created_at timestamp(6) with time zone,
    end_time time(0) without time zone NOT NULL,
    lunch_duration integer NOT NULL,
    period_duration integer NOT NULL,
    school_id character varying(255) NOT NULL,
    start_time time(0) without time zone NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transactions (
    id character varying(255) NOT NULL,
    amount numeric(38,2) NOT NULL,
    balance numeric(38,2) NOT NULL,
    created_at timestamp(6) with time zone,
    direction character varying(255) NOT NULL,
    reference_id character varying(255) NOT NULL,
    reference_type character varying(255),
    school_id character varying(255) NOT NULL,
    transaction_type character varying(255) NOT NULL,
    academic_year_id character varying(255) NOT NULL,
    term_id character varying(255) NOT NULL,
    CONSTRAINT transactions_direction_check CHECK (((direction)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT'::character varying])::text[]))),
    CONSTRAINT transactions_reference_type_check CHECK (((reference_type)::text = ANY ((ARRAY['STUDENT'::character varying, 'EXPENSE'::character varying, 'SYSTEM'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT transactions_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['FEE_ASSIGNMENT'::character varying, 'PAYMENT'::character varying, 'DISCOUNT'::character varying, 'REFUND'::character varying, 'EXPENSE'::character varying, 'ADJUSTMENT'::character varying])::text[])))
);


--
-- Name: user_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_sessions (
    id character varying(255) NOT NULL,
    active boolean NOT NULL,
    browser character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    device character varying(255) NOT NULL,
    device_type character varying(255) NOT NULL,
    ip_address character varying(255) NOT NULL,
    os character varying(255) NOT NULL,
    school_id character varying(255),
    updated_at timestamp(6) with time zone,
    user_agent character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    email character varying(255) NOT NULL,
    family_name character varying(255) NOT NULL,
    given_name character varying(255) NOT NULL,
    hint character varying(255),
    password character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RESET_PASSWORD'::character varying, 'INACTIVE'::character varying, 'DELETED'::character varying])::text[])))
);


--
-- Name: weeks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.weeks (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    objectives jsonb,
    school_id character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    subtopic character varying(255),
    topic character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    week integer NOT NULL,
    scheme_id character varying(255) NOT NULL,
    CONSTRAINT weeks_state_check CHECK (((state)::text = ANY ((ARRAY['NOT_STARTED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying])::text[])))
);


--
-- Name: working_days; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.working_days (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    day character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    timing_id character varying(255) NOT NULL,
    objectives jsonb,
    CONSTRAINT working_days_day_check CHECK (((day)::text = ANY ((ARRAY['MONDAY'::character varying, 'TUESDAY'::character varying, 'WEDNESDAY'::character varying, 'THURSDAY'::character varying, 'FRIDAY'::character varying, 'SATURDAY'::character varying, 'SUNDAY'::character varying])::text[])))
);


--
-- Name: academic_years academic_years_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_years
    ADD CONSTRAINT academic_years_pkey PRIMARY KEY (id);


--
-- Name: activities activities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activities
    ADD CONSTRAINT activities_pkey PRIMARY KEY (id);


--
-- Name: assessment_approval_request assessment_approval_request_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_approval_request
    ADD CONSTRAINT assessment_approval_request_pkey PRIMARY KEY (id);


--
-- Name: assessment_scores assessment_scores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_scores
    ADD CONSTRAINT assessment_scores_pkey PRIMARY KEY (id);


--
-- Name: assessment_templates assessment_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_templates
    ADD CONSTRAINT assessment_templates_pkey PRIMARY KEY (id);


--
-- Name: assessments assessments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessments
    ADD CONSTRAINT assessments_pkey PRIMARY KEY (id);


--
-- Name: attendance attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: behaviour_categories behaviour_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behaviour_categories
    ADD CONSTRAINT behaviour_categories_pkey PRIMARY KEY (id);


--
-- Name: behaviours behaviours_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behaviours
    ADD CONSTRAINT behaviours_pkey PRIMARY KEY (id);


--
-- Name: class_masters class_masters_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_masters
    ADD CONSTRAINT class_masters_pkey PRIMARY KEY (id);


--
-- Name: class_sections class_sections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_sections
    ADD CONSTRAINT class_sections_pkey PRIMARY KEY (id);


--
-- Name: class_sessions class_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_sessions
    ADD CONSTRAINT class_sessions_pkey PRIMARY KEY (id);


--
-- Name: class_streams class_streams_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_streams
    ADD CONSTRAINT class_streams_pkey PRIMARY KEY (id);


--
-- Name: class_subject_assessment_life_cycle class_subject_assessment_life_cycle_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subject_assessment_life_cycle
    ADD CONSTRAINT class_subject_assessment_life_cycle_pkey PRIMARY KEY (id);


--
-- Name: class_subjects class_subjects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subjects
    ADD CONSTRAINT class_subjects_pkey PRIMARY KEY (id);


--
-- Name: classes classes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT classes_pkey PRIMARY KEY (id);


--
-- Name: enrollment_subjcts enrollment_subjcts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollment_subjcts
    ADD CONSTRAINT enrollment_subjcts_pkey PRIMARY KEY (id);


--
-- Name: enrollments enrollments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT enrollments_pkey PRIMARY KEY (id);


--
-- Name: expense_categories expense_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_categories
    ADD CONSTRAINT expense_categories_pkey PRIMARY KEY (id);


--
-- Name: expenses expenses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (id);


--
-- Name: fee_categories fee_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_categories
    ADD CONSTRAINT fee_categories_pkey PRIMARY KEY (id);


--
-- Name: fee_discounts fee_discounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_discounts
    ADD CONSTRAINT fee_discounts_pkey PRIMARY KEY (id);


--
-- Name: fee_structures fee_structures_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures
    ADD CONSTRAINT fee_structures_pkey PRIMARY KEY (id);


--
-- Name: holidays holidays_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.holidays
    ADD CONSTRAINT holidays_pkey PRIMARY KEY (id);


--
-- Name: houses houses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.houses
    ADD CONSTRAINT houses_pkey PRIMARY KEY (id);


--
-- Name: lessons lessons_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lessons
    ADD CONSTRAINT lessons_pkey PRIMARY KEY (id);


--
-- Name: material_categories material_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_categories
    ADD CONSTRAINT material_categories_pkey PRIMARY KEY (id);


--
-- Name: material_transactions material_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_transactions
    ADD CONSTRAINT material_transactions_pkey PRIMARY KEY (id);


--
-- Name: materials materials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materials
    ADD CONSTRAINT materials_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: parents parents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parents
    ADD CONSTRAINT parents_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: periods periods_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periods
    ADD CONSTRAINT periods_pkey PRIMARY KEY (id);


--
-- Name: reference_sequences reference_sequences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reference_sequences
    ADD CONSTRAINT reference_sequences_pkey PRIMARY KEY (reference_type, year);


--
-- Name: request_demos request_demos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.request_demos
    ADD CONSTRAINT request_demos_pkey PRIMARY KEY (id);


--
-- Name: rooms rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT rooms_pkey PRIMARY KEY (id);


--
-- Name: save_report save_report_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.save_report
    ADD CONSTRAINT save_report_pkey PRIMARY KEY (id);


--
-- Name: scheme_of_works scheme_of_works_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheme_of_works
    ADD CONSTRAINT scheme_of_works_pkey PRIMARY KEY (id);


--
-- Name: school_users school_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.school_users
    ADD CONSTRAINT school_users_pkey PRIMARY KEY (id);


--
-- Name: schools schools_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schools
    ADD CONSTRAINT schools_pkey PRIMARY KEY (id);


--
-- Name: sections sections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sections
    ADD CONSTRAINT sections_pkey PRIMARY KEY (id);


--
-- Name: stream_subjects stream_subjects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stream_subjects
    ADD CONSTRAINT stream_subjects_pkey PRIMARY KEY (id);


--
-- Name: streams streams_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.streams
    ADD CONSTRAINT streams_pkey PRIMARY KEY (id);


--
-- Name: student_assessments student_assessments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_assessments
    ADD CONSTRAINT student_assessments_pkey PRIMARY KEY (id);


--
-- Name: student_fees student_fees_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_fees
    ADD CONSTRAINT student_fees_pkey PRIMARY KEY (id);


--
-- Name: student_ledger_entries student_ledger_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_ledger_entries
    ADD CONSTRAINT student_ledger_entries_pkey PRIMARY KEY (id);


--
-- Name: student_parents student_parents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_parents
    ADD CONSTRAINT student_parents_pkey PRIMARY KEY (id);


--
-- Name: students students_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_pkey PRIMARY KEY (id);


--
-- Name: subject_groups subject_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subject_groups
    ADD CONSTRAINT subject_groups_pkey PRIMARY KEY (id);


--
-- Name: subjects subjects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subjects
    ADD CONSTRAINT subjects_pkey PRIMARY KEY (id);


--
-- Name: supplies supplies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplies
    ADD CONSTRAINT supplies_pkey PRIMARY KEY (id);


--
-- Name: teacher_subjects teacher_subjects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_subjects
    ADD CONSTRAINT teacher_subjects_pkey PRIMARY KEY (id);


--
-- Name: teachers teachers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teachers
    ADD CONSTRAINT teachers_pkey PRIMARY KEY (id);


--
-- Name: terms terms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terms
    ADD CONSTRAINT terms_pkey PRIMARY KEY (id);


--
-- Name: timetables timetables_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timetables
    ADD CONSTRAINT timetables_pkey PRIMARY KEY (id);


--
-- Name: timings timings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timings
    ADD CONSTRAINT timings_pkey PRIMARY KEY (id);


--
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);


--
-- Name: classes uk9m9i8bqbc3wonw0j5nmfs1f4k; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT uk9m9i8bqbc3wonw0j5nmfs1f4k UNIQUE (next_class);


--
-- Name: user_sessions user_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT user_sessions_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: weeks weeks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weeks
    ADD CONSTRAINT weeks_pkey PRIMARY KEY (id);


--
-- Name: working_days working_days_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.working_days
    ADD CONSTRAINT working_days_pkey PRIMARY KEY (id);


--
-- Name: class_sessions fk1w2pxpii8b2dhn7pcylfpiygh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_sessions
    ADD CONSTRAINT fk1w2pxpii8b2dhn7pcylfpiygh FOREIGN KEY (class_id) REFERENCES public.classes(id);


--
-- Name: class_streams fk29h80fqklfghnht7p0tmm66w2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_streams
    ADD CONSTRAINT fk29h80fqklfghnht7p0tmm66w2 FOREIGN KEY (stream_id) REFERENCES public.streams(id);


--
-- Name: student_fees fk2aain98dlsyxpvpacg1y1q9wa; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_fees
    ADD CONSTRAINT fk2aain98dlsyxpvpacg1y1q9wa FOREIGN KEY (fee_id) REFERENCES public.fee_structures(id);


--
-- Name: students fk2uh7raxh0866jn0wdb6cef93t; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT fk2uh7raxh0866jn0wdb6cef93t FOREIGN KEY (session_id) REFERENCES public.class_sessions(id);


--
-- Name: enrollments fk3ytmm6myy6y07xf42obe22bog; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fk3ytmm6myy6y07xf42obe22bog FOREIGN KEY (stream_id) REFERENCES public.streams(id);


--
-- Name: class_sessions fk4ky5x12o633e675tsn5wo3p9w; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_sessions
    ADD CONSTRAINT fk4ky5x12o633e675tsn5wo3p9w FOREIGN KEY (section_id) REFERENCES public.sections(id);


--
-- Name: assessment_approval_request fk4wx15c67oh73ppwy66i3r2fc7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_approval_request
    ADD CONSTRAINT fk4wx15c67oh73ppwy66i3r2fc7 FOREIGN KEY (teacher_subject_id) REFERENCES public.teacher_subjects(id);


--
-- Name: supplies fk51h6lqo14yvub06mnr3e7se08; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplies
    ADD CONSTRAINT fk51h6lqo14yvub06mnr3e7se08 FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: fee_structures fk5cgvonnjlrpfixvs1dxwyngys; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures
    ADD CONSTRAINT fk5cgvonnjlrpfixvs1dxwyngys FOREIGN KEY (class_id) REFERENCES public.classes(id);


--
-- Name: scheme_of_works fk5id0cbl3gn4ni43h3y5by72hu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheme_of_works
    ADD CONSTRAINT fk5id0cbl3gn4ni43h3y5by72hu FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: payments fk5wsfxq9ps0ai3ubn9d60i2k84; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk5wsfxq9ps0ai3ubn9d60i2k84 FOREIGN KEY (fee_id) REFERENCES public.fee_structures(id);


--
-- Name: holidays fk624y9nnngs7a3cqnktfxqio85; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.holidays
    ADD CONSTRAINT fk624y9nnngs7a3cqnktfxqio85 FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id);


--
-- Name: teacher_subjects fk6dcl3ihufp4v0j1fuxlw4ksoj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_subjects
    ADD CONSTRAINT fk6dcl3ihufp4v0j1fuxlw4ksoj FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);


--
-- Name: periods fk6excuje3oa6g6hl2mob1ov784; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.periods
    ADD CONSTRAINT fk6excuje3oa6g6hl2mob1ov784 FOREIGN KEY (session_id) REFERENCES public.class_sessions(id);


--
-- Name: payments fk6ooq278k2bs5xi8t5o6oort1v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk6ooq278k2bs5xi8t5o6oort1v FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: student_fees fk6sd6675wn2j47vim7bvoksyr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_fees
    ADD CONSTRAINT fk6sd6675wn2j47vim7bvoksyr FOREIGN KEY (enrollment_id) REFERENCES public.enrollments(id);


--
-- Name: students fk7bbpphkk8f0aoav3iiih3mh4e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT fk7bbpphkk8f0aoav3iiih3mh4e FOREIGN KEY (parent_id) REFERENCES public.parents(id);


--
-- Name: assessment_approval_request fk8iae3jvmt390861o7o9acmeao; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_approval_request
    ADD CONSTRAINT fk8iae3jvmt390861o7o9acmeao FOREIGN KEY (class_subject_assessment_life_cycle_id) REFERENCES public.class_subject_assessment_life_cycle(id);


--
-- Name: enrollments fk8kf1u1857xgo56xbfmnif2c51; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fk8kf1u1857xgo56xbfmnif2c51 FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: user_sessions fk8klxsgb8dcjjklmqebqp1twd5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT fk8klxsgb8dcjjklmqebqp1twd5 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: timetables fk8pi3shd40crgfecetgy14fjp8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timetables
    ADD CONSTRAINT fk8pi3shd40crgfecetgy14fjp8 FOREIGN KEY (working_day_id) REFERENCES public.working_days(id);


--
-- Name: student_assessments fk93o914cvwhkjqydio55qukatd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_assessments
    ADD CONSTRAINT fk93o914cvwhkjqydio55qukatd FOREIGN KEY (enrollment_id) REFERENCES public.enrollments(id);


--
-- Name: timetables fk97bk05qoxlf34fpw5fl2d3kd9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timetables
    ADD CONSTRAINT fk97bk05qoxlf34fpw5fl2d3kd9 FOREIGN KEY (period_id) REFERENCES public.periods(id);


--
-- Name: school_users fk9cbo82usk5yvw60opiibvplf9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.school_users
    ADD CONSTRAINT fk9cbo82usk5yvw60opiibvplf9 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: fee_discounts fk9k8t5yj0ldh9cs86em5qvailg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_discounts
    ADD CONSTRAINT fk9k8t5yj0ldh9cs86em5qvailg FOREIGN KEY (fee_id) REFERENCES public.fee_structures(id);


--
-- Name: scheme_of_works fk9vgv62vkvge0jgh0ldtb6hxdl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheme_of_works
    ADD CONSTRAINT fk9vgv62vkvge0jgh0ldtb6hxdl FOREIGN KEY (term_id) REFERENCES public.terms(id);


--
-- Name: notifications fk9y21adhxn0ayjhfocscqox7bh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk9y21adhxn0ayjhfocscqox7bh FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: stream_subjects fkail0dd8sl4ntfxjpnb1392cyy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stream_subjects
    ADD CONSTRAINT fkail0dd8sl4ntfxjpnb1392cyy FOREIGN KEY (stream_id) REFERENCES public.streams(id);


--
-- Name: supplies fkas2jlyymb9ksyht0fge5w2157; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.supplies
    ADD CONSTRAINT fkas2jlyymb9ksyht0fge5w2157 FOREIGN KEY (material_id) REFERENCES public.materials(id);


--
-- Name: transactions fkb03s7n2asb4f4w332h65g7bru; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT fkb03s7n2asb4f4w332h65g7bru FOREIGN KEY (term_id) REFERENCES public.terms(id);


--
-- Name: teachers fkb8dct7w2j1vl1r2bpstw5isc0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teachers
    ADD CONSTRAINT fkb8dct7w2j1vl1r2bpstw5isc0 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: terms fkb9km6i28s7kp96njp5lunwrot; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terms
    ADD CONSTRAINT fkb9km6i28s7kp96njp5lunwrot FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id);


--
-- Name: stream_subjects fkbdbfnnvsahh1mb4cg3q1b3bmt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stream_subjects
    ADD CONSTRAINT fkbdbfnnvsahh1mb4cg3q1b3bmt FOREIGN KEY (subject_group_id) REFERENCES public.subject_groups(id);


--
-- Name: student_fees fkbotp74x1rs66lldvu21nnc1vk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_fees
    ADD CONSTRAINT fkbotp74x1rs66lldvu21nnc1vk FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: class_sections fkbx4cmdkfkbu0idn8o68bybcs8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_sections
    ADD CONSTRAINT fkbx4cmdkfkbu0idn8o68bybcs8 FOREIGN KEY (class_id) REFERENCES public.classes(id);


--
-- Name: parents fkchh8tf8w072tapgqoijrahojk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parents
    ADD CONSTRAINT fkchh8tf8w072tapgqoijrahojk FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: class_subjects fkck6avvuoer3mgm2mbjgs0gw6m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subjects
    ADD CONSTRAINT fkck6avvuoer3mgm2mbjgs0gw6m FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: class_subject_assessment_life_cycle fkcwfx3w3d4bdoomnjbubbbrx55; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subject_assessment_life_cycle
    ADD CONSTRAINT fkcwfx3w3d4bdoomnjbubbbrx55 FOREIGN KEY (teacher_subject_id) REFERENCES public.teacher_subjects(id);


--
-- Name: enrollment_subjcts fkd9ke39kofr3pgee8ga1m9s1sg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollment_subjcts
    ADD CONSTRAINT fkd9ke39kofr3pgee8ga1m9s1sg FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: lessons fkddyw0tfmvg743y30f1hoxmo40; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lessons
    ADD CONSTRAINT fkddyw0tfmvg743y30f1hoxmo40 FOREIGN KEY (week_id) REFERENCES public.weeks(id);


--
-- Name: behaviours fkdnf1b2fu5xgkotwkns0tooyql; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behaviours
    ADD CONSTRAINT fkdnf1b2fu5xgkotwkns0tooyql FOREIGN KEY (behaviour_category_id) REFERENCES public.behaviour_categories(id);


--
-- Name: houses_house_masters fkdt6g9wdbcfasb5reudcidwfyb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.houses_house_masters
    ADD CONSTRAINT fkdt6g9wdbcfasb5reudcidwfyb FOREIGN KEY (house_entity_id) REFERENCES public.houses(id);


--
-- Name: teacher_subjects fkdweqkwxroox2u7pbmksehx04i; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_subjects
    ADD CONSTRAINT fkdweqkwxroox2u7pbmksehx04i FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: student_parents fkeamoo6ghyb022owmr29yqha3v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_parents
    ADD CONSTRAINT fkeamoo6ghyb022owmr29yqha3v FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: stream_subjects fkerpv1ch1wm02obb18u3m5asn8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stream_subjects
    ADD CONSTRAINT fkerpv1ch1wm02obb18u3m5asn8 FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: class_streams fkf03xc0u76j6x051o0km11gnuf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_streams
    ADD CONSTRAINT fkf03xc0u76j6x051o0km11gnuf FOREIGN KEY (class_id) REFERENCES public.classes(id);


--
-- Name: assessment_approval_request fkfhnvywwsb9s6m0fhmpa0ophfh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_approval_request
    ADD CONSTRAINT fkfhnvywwsb9s6m0fhmpa0ophfh FOREIGN KEY (class_master_id) REFERENCES public.class_masters(id);


--
-- Name: fee_structures fkfn1cve75y7cyo38xyrcgx1cty; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures
    ADD CONSTRAINT fkfn1cve75y7cyo38xyrcgx1cty FOREIGN KEY (term_id) REFERENCES public.terms(id);


--
-- Name: attendance fkfpxtsy79idkv1ot8h4w34r624; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT fkfpxtsy79idkv1ot8h4w34r624 FOREIGN KEY (enrollment_id) REFERENCES public.enrollments(id);


--
-- Name: expenses fkg7aulw52en8nct0mjq8uut03q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT fkg7aulw52en8nct0mjq8uut03q FOREIGN KEY (category_id) REFERENCES public.expense_categories(id);


--
-- Name: fee_structures fkgvda8q1mibfvp8wfptqk0dfiu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures
    ADD CONSTRAINT fkgvda8q1mibfvp8wfptqk0dfiu FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id);


--
-- Name: assessments fkgw5hicsdjn6g6m8qe83w5e4um; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessments
    ADD CONSTRAINT fkgw5hicsdjn6g6m8qe83w5e4um FOREIGN KEY (template_id) REFERENCES public.assessment_templates(id);


--
-- Name: behaviours fkgyqmdls74ak2nexi80r62v9l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behaviours
    ADD CONSTRAINT fkgyqmdls74ak2nexi80r62v9l FOREIGN KEY (enrollment_id) REFERENCES public.enrollments(id);


--
-- Name: enrollments fkh00337cnw2p14fuan7x3rwye6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fkh00337cnw2p14fuan7x3rwye6 FOREIGN KEY (section_id) REFERENCES public.sections(id);


--
-- Name: student_parents fkh8yi0uy8nbnfqhtsnw3kv158v; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_parents
    ADD CONSTRAINT fkh8yi0uy8nbnfqhtsnw3kv158v FOREIGN KEY (parent_id) REFERENCES public.parents(id);


--
-- Name: fee_structures fkhblxcemkiga5nmih2kmiajpry; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures
    ADD CONSTRAINT fkhblxcemkiga5nmih2kmiajpry FOREIGN KEY (category_id) REFERENCES public.fee_categories(id);


--
-- Name: class_sections fkhqd3j575i4qwi1rw8rqwk6ava; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_sections
    ADD CONSTRAINT fkhqd3j575i4qwi1rw8rqwk6ava FOREIGN KEY (section_id) REFERENCES public.sections(id);


--
-- Name: student_assessments fkhta1bb0bcppxnshp4jq1iv04j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_assessments
    ADD CONSTRAINT fkhta1bb0bcppxnshp4jq1iv04j FOREIGN KEY (term_id) REFERENCES public.terms(id);


--
-- Name: class_subjects fkhvs5q46sgcd36rcgv9w10xrf6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subjects
    ADD CONSTRAINT fkhvs5q46sgcd36rcgv9w10xrf6 FOREIGN KEY (subject_group_id) REFERENCES public.subject_groups(id);


--
-- Name: fee_discounts fki5psf7fyf5vkukby3ekkfmhqh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_discounts
    ADD CONSTRAINT fki5psf7fyf5vkukby3ekkfmhqh FOREIGN KEY (enrollment_id) REFERENCES public.enrollments(id);


--
-- Name: houses_house_masters fkiglocndxmsvnj8y0tct78fwoi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.houses_house_masters
    ADD CONSTRAINT fkiglocndxmsvnj8y0tct78fwoi FOREIGN KEY (house_masters_id) REFERENCES public.teachers(id);


--
-- Name: fee_structures fkjrwf0maa5hj4sh4hcfoedm0au; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures
    ADD CONSTRAINT fkjrwf0maa5hj4sh4hcfoedm0au FOREIGN KEY (material_id) REFERENCES public.materials(id);


--
-- Name: audit_logs fkjs4iimve3y0xssbtve5ysyef0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT fkjs4iimve3y0xssbtve5ysyef0 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: subject_groups fkk0hjcs93b9wy8st1udr1okp0g; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subject_groups
    ADD CONSTRAINT fkk0hjcs93b9wy8st1udr1okp0g FOREIGN KEY (stream_id) REFERENCES public.streams(id);


--
-- Name: students fkkc4i4nibw3h6jlvuf7crv1wiq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT fkkc4i4nibw3h6jlvuf7crv1wiq FOREIGN KEY (house_id) REFERENCES public.houses(id);


--
-- Name: class_masters fkkdd4xudgowph1y858x6ma81ga; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_masters
    ADD CONSTRAINT fkkdd4xudgowph1y858x6ma81ga FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);


--
-- Name: class_masters fkkjefs43tat54f6l652chqpumd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_masters
    ADD CONSTRAINT fkkjefs43tat54f6l652chqpumd FOREIGN KEY (class_session_id) REFERENCES public.class_sessions(id);


--
-- Name: assessment_approval_request fkl0li68h4pfds67f12l86u8l3p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_approval_request
    ADD CONSTRAINT fkl0li68h4pfds67f12l86u8l3p FOREIGN KEY (term_id) REFERENCES public.terms(id);


--
-- Name: class_sessions fkl3sdqueonkcnteb8w4lywqxxs; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_sessions
    ADD CONSTRAINT fkl3sdqueonkcnteb8w4lywqxxs FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id);


--
-- Name: teacher_subjects fkl3w3j8go9afmwi3o7e2p21gs2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_subjects
    ADD CONSTRAINT fkl3w3j8go9afmwi3o7e2p21gs2 FOREIGN KEY (class_session_id) REFERENCES public.class_sessions(id);


--
-- Name: assessment_scores fkl4a32aatfnldg7g84hqa4cqt2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_scores
    ADD CONSTRAINT fkl4a32aatfnldg7g84hqa4cqt2 FOREIGN KEY (class_subject_assessment_life_cycle_id) REFERENCES public.class_subject_assessment_life_cycle(id);


--
-- Name: timetables fkl4im8hrk5tq4fqd964us3se9e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timetables
    ADD CONSTRAINT fkl4im8hrk5tq4fqd964us3se9e FOREIGN KEY (teacher_subject_id) REFERENCES public.teacher_subjects(id);


--
-- Name: material_transactions fklhfagx28b99u5d3wmlso7bbu1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.material_transactions
    ADD CONSTRAINT fklhfagx28b99u5d3wmlso7bbu1 FOREIGN KEY (material_id) REFERENCES public.materials(id);


--
-- Name: enrollments fkloh1q3o1ua6yuw9mwqdvhc54u; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fkloh1q3o1ua6yuw9mwqdvhc54u FOREIGN KEY (class_id) REFERENCES public.classes(id);


--
-- Name: class_sessions fkmga22byuuk6i095v4p8xdd8hu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_sessions
    ADD CONSTRAINT fkmga22byuuk6i095v4p8xdd8hu FOREIGN KEY (stream_id) REFERENCES public.streams(id);


--
-- Name: subject_groups fkmh4m1p1dxqurthwgrx7xjwd0c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subject_groups
    ADD CONSTRAINT fkmh4m1p1dxqurthwgrx7xjwd0c FOREIGN KEY (class_id) REFERENCES public.classes(id);


--
-- Name: materials fkmknh0urg319ho0gysamyymqu1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.materials
    ADD CONSTRAINT fkmknh0urg319ho0gysamyymqu1 FOREIGN KEY (category_id) REFERENCES public.material_categories(id);


--
-- Name: student_assessments fknmuk4nwq3mk6k1r1gnpg8scoc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_assessments
    ADD CONSTRAINT fknmuk4nwq3mk6k1r1gnpg8scoc FOREIGN KEY (teacher_subject_id) REFERENCES public.teacher_subjects(id);


--
-- Name: weeks fknsvahsplu0jxyd4aa7qg4lbho; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.weeks
    ADD CONSTRAINT fknsvahsplu0jxyd4aa7qg4lbho FOREIGN KEY (scheme_id) REFERENCES public.scheme_of_works(id);


--
-- Name: enrollment_subjcts fko08f2cp1g15dmq8w6y08jgkc2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollment_subjcts
    ADD CONSTRAINT fko08f2cp1g15dmq8w6y08jgkc2 FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: working_days fko0ewaar5ci2julfw3osr34sfk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.working_days
    ADD CONSTRAINT fko0ewaar5ci2julfw3osr34sfk FOREIGN KEY (timing_id) REFERENCES public.timings(id);


--
-- Name: assessment_scores fkoqg9f90mp4yy51u91b2ppotcg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assessment_scores
    ADD CONSTRAINT fkoqg9f90mp4yy51u91b2ppotcg FOREIGN KEY (student_assessment_id) REFERENCES public.student_assessments(id);


--
-- Name: classes fkowuioo7hfq5fjogu76uqdinjh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT fkowuioo7hfq5fjogu76uqdinjh FOREIGN KEY (next_class) REFERENCES public.classes(id);


--
-- Name: class_subject_assessment_life_cycle fkp6eceartnlo03aoj1v5t9obni; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subject_assessment_life_cycle
    ADD CONSTRAINT fkp6eceartnlo03aoj1v5t9obni FOREIGN KEY (assessment_id) REFERENCES public.assessments(id);


--
-- Name: enrollment_subjcts fkq9qed7wqa4wn0aekwrhppieb5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollment_subjcts
    ADD CONSTRAINT fkq9qed7wqa4wn0aekwrhppieb5 FOREIGN KEY (enrollment_id) REFERENCES public.enrollments(id);


--
-- Name: scheme_of_works fkqi410ve2pam753b8n2wlk6t0k; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheme_of_works
    ADD CONSTRAINT fkqi410ve2pam753b8n2wlk6t0k FOREIGN KEY (session_id) REFERENCES public.class_sessions(id);


--
-- Name: class_subject_assessment_life_cycle fkqiwuhnf3tcuonrdctpd1mcnvg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subject_assessment_life_cycle
    ADD CONSTRAINT fkqiwuhnf3tcuonrdctpd1mcnvg FOREIGN KEY (term_id) REFERENCES public.terms(id);


--
-- Name: class_subjects fkqj4bwb8mpht4mqjlx321qt7i0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.class_subjects
    ADD CONSTRAINT fkqj4bwb8mpht4mqjlx321qt7i0 FOREIGN KEY (class_id) REFERENCES public.classes(id);


--
-- Name: enrollments fkr4akji031thjyvok61sjcoja6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fkr4akji031thjyvok61sjcoja6 FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id);


--
-- Name: transactions fkro22w2xvcb3enq2xjbf4ot0p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT fkro22w2xvcb3enq2xjbf4ot0p FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id);


--
-- Name: audit_logs fkrwxxbw9vojq84geyfmg78sn1u; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT fkrwxxbw9vojq84geyfmg78sn1u FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id);


--
-- Name: timetables fkskk5bd8w1imseo6ardau7fblx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.timetables
    ADD CONSTRAINT fkskk5bd8w1imseo6ardau7fblx FOREIGN KEY (room_id) REFERENCES public.rooms(id);


--
-- Name: classes fktnoplshgrmo7lk0o8yes7686n; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.classes
    ADD CONSTRAINT fktnoplshgrmo7lk0o8yes7686n FOREIGN KEY (assessment_template_id) REFERENCES public.assessment_templates(id);


--
-- PostgreSQL database dump complete
--
