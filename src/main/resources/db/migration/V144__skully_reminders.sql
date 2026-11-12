-- Skully (the parents' WhatsApp assistant) reminders: which kinds each school sends, what was sent to whom (so a
-- reminder goes out once), and the parents who replied STOP.
CREATE TABLE skully_settings (
    school_id character varying(255) NOT NULL,
    exam_reminders boolean NOT NULL DEFAULT true,
    event_reminders boolean NOT NULL DEFAULT true,
    fee_reminders boolean NOT NULL DEFAULT true,
    updated_at timestamp(6) with time zone,
    CONSTRAINT skully_settings_pkey PRIMARY KEY (school_id)
);

-- One row per reminder item; the items for one parent on one day share a message_id (one WhatsApp message).
-- status: SENDING (handed to Skully), SENT (MSG91 accepted it). Failed sends are deleted, so they are tried again.
CREATE TABLE skully_reminders (
    id character varying(255) NOT NULL,
    message_id character varying(64) NOT NULL,
    school_id character varying(255) NOT NULL,
    parent_id character varying(255) NOT NULL,
    phone character varying(32) NOT NULL,
    kind character varying(16) NOT NULL,
    item_key character varying(255) NOT NULL,
    status character varying(16) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT skully_reminders_pkey PRIMARY KEY (id),
    CONSTRAINT uq_skully_reminders_parent_item UNIQUE (parent_id, item_key)
);
CREATE INDEX idx_skully_reminders_message ON skully_reminders (message_id);
CREATE INDEX idx_skully_reminders_school_time ON skully_reminders (school_id, created_at);
CREATE INDEX idx_skully_reminders_parent_time ON skully_reminders (parent_id, created_at);

-- WhatsApp numbers (digits, with country code) that replied STOP.
CREATE TABLE skully_opt_outs (
    phone character varying(32) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT skully_opt_outs_pkey PRIMARY KEY (phone)
);
