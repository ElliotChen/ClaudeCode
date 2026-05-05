CREATE TABLE employees (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(50),
    rank          VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    department_id UUID REFERENCES departments(id),
    hire_date     DATE NOT NULL
);

CREATE INDEX idx_employees_department_id ON employees(department_id);
CREATE INDEX idx_employees_status ON employees(status);
