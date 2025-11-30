-- H2 DDL for Test Record Table (Oracle Compatibility Mode)
-- 테스트 환경용 스키마

-- Drop existing objects
DROP TABLE IF EXISTS test_record;
DROP SEQUENCE IF EXISTS test_record_seq;

-- Create Sequence
CREATE SEQUENCE test_record_seq START WITH 1 INCREMENT BY 1;

-- Create Table
CREATE TABLE test_record (
    id BIGINT PRIMARY KEY,
    data1 VARCHAR(100) NOT NULL,
    data2 VARCHAR(200),
    amount DECIMAL(18,2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL
);

-- Create Index for Performance
CREATE INDEX idx_test_record_status ON test_record(status);
CREATE INDEX idx_test_record_created_at ON test_record(created_at);
