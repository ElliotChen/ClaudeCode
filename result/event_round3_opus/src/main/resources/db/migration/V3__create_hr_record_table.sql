CREATE TABLE hr_records (
    id          UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    detail      VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_hr_records_employee_id ON hr_records(employee_id);
