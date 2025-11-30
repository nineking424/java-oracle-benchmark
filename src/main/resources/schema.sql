-- Oracle DDL for Test Record Table
-- 벤치마크 테스트용 테이블 및 시퀀스 정의

-- Drop existing objects (optional - for clean recreation)
-- DROP SEQUENCE test_record_seq;
-- DROP TABLE test_record;

-- Create Sequence
CREATE SEQUENCE test_record_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Create Table
CREATE TABLE test_record (
    id NUMBER(19) PRIMARY KEY,
    data1 VARCHAR2(100) NOT NULL,
    data2 VARCHAR2(200),
    amount NUMBER(18,2),
    status VARCHAR2(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL
);

-- Create Index for Performance (optional)
CREATE INDEX idx_test_record_status ON test_record(status);
CREATE INDEX idx_test_record_created_at ON test_record(created_at);

-- Comments
COMMENT ON TABLE test_record IS '벤치마크 테스트용 테이블';
COMMENT ON COLUMN test_record.id IS '레코드 식별자';
COMMENT ON COLUMN test_record.data1 IS '테스트 문자열 1 (필수)';
COMMENT ON COLUMN test_record.data2 IS '테스트 문자열 2 (선택)';
COMMENT ON COLUMN test_record.amount IS '금액 필드';
COMMENT ON COLUMN test_record.status IS '상태 코드 (기본값: ACTIVE)';
COMMENT ON COLUMN test_record.created_at IS '생성 시각 (UTC)';
