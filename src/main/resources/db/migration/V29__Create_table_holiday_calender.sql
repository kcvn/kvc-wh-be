CREATE TABLE holidays_calendar (
    id VARCHAR(50) PRIMARY KEY DEFAULT gen_random_uuid(),
    day_off TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
